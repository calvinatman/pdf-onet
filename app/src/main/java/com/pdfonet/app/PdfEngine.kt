package com.pdfonet.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream

data class PdfOptions(
    val pageSize: String = "A4",
    val landscape: Boolean = false,
    val fontSize: Int = 12,
    val margin: Int = 40,
    val pageNumbers: Boolean = true
)

data class PdfResult(val name: String, val uri: Uri, val pages: Int, val size: Long)

object PdfEngine {
    val sizes = linkedMapOf(
        "A4" to (595 to 842),
        "A5" to (420 to 595),
        "A3" to (842 to 1191),
        "Letter" to (612 to 792),
        "Legal" to (612 to 1008)
    )

    fun create(ctx: Context, text: String, name: String, o: PdfOptions): PdfResult {
        require(text.isNotBlank()) { "Text is empty" }
        val (bw, bh) = sizes[o.pageSize] ?: sizes.getValue("A4")
        val pw = if (o.landscape) bh else bw
        val ph = if (o.landscape) bw else bh
        val m = o.margin
        val cw = pw - 2 * m
        val ch = ph - 2 * m - (if (o.pageNumbers) 14 else 0)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.BLACK
            textSize = o.fontSize.toFloat()
            typeface = Typeface.SANS_SERIF
        }
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF777777.toInt()
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, cw)
            .setLineSpacing(0f, 1.2f)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

        val doc = PdfDocument()
        val total = layout.lineCount
        var start = 0
        var pages = 0
        while (start < total) {
            val top = layout.getLineTop(start)
            var end = start
            while (end < total && layout.getLineBottom(end) - top <= ch) end++
            if (end == start) end = start + 1
            val bottom = layout.getLineBottom(end - 1)
            pages++
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, pages).create())
            val c = page.canvas
            c.save()
            c.translate(m.toFloat(), m.toFloat() - top)
            c.clipRect(0f, top.toFloat(), cw.toFloat(), bottom.toFloat())
            layout.draw(c)
            c.restore()
            if (o.pageNumbers) c.drawText(pages.toString(), pw / 2f, ph - m / 2f, numPaint)
            doc.finishPage(page)
            start = end
        }

        val tmp = File(ctx.cacheDir, name)
        FileOutputStream(tmp).use { doc.writeTo(it) }
        doc.close()

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDF ONET")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create file")
        ctx.contentResolver.openOutputStream(uri)!!.use { out ->
            tmp.inputStream().use { it.copyTo(out) }
        }
        val size = tmp.length()
        tmp.delete()
        return PdfResult(name, uri, pages, size)
    }
}
