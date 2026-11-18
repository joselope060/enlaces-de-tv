package com.example.mislinks

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Filas de la pantalla principal: cabeceras, enlaces, historial y notas. */
sealed class Row {
    data class Header(val text: String) : Row()
    data class Note(val text: String) : Row()
    data class Item(val link: Link) : Row()
    data class Hist(val entry: HistoryEntry) : Row()
}

class LinkAdapter(
    private val onOpen: (Link) -> Unit,
    private val onLong: (Link) -> Unit,
    private val onOpenHistory: (HistoryEntry) -> Unit,
    private val onLongHistory: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<Row> = emptyList()
    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    fun submit(list: List<Row>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.name)
        val meta: TextView = v.findViewById(R.id.meta)
        val url: TextView = v.findViewById(R.id.url)
    }

    class TextVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.text)
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> TYPE_HEADER
        is Row.Note -> TYPE_NOTE
        else -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> TextVH(inflater.inflate(R.layout.item_header, parent, false))
            TYPE_NOTE -> TextVH(inflater.inflate(R.layout.item_note, parent, false))
            else -> VH(inflater.inflate(R.layout.item_link, parent, false))
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as TextVH).text.text = row.text
            is Row.Note -> (holder as TextVH).text.text = row.text
            is Row.Item -> {
                val h = holder as VH
                val l = row.link
                val type = l.parsed?.type?.label ?: LinkType.WEB.label
                h.name.text = l.name
                h.meta.text = if (l.group.isBlank()) type else "$type · ${l.group}"
                h.url.text = l.url
                h.itemView.setOnClickListener { onOpen(l) }
                h.itemView.setOnLongClickListener {
                    onLong(l)
                    true
                }
                h.itemView.setOnKeyListener(menuKey { onLong(l) })
            }
            is Row.Hist -> {
                val h = holder as VH
                val e = row.entry
                h.name.text = e.name
                h.meta.text = "${LinkType.ACE.label} · ${dateFormat.format(Date(e.time))}"
                h.url.text = e.url
                h.itemView.setOnClickListener { onOpenHistory(e) }
                h.itemView.setOnLongClickListener {
                    onLongHistory(e)
                    true
                }
                h.itemView.setOnKeyListener(menuKey { onLongHistory(e) })
            }
        }
    }

    /** En el mando de TV, la tecla Menú sobre una fila abre sus opciones (equivale a la pulsación larga). */
    private fun menuKey(action: () -> Unit) = View.OnKeyListener { _, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
            action()
            true
        } else {
            false
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        const val TYPE_NOTE = 2
    }
}
