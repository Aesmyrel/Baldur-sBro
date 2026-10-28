package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatureFacts
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag

/** Ce que l'assemblage a dû écarter : liens vers des entrées absentes de la base. */
data class Dangling(val from: ContentId, val to: ContentId, val relation: Relation)

/**
 * Assemble les entrées normalisées : importe les capacités et états auxquels elles renvoient,
 * calcule les étiquettes qui dépendent d'autres entrées, ajoute les liens réciproques et écarte
 * les liens vers des entrées absentes (signalés).
 */
class Assembler(
    private val features: FeatureNormalizer,
    private val conditions: ConditionNormalizer,
    /** Ajustement final d'une entrée avant le calcul des liens (étiquette de nom imposée, par exemple). */
    private val adjust: (Entry) -> Entry = { it },
) {
    val dangling = mutableListOf<Dangling>()

    fun assemble(primary: List<Entry>): List<Entry> {
        val byId = linkedMapOf<ContentId, Entry>()
        primary.forEach { add(byId, it) }

        // 1. Capacités et états référencés, de proche en proche (une capacité peut appliquer un état).
        var frontier = byId.values.flatMap { it.links }.map { it.target }.toSet()
        while (frontier.isNotEmpty()) {
            val added = frontier.filter { it !in byId }.mapNotNull { id ->
                when (id.kind) {
                    "feature" -> features.entry(id)
                    "condition" -> conditions.entry(id)
                    else -> null
                }
            }
            added.forEach { add(byId, it) }
            frontier = added.flatMap { it.links }.map { it.target }.toSet()
        }

        // 2. Étiquette d'une capacité ou d'un état : la moins stricte de celles des entrées qui y mènent
        //    (on le découvre dès la première d'entre elles). Calculée jusqu'à stabilité.
        var changed = true
        while (changed) {
            changed = false
            for ((id, entry) in byId) {
                if (entry.type != EntryType.FEATURE && entry.type != EntryType.CONDITION) continue
                val owners = byId.values.filter { o -> o.id != id && o.links.any { it.target == id } && o.tag != SpoilerTag.LOCKED }
                if (owners.isEmpty()) continue
                val tag = owners.map { it.tag }.minWith(TAG_ORDER)
                val title = owners.map { it.titleTag }.minWith(TAG_ORDER)
                if (tag != entry.tag || title != entry.titleTag) {
                    byId[id] = entry.copy(tag = tag, titleTag = title)
                    changed = true
                }
            }
        }

        byId.replaceAll { _, e -> adjust(e) }

        // 3. Liens réciproques (« accordé par », « appliqué par » ne sont pas stockés à part : lus côté appli).
        val reverse = mutableMapOf<ContentId, MutableList<Link>>()
        for (entry in byId.values) for (link in entry.links) {
            val back = when (link.relation) {
                Relation.GRANTS -> Relation.GRANTED_BY
                Relation.GRANTED_BY -> Relation.GRANTS
                else -> null
            } ?: continue
            reverse.getOrPut(link.target) { mutableListOf() } += Link(entry.id, back)
        }

        // 4. Liens vers des entrées absentes écartés ; étiquette de chaque lien = la plus stricte des deux bouts.
        return byId.values.map { entry ->
            val links = (entry.links + reverse[entry.id].orEmpty()).distinctBy { it.target to it.relation }
            val kept = links.mapNotNull { link ->
                val target = byId[link.target]
                if (target == null) {
                    dangling += Dangling(entry.id, link.target, link.relation)
                    null
                } else {
                    link.copy(tag = entry.tag.strictest(target.tag))
                }
            }
            val facts = entry.facts
            val cleaned = if (facts is FeatureFacts) facts.copy(applies = facts.applies.filter { it in byId }) else facts
            entry.copy(links = kept.sortedWith(compareBy({ it.relation }, { it.target.value })), facts = cleaned)
        }.sortedBy { it.id.value }
    }

    private fun add(byId: MutableMap<ContentId, Entry>, entry: Entry) {
        val existing = byId[entry.id]
        require(existing == null) { "Identifiant en double : ${entry.id} (${existing?.attribution?.firstOrNull()?.title} et ${entry.attribution.firstOrNull()?.title})" }
        byId[entry.id] = entry
    }

    companion object {
        /** Ordre de « strictesse » : acte, puis niveau BG3, puis autres œuvres. */
        val TAG_ORDER: Comparator<SpoilerTag> = compareBy({ it.act }, { it.level }, { it.otherWorks })
    }
}
