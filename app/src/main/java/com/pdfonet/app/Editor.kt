package com.pdfonet.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.io.File

class EditorState(ctx: Context) {
    private val draft = File(ctx.filesDir, "draft.txt")
    var tfv by mutableStateOf(
        TextFieldValue(try { if (draft.exists()) draft.readText() else "" } catch (e: Exception) { "" })
    )
    private val undoS = ArrayDeque<TextFieldValue>()
    private val redoS = ArrayDeque<TextFieldValue>()
    private var last = 0L

    private fun push(v: TextFieldValue) {
        if (v.text.length > 200_000) return
        undoS.addLast(v)
        if (undoS.size > 60) undoS.removeFirst()
    }

    fun onChange(n: TextFieldValue) {
        if (n.text != tfv.text) {
            val now = System.currentTimeMillis()
            if (now - last > 800 || undoS.isEmpty()) push(tfv)
            last = now
            redoS.clear()
        }
        tfv = n
    }

    fun apply(n: TextFieldValue) {
        push(tfv)
        redoS.clear()
        last = 0L
        tfv = n
    }

    fun load(t: String) = apply(TextFieldValue(t))

    fun undo() {
        if (undoS.isNotEmpty()) {
            redoS.addLast(tfv)
            tfv = undoS.removeLast()
        }
    }

    fun redo() {
        if (redoS.isNotEmpty()) {
            undoS.addLast(tfv)
            tfv = redoS.removeLast()
        }
    }

    fun saveDraft() {
        try { draft.writeText(tfv.text) } catch (e: Exception) { }
    }
}

fun wrapSel(v: TextFieldValue, l: String, r: String): TextFieldValue {
    val s = v.selection.min
    val e = v.selection.max
    val t = v.text
    return TextFieldValue(
        t.substring(0, s) + l + t.substring(s, e) + r + t.substring(e),
        TextRange(s + l.length, e + l.length)
    )
}

fun prefixLine(v: TextFieldValue, prefix: String): TextFieldValue {
    val t = v.text
    val s = v.selection.min.coerceIn(0, t.length)
    val ls = if (s == 0) 0 else t.lastIndexOf('\n', s - 1) + 1
    var le = t.indexOf('\n', ls)
    if (le < 0) le = t.length
    val line = t.substring(ls, le)
    val old = if (prefix.startsWith("#")) (Regex("^#{1,3}\\s*").find(line)?.value?.length ?: 0) else 0
    val nt = t.substring(0, ls) + prefix + line.substring(old) + t.substring(le)
    val d = prefix.length - old
    return TextFieldValue(
        nt,
        TextRange((v.selection.start + d).coerceAtLeast(ls), (v.selection.end + d).coerceAtLeast(ls))
    )
}

fun insertBlock(v: TextFieldValue, block: String): TextFieldValue {
    val t = v.text
    val e = v.selection.max.coerceIn(0, t.length)
    val ins = (if (e > 0 && t[e - 1] != '\n') "\n" else "") + block + "\n"
    return TextFieldValue(t.substring(0, e) + ins + t.substring(e), TextRange(e + ins.length))
}
