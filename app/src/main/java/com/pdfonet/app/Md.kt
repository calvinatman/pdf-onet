package com.pdfonet.app

data class Seg(val t: String, val b: Boolean, val i: Boolean, val c: Boolean)

// kind: 0 paragraph, 1 heading, 2 bullet, 3 numbered, 4 quote, 5 code block, 6 rule, 7 page break, 8 image
data class ML(
    val kind: Int,
    val segs: List<Seg> = emptyList(),
    val level: Int = 0,
    val indent: Int = 0,
    val gap: Boolean = false,
    val text: String = "",
    val prefix: String = ""
)

object Md {
    private val head = Regex("^(#{1,3})\\s+(.*)$")
    private val bullet = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val num = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
    private val rule = Regex("^(-{3,}|\\*{3,}|_{3,})$")

    fun segs(s: String): List<Seg> {
        val out = ArrayList<Seg>()
        val sb = StringBuilder()
        var b = false
        var i = false
        var c = false
        fun flush() {
            if (sb.isNotEmpty()) {
                out.add(Seg(sb.toString(), b, i, c))
                sb.setLength(0)
            }
        }
        var k = 0
        while (k < s.length) {
            val ch = s[k]
            when {
                ch == '`' -> { flush(); c = !c; k++ }
                !c && ch == '*' && k + 1 < s.length && s[k + 1] == '*' -> { flush(); b = !b; k += 2 }
                !c && ch == '*' -> { flush(); i = !i; k++ }
                else -> { sb.append(ch); k++ }
            }
        }
        flush()
        return out
    }

    fun parse(text: String): List<ML> {
        val out = ArrayList<ML>()
        val buf = StringBuilder()
        var code = false
        var gap = false
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            val t = line.trim()
            if (t.startsWith("```")) {
                if (code) {
                    out.add(ML(5, text = buf.toString().trimEnd('\n'), gap = gap))
                    buf.setLength(0)
                    code = false
                    gap = false
                } else {
                    code = true
                }
                continue
            }
            if (code) { buf.append(line).append('\n'); continue }
            if (t.isEmpty()) { gap = true; continue }
            val ml: ML = when {
                t == "[pagebreak]" -> ML(7)
                rule.matches(t) -> ML(6)
                t.startsWith("![") && t.contains("](img:") ->
                    ML(8, text = t.substringAfter("](img:").substringBefore(")"))
                head.matches(t) -> {
                    val m = head.find(t)!!
                    ML(1, segs(m.groupValues[2]), level = m.groupValues[1].length)
                }
                t.startsWith(">") -> ML(4, segs(t.removePrefix(">").trim()))
                bullet.matches(line) -> {
                    val m = bullet.find(line)!!
                    ML(2, segs(m.groupValues[2]), indent = m.groupValues[1].length)
                }
                num.matches(line) -> {
                    val m = num.find(line)!!
                    ML(3, segs(m.groupValues[3]), indent = m.groupValues[1].length, prefix = m.groupValues[2] + ". ")
                }
                else -> ML(0, segs(t))
            }
            out.add(ml.copy(gap = gap))
            gap = false
        }
        if (code && buf.isNotEmpty()) out.add(ML(5, text = buf.toString().trimEnd('\n'), gap = gap))
        return out
    }
}
