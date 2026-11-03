package fr.carnetdefaerun.core.ui

import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.navigation.Destination

/** Identifiant de catégorie dans les destinations et les liens profonds (`spell`, `subclass`…). */
val EntryType.slug: String get() = name.lowercase()

fun entryTypeOf(slug: String): EntryType? = EntryType.entries.firstOrNull { it.slug == slug }

/** Écran d'une entrée : une fiche de lore s'ouvre dans le codex, tout le reste dans le grimoire. */
fun destinationOf(id: ContentId, type: EntryType): Destination =
    if (type == EntryType.LORE) Destination.LoreEntry(id.value) else Destination.GameEntry(id.value)
