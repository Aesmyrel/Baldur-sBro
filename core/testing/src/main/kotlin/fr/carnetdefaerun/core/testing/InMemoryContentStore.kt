package fr.carnetdefaerun.core.testing

import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.EntryHeader
import fr.carnetdefaerun.core.database.content.EntryRow
import fr.carnetdefaerun.core.database.content.LinkRow
import fr.carnetdefaerun.core.database.content.NameRow
import fr.carnetdefaerun.core.database.content.TextHitRow
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Reference
import fr.carnetdefaerun.core.model.content.listSubtype
import fr.carnetdefaerun.core.model.search.Folding

/**
 * Base de contenu en mémoire, pour les tests des dépôts et les captures d'écran. Noms et texte indexés comme le fait le pipeline
 * (formes repliées ; texte des blocs cherché par début de mot).
 */
class InMemoryContentStore(entries: List<Entry>) : ContentStore {
    private val rows = entries.associate { e ->
        e.id to EntryRow(EntryHeader(e.id, e.type, e.listSubtype, e.name, e.tag, e.titleTag), ContentFormat.strict.encodeToString(Entry.serializer(), e))
    }
    private val links = entries.flatMap { e -> e.links.map { LinkRow(e.id, it.target, it.relation, it.tag) } }
    private val names = entries.flatMap { e ->
        (listOf("fr" to e.name.fr, "en" to e.name.en) + e.aliases.map { "alias" to it })
            .map { (kind, text) -> kind to Folding.fold(text) }.filter { it.second.isNotEmpty() }.distinctBy { it.second }
            .map { (kind, form) -> NameRow(e.id, form, kind) }
    }
    private val texts = entries.flatMap { e -> e.blocks.map { b -> TextHitRow(e.id, b.id, b.tag, b.text) to Folding.fold(b.text).split(' ') } }

    override fun entry(id: ContentId) = rows[id]
    override fun entries(type: EntryType) = rows.values.filter { it.type == type }
    override fun headers() = rows.values.map { it.header }
    override fun linksFrom(id: ContentId) = links.filter { it.source == id }
    override fun linksTo(id: ContentId) = links.filter { it.target == id }
    override fun names() = names
    override fun searchText(words: List<String>, limit: Int) =
        texts.filter { (_, textWords) -> words.all { w -> textWords.any { it.startsWith(w) } } }.map { it.first }.take(limit)
    override fun references() = emptyMap<String, Reference>()
    override fun meta() = emptyMap<String, String>()
}
