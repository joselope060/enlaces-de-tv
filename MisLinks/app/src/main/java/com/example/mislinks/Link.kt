package com.example.mislinks

import java.util.UUID

enum class LinkType(val label: String) {
    ACE("AceStream"),
    MAGNET("Magnet"),
    WEB("Web")
}

data class Link(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var url: String,
    var group: String = ""
) {
    val parsed: Parsed? get() = Parsed.from(url)
}

/** Resultado de interpretar un enlace escrito por el usuario. */
data class Parsed(val type: LinkType, val aceId: String?, val url: String) {

    fun defaultName(): String =
        if (aceId != null) "Canal ${aceId.take(6)}" else url.take(40)

    companion object {
        private val ID = Regex("^(?:acestream://)?([0-9a-f]{40})$", RegexOption.IGNORE_CASE)
        private val GET = Regex("getstream\\?id=([0-9a-f]{40})", RegexOption.IGNORE_CASE)
        private val WEB = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

        /** Devuelve null si el texto no es un enlace admitido. */
        fun from(raw: String): Parsed? {
            val s = raw.trim()
            ID.find(s)?.let { return ace(it.groupValues[1]) }
            GET.find(s)?.let { return ace(it.groupValues[1]) }
            if (s.startsWith("magnet:?", ignoreCase = true)) return Parsed(LinkType.MAGNET, null, s)
            if (WEB.matches(s)) return Parsed(LinkType.WEB, null, s)
            return null
        }

        private fun ace(id: String): Parsed {
            val low = id.lowercase()
            return Parsed(LinkType.ACE, low, "acestream://$low")
        }
    }
}

/** Un enlace de AceStream reproducido recientemente. */
data class HistoryEntry(val url: String, val name: String, val time: Long)
