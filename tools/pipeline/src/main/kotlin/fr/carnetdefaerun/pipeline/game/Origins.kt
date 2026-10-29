package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.AbilityIncrease
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.Attribution
import fr.carnetdefaerun.core.model.content.BackgroundFacts
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatFacts
import fr.carnetdefaerun.core.model.content.Facts
import fr.carnetdefaerun.core.model.content.LevelFeature
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.Proficiencies
import fr.carnetdefaerun.core.model.content.RaceFacts
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.content.SubraceFacts
import fr.carnetdefaerun.core.model.content.WeaponType
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.pipeline.i18n.NameResolver
import fr.carnetdefaerun.pipeline.wiki.CachedPage
import fr.carnetdefaerun.pipeline.wiki.TableCell
import fr.carnetdefaerun.pipeline.wiki.WikiClient
import fr.carnetdefaerun.pipeline.wiki.WikiTables
import fr.carnetdefaerun.pipeline.wiki.Wikitext

const val RACES_PAGE = "Races"
const val BACKGROUNDS_PAGE = "Backgrounds"
const val FEATS_PAGE = "Feats"

/** Tout ce qui se choisit à la création du personnage : visible dès le prologue. */
private val CREATION = SpoilerTag(Act.PROLOGUE, SpoilerLevel.NONE, SpoilerLevel.NONE)

/**
 * Races et sous-races, historiques et dons, lus dans les pages de synthèse de bg3.wiki (aucune table Cargo).
 * Seuls des faits sont retenus : vitesse, maîtrises, compétences, capacités (renvoyant à leur fiche),
 * augmentation de caractéristique. Les descriptions du wiki ne servent qu'à lire ces faits.
 */
