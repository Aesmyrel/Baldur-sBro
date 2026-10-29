package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.Damage
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.LearnVia
import fr.carnetdefaerun.core.model.content.LearnedAt
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.content.SpellFacts
import fr.carnetdefaerun.core.model.content.SpellSchool
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.pipeline.i18n.NameResolver
import fr.carnetdefaerun.pipeline.wiki.Template
import fr.carnetdefaerun.pipeline.wiki.Wikitext

/** Les douze classes du jeu (noms des pages de bg3.wiki). */
val CLASS_NAMES = listOf(
    "Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk",
    "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard",
)

/**
 * Sorts apprenables par une classe, une sous-classe ou une race (objectif V1, PLAN §8).
 * Les sorts réservés aux PNJ ou aux objets viendront avec les objets qui les accordent.
 */
class SpellNormalizer(private val corpus: Corpus, private val names: NameResolver) {

    fun isLearnable(page: WikiPage): Boolean {
        val t = page.template
        if (t["type"]?.lowercase() != "spell" || t.flag("npc only")) return false
        return t["classes"] != null || t.named.keys.any { it.startsWith("class learns at level") || it.startsWith("race learns at level") }
    }

    fun normalize(): List<Entry> = corpus.pages(Collection.FEATURES).filter(::isLearnable).map(::entry)

    private fun entry(page: WikiPage): Entry {
        val t = page.template
        val unknown = mutableListOf<String>()
        val englishName = t["name"]?.let(Wikitext::plain) ?: Corpus.baseTitle(page.title, "spell", "cantrip")
        val id = Corpus.id("spell", Corpus.baseTitle(page.title, "spell", "cantrip"))

        val level = when (val raw = t["level"]?.lowercase()) {
            "cantrip" -> 0
            else -> raw?.toIntOrNull() ?: run { unknown += "level"; -1 }
        }
        val school = t["school"]?.let { s -> SpellSchool.entries.firstOrNull { it.name.equals(s.trim(), ignoreCase = true) } }
        if (school == null) unknown += "school"
        val costs = Parsing.costs(t["cost"], corpus.resourceAliases) ?: emptyList<Nothing>().also { unknown += "costs" }
        val m = Mechanics.of(t, unknown)
        val higher = t["higher levels"]
        val upcast = higher?.let(::upcastDice) ?: emptyList()

        val links = mutableListOf<Link>()
        conditionNames(t).forEach { links += Link(Corpus.id("condition", Corpus.baseTitle(it, "condition")), Relation.APPLIES) }
        t["granted by items"]?.split(',')?.map { Wikitext.plain(it.substringBefore(':')) }?.filter { it.isNotBlank() }
            ?.forEach { links += Link(Corpus.id("item", it), Relation.GRANTED_BY) }

        return Entry(
            id = id,
            type = EntryType.SPELL,
            name = names.resolve(id, englishName),
            // Sort de classe ou de race : connu dès la création du personnage.
            tag = SpoilerTag.SAFE,
            titleTag = SpoilerTag.SAFE,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(page.attribution),
            facts = SpellFacts(
                level = level,
                school = school,
                costs = costs,
                range = m.range,
                area = m.area,
                attackRoll = m.attackRoll,
                save = m.save,
                saveEffect = m.saveEffect,
                damage = m.damage,
                weaponDamage = m.weaponDamage,
                upcastPerLevel = upcast,
                upcastable = higher != null,
                concentration = t.flag("concentration"),
                ritual = t.flag("ritual"),
                durationTurns = m.durationTurns,
                durationUntil = m.durationUntil,
                recharge = Parsing.recharge(t["recharge"]),
                learnedBy = learnedBy(t, unknown),
                scribable = t["scribing"]?.let { it.lowercase() in setOf("yes", "y", "true") },
                gameUid = t["uid"],
            ),
            links = links.distinct(),
            unknown = unknown.distinct(),
        )
    }

    /** Dés par niveau d'emplacement, seulement quand la phrase n'ajoute rien d'autre que ces dés. */
    private fun upcastDice(text: String): List<Damage> {
        val pattern = Regex("\\{\\{DamageText\\|([^|}]+)\\|([^|}]+)}}")
        if (!Regex("for each (spell )?slot level above", RegexOption.IGNORE_CASE).containsMatchIn(text)) return emptyList()
        val sentences = text.split(Regex("(?<=\\.)\\s+")).filter { it.isNotBlank() }
        if (sentences.size != 1) return emptyList()
        return pattern.findAll(text).mapNotNull { Parsing.damage(it.groupValues[1], it.groupValues[2]) }.toList()
    }

    private fun learnedBy(t: Template, unknown: MutableList<String>): List<LearnedAt> {
        val result = mutableListOf<LearnedAt>()
        for ((key, value) in t.named) {
            val m = Regex("^(class|race) learns at level (\\d+)$").matchEntire(key) ?: continue
            val racial = m.groupValues[1] == "race"
            val level = m.groupValues[2].toInt()
            for (part in Wikitext.splitTopLevel(value.replace(",", "|")).map(String::trim).filter(String::isNotEmpty)) {
                val who = Wikitext.plain(part.substringBefore(':')).trim()
                val note = part.substringAfter(':', "").trim()
                val source = sourceId(who, racial) ?: run { unknown += "learned_by"; null } ?: continue
                val (via, variant) = when {
                    racial -> LearnVia.RACIAL to note.ifEmpty { null }?.let(Wikitext::plain)
                    note.isEmpty() -> LearnVia.SPELL_LIST to null
                    Regex("^(Domain|Oath|Circle|Storm) Spell$").matches(note) -> LearnVia.SUBCLASS_SPELL to null
                    note.contains("Magical Secrets") -> LearnVia.MAGICAL_SECRETS to null
                    else -> LearnVia.OTHER to Wikitext.plain(note)
                }
                result += LearnedAt(source, level, via, variant)
            }
        }
        // Sans niveau précisé, `classes` donne la liste de classe (niveau minimal inconnu ici).
        if (result.isEmpty()) {
            t["classes"]?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.forEach { who ->
                sourceId(who, racial = false)?.let { result += LearnedAt(it, 0, LearnVia.SPELL_LIST) } ?: run { unknown += "learned_by" }
            }
        }
        return result.distinct()
    }

    private fun sourceId(who: String, racial: Boolean) = when {
        who.isBlank() -> null
        racial -> Corpus.id("race", who)
        who in CLASS_NAMES -> Corpus.id("class", who)
        else -> Corpus.id("subclass", Corpus.baseTitle(who, "barbarian subclass"))
    }
}
