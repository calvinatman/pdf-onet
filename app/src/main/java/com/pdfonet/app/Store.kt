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
