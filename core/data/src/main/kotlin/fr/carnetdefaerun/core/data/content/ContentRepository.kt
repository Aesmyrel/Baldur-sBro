package fr.carnetdefaerun.core.data.content

import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.EntryRow
import fr.carnetdefaerun.core.model.content.ConditionFacts
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Effect
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatureFacts
import fr.carnetdefaerun.core.model.content.Grants
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.LoreSection
import fr.carnetdefaerun.core.model.content.PlaceFacts
import fr.carnetdefaerun.core.model.content.SpellFacts
import fr.carnetdefaerun.core.model.content.WearerCondition
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.model.spoiler.SpoilerGate
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.gated

/**
 * Seule porte d'accès des écrans au contenu : chaque fiche, nom, source, bloc et lien passe par
 * [SpoilerGate] (docs/PLAN.md §2). [revealed] contient les clés de [RevealIds] que le joueur a
 * choisi de dévoiler ; [showActOfHidden] est son réglage « indiquer l'acte des contenus masqués ».
 */
class ContentRepository internal constructor(private val store: ContentStore) {

    fun list(type: EntryType, profile: SpoilerProfile, revealed: Set<String> = emptySet()): EntryList {
        val rows = store.entries(type)
        val visible = rows.filter { titleVisible(it, profile, revealed) }
        return EntryList(visible.map { it.summary() }, someHidden = visible.size < rows.size)
    }

    fun entry(id: ContentId, profile: SpoilerProfile, revealed: Set<String> = emptySet(), showActOfHidden: Boolean = false): Gated<EntryView>? {
        val row = store.entry(id) ?: return null
        if (!SpoilerGate.isVisible(row.tag, profile, RevealIds.entry(id) in revealed)) {
            return Gated.Hidden(RevealKey.of(RevealIds.entry(id)), if (showActOfHidden) row.tag.act else null)
        }
        val entry = row.entry()

        // Entrées liées : le lien et le nom de la cible doivent être visibles tous les deux.
        val targets = entry.links.associate { link -> link.target to store.entry(link.target) }
        fun linkVisible(target: ContentId): Boolean {
            val link = entry.links.firstOrNull { it.target == target } ?: return false
            val targetRow = targets[target] ?: return false
            return SpoilerGate.isVisible(link.tag, profile) && titleVisible(targetRow, profile, revealed)
        }
        val links = entry.links.filter { linkVisible(it.target) }.map { link ->
            val t = targets.getValue(link.target)!!
            LinkedEntry(t.id, t.type, link.relation, t.entry().name)
        }

        var someEffectsHidden = false
        var hiddenSources: RevealKey? = null
        val facts = when (val f = entry.facts) {
            is ItemFacts -> {
                val sourcesRevealed = RevealIds.sources(id) in revealed
                val sources = f.sources.filter { SpoilerGate.isVisible(it.tag, profile, sourcesRevealed) && (it.place == null || linkVisible(it.place!!)) }
                if (sources.size < f.sources.size) hiddenSources = RevealKey.of(RevealIds.sources(id))
                val effects = f.effects.filter { effect -> effectTargets(effect).all(::linkVisible) }
                someEffectsHidden = effects.size < f.effects.size
                f.copy(
                    sources = sources.map { s -> if (s.place != null && !linkVisible(s.place!!)) s.copy(place = null) else s },
                    effects = effects,
                    weapon = f.weapon?.let { w -> w.copy(weaponActions = w.weaponActions.filter(::linkVisible)) },
                )
            }
            is FeatureFacts -> f.copy(applies = f.applies.filter(::linkVisible))
            is PlaceFacts -> f.copy(region = f.region?.takeIf(::linkVisible))
            is SpellFacts -> f
            is ConditionFacts -> f
            // Les sections du codex sont filtrées une à une ci-dessous, jamais transmises en bloc.
            is LoreFacts -> f.copy(sections = emptyList())
        }
        val sources = (facts as? ItemFacts)?.sources.orEmpty()

        val blocks = entry.blocks.map { block ->
            block.gated(RevealIds.block(id, block.id), block.tag, profile, RevealIds.block(id, block.id) in revealed, showActOfHidden)
        }
        val sections = (entry.facts as? LoreFacts)?.sections.orEmpty().map { section ->
            val key = RevealIds.block(id, section.id)
            section.gated(key, section.tag, profile, key in revealed, showActOfHidden)
        }
        // Références : seulement celles que citent les sections visibles.
        val cited = sections.filterIsInstance<Gated.Visible<LoreSection>>().flatMap { s -> s.value.paragraphs.flatMap { p -> p.refs.map { it.ref } } }.toSet()
        val references = if (cited.isEmpty()) emptyList() else store.references().filterKeys { it in cited }.values.sortedBy { it.id }
        return Gated.Visible(
            EntryView(
                id = entry.id, type = entry.type, name = entry.name, aliases = entry.aliases,
                licence = entry.licence, attribution = entry.attribution,
                facts = facts, sources = sources, hiddenSources = hiddenSources, someEffectsHidden = someEffectsHidden,
                blocks = blocks, sections = sections, references = references, links = links, unknown = entry.unknown,
            ),
        )
    }

    private fun titleVisible(row: EntryRow, profile: SpoilerProfile, revealed: Set<String>) =
        SpoilerGate.isVisible(row.titleTag, profile, RevealIds.title(row.id) in revealed || RevealIds.entry(row.id) in revealed)

    private fun effectTargets(effect: Effect): List<ContentId> = when (effect) {
        is Grants -> listOf(effect.target)
        is WearerCondition -> listOf(effect.condition)
        else -> emptyList()
    }

    private fun EntryRow.summary() = EntrySummary(id, type, subtype, entry().name)
}
