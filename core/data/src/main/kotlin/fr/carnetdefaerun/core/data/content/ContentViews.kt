package fr.carnetdefaerun.core.data.content

import fr.carnetdefaerun.core.model.content.Attribution
import fr.carnetdefaerun.core.model.content.Block
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Facts
import fr.carnetdefaerun.core.model.content.ItemSource
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey

/** Entrée telle qu'une liste l'affiche : seulement ce que dit son nom. */
data class EntrySummary(val id: ContentId, val type: EntryType, val subtype: String?, val name: Name)

/**
 * Liste filtrée. Les entrées masquées n'y figurent pas (ni leur nombre ni leur place dans l'ordre
 * alphabétique ne sont donnés) : [someHidden] permet seulement d'afficher un voile générique.
 */
data class EntryList(val visible: List<EntrySummary>, val someHidden: Boolean)

/** Entrée liée, visible depuis la fiche courante. */
data class LinkedEntry(val id: ContentId, val type: EntryType, val relation: Relation, val name: Name)

/**
 * Fiche telle qu'un écran la reçoit : tout ce qui est verrouillé en a été retiré, y compris les
 * identifiants des entrées liées masquées (ils dérivent de noms anglais). Une partie masquée n'est
 * représentée que par une poignée opaque, pour afficher un voile et permettre la révélation.
 */
data class EntryView(
    val id: ContentId,
    val type: EntryType,
    val name: Name,
    val aliases: List<String>,
    val licence: String,
    val attribution: List<Attribution>,
    /** Faits, nettoyés de toute référence à une entrée masquée et des sources d'obtention masquées. */
    val facts: Facts,
    val sources: List<ItemSource>,
    /** Présente si au moins une source d'obtention est masquée (un seul voile, sans compte). */
    val hiddenSources: RevealKey?,
    /** Vrai si des effets renvoient à des entrées masquées (retirés des faits). */
    val someEffectsHidden: Boolean,
    val blocks: List<Gated<Block>>,
    val links: List<LinkedEntry>,
    val unknown: List<String>,
)

/** Clés de révélation : ce que le joueur a choisi de dévoiler, enregistré dans sa base. */
object RevealIds {
    fun entry(id: ContentId) = "entry:${id.value}"
    fun title(id: ContentId) = "title:${id.value}"
    fun sources(id: ContentId) = "sources:${id.value}"
    fun block(id: ContentId, blockId: String) = "block:${id.value}#$blockId"
}
