package fr.carnetdefaerun.pipeline.wiki

/**
 * Lecture du wikitexte de MediaWiki, limitée à ce dont le pipeline a besoin : modèles et paramètres,
 * liens internes, et un texte brut de secours pour les noms.
 */
data class Template(
    /** Nom normalisé : minuscules, sans espaces ni tirets bas (`Equipment page` = `EquipmentPage`). */
    val name: String,
    val positional: List<String>,
    val named: Map<String, String>,
) {
    operator fun get(key: String): String? = named[key]?.takeIf { it.isNotBlank() }

    fun arg(index: Int): String? = positional.getOrNull(index)?.takeIf { it.isNotBlank() }

    fun flag(key: String): Boolean = get(key)?.lowercase() in setOf("yes", "y", "true", "1")
}

data class WikiLink(val target: String, val label: String)

object Wikitext {
    fun normalizeName(name: String) = name.replace(Regex("[\\s_]"), "").lowercase()

    fun stripComments(text: String) = text.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    /** Tous les modèles du texte, imbriqués compris, dans l'ordre de leur ouverture. */
    fun templates(text: String): List<Template> {
        val source = stripComments(text)
        val found = mutableListOf<Pair<Int, Template>>()
        val stack = ArrayDeque<Int>()
        var i = 0
        while (i < source.length) {
            when {
                source.startsWith("{{{", i) -> {
                    // Paramètre de modèle ({{{x}}}) : on le saute sans l'interpréter.
                    val end = source.indexOf("}}}", i + 3)
                    i = if (end < 0) source.length else end + 3
                }
                source.startsWith("{{", i) -> {
                    stack.addLast(i)
                    i += 2
                }
                source.startsWith("}}", i) && stack.isNotEmpty() -> {
                    val start = stack.removeLast()
                    parseTemplate(source.substring(start + 2, i))?.let { found += start to it }
                    i += 2
                }
                else -> i++
            }
        }
        return found.sortedBy { it.first }.map { it.second }
    }

    /** Premier modèle portant l'un des [names] (comparés une fois normalisés). */
    fun pageTemplate(text: String, vararg names: String): Template? {
        val wanted = names.map(::normalizeName).toSet()
        return templates(text).firstOrNull { it.name in wanted }
    }

    fun hasTemplate(text: String, vararg names: String): Boolean {
        val wanted = names.map(::normalizeName).toSet()
        return templates(text).any { it.name in wanted }
    }

    fun links(text: String): List<WikiLink> =
        Regex("\\[\\[([^\\[\\]|]+)(?:\\|([^\\[\\]]*))?]]").findAll(stripComments(text)).map {
            val target = it.groupValues[1].substringBefore('#').trim()
            WikiLink(target, it.groupValues[2].ifBlank { it.groupValues[1] }.trim())
        }.toList()

    /** Découpe le corps d'un modèle sur les barres verticales de premier niveau. */
    internal fun splitTopLevel(body: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var i = 0
        while (i < body.length) {
            val two = if (i + 1 < body.length) body.substring(i, i + 2) else ""
            when {
                two == "{{" || two == "[[" -> { depth++; current.append(two); i += 2 }
                (two == "}}" || two == "]]") && depth > 0 -> { depth--; current.append(two); i += 2 }
                body[i] == '|' && depth == 0 -> { parts += current.toString(); current.clear(); i++ }
                else -> { current.append(body[i]); i++ }
            }
        }
        parts += current.toString()
        return parts
    }

    private fun parseTemplate(body: String): Template? {
        val parts = splitTopLevel(body)
        val rawName = parts.first().trim()
        if (rawName.isEmpty() || rawName.startsWith("#")) return null // fonctions d'analyse (#if…)
        val name = normalizeName(rawName)
        val positional = mutableListOf<String>()
        val named = linkedMapOf<String, String>()
        for (part in parts.drop(1)) {
            val eq = namedSplit(part)
            if (eq != null) named[eq.first] = eq.second else positional += part.trim()
        }
        return Template(name, positional, named)
    }

    /** `clé = valeur` si le premier `=` est hors de tout lien ou modèle. */
    private fun namedSplit(part: String): Pair<String, String>? {
        var depth = 0
        var i = 0
        while (i < part.length) {
            val two = if (i + 1 < part.length) part.substring(i, i + 2) else ""
            when {
                two == "{{" || two == "[[" -> { depth++; i += 2 }
                two == "}}" || two == "]]" -> { depth--; i += 2 }
                part[i] == '=' && depth == 0 -> {
                    val key = part.substring(0, i).trim()
                    return if (key.isEmpty()) null else key to part.substring(i + 1).trim()
                }
                else -> i++
            }
        }
        return null
    }

    /**
     * Texte brut d'une valeur courte (nom, lieu) : liens réduits à leur libellé, modèles de lien
     * réduits à leur premier argument, gras et italique retirés. Ne sert jamais à reprendre une description.
     */
    fun plain(value: String): String {
        var s = stripComments(value)
        s = s.replace(Regex("<ref[^>]*/>"), "").replace(Regex("<ref[^>]*>.*?</ref>", RegexOption.DOT_MATCHES_ALL), "")
        repeat(4) {
            s = s.replace(Regex("\\{\\{([^{}]*)}}")) { m ->
                val parts = splitTopLevel(m.groupValues[1])
                val args = parts.drop(1).filterNot { it.contains('=') }.map(String::trim)
                val labelled = normalizeName(parts.first()) in LABELLED && args.size >= 2
                if (labelled) args[1] else args.firstOrNull().orEmpty()
            }
        }
        s = s.replace(Regex("\\[\\[(?:[^\\[\\]|]+\\|)?([^\\[\\]]+)]]"), "$1")
        s = s.replace(Regex("'{2,}"), "").replace(Regex("<[^>]+>"), "")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /** Modèles de lien dont le second argument est le libellé affiché. */
    private val LABELLED = setOf("charlink", "smcharlink", "mdcharlink", "sai", "smsai")
}
