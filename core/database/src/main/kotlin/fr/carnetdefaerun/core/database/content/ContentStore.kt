package fr.carnetdefaerun.core.database.content

import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag

/** Ligne de la table des entrées : en-tête lisible sans décoder la fiche, et la fiche complète à la demande. */
data class EntryRow(
    val id: ContentId,
    val type: EntryType,
    val subtype: String?,
    val nameFr: String,
    val nameEn: String,
    val tag: SpoilerTag,
    val titleTag: SpoilerTag,
    val payload: String,
) {
    fun entry(): Entry = ContentFormat.lenient.decodeFromString(Entry.serializer(), payload)
}

data class LinkRow(val source: ContentId, val target: ContentId, val relation: Relation, val tag: SpoilerTag)

/** Forme normalisée d'un nom (français, anglais, alias), pour l'index de recherche. */
data class NameRow(val entryId: ContentId, val form: String, val kind: String)

/**
 * Accès en lecture à la base de contenu. Réservé aux dépôts de core:data : un écran n'y accède jamais,
 * car rien ici n'est filtré par le verrou anti-spoiler.
 */
interface ContentStore {
    fun entry(id: ContentId): EntryRow?
    fun entries(type: EntryType): List<EntryRow>
    fun linksFrom(id: ContentId): List<LinkRow>
    fun linksTo(id: ContentId): List<LinkRow>
    fun names(): List<NameRow>
    fun meta(): Map<String, String>
}
