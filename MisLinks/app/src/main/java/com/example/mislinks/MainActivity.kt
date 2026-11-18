package com.example.mislinks

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.app.UiModeManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var store: LinkStore
    private lateinit var listAdapter: LinkAdapter
    private lateinit var emptyView: TextView
    private val links = mutableListOf<Link>()
    private val history = mutableListOf<HistoryEntry>()
    private var query = ""

    /** True cuando la app corre en una Android TV (modo de interfaz "televisión"). */
    private val isTv: Boolean by lazy {
        val ui = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        ui.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = LinkStore(this)
        links.addAll(store.load())
        history.addAll(store.loadHistory())

        emptyView = findViewById(R.id.empty)
        listAdapter = LinkAdapter(
            onOpen = ::openLink,
            onLong = ::showActions,
            onOpenHistory = ::openHistory,
            onLongHistory = ::showHistoryActions
        )

        val recycler = findViewById<RecyclerView>(R.id.list)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = listAdapter

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString().orEmpty()
                refresh()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Móvil: botón flotante + barra con menú. TV: botones "Añadir" y "Opciones" en la cabecera.
        val addButton = findViewById<View?>(R.id.fab) ?: findViewById<View>(R.id.btn_add)
        addButton.setOnClickListener { showEditor(null, "") }

        val toolbar = findViewById<MaterialToolbar?>(R.id.toolbar)
        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.main)
            toolbar.setOnMenuItemClickListener { handleMenu(it.itemId) }
        }
        findViewById<View?>(R.id.btn_menu)?.setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                menuInflater.inflate(R.menu.main, menu)
                setOnMenuItemClickListener { handleMenu(it.itemId) }
            }.show()
        }

        refresh()
        if (isTv) {
            // Que el mando empiece directamente sobre la lista (no sobre el buscador, que abriría el teclado).
            recycler.post { recycler.requestFocus() }
        }
        if (savedInstanceState == null) handleShared(intent)
    }

    // ---------- Lista: mis enlaces y, justo debajo, el historial ----------

    private fun refresh() {
        val q = query.trim().lowercase()
        val shownLinks = links
            .filter { q.isEmpty() || (it.name + " " + it.group).lowercase().contains(q) }
            .sortedBy { it.group.lowercase() }
        val shownHistory = history
            .filter { q.isEmpty() || it.name.lowercase().contains(q) }

        if (q.isNotEmpty() && shownLinks.isEmpty() && shownHistory.isEmpty()) {
            listAdapter.submit(emptyList())
            emptyView.setText(R.string.empty_search)
            emptyView.visibility = View.VISIBLE
            return
        }
        emptyView.visibility = View.GONE

        val rows = mutableListOf<Row>()
        rows.add(Row.Header(getString(R.string.section_links)))
        if (shownLinks.isEmpty()) {
            rows.add(Row.Note(getString(R.string.empty_none)))
        } else {
            shownLinks.forEach { rows.add(Row.Item(it)) }
        }
        rows.add(Row.Header(getString(R.string.section_history, history.size, LinkStore.MAX_HISTORY)))
        if (shownHistory.isEmpty()) {
            rows.add(Row.Note(getString(R.string.history_empty)))
        } else {
            shownHistory.forEach { rows.add(Row.Hist(it)) }
        }
        listAdapter.submit(rows)
    }

    private fun persist() {
        store.save(links)
        refresh()
    }

    private fun persistHistory() {
        store.saveHistory(history)
        refresh()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    // ---------- Historial ----------

    /** Guarda el enlace al principio del historial (sin duplicados, máximo 20). */
    private fun recordHistory(name: String, url: String) {
        history.removeAll { it.url == url }
        history.add(0, HistoryEntry(url = url, name = name, time = System.currentTimeMillis()))
        while (history.size > LinkStore.MAX_HISTORY) {
            history.removeAt(history.size - 1)
        }
        persistHistory()
    }

    private fun openHistory(e: HistoryEntry) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(e.url)))
            recordHistory(e.name, e.url)
        } catch (ex: ActivityNotFoundException) {
            toast(getString(R.string.no_app))
        }
    }

    private fun showHistoryActions(e: HistoryEntry) {
        val aceId = Parsed.from(e.url)?.aceId
        val labels = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (aceId != null) {
            labels.add(getString(R.string.act_player))
            actions.add { playWithEngine(e.name, aceId) }
            labels.add(getString(R.string.act_copy_engine))
            actions.add { copy(store.template.replace("{id}", aceId)) }
        }
        if (links.none { it.url == e.url }) {
            labels.add(getString(R.string.act_save_link))
            actions.add {
                links.add(Link(name = e.name, url = e.url))
                persist()
                toast(getString(R.string.saved))
            }
        }
        labels.add(getString(R.string.act_remove_history))
        actions.add {
            history.remove(e)
            persistHistory()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(e.name)
            .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
            .show()
    }

    private fun confirmClearHistory() {
        if (history.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_message)
            .setPositiveButton(R.string.clear_history_yes) { _, _ ->
                history.clear()
                persistHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------- Abrir y copiar ----------

    private fun openLink(l: Link) {
        val p = l.parsed
        val isAce = p != null && p.type == LinkType.ACE && p.aceId != null
        val target = if (isAce) "acestream://${p!!.aceId}" else l.url
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
            if (isAce) recordHistory(l.name, target)
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_app))
        }
    }

    private fun playWithEngine(name: String, aceId: String) {
        val url = store.template.replace("{id}", aceId)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/*")
        try {
            startActivity(Intent.createChooser(intent, null))
            recordHistory(name, "acestream://$aceId")
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_player))
        }
    }

    private fun copy(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("enlace", text))
        toast(getString(R.string.copied))
    }

    private fun showActions(l: Link) {
        val p = l.parsed
        val aceId = if (p != null && p.type == LinkType.ACE) p.aceId else null
        val labels = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (aceId != null) {
            labels.add(getString(R.string.act_player))
            actions.add { playWithEngine(l.name, aceId) }
            labels.add(getString(R.string.act_copy_engine))
            actions.add { copy(store.template.replace("{id}", aceId)) }
        }
        labels.add(getString(R.string.act_copy_link))
        actions.add { copy(l.url) }
        labels.add(getString(R.string.act_edit))
        actions.add { showEditor(l, "") }
        labels.add(getString(R.string.act_delete))
        actions.add {
            links.remove(l)
            persist()
            toast(getString(R.string.deleted))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(l.name)
            .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
            .show()
    }

    // ---------- Añadir y editar ----------

    private fun showEditor(existing: Link?, prefill: String) {
        val view = layoutInflater.inflate(R.layout.dialog_edit, null)
        val name = view.findViewById<EditText>(R.id.f_name)
        val url = view.findViewById<EditText>(R.id.f_url)
        val group = view.findViewById<EditText>(R.id.f_group)
        if (existing != null) {
            name.setText(existing.name)
            url.setText(existing.url)
            group.setText(existing.group)
        } else {
            url.setText(prefill)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_title else R.string.edit_title)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val p = Parsed.from(url.text.toString())
                if (p == null) {
                    url.error = getString(R.string.invalid_link)
                    return@setOnClickListener
                }
                val finalName = TextFormat.clean(name.text.toString()).ifEmpty { p.defaultName() }
                val finalGroup = group.text.toString().trim()
                if (existing != null) {
                    existing.name = finalName
                    existing.url = p.url
                    existing.group = finalGroup
                } else {
                    links.add(Link(name = finalName, url = p.url, group = finalGroup))
                }
                persist()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    /** Si otra app comparte texto con esta, se abre el editor con el enlace ya relleno. */
    private fun handleShared(i: Intent?) {
        if (i == null || i.action != Intent.ACTION_SEND) return
        val text = i.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val found = Regex("(acestream://[0-9a-fA-F]{40}|magnet:\\?\\S+|https?://\\S+|\\b[0-9a-fA-F]{40}\\b)")
            .find(text)?.value ?: text.trim()
        showEditor(null, found)
    }

    // ---------- Menú ----------

    private fun handleMenu(id: Int): Boolean = when (id) {
        R.id.m_import -> { importFromClipboard(); true }
        R.id.m_m3u -> { share(TextFormat.m3u(links, store.template)); true }
        R.id.m_backup -> { share(TextFormat.backup(links)); true }
        R.id.m_settings -> { showSettings(); true }
        R.id.m_clear_history -> { confirmClearHistory(); true }
        R.id.m_clear -> { confirmClear(); true }
        else -> false
    }

    private fun importFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        val text = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(this).toString()
        } else {
            ""
        }
        val fresh = TextFormat.parse(text).filter { n ->
            links.none { it.url == n.url && it.name == n.name }
        }
        if (fresh.isEmpty()) {
            toast(getString(R.string.import_none))
            return
        }
        links.addAll(fresh)
        persist()
        toast(getString(R.string.import_ok, fresh.size))
    }

    private fun share(text: String) {
        if (links.isEmpty()) {
            toast(getString(R.string.export_none))
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, null))
    }

    private fun showSettings() {
        val input = layoutInflater.inflate(R.layout.dialog_settings, null)
        val field = input.findViewById<EditText>(R.id.f_template)
        field.setText(store.template)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setView(input)
            .setPositiveButton(R.string.save, null)
            .setNeutralButton(R.string.reset, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val v = field.text.toString().trim()
                if (!v.contains("{id}")) {
                    field.error = getString(R.string.template_needs_id)
                    return@setOnClickListener
                }
                store.template = v
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                field.setText(LinkStore.DEFAULT_TEMPLATE)
            }
        }
        dialog.show()
    }

    private fun confirmClear() {
        if (links.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_title)
            .setMessage(R.string.clear_message)
            .setPositiveButton(R.string.clear_yes) { _, _ ->
                links.clear()
                persist()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