class OriginNormalizer(
    private val corpus: Corpus,
    private val names: NameResolver,
    private val features: FeatureNormalizer,
    private val spells: Set<ContentId>,
) {
    private val pages get() = corpus.raw(Collection.ORIGINS)

    fun normalize(): List<Entry> = races() + backgrounds() + feats()

    // ---------- Races et sous-races ----------

    /** Grand tableau « Playable races » : race, sous-race, vitesse, maîtrises (race, sous-race), capacités (race, sous-race). */
    fun races(): List<Entry> {
        val page = pages[RACES_PAGE] ?: return emptyList()
        val text = Wikitext.stripComments(page.text)
        val table = WikiTables.parse(text).firstOrNull { t -> t.rows.flatten().any { Wikitext.plain(it.text).equals("Base Speed", ignoreCase = true) } }
            ?: return emptyList()
        val rows = table.grid().filter { it.size >= 7 && it[0].header && Wikitext.links(it[0].text).any { l -> !l.target.startsWith("File:") } }
        val byRace = rows.groupBy { row -> name(row[0]) }.filterKeys { it != null }
        return byRace.flatMap { (raceName, raceRows) ->
            val raceId = Corpus.id("race", raceName!!)
            val first = raceRows.first()
            // Vitesse de la race : celle que toutes ses lignes ont en commun.
            val raceSpeed = raceRows.mapNotNull { speed(it[2]) }.distinct().singleOrNull()
            val race = entry(raceId, EntryType.RACE, raceName, page, RaceFacts(raceSpeed, proficiencies(first[3].text), featureList(first[5].text)))
            // Une sous-race a sa propre cellule (`row[1] !== row[0]`) ; ses maîtrises et capacités sont celles des
            // colonnes « From Subrace » quand elles ne sont pas fusionnées avec celles de la race.
            val subraces = raceRows.filter { it[1] !== it[0] }.mapNotNull { row ->
                val subName = name(row[1]) ?: return@mapNotNull null
                val subId = Corpus.id("subrace", subName)
                entry(subId, EntryType.SUBRACE, subName, page, SubraceFacts(
                    parentRace = raceId,
                    speedMetres = speed(row[2]).takeIf { it != raceSpeed },
                    proficiencies = if (row[4] === row[3]) Proficiencies() else proficiencies(row[4].text),
                    features = if (row[6] === row[5]) emptyList() else featureList(row[6].text),
                ), parent = raceId)
            }
            listOf(race) + subraces
        }
    }

    private fun name(cell: TableCell): String? =
        Wikitext.links(cell.text).lastOrNull { !it.target.startsWith("File:") }?.label?.trim()?.takeIf { it.isNotBlank() }

    private fun speed(cell: TableCell): Double? =
        Wikitext.templates(cell.text).firstOrNull { it.name == "dist" }?.get("m")?.toDoubleOrNull()

    private fun proficiencies(text: String): Proficiencies {
        val templates = Wikitext.templates(text)
        val weapons = templates.filter { it.name == "weapontype" || it.name == "weaponicon" }.mapNotNull { t -> t.arg(0)?.let(WeaponType::byWikiName) } +
            Wikitext.links(text).mapNotNull { WeaponType.byWikiName(it.target) }
        val skills = templates.filter { it.name == "skill" }.mapNotNull { t -> t.arg(0)?.let(::skill) }
        val choices = Regex("(?i)\\b(one|two)\\s+\\[\\[skill").find(text)?.groupValues?.get(1)?.let { if (it.lowercase() == "one") 1 else 2 } ?: 0
        return Proficiencies(
            armour = buildList {
                if (templates.any { it.name == "lightarmour" }) add(ArmourProficiency.LIGHT)
                if (templates.any { it.name == "mediumarmour" }) add(ArmourProficiency.MEDIUM)
                if (templates.any { it.name == "heavyarmour" }) add(ArmourProficiency.HEAVY)
                if (templates.any { it.name == "shields" }) add(ArmourProficiency.SHIELD)
            },
            weapons = weapons.distinct(),
            skillChoices = choices,
            skills = skills.distinct(),
        )
    }

    /** Capacités d'une case : modèles Pass, SAI (hors sorts, déjà rattachés par leur fiche) et liens en gras. */
    private fun featureList(text: String): List<LevelFeature> {
        val found = mutableListOf<Pair<String, String>>()
        for (t in Wikitext.templates(text)) {
            if (t.name !in setOf("pass", "sai", "mdpass", "mdsai", "lgpass", "lgsai")) continue
            val target = t.arg(0)?.trim() ?: continue
            if (spellId(target) != null) continue
            found += target to (t.arg(1)?.takeIf { !it.contains('=') } ?: Corpus.baseTitle(target, "passive feature", "passive", "action"))
        }
        Regex("'''\\[\\[([^\\]|]+)(?:\\|([^\\]]+))?]]'''").findAll(text).forEach { m ->
            found += m.groupValues[1].trim() to m.groupValues[2].ifBlank { m.groupValues[1] }.trim()
        }
        return found.distinctBy { it.first }.map { (target, label) ->
            val id = Corpus.id("feature", target).takeIf(features::has)
            LevelFeature(names.resolve(id ?: Corpus.id("feature", label), label), id)
        }
    }

    // ---------- Historiques ----------

    /** Sections « == Historique == » : la ligne des compétences (« Skill proficiencies » ou « Improves »). */
    fun backgrounds(): List<Entry> {
        val page = pages[BACKGROUNDS_PAGE] ?: return emptyList()
        val text = Wikitext.stripComments(page.text)
        val sections = Regex("(?m)^==\\s*([^=]+?)\\s*==\\s*$").findAll(text).toList()
        return sections.mapIndexedNotNull { i, m ->
            val body = text.substring(m.range.last, sections.getOrNull(i + 1)?.range?.first ?: text.length)
            val line = body.lines().firstOrNull { it.contains("Skill proficiencies", ignoreCase = true) || it.contains("Improves:", ignoreCase = true) } ?: return@mapIndexedNotNull null
            val skills = Wikitext.links(line).mapNotNull { skill(it.target) }
            if (skills.isEmpty()) return@mapIndexedNotNull null
            // « Haunted One (Dark Urge only) » : le nom de l'historique est celui de sa page principale.
            val title = Wikitext.pageTemplate(body, "Main")?.arg(0)?.trim() ?: m.groupValues[1].substringBefore('(').trim()
            entry(Corpus.id("background", title), EntryType.BACKGROUND, title, page, BackgroundFacts(skills.distinct()))
        }
    }

    // ---------- Dons ----------

    /** Modèles `{{table feat}}` : nom, capacités (featureN), augmentation de caractéristique lue dans les descriptions. */
    fun feats(): List<Entry> {
        val page = pages[FEATS_PAGE] ?: return emptyList()
        return Wikitext.templates(Wikitext.stripComments(page.text)).filter { it.name == "tablefeat" }.mapNotNull { t ->
            val name = t["name"]?.let(Wikitext::plain)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val featureCells = t.named.filterKeys { Regex("feature\\d+").matches(it) }.toSortedMap(compareBy { it.removePrefix("feature").toInt() }).values
            val descriptions = t.named.filterKeys { it == "description" || Regex("description\\d+").matches(it) }.values.joinToString("\n")
            entry(Corpus.id("feat", name), EntryType.FEAT, name, page, FeatFacts(
                features = featureCells.flatMap(::featureList).distinctBy { it.id ?: it.name.en },
                abilityIncrease = abilityIncrease(descriptions),
            ))
        }
    }

    /**
     * Augmentation lue dans la phrase qui la décrit : « Your [[Charisma]] ability score increases by 1, to a maximum
     * of 20 », « Your Strength or Dexterity… », « Increase one ability score by 2, or two ability scores by 1 ».
     */
    private fun abilityIncrease(text: String): AbilityIncrease? {
        val plain = Wikitext.plain(text)
        val sentence = plain.split(Regex("(?<=[.;])\\s+|\\n")).firstOrNull {
            Regex("(?i)abilit(y|ies)( score)?s? (increases?|by)|increase (one|an|two)").containsMatchIn(it)
        } ?: return null
        val amount = Regex("(?i)by (\\d)").find(sentence)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val cap = Regex("(?i)maximum of (\\d+)").find(sentence)?.groupValues?.get(1)?.toIntOrNull() ?: 20
        val abilities = Ability.entries.filter { a -> Regex("(?i)\\b${a.name}\\b").containsMatchIn(sentence) }
        val splittable = Regex("(?i)two ability scores by 1").containsMatchIn(sentence)
        return AbilityIncrease(amount, abilities, cap, splittable)
    }

    // ---------- Outils ----------

    private fun entry(id: ContentId, type: EntryType, english: String, page: CachedPage, facts: Facts, parent: ContentId? = null): Entry {
        val title = when (type) {
            EntryType.RACE, EntryType.SUBRACE -> RACES_PAGE
            EntryType.BACKGROUND -> BACKGROUNDS_PAGE
            else -> FEATS_PAGE
        }
        val featureIds = when (facts) {
            is RaceFacts -> facts.features
            is SubraceFacts -> facts.features
            is FeatFacts -> facts.features
            else -> emptyList()
        }.mapNotNull { it.id }
        return Entry(
            id = id,
            type = type,
            name = names.resolve(id, english),
            tag = CREATION,
            titleTag = CREATION,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(Attribution(SOURCE_BG3WIKI, title, page.rev, WikiClient.url(title))),
            facts = facts,
            links = featureIds.distinct().map { Link(it, Relation.GRANTS) } + listOfNotNull(parent?.let { Link(it, Relation.VARIANT_OF) }),
        )
    }

    private fun spellId(target: String): ContentId? =
        Corpus.id("spell", Corpus.baseTitle(target.substringBefore('#'), "spell", "cantrip")).takeIf { it in spells }

    private fun skill(name: String): Skill? = Skill.entries.firstOrNull { it.name.replace('_', ' ').equals(name.trim(), ignoreCase = true) }
}
