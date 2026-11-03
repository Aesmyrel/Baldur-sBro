package fr.carnetdefaerun.core.data.search

import fr.carnetdefaerun.core.data.content.EntrySummary
import fr.carnetdefaerun.core.data.content.RevealIds
import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.EntryHeader
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.model.search.IndexedName
import fr.carnetdefaerun.core.model.search.MatchQuality
import fr.carnetdefaerun.core.model.search.NameIndex
import fr.carnetdefaerun.core.model.search.NameKind
import fr.carnetdefaerun.core.model.search.NameMatch
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.model.spoiler.SpoilerGate
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag

/** Résultat de recherche tel qu'un écran le reçoit : une entrée visible, trouvée par son nom ou par son texte. */
data class SearchHit(
    val entry: EntrySummary,
    /** Qualité de la correspondance du nom ; null si l'entrée n'a été trouvée que par son texte. */
    val nameMatch: MatchQuality?,
    /** Quel nom a correspondu (le français, l'anglais ou un alias) ; null pour un résultat plein texte. */
    val matchedKind: NameKind?,
    /** Extrait du passage trouvé, pour un résultat plein texte. */
    val excerpt: String?,
)

/**
 * Résultats filtrés. Les résultats masqués ne sont ni donnés ni comptés : [hidden] n'est rempli que si le joueur
 * a demandé à chercher aussi dans le contenu masqué, et ne contient que des poignées opaques (un voile chacune).
 */
data class SearchResults(val hits: List<SearchHit>, val hidden: List<RevealKey>) {
    companion object {
        val EMPTY = SearchResults(emptyList(), emptyList())
    }
}

/**
 * Recherche globale (docs/PLAN.md §4) : noms d'abord (index en mémoire, tolérant aux fautes), puis texte intégral
 * des blocs. Chaque résultat passe par [SpoilerGate] avant d'être rendu, comme dans le dépôt de contenu : un nom
 * dont le titre est verrouillé, un passage dont le bloc ou la fiche est verrouillé ne sont jamais renvoyés.
 */
class SearchRepository internal constructor(private val store: ContentStore) {
    private val headers: Map<ContentId, EntryHeader> by lazy { store.headers().associateBy { it.id } }
    private val index: NameIndex by lazy {
        NameIndex(store.names().map { IndexedName(it.entryId, it.form, KINDS[it.kind] ?: NameKind.ALIAS) })
    }

    /** Charge l'index et les en-têtes (quelques dizaines de millisecondes), pour une première frappe instantanée. */
    fun warmUp() {
        headers
        index
    }

    fun search(
        query: String,
        profile: SpoilerProfile,
        revealed: Set<String> = emptySet(),
        corrections: Map<ContentId, String> = emptyMap(),
        favorites: Set<ContentId> = emptySet(),
        includeHidden: Boolean = false,
        limit: Int = DEFAULT_LIMIT,
    ): SearchResults {
        val words = Folding.fold(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return SearchResults.EMPTY
        val gate = Gate(profile, revealed)
        val hits = mutableListOf<SearchHit>()
        val hidden = LinkedHashSet<RevealKey>()
        val found = HashSet<ContentId>()

        val byName = (index.search(query) + correctionMatches(query, corrections))
            .groupBy { it.id }.values.map { matches -> matches.minWith(NameIndex.ORDER) }
            .mapNotNull { match -> headers[match.id]?.let { match to it } }
            .sortedWith(
                compareBy<Pair<NameMatch, EntryHeader>>({ it.first.quality }, { it.first.distance })
                    .thenBy { it.second.id !in favorites }
                    .thenBy { it.second.tag.act != profile.currentAct }
                    .then { a, b -> NameIndex.ORDER.compare(a.first, b.first) },
            )
        for ((match, header) in byName) {
            if (hits.size >= limit && !includeHidden) break
            if (gate.titleVisible(header)) {
                if (hits.size < limit) hits += SearchHit(header.summary(), match.quality, match.kind, excerpt = null)
                found += header.id
            } else if (includeHidden) {
                hidden += RevealKey.of(RevealIds.title(header.id))
            }
        }

        // Texte intégral : seulement à partir de trois lettres, un passage par entrée, après les noms.
        if (words.sumOf { it.length } >= MIN_TEXT_LENGTH && hits.size < limit) {
            val textHits = store.searchText(words, TEXT_CANDIDATES).mapNotNull { hit -> headers[hit.entryId]?.let { hit to it } }
            val ordered = textHits.sortedBy { (_, header) -> header.id !in favorites }
            for ((hit, header) in ordered) {
                if (header.id in found) continue
                val key = when {
                    !gate.titleVisible(header) -> RevealIds.title(header.id)
                    !gate.visible(header.tag, RevealIds.entry(header.id)) -> RevealIds.entry(header.id)
                    !gate.visible(hit.tag, RevealIds.block(header.id, hit.blockId)) -> RevealIds.block(header.id, hit.blockId)
                    else -> null
                }
                if (key == null) {
                    if (hits.size < limit) hits += SearchHit(header.summary(), nameMatch = null, matchedKind = null, excerpt = hit.excerpt)
                    found += header.id
                } else if (includeHidden) {
                    hidden += RevealKey.of(key)
                }
            }
        }
        return SearchResults(hits, if (includeHidden) hidden.toList() else emptyList())
    }

    /** Les noms corrigés par le joueur se cherchent comme des noms français. */
    private fun correctionMatches(query: String, corrections: Map<ContentId, String>): List<NameMatch> =
        if (corrections.isEmpty()) emptyList()
        else NameIndex(corrections.map { (id, fr) -> IndexedName(id, Folding.fold(fr), NameKind.FR) }).search(query)

    /** Le verrou, avec les révélations du joueur ; l'empreinte d'une clé n'est calculée que pour un contenu verrouillé. */
    private class Gate(private val profile: SpoilerProfile, private val revealed: Set<String>) {
        fun visible(tag: SpoilerTag, vararg keys: String) =
            SpoilerGate.isVisible(tag, profile) || SpoilerGate.isVisible(tag, profile, keys.any(::isRevealed))

        fun titleVisible(header: EntryHeader) = visible(header.titleTag, RevealIds.title(header.id), RevealIds.entry(header.id))

        private fun isRevealed(key: String) = revealed.isNotEmpty() && RevealKey.of(key).token in revealed
    }

    private fun EntryHeader.summary() = EntrySummary(id, type, subtype, name)

    companion object {
        const val DEFAULT_LIMIT = 60
        private const val MIN_TEXT_LENGTH = 3
        private const val TEXT_CANDIDATES = 400
        private val KINDS = mapOf("fr" to NameKind.FR, "en" to NameKind.EN, "alias" to NameKind.ALIAS)
    }
}
