package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.ConditionFacts
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatureFacts
import fr.carnetdefaerun.core.model.content.FeatureKind
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.PlaceFacts
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.pipeline.i18n.NameResolver
import fr.carnetdefaerun.pipeline.wiki.Template
import fr.carnetdefaerun.pipeline.wiki.Wikitext

/** Lieux du jeu (pages « Location page »), avec leur acte. */
class PlaceNormalizer(private val corpus: Corpus, private val names: NameResolver) {
    /** Nom de lieu tel qu'employé dans les objets (titre de page ou paramètre `name`) → acte. */
    val acts: Map<String, Act?> by lazy {
        corpus.pages(Collection.LOCATIONS).flatMap { p ->
            val act = Parsing.act(p.template["act"])
            listOfNotNull(p.title to act, p.template["name"]?.let { Wikitext.plain(it) to act })
        }.toMap()
    }

    fun normalize(): List<Entry> = corpus.pages(Collection.LOCATIONS).map { page ->
        val t = page.template
        val id = Corpus.id("place", page.title)
        val act = Parsing.act(t["act"])
        val region = t["region"]?.let(Wikitext::plain)?.takeIf { it in acts }?.let { Corpus.id("place", it) }
        val tag = SpoilerTag.of(act ?: Act.ACT_3)
        Entry(
            id = id,
            type = EntryType.PLACE,
            name = names.resolve(id, t["name"]?.let(Wikitext::plain) ?: page.title),
            tag = tag,
            titleTag = tag,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(page.attribution),
            facts = PlaceFacts(act = act, region = region),
            links = listOfNotNull(region?.let { Link(it, Relation.LOCATED_IN) }),
            unknown = if (act == null) listOf("act") else emptyList(),
        )
    }
}

/**
 * Capacités (passives, actions, actions d'arme) et états, importés seulement quand une entrée
 * retenue y renvoie. Leurs effets sont rédigés par le projet (lot d'écriture), jamais repris.
 */
class FeatureNormalizer(private val corpus: Corpus, private val names: NameResolver) {
    private val passives: Map<ContentId, WikiPage> by lazy {
        corpus.pages(Collection.PASSIVES).associateBy { Corpus.id("feature", it.title) }
    }
    private val byId: Map<ContentId, WikiPage> by lazy {
        val actions = corpus.pages(Collection.FEATURES).filter { it.template["type"]?.lowercase() != "spell" }
        actions.associateBy { Corpus.id("feature", it.title) } + passives
    }

    fun has(id: ContentId) = id in byId

    fun entry(id: ContentId): Entry? {
        val page = byId[id] ?: return null
        val t = page.template
        val unknown = mutableListOf<String>()
        val kind = when {
            id in passives -> FeatureKind.PASSIVE
            t["type"]?.lowercase() == "weapon action" -> FeatureKind.WEAPON_ACTION
            else -> FeatureKind.ACTION
        }
        val costs = Parsing.costs(t["cost"], corpus.resourceAliases) ?: emptyList<Nothing>().also { unknown += "costs" }
        val applies = conditionNames(t).map { Corpus.id("condition", Corpus.baseTitle(it, "condition")) }
        return Entry(
            id = id,
            type = EntryType.FEATURE,
            name = names.resolve(id, t["name"]?.let(Wikitext::plain) ?: Corpus.baseTitle(page.title, "passive", "action")),
            // Étiquette calculée d'après les entrées qui y renvoient (Assembler).
            tag = SpoilerTag.LOCKED,
            titleTag = SpoilerTag.LOCKED,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(page.attribution),
            facts = FeatureFacts(kind, costs, Parsing.recharge(t["recharge"]), applies, t["uid"]),
            links = applies.map { Link(it, Relation.APPLIES) },
            unknown = (unknown + "effect_text").distinct(),
        )
    }
}

class ConditionNormalizer(private val corpus: Corpus, private val names: NameResolver) {
    private val byId: Map<ContentId, WikiPage> by lazy {
        corpus.pages(Collection.CONDITIONS).associateBy { Corpus.id("condition", Corpus.baseTitle(it.title, "condition")) }
    }

    fun entry(id: ContentId): Entry? {
        val page = byId[id] ?: return null
        val t = page.template
        return Entry(
            id = id,
            type = EntryType.CONDITION,
            name = names.resolve(id, t["name"]?.let(Wikitext::plain) ?: Corpus.baseTitle(page.title, "condition")),
            tag = SpoilerTag.LOCKED,
            titleTag = SpoilerTag.LOCKED,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(page.attribution),
            facts = ConditionFacts(
                groups = t["status groups"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                gameUid = t["uid"],
            ),
            unknown = listOf("effect_text"),
        )
    }
}

fun conditionNames(t: Template): List<String> =
    t.named.filterKeys { Regex("^condition( \\d)?$").matches(it) }.values
        .flatMap { it.split(',') }.map { Wikitext.plain(it) }.filter { it.isNotBlank() }.distinct()
