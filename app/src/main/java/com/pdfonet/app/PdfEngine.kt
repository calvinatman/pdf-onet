package com.pdfonet.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfResult(val name: String, val uri: Uri, val pages: Int, val size: Long)

sealed class Block {
    class Para(
        val text: CharSequence,
        val size: Float,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val mono: Boolean = false,
        val indent: Float = 0f,
        val before: Float = 0f,
        val after: Float = 0f,
        val bg: Int = 0,
        val bar: Boolean = false,
        val hang: Int = 0,
        val level: Int = 0,
        val chapter: Boolean = false,
        val plain: String = ""
    ) : Block()

    object Rule : Block()
    object Break : Block()
    class Img(val path: String) : Block()
}

class DrawItem(
    val type: Int, val layout: StaticLayout?, val start: Int, val end: Int,
    val x: Float, val y: Float, val w: Float, val h: Float,
    val bmp: Bitmap?, val bg: Int, val bar: Boolean
)

class PageData(var chapter: Int = 0, val cover: Boolean = false) {
    val items = ArrayList<DrawItem>()
    var cTitle = ""
    var cAuthor = ""
}

object PdfEngine {
    val sizes = linkedMapOf(
        "A4" to (595 to 842), "A5" to (420 to 595), "A3" to (842 to 1191),
        "Letter" to (612 to 792), "Legal" to (612 to 1008)
    )

    fun create(ctx: Context, text: String, markdown: Boolean, st: PdfStyle, tree: String?, base: String): List<PdfResult> {
        require(text.isNotBlank()) { "Text is empty" }
        val fs = st.fontSize.toFloat()
        val (bw, bh) = sizes[st.pageSize] ?: sizes.getValue("A4")
        val pw = if (st.landscape) bh else bw
        val ph = if (st.landscape) bw else bh
        val m = st.marginPt()
        val hh = if (st.header.isNotBlank()) 18 else 0
        val fh = if (st.footer.isNotBlank() || st.pageNum != "None") 18 else 0
        val tf = Fonts.typeface(ctx, st.font)
        val chapterSplit = markdown && st.splitMode == "By heading"

        var blocks: List<Block> =
            if (markdown) toBlocks(ctx, Md.parse(text), fs, chapterSplit)
            else listOf(Block.Para(text, fs))

        val cover = if (st.coverTitle.isNotBlank()) PageData(cover = true).apply {
            cTitle = st.coverTitle
            cAuthor = st.coverAuthor
        } else null

        if (markdown && st.toc) {
            val heads = blocks.filterIsInstance<Block.Para>().filter { it.level > 0 }
            if (heads.isNotEmpty()) {
                val p1 = paginate(tocBlocks(heads, fs, null) + Block.Break + blocks, st, tf, pw, ph, m, hh, fh, cover)
                blocks = tocBlocks(heads, fs, p1.second) + Block.Break + blocks
            }
        }

        val pages = paginate(blocks, st, tf, pw, ph, m, hh, fh, cover).first
        val groups: List<List<PageData>> = when {
            st.splitMode == "Every N pages" -> pages.chunked(st.splitN.coerceAtLeast(1))
            chapterSplit -> pages.groupBy { it.chapter }.values.toList()
            else -> listOf(pages)
        }
        return groups.mapIndexed { i, g ->
            val name = if (groups.size == 1) "$base.pdf"
            else base + "-part" + String.format(Locale.US, "%02d", i + 1) + ".pdf"
            val tmp = render(ctx, g, st, pw, ph, m, hh, fh, tf, name)
            save(ctx, tmp, name, tree, g.size)
        }
    }

