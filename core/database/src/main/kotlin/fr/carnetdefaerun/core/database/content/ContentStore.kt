package fr.carnetdefaerun.core.database.content

import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.Reference
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag

/** En-tête d'une entrée, lisible sans décoder la fiche : ce qu'il faut pour filtrer et afficher une liste. */
data class EntryHeader(
    val id: ContentId,
    val type: EntryType,
    val subtype: String?,
    val name: Name,
    val tag: SpoilerTag,
    val titleTag: SpoilerTag,
)

/** Ligne de la table des entrées : en-tête, et la fiche complète décodée à la demande. */
data class EntryRow(val header: EntryHeader, val payload: String) {
    val id: ContentId get() = header.id
    val type: EntryType get() = header.type
    val subtype: String? get() = header.subtype
    val name: Name get() = header.name
    val tag: SpoilerTag get() = header.tag
    val titleTag: SpoilerTag get() = header.titleTag

    fun entry(): Entry = ContentFormat.lenient.decodeFromString(Entry.serializer(), payload)
}

data class LinkRow(val source: ContentId, val target: ContentId, val relation: Relation, val tag: SpoilerTag)

/** Forme normalisée d'un nom (français, anglais, alias), pour l'index de recherche. */
data class NameRow(val entryId: ContentId, val form: String, val kind: String)

/** Bloc de texte trouvé par l'index plein texte, avec son étiquette et un extrait autour des mots trouvés. */
data class TextHitRow(val entryId: ContentId, val blockId: String, val tag: SpoilerTag, val excerpt: String)

/**
 * Accès en lecture à la base de contenu. Réservé aux dépôts de core:data : un écran n'y accède jamais,
 * car rien ici n'est filtré par le verrou anti-spoiler.
 */
interface ContentStore {
    fun entry(id: ContentId): EntryRow?
    fun entries(type: EntryType): List<EntryRow>
    fun headers(): List<EntryHeader>
    fun linksFrom(id: ContentId): List<LinkRow>
    fun linksTo(id: ContentId): List<LinkRow>
    fun names(): List<NameRow>

    /** Blocs dont le texte contient chacun des [words] (déjà repliés, lettres et chiffres seulement) en début de mot. */
    fun searchText(words: List<String>, limit: Int): List<TextHitRow>
    fun references(): Map<String, Reference>
    fun meta(): Map<String, String>
}
