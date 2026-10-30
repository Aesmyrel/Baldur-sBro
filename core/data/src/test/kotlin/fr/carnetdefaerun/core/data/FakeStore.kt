package fr.carnetdefaerun.core.data

import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.EntryRow
import fr.carnetdefaerun.core.database.content.LinkRow
import fr.carnetdefaerun.core.database.content.NameRow
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType

/** Base de contenu en mémoire, pour les tests du dépôt. */
internal class FakeStore(entries: List<Entry>) : ContentStore {
    private val rows = entries.associate { e ->
        e.id to EntryRow(e.id, e.type, null, e.name.fr, e.name.en, e.tag, e.titleTag, ContentFormat.strict.encodeToString(Entry.serializer(), e))
    }
    private val links = entries.flatMap { e -> e.links.map { LinkRow(e.id, it.target, it.relation, it.tag) } }
    override fun entry(id: ContentId) = rows[id]
    override fun entries(type: EntryType) = rows.values.filter { it.type == type }
    override fun linksFrom(id: ContentId) = links.filter { it.source == id }
    override fun linksTo(id: ContentId) = links.filter { it.target == id }
    override fun names() = emptyList<NameRow>()
    override fun references() = emptyMap<String, fr.carnetdefaerun.core.model.content.Reference>()
    override fun meta() = emptyMap<String, String>()
}
