package com.example.mislinks

/** Importación y exportación en texto: "Nombre | enlace | grupo", M3U o enlaces sueltos. */
object TextFormat {

    private val SPLIT = Regex("\\s*\\|\\s*")
    private val SPACES = Regex("\\s+")
    private val GROUP = Regex("group-title=\"([^\"]*)\"")

    fun clean(name: String): String = name.replace('|', '/').trim()

    fun parse(text: String): List<Link> {
        val out = mutableListOf<Link>()
        var extName = ""
        var extGroup = ""
        for (raw in text.lines()) {
            val s = raw.trim()
            if (s.isEmpty()) continue
            if (s.startsWith("#EXTINF", ignoreCase = true)) {
                extName = s.substringAfterLast(',', "").trim()
                extGroup = GROUP.find(s)?.groupValues?.get(1).orEmpty()
                continue
            }
            if (s.startsWith("#")) continue

            var name = extName
            var link = s
            var group = extGroup
            if (name.isEmpty()) {
                val parts = s.split(SPLIT)
                if (parts.size > 1) {
                    if (Parsed.from(parts[1]) != null) {
                        name = parts[0]
                        link = parts[1]
                        group = parts.getOrElse(2) { "" }
                    } else {
                        link = parts.last()
                        name = parts.dropLast(1).joinToString(" ")
                    }
                } else {
                    val words = s.split(SPACES)
                    if (words.size > 1) {
                        link = words.last()
                        name = words.dropLast(1).joinToString(" ")
                    }
                }
            }
            extName = ""
            extGroup = ""

            val p = Parsed.from(link) ?: continue
            val finalName = clean(name).ifEmpty { p.defaultName() }
            out.add(Link(name = finalName, url = p.url, group = group.trim()))
        }
        return out
    }

    fun m3u(links: List<Link>, template: String): String = buildString {
        append("#EXTM3U\n")
        for (l in links) {
            val p = l.parsed
            val target = if (p != null && p.type == LinkType.ACE && p.aceId != null) {
                template.replace("{id}", p.aceId)
            } else {
                l.url
            }
            append("#EXTINF:-1")
            if (l.group.isNotBlank()) {
                append(" group-title=\"").append(l.group.replace('"', '\'')).append('"')
            }
            append(',').append(l.name).append('\n').append(target).append('\n')
        }
    }

    fun backup(links: List<Link>): String =
        links.joinToString("\n") { "${it.name} | ${it.url} | ${it.group}" }
}