    fun fromImages(ctx: Context, uris: List<Uri>, st: PdfStyle, tree: String?, base: String): List<PdfResult> {
        require(uris.isNotEmpty()) { "No images" }
        val (bw, bh) = sizes[st.pageSize] ?: sizes.getValue("A4")
        val m = st.marginPt()
        val doc = PdfDocument()
        var n = 0
        for (u in uris) {
            val bmp = decode(ctx, u, 2000) ?: continue
            val land = bmp.width > bmp.height
            val pw = if (land) bh else bw
            val ph = if (land) bw else bh
            n++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, n).create())
            val c = page.canvas
            c.drawColor(android.graphics.Color.WHITE)
            val sc = minOf((pw - 2f * m) / bmp.width, (ph - 2f * m) / bmp.height)
            val w = bmp.width * sc
            val h = bmp.height * sc
            val l = (pw - w) / 2f
            val t = (ph - h) / 2f
            c.drawBitmap(bmp, null, RectF(l, t, l + w, t + h), Paint(Paint.FILTER_BITMAP_FLAG))
            doc.finishPage(page)
            bmp.recycle()
        }
        require(n > 0) { "Could not read images" }
        val name = "$base.pdf"
        val tmp = File(ctx.cacheDir, name)
        FileOutputStream(tmp).use { doc.writeTo(it) }
        doc.close()
        return listOf(save(ctx, tmp, name, tree, n))
    }

    fun decode(ctx: Context, uri: Uri, max: Int): Bitmap? {
        return try {
            val r = ctx.contentResolver
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            r.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o) }
            var s = 1
            while (o.outWidth / s > max || o.outHeight / s > max) s *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = s }
            var bmp: Bitmap? = r.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o2) }
            val deg = try {
                r.openInputStream(uri)!!.use {
                    when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                        6 -> 90f
                        3 -> 180f
                        8 -> 270f
                        else -> 0f
                    }
                }
            } catch (e: Exception) { 0f }
            if (bmp != null && deg != 0f) {
                val mx = Matrix().apply { postRotate(deg) }
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, mx, true)
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun save(ctx: Context, tmp: File, name: String, tree: String?, pages: Int): PdfResult {
        var uri: Uri? = null
        if (tree != null) {
            uri = try {
                val t = Uri.parse(tree)
                val parent = DocumentsContract.buildDocumentUriUsingTree(t, DocumentsContract.getTreeDocumentId(t))
                DocumentsContract.createDocument(ctx.contentResolver, parent, "application/pdf", name)
            } catch (e: Exception) {
                null
            }
        }
        val target: Uri = uri ?: run {
            val v = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDF ONET")
            }
            ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: error("Could not create file")
        }
        ctx.contentResolver.openOutputStream(target)!!.use { o -> tmp.inputStream().use { it.copyTo(o) } }
        val size = tmp.length()
        tmp.delete()
        return PdfResult(name, target, pages, size)
    }

    private fun spannable(prefix: String, segs: List<Seg>): SpannableStringBuilder {
        val sb = SpannableStringBuilder(prefix)
        for (g in segs) {
            val s = sb.length
            sb.append(g.t)
            val e = sb.length
            if (e == s) continue
            if (g.b) sb.setSpan(StyleSpan(Typeface.BOLD), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (g.i) sb.setSpan(StyleSpan(Typeface.ITALIC), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (g.c) {
                sb.setSpan(TypefaceSpan("monospace"), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(BackgroundColorSpan(0x22808080), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    private fun toBlocks(ctx: Context, mls: List<ML>, fs: Float, chapterSplit: Boolean): List<Block> {
        val out = ArrayList<Block>()
        for (ml in mls) {
            val gapB = if (ml.gap) fs * 0.6f else 0f
            when (ml.kind) {
                0 -> out.add(Block.Para(spannable("", ml.segs), fs, before = gapB, after = fs * 0.3f))
                1 -> {
                    val mult = when (ml.level) { 1 -> 1.9f; 2 -> 1.55f; else -> 1.25f }
                    out.add(
                        Block.Para(
                            spannable("", ml.segs), fs * mult, bold = true,
                            before = fs * 0.8f + gapB, after = fs * 0.35f, level = ml.level,
                            chapter = chapterSplit && ml.level == 1,
                            plain = ml.segs.joinToString("") { it.t }
                        )
                    )
                }
                2 -> out.add(
                    Block.Para(
                        spannable("\u2022  ", ml.segs), fs, indent = 12f + ml.indent * 5f,
                        before = gapB, after = fs * 0.2f, hang = (fs * 1.1f).toInt()
                    )
                )
                3 -> out.add(
                    Block.Para(
                        spannable(ml.prefix, ml.segs), fs, indent = 12f + ml.indent * 5f,
                        before = gapB, after = fs * 0.2f, hang = (fs * 1.5f).toInt()
                    )
                )
                4 -> out.add(
                    Block.Para(
                        spannable("", ml.segs), fs, italic = true, indent = 14f,
                        before = gapB, after = fs * 0.3f, bg = 0x14808080, bar = true
                    )
                )
                5 -> out.add(
                    Block.Para(ml.text, fs * 0.9f, mono = true, before = gapB, after = fs * 0.4f, bg = 0x1A808080)
                )
                6 -> out.add(Block.Rule)
                7 -> out.add(Block.Break)
                8 -> out.add(Block.Img(File(ctx.filesDir, "images/" + ml.text).path))
            }
        }
        return out
    }

    private fun tocBlocks(heads: List<Block.Para>, fs: Float, pn: List<Int>?): List<Block> {
        val out = ArrayList<Block>()
        out.add(Block.Para("Contents", fs * 1.6f, bold = true, after = fs * 0.6f))
        heads.forEachIndexed { i, h ->
            val p = pn?.getOrNull(i)?.toString() ?: "00"
            out.add(Block.Para(h.plain + "   \u00B7   " + p, fs, indent = (h.level - 1) * 14f, after = 3f))
        }
        return out
    }

    private fun layoutFor(b: Block.Para, st: PdfStyle, tf: Typeface, width: Int): StaticLayout {
        val style = (if (b.bold) 1 else 0) or (if (b.italic) 2 else 0)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Palette.text(st.textColor)
            textSize = b.size
            typeface = if (b.mono) Typeface.MONOSPACE else if (style == 0) tf else Typeface.create(tf, style)
        }
        val ss = SpannableStringBuilder(b.text)
        if (b.hang > 0 && ss.isNotEmpty()) {
            ss.setSpan(LeadingMarginSpan.Standard(0, b.hang), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val al = when (st.align) {
            "Center" -> Layout.Alignment.ALIGN_CENTER
            "Right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val justify = st.align == "Justify" && b.level == 0 && !b.mono && b.bg == 0
        return StaticLayout.Builder.obtain(ss, 0, ss.length, paint, width)
            .setLineSpacing(0f, st.lineSpacing.toFloatOrNull() ?: 1.2f)
            .setAlignment(al)
            .setIncludePad(false)
            .setJustificationMode(if (justify) Layout.JUSTIFICATION_MODE_INTER_WORD else Layout.JUSTIFICATION_MODE_NONE)
            .build()
    }

    private fun paginate(
        blocks: List<Block>, st: PdfStyle, tf: Typeface,
        pw: Int, ph: Int, m: Int, hh: Int, fh: Int, cover: PageData?
    ): Pair<List<PageData>, List<Int>> {
        val cw = pw - 2 * m
        val ch = (ph - 2 * m - hh - fh).toFloat()
        val pages = ArrayList<PageData>()
        if (cover != null) pages.add(cover)
        var cur = PageData()
        var y = 0f
        var chapter = 0
        val heads = ArrayList<Int>()
        fun flush() {
            pages.add(cur)
            cur = PageData(chapter)
            y = 0f
        }
        for (b in blocks) {
            when (b) {
                is Block.Para -> {
                    if (b.chapter) {
                        if (cur.items.isNotEmpty()) flush()
                        chapter++
                        cur.chapter = chapter
                    }
                    val w = (cw - b.indent.toInt()).coerceAtLeast(40)
                    val lay = layoutFor(b, st, tf, w)
                    if (b.level > 0 && y > 0f && ch - y < b.size * 3f) flush()
                    if (y > 0f) y += b.before
                    if (b.level > 0) heads.add(pages.size + 1)
                    var line = 0
                    while (line < lay.lineCount) {
                        val top = lay.getLineTop(line)
                        val avail = ch - y
                        var end = line
                        while (end < lay.lineCount && lay.getLineBottom(end) - top <= avail) end++
                        if (end == line) {
                            if (y > 0f) { flush(); continue } else end = line + 1
                        }
                        val hgt = (lay.getLineBottom(end - 1) - top).toFloat()
                        cur.items.add(DrawItem(0, lay, line, end, b.indent, y, w.toFloat(), hgt, null, b.bg, b.bar))
                        y += hgt
                        line = end
                        if (line < lay.lineCount) flush()
                    }
                    y += b.after
                }
                Block.Rule -> {
                    if (y > 0f && y + 14f > ch) flush()
                    cur.items.add(DrawItem(1, null, 0, 0, 0f, y + 6f, cw.toFloat(), 0f, null, 0, false))
                    y += 14f
                }
                Block.Break -> {
                    if (cur.items.isNotEmpty()) flush()
                }
                is Block.Img -> {
                    val bmp = BitmapFactory.decodeFile(b.path)
                    if (bmp != null) {
                        val sc = minOf(1f, cw / bmp.width.toFloat(), ch / bmp.height.toFloat())
                        val iw = bmp.width * sc
                        val ih = bmp.height * sc
                        if (y > 0f && y + ih > ch) flush()
                        cur.items.add(DrawItem(2, null, 0, 0, (cw - iw) / 2f, y, iw, ih, bmp, 0, false))
                        y += ih + 8f
                    }
                }
            }
        }
        if (cur.items.isNotEmpty() || pages.isEmpty()) pages.add(cur)
        return Pair(pages, heads)
    }

    private fun drawCover(c: android.graphics.Canvas, pd: PageData, pw: Int, ph: Int, m: Int, tc: Int, tf: Typeface) {
        val w = pw - 2 * m - 20
        fun lay(t: String, size: Float, bold: Boolean, y: Float): Float {
            if (t.isBlank()) return y
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tc
                textSize = size
                typeface = if (bold) Typeface.create(tf, Typeface.BOLD) else tf
            }
            val l = StaticLayout.Builder.obtain(t, 0, t.length, p, w)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            c.save()
            c.translate(m + 10f, y)
            l.draw(c)
            c.restore()
            return y + l.height + 14f
        }
        var y = ph * 0.30f
        y = lay(pd.cTitle, 34f, true, y)
        y = lay(pd.cAuthor, 16f, false, y + 6f)
        lay(SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date()), 12f, false, y)
    }

    private fun render(
        ctx: Context, pages: List<PageData>, st: PdfStyle,
        pw: Int, ph: Int, m: Int, hh: Int, fh: Int, tf: Typeface, name: String
    ): File {
        val doc = PdfDocument()
        val paper = Palette.paper(st.paperColor)
        val tc = Palette.text(st.textColor)
        val gray = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF888888.toInt()
            textSize = 9f
        }
        val wm = if (st.watermark.isNotBlank()) Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x40808080
            textSize = minOf(pw, ph) / 9f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        } else null
        val total = pages.size
        pages.forEachIndexed { idx, pd ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, idx + 1).create())
            val c = page.canvas
            c.drawColor(paper)
            if (wm != null) {
                c.save()
                c.rotate(-45f, pw / 2f, ph / 2f)
                c.drawText(st.watermark, pw / 2f, ph / 2f, wm)
                c.restore()
            }
            if (pd.cover) {
                drawCover(c, pd, pw, ph, m, tc, tf)
            } else {
                for (d in pd.items) {
                    val ox = m + d.x
                    val oy = m + hh + d.y
                    when (d.type) {
                        0 -> {
                            val lay = d.layout!!
                            val top = lay.getLineTop(d.start)
                            if (d.bg != 0) {
                                val p = Paint().apply { color = d.bg }
                                c.drawRect(ox - 4f, oy - 2f, ox + d.w + 4f, oy + d.h + 2f, p)
                            }
                            if (d.bar) {
                                val p = Paint().apply { color = 0xFF888888.toInt() }
                                c.drawRect(ox - 10f, oy, ox - 7f, oy + d.h, p)
                            }
                            c.save()
                            c.translate(ox, oy - top)
                            c.clipRect(0f, top.toFloat(), lay.width.toFloat(), lay.getLineBottom(d.end - 1).toFloat())
                            lay.draw(c)
                            c.restore()
                        }
                        1 -> {
                            val p = Paint().apply { color = 0xFF999999.toInt(); strokeWidth = 1f }
                            c.drawLine(ox, oy, ox + d.w, oy, p)
                        }
                        2 -> c.drawBitmap(d.bmp!!, null, RectF(ox, oy, ox + d.w, oy + d.h), null)
                    }
                }
            }
            if (hh > 0) {
                gray.textAlign = Paint.Align.CENTER
                c.drawText(st.header, pw / 2f, m + 8f, gray)
            }
            if (fh > 0) {
                if (st.footer.isNotBlank()) {
                    gray.textAlign = Paint.Align.LEFT
                    c.drawText(st.footer, m.toFloat(), ph - m - 4f, gray)
                }
                val n = idx + 1
                val label = if (pd.cover) null else when (st.pageNum) {
                    "1" -> "$n"
                    "Page 1" -> "Page $n"
                    "Page 1 of N" -> "Page $n of $total"
                    else -> null
                }
                if (label != null) {
                    gray.textAlign = Paint.Align.RIGHT
                    c.drawText(label, (pw - m).toFloat(), ph - m - 4f, gray)
                }
            }
            doc.finishPage(page)
        }
        val f = File(ctx.cacheDir, name)
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return f
    }
}
