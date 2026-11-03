package fr.carnetdefaerun.core.model.search

import fr.carnetdefaerun.core.model.content.ContentId

/** Qualité d'une correspondance de nom, de la meilleure à la plus faible (docs/PLAN.md §4). */
enum class MatchQuality {
    /** Le nom entier, à la forme repliée près. */
    EXACT,

    /** Le début du nom. */
    PREFIX,

    /** Chaque mot de la recherche commence un mot du nom, dans n'importe quel ordre. */
    WORD,

    /** Comme [WORD], avec des fautes de frappe tolérées. */
    TYPO,
}

enum class NameKind { FR, EN, ALIAS }

/** Un nom indexé : [form] est déjà replié par [Folding]. */
data class IndexedName(val id: ContentId, val form: String, val kind: NameKind)

/** Meilleure correspondance d'une entrée. [distance] compte les fautes tolérées (0 hors [MatchQuality.TYPO]). */
data class NameMatch(val id: ContentId, val quality: MatchQuality, val distance: Int, val kind: NameKind, val form: String)

/**
 * Index des noms en mémoire : un dictionnaire des mots, chacun relié aux noms qui le contiennent.
 * Chaque mot de la recherche est comparé aux mots du dictionnaire, par préfixe puis avec une distance de
 * Damerau-Levenshtein bornée (1 faute jusqu'à 5 lettres, 2 au-delà, aucune sous 3 lettres) mesurée sur le
 * début du mot, pour tolérer une faute pendant la frappe. Un nom correspond si chacun des mots cherchés y trouve
 * un mot. Aucun filtrage ici : l'appelant applique le verrou anti-spoiler à chaque résultat.
 */
class NameIndex(names: List<IndexedName>) {
    private val names = names.filter { it.form.isNotEmpty() }.distinct()
    private val formWords: List<List<String>> = this.names.map { it.form.split(' ') }
    private val compact: List<String> = this.names.map { it.form.replace(" ", "") }
    private val dictionary: List<String> = formWords.flatten().distinct()

    val size: Int get() = names.size

    fun search(query: String, limit: Int = Int.MAX_VALUE): List<NameMatch> {
        val q = Folding.fold(query)
        if (q.isEmpty()) return emptyList()
        val tokens = q.split(' ')
        val qCompact = q.replace(" ", "")
        // Pour chaque mot cherché : les mots du dictionnaire qui lui correspondent, avec leur nombre de fautes.
        val perToken: List<Map<String, Int>> = tokens.map { token -> dictionary.mapNotNull { word -> wordDistance(token, word)?.let { word to it } }.toMap() }

        val best = HashMap<ContentId, NameMatch>()
        for (i in names.indices) {
            val name = names[i]
            val quality: MatchQuality
            var distance = 0
            when {
                name.form == q || compact[i] == qCompact -> quality = MatchQuality.EXACT
                name.form.startsWith(q) || compact[i].startsWith(qCompact) -> quality = MatchQuality.PREFIX
                else -> {
                    val words = formWords[i]
                    for (matches in perToken) {
                        val d = words.minOfOrNull { matches[it] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
                        if (d == Int.MAX_VALUE) { distance = -1; break }
                        distance += d
                    }
                    if (distance < 0) continue
                    quality = if (distance == 0) MatchQuality.WORD else MatchQuality.TYPO
                }
            }
            val match = NameMatch(name.id, quality, distance, name.kind, name.form)
            val current = best[name.id]
            if (current == null || ORDER.compare(match, current) < 0) best[name.id] = match
        }
        return best.values.sortedWith(ORDER).take(limit)
    }

    /** Fautes entre le mot cherché et le début d'un mot du dictionnaire, ou null au-delà de la tolérance. */
    private fun wordDistance(token: String, word: String): Int? {
        if (word.startsWith(token)) return 0
        if (token.length < MIN_TYPO_LENGTH) return null
        val max = if (token.length <= 5) 1 else 2
        var best = Int.MAX_VALUE
        for (len in (token.length - max)..(token.length + max)) {
            if (len < 1 || len > word.length) continue
            val d = damerauLevenshtein(token, word, len, max)
            if (d < best) best = d
        }
        return best.takeIf { it <= max }
    }

    companion object {
        private const val MIN_TYPO_LENGTH = 3

        /** Meilleure qualité, moins de fautes, nom français d'abord, nom le plus court, ordre alphabétique. */
        val ORDER: Comparator<NameMatch> = compareBy<NameMatch>({ it.quality }, { it.distance }, { it.kind }, { it.form.length }, { it.form })

        /**
         * Distance de Damerau-Levenshtein (variante « alignement optimal » : transposition de deux lettres voisines)
         * entre [a] et les [len] premières lettres de [b], arrêtée dès qu'elle dépasse [max].
         */
        fun damerauLevenshtein(a: String, b: String, len: Int = b.length, max: Int = Int.MAX_VALUE): Int {
            val n = a.length
            val m = len
            if (kotlin.math.abs(n - m) > max) return max + 1
            var prev2 = IntArray(m + 1)
            var prev = IntArray(m + 1) { it }
            var cur = IntArray(m + 1)
            for (i in 1..n) {
                cur[0] = i
                var rowMin = cur[0]
                for (j in 1..m) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, prev2[j - 2] + 1)
                    cur[j] = v
                    if (v < rowMin) rowMin = v
                }
                if (rowMin > max) return max + 1
                val t = prev2; prev2 = prev; prev = cur; cur = t
            }
            return prev[m]
        }
    }
}
