package fr.carnetdefaerun.core.data.content

import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.EntryHeader
import fr.carnetdefaerun.core.database.content.EntryRow
import fr.carnetdefaerun.core.model.content.ClassFacts
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
import fr.carnetdefaerun.core.model.content.SubclassFacts
import fr.carnetdefaerun.core.model.content.SubraceFacts
import fr.carnetdefaerun.core.model.content.RaceFacts
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.content.BackgroundFacts
import fr.carnetdefaerun.core.model.content.FeatFacts
import fr.carnetdefaerun.core.model.content.WearerCondition
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.model.spoiler.SpoilerGate
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.core.model.spoiler.gated

/**
 * Seule porte d'accès des écrans au contenu : chaque fiche, nom, source, bloc et lien passe par
 * [SpoilerGate] (docs/PLAN.md §2). [revealed] contient les jetons ([RevealKey.token]) de ce que le joueur
 * a choisi de dévoiler : l'écran ne connaît que ces jetons, la base du joueur ne stocke qu'eux.
 * [showActOfHidden] est son réglage « indiquer l'acte des contenus masqués ».
 * Les écrans ne voient jamais un [ContentStore] (core:database ne leur est pas exposé) : seuls les tests et
 * l'injection de dépendances construisent ce dépôt.
 */
class ContentRepository(private val store: ContentStore) {

    /** En-têtes par type, lus une fois : la base de contenu ne change pas pendant la vie de l'appli. */
    private val headers: Map<EntryType, List<EntryHeader>> by lazy { store.headers().groupBy { it.type } }

    fun list(type: EntryType, profile: SpoilerProfile, revealed: Set<String> = emptySet()): EntryList {
        val rows = headers[type].orEmpty()
        val visible = rows.filter { titleVisible(it.id, it.titleTag, profile, revealed) }
        return EntryList(visible.map { EntrySummary(it.id, it.type, it.subtype, it.name) }, someHidden = visible.size < rows.size)
    }

    fun entry(id: ContentId, profile: SpoilerProfile, revealed: Set<String> = emptySet(), showActOfHidden: Boolean = false): Gated<EntryView>? {
        val row = store.entry(id) ?: return null
        if (!SpoilerGate.isVisible(row.tag, profile, revealed.has(RevealIds.entry(id)))) {
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
        val linked = entry.links.filter { linkVisible(it.target) }.map { link -> link to targets.getValue(link.target)!!.entry() }
        val links = linked.map { (link, t) -> LinkedEntry(t.id, t.type, link.relation, t.name) }
        // Attribution : la page de la fiche, puis celles des états qu'elle applique et des capacités qu'elle accorde,
        // dont ses effets rédigés s'inspirent (CC BY-NC-SA). Seulement celles des fiches liées visibles : un titre de
        // page anglais ne doit jamais trahir une fiche masquée.
        val attribution = (entry.attribution + linked.filter { (link, _) -> link.relation in ATTRIBUTED_RELATIONS }.flatMap { it.second.attribution.take(1) })
            .distinctBy { it.url }

        var someEffectsHidden = false
        var hiddenSources: RevealKey? = null
        val facts = when (val f = entry.facts) {
            is ItemFacts -> {
                val sourcesRevealed = revealed.has(RevealIds.sources(id))
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
            is ClassFacts -> f.copy(levels = f.levels.map { l -> l.copy(features = l.features.filter { it.id == null || linkVisible(it.id!!) }) })
            is RaceFacts -> f.copy(features = f.features.filter { it.id == null || linkVisible(it.id!!) })
            is SubraceFacts -> f.copy(features = f.features.filter { it.id == null || linkVisible(it.id!!) })
            is FeatFacts -> f.copy(features = f.features.filter { it.id == null || linkVisible(it.id!!) })
            is BackgroundFacts -> f
            is SubclassFacts -> f.copy(
                levels = f.levels.map { l ->
                    l.copy(
                        features = l.features.filter { it.id == null || linkVisible(it.id!!) },
                        choices = l.choices.map { c ->
                            c.copy(options = c.options.filter { it.id == null || linkVisible(it.id!!) }.map { o -> o.copy(grants = o.grants.filter(::linkVisible)) })
                        },
                        spells = l.spells.filter { linkVisible(it.spell) },
                    )
                },
            )
            // Les sections du codex sont filtrées une à une ci-dessous, jamais transmises en bloc.
            is LoreFacts -> f.copy(sections = emptyList())
        }
        val sources = (facts as? ItemFacts)?.sources.orEmpty()

        val blocks = entry.blocks.map { block ->
            block.gated(RevealIds.block(id, block.id), block.tag, profile, revealed.has(RevealIds.block(id, block.id)), showActOfHidden)
        }
        val sections = (entry.facts as? LoreFacts)?.sections.orEmpty().map { section ->
            val key = RevealIds.block(id, section.id)
            section.gated(key, section.tag, profile, revealed.has(key), showActOfHidden)
        }
        // Références : seulement celles que citent les sections visibles.
        val cited = sections.filterIsInstance<Gated.Visible<LoreSection>>().flatMap { s -> s.value.paragraphs.flatMap { p -> p.refs.map { it.ref } } }.toSet()
        val references = if (cited.isEmpty()) emptyList() else store.references().filterKeys { it in cited }.values.sortedBy { it.id }
        return Gated.Visible(
            EntryView(
                id = entry.id, type = entry.type, name = entry.name, aliases = entry.aliases,
                licence = entry.licence, attribution = attribution,
                facts = facts, sources = sources, hiddenSources = hiddenSources, someEffectsHidden = someEffectsHidden,
                blocks = blocks, sections = sections, references = references, links = links, unknown = entry.unknown,
            ),
        )
    }

    private fun titleVisible(row: EntryRow, profile: SpoilerProfile, revealed: Set<String>) = titleVisible(row.id, row.titleTag, profile, revealed)

    private fun titleVisible(id: ContentId, titleTag: SpoilerTag, profile: SpoilerProfile, revealed: Set<String>) =
        SpoilerGate.isVisible(titleTag, profile) || SpoilerGate.isVisible(titleTag, profile, revealed.has(RevealIds.title(id)) || revealed.has(RevealIds.entry(id)))

    private fun effectTargets(effect: Effect): List<ContentId> = when (effect) {
        is Grants -> listOf(effect.target)
        is WearerCondition -> listOf(effect.condition)
        else -> emptyList()
    }
}

/** Relations dont la cible a servi à rédiger les effets de la fiche : sa page entre dans l'attribution. */
private val ATTRIBUTED_RELATIONS = setOf(Relation.APPLIES, Relation.GRANTS)

/** Le joueur a-t-il dévoilé ce contenu ? [raw] est une clé de [RevealIds], comparée par son jeton. */
private fun Set<String>.has(raw: String): Boolean = isNotEmpty() && RevealKey.of(raw).token in this
