package com.pdfonet.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryItem(
    val id: Long,
    val name: String,
    val uri: String,
    val pages: Int,
    val size: Long,
    val time: Long,
    val fav: Boolean = false
)

object Store {
    private fun file(c: Context) = File(c.filesDir, "history.json")

    fun load(c: Context): List<HistoryItem> = try {
        val a = JSONArray(file(c).readText())
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            HistoryItem(
                o.getLong("id"), o.getString("name"), o.getString("uri"),
                o.getInt("pages"), o.getLong("size"), o.getLong("time"), o.optBoolean("fav")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(c: Context, list: List<HistoryItem>) {
        val a = JSONArray()
        list.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id).put("name", it.name).put("uri", it.uri)
                    .put("pages", it.pages).put("size", it.size)
                    .put("time", it.time).put("fav", it.fav)
            )
        }
        file(c).writeText(a.toString())
    }
}

data class Tpl(val id: Long, val name: String, val body: String)

object Templates {
    private fun file(c: Context) = File(c.filesDir, "templates.json")

    fun load(c: Context): List<Tpl> = try {
        val a = JSONArray(file(c).readText())
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Tpl(o.getLong("id"), o.getString("name"), o.getString("body"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(c: Context, list: List<Tpl>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("body", it.body)) }
        file(c).writeText(a.toString())
    }

    val builtIn = listOf(
        Tpl(-1, "Resume", """
# Your Name
Phone | Email | City

## Summary
Write two or three lines about yourself.

## Experience
**Job Title**, Company (2022 - Present)
- Achievement one
- Achievement two

## Education
**Degree**, Institute (Year)

## Skills
- Skill one
- Skill two
""".trimIndent()),
        Tpl(-2, "Letter", """
Date: 

To,
Recipient Name
Address

Subject: Your subject here

Dear Sir/Madam,

Write the body of your letter here.

Yours sincerely,
Your Name
""".trimIndent()),
        Tpl(-3, "Notes", """
# Notes Title

## Key points
- Point one
- Point two

## To do
1. First task
2. Second task

> Important: write a highlighted remark here.
""".trimIndent()),
        Tpl(-4, "Report", """
# Report Title

## Summary
Short summary of the report.

## Details
Write the main content here. Use **bold** and *italic* for emphasis.

---

## Conclusion
Final thoughts and next steps.
""".trimIndent())
    )
}
