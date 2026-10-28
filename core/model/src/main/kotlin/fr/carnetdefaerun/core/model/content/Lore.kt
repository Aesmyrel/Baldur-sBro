package fr.carnetdefaerun.core.model.content

import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Thèmes du codex (docs/PLAN.md §7). */
@Serializable
enum class LoreTheme {
    @SerialName("pantheon") PANTHEON,
    @SerialName("cosmologie") COSMOLOGY,
    @SerialName("peuples") PEOPLES,
    @SerialName("bestiaire") BESTIARY,
    @SerialName("factions") FACTIONS,
    @SerialName("lieux") PLACES,
    @SerialName("personnages") CHARACTERS,
    @SerialName("objets") LEGENDARY_ITEMS,
    @SerialName("chronologie") TIMELINE,
    @SerialName("saga") SAGA,
    @SerialName("livres") GAME_BOOKS,
    @SerialName("plus-loin") FURTHER_READING,
}

/** Sorte de section d'une fiche de lore. */
@Serializable
enum class LoreSectionType {
    /** Le sujet dans l'univers de D&D. */
    @SerialName("general") GENERAL,

    /** Ce qu'en montre BG3. */
    @SerialName("bg3") BG3,

    /** Ce qu'en disent les autres œuvres (jeux précédents, romans, aventures). */
    @SerialName("autres_oeuvres") OTHER_WORKS,

    /** Écarts de canon entre éditions ou entre œuvres, présentés sans trancher. */
    @SerialName("ecarts") CANON_DIFFERENCES,

    /** Œuvres où le sujet est développé. */
    @SerialName("plus_loin") FURTHER_READING,
}

/** Nature d'une affirmation : ce que dit BG3, ce que dit le canon de D&D, ou notre lecture. */
@Serializable
enum class CanonNature {
    @SerialName("canon_bg3") CANON_BG3,
    @SerialName("canon_dnd") CANON_DND,
    @SerialName("interpretation") INTERPRETATION,
}

/** Édition de D&D d'où vient une affirmation (le lore a varié d'une édition à l'autre). */
@Serializable
enum class Edition {
    @SerialName("add1") ADND_1,
    @SerialName("add2") ADND_2,
    @SerialName("3e") THIRD,
    @SerialName("3.5") THIRD_POINT_FIVE,
    @SerialName("4e") FOURTH,
    @SerialName("5e-2014") FIFTH_2014,
    @SerialName("5e-2024") FIFTH_2024,
    @SerialName("multi") MULTI,
}

/** Renvoi vers une référence du registre, avec sa localisation quand on la connaît. */
@Serializable
data class Citation(val ref: String, val page: String? = null, val chapter: String? = null)

/**
 * Paragraphe rédigé par le projet : au moins une référence, une nature, une édition pour le canon
 * de D&D. Des paragraphes d'un même [variantGroup] présentent des versions contradictoires côte à côte.
 */
@Serializable
data class LoreParagraph(
    val text: String,
    val nature: CanonNature,
    val edition: Edition? = null,
    val refs: List<Citation>,
    val variantGroup: String? = null,
)

/** Section d'une fiche, avec sa propre étiquette ; son identifiant est un numéro d'ordre (`s1`), jamais un titre. */
@Serializable
data class LoreSection(
    val id: String,
    val type: LoreSectionType,
    val title: String,
    val tag: SpoilerTag,
    val paragraphs: List<LoreParagraph>,
)

@Serializable
@SerialName("lore")
data class LoreFacts(
    val theme: LoreTheme,
    /** Accroche d'une ligne, sous l'étiquette de la fiche. */
    val summary: String,
    val sections: List<LoreSection>,
    /** Temps de lecture estimé, en minutes. */
    val readingMinutes: Int,
) : Facts

/** Sorte de référence (registre `content/references.yaml`). */
@Serializable
enum class ReferenceKind {
    @SerialName("livre") BOOK,
    @SerialName("aventure") ADVENTURE,
    @SerialName("roman") NOVEL,
    @SerialName("jeu") GAME,
    @SerialName("magazine") MAGAZINE,
    @SerialName("wiki") WIKI,
    @SerialName("site") WEBSITE,
}

/** Comment une référence a été consultée : directement, ou citée par une source secondaire (signalé). */
@Serializable
enum class Consultation {
    @SerialName("directe") DIRECT,
    @SerialName("secondaire") SECONDARY,
}

@Serializable
data class Reference(
    val id: String,
    val kind: ReferenceKind,
    val titleOriginal: String,
    val titleFrench: String? = null,
    val publisher: String? = null,
    val year: Int? = null,
    val edition: Edition? = null,
    val url: String? = null,
    val revision: Long? = null,
    val isbn: String? = null,
    val licence: String? = null,
    val consultation: Consultation,
    /** Pour une référence citée par une source secondaire : l'identifiant de cette source. */
    val via: String? = null,
)
