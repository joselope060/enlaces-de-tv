package com.example.mislinks

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Guarda la lista y los ajustes en SharedPreferences (solo en este dispositivo). */
class LinkStore(context: Context) {

    private val prefs = context.getSharedPreferences("mislinks", Context.MODE_PRIVATE)

    var template: String
        get() = prefs.getString(KEY_TPL, DEFAULT_TEMPLATE) ?: DEFAULT_TEMPLATE
        set(value) {
            prefs.edit().putString(KEY_TPL, value).apply()
        }

    fun load(): MutableList<Link> {
        val out = mutableListOf<Link>()
        try {
            val arr = JSONArray(prefs.getString(KEY_LINKS, "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Link(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        url = o.getString("url"),
                        group = o.optString("group", "")
                    )
                )
            }
        } catch (e: Exception) {
            // Datos dañados: se empieza con la lista vacía.
        }
        return out
    }

    fun save(list: List<Link>) {
        val arr = JSONArray()
        for (l in list) {
            arr.put(
                JSONObject()
                    .put("id", l.id)
                    .put("name", l.name)
                    .put("url", l.url)
                    .put("group", l.group)
            )
        }
        prefs.edit().putString(KEY_LINKS, arr.toString()).apply()
    }

    fun loadHistory(): MutableList<HistoryEntry> {
        val out = mutableListOf<HistoryEntry>()
        try {
            val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(HistoryEntry(o.getString("url"), o.getString("name"), o.getLong("time")))
            }
        } catch (e: Exception) {
            // Datos dañados: se empieza con el historial vacío.
        }
        return out
    }

    fun saveHistory(list: List<HistoryEntry>) {
        val arr = JSONArray()
        for (h in list) {
            arr.put(
                JSONObject()
                    .put("url", h.url)
                    .put("name", h.name)
                    .put("time", h.time)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        const val DEFAULT_TEMPLATE = "http://127.0.0.1:6878/ace/getstream?id={id}"
        const val MAX_HISTORY = 20
        private const val KEY_LINKS = "links"
        private const val KEY_HISTORY = "history"
        private const val KEY_TPL = "template"
    }
}
