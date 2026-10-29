package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.Attribution
import fr.carnetdefaerun.core.model.content.ClassFacts
import fr.carnetdefaerun.core.model.content.ClassLevel
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.LevelFeature
import fr.carnetdefaerun.core.model.content.LevelValue
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.Proficiencies
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.ResourceCount
import fr.carnetdefaerun.core.model.content.Skill
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

/** Les douze classes jouables (pages de bg3.wiki du même nom). */
val CLASS_PAGES = listOf("Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk", "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard")

/** Page qui porte le tableau des maîtrises de multiclasse. */
const val CLASSES_OVERVIEW_PAGE = "Classes"

/**
 * Classes : tableau de progression (niveaux 1 à 12), points de vie, maîtrises, caractéristique d'incantation,
 * lus dans le wikitexte des pages de classe (aucune table Cargo n'existe pour elles). Seuls des faits sont
 * retenus ; les noms des capacités renvoient aux fiches de capacités quand elles existent.
 */
class ClassNormalizer(private val corpus: Corpus, private val names: NameResolver, private val features: FeatureNormalizer) {

    fun normalize(): List<Entry> {
        val pages = corpus.raw(Collection.CLASSES)
        val multiclass = pages[CLASSES_OVERVIEW_PAGE]?.let { multiclassProficiencies(it.text) }.orEmpty()
        return CLASS_PAGES.map { title ->
            val page = checkNotNull(pages[title]) { "Page de classe absente du cache : $title" }
            entry(title, page, multiclass[title])
        }
    }

    private fun entry(title: String, page: CachedPage, multiclass: Proficiencies?): Entry {
        val id = Corpus.id("class", title)
        val text = Wikitext.stripComments(page.text)
        val unknown = mutableListOf<String>()
        val levels = progression(text, title)
        // Recoupement : les emplacements annoncés dans la section de chaque niveau doivent être ceux du tableau.
        val mismatches = slotMismatches(text, levels)
        mismatches.forEach { System.err.println("  ! $title : $it") }
        if (mismatches.isNotEmpty()) unknown += "spell_slots"
        val hp = hitPoints(text)
        if (hp == null) unknown += "hit_points"
        val subclassLevel = levels.firstOrNull { it.subclassChoice }?.level ?: 0.also { unknown += "subclass_level" }
        if (multiclass == null) unknown += "multiclass_proficiencies"
        val facts = ClassFacts(
            hitPointsAtFirstLevel = hp?.first ?: 0,
            hitPointsPerLevel = hp?.second ?: 0,
            savingThrows = abilities(definition(text, "Saving throw proficiencies")),
            spellcastingAbility = abilities(definition(text, "Spellcasting Ability")).firstOrNull() ?: spellAbilityFromKeyAbilities(text),
            proficiencies = startingProficiencies(text),
            multiclassProficiencies = multiclass ?: Proficiencies(),
            subclassLevel = subclassLevel,
            pactMagic = levels.any { l -> l.features.any { it.name.en.equals("Pact Magic", ignoreCase = true) } },
            levels = levels,
        )
        if (facts.savingThrows.size != 2) unknown += "saving_throws"
        if (levels.size != 12) unknown += "levels"
        // Une classe se choisit à la création du personnage : rien n'y est un spoiler.
        val tag = SpoilerTag(Act.PROLOGUE, SpoilerLevel.NONE, SpoilerLevel.NONE)
        return Entry(
            id = id,
            type = EntryType.CLASS,
            name = names.resolve(id, title),
            tag = tag,
            titleTag = tag,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(Attribution(SOURCE_BG3WIKI, title, page.rev, WikiClient.url(title))),
            facts = facts,
            links = levels.flatMap { it.features }.mapNotNull { it.id }.distinct().map { Link(it, Relation.GRANTS) },
            unknown = unknown,
        )
    }

    // ---------- Tableau de progression ----------

    private fun progression(text: String, classTitle: String): List<ClassLevel> {
        val section = section(text, Regex("^==\\s*Class [Pp]rogression\\s*==\\s*$", RegexOption.MULTILINE)) ?: return emptyList()
        val table = WikiTables.parse(section).firstOrNull() ?: return emptyList()
        val grid = table.grid()
        val headerIndex = grid.indexOfFirst { row -> row.all { it.header } && row.any { plain(it) == "level" } }
        if (headerIndex < 0) return emptyList()
        val columns = grid[headerIndex].map { column(it) }
        return grid.drop(headerIndex + 1).mapNotNull { row ->
            val level = row.firstOrNull()?.let { Regex("(\\d+)").find(Wikitext.plain(it.text))?.value?.toIntOrNull() } ?: return@mapNotNull null
            level(level, row, columns, classTitle)
        }.distinctBy { it.level }.sortedBy { it.level }
    }

    private sealed interface Column {
        data object Level : Column
        data object Proficiency : Column
        data object Features : Column
        /** Colonne « Subclass » (clerc) : on n'y lit que le choix de la sous-classe. */
        data object Subclass : Column
        data object Cantrips : Column
        data object Spells : Column
        data class Slot(val spellLevel: Int) : Column
        data class Res(val resource: Resource) : Column
        data class Value(val key: String) : Column
    }

    private fun column(cell: TableCell): Column {
        val raw = cell.text
        val p = plain(cell)
        Wikitext.templates(raw).firstOrNull { it.name == "resource" || it.name == "r" }?.let { t ->
            val code = t.arg(0)?.trim().orEmpty()
            val canonical = corpus.resourceAliases[code.lowercase()] ?: corpus.resourceAliases[code] ?: code
            Resource.byWikiName(canonical)?.let { return Column.Res(it) }
        }
        return when {
            p == "level" -> Column.Level
            p == "proficiency bonus" -> Column.Proficiency
            p == "features" -> Column.Features
            p == "subclass" -> Column.Subclass
            p.startsWith("cantrips known") -> Column.Cantrips
            p.startsWith("spells known") || p.startsWith("spells learned") -> Column.Spells
            ORDINAL.matches(p) -> Column.Slot(ORDINAL.find(p)!!.groupValues[1].toInt())
            else -> Column.Value(ContentFormat.slug(Wikitext.plain(raw)).replace('-', '_'))
        }
    }

    private fun level(level: Int, row: List<TableCell>, columns: List<Column>, classTitle: String): ClassLevel {
        var features = emptyList<LevelFeature>()
        var feat = false
        var subclassChoice = false
        var proficiency = 0
        var cantrips: Int? = null
        var spells: Int? = null
        val slots = sortedMapOf<Int, Int>()
        val resources = mutableListOf<ResourceCount>()
        val values = mutableListOf<LevelValue>()
        row.forEachIndexed { i, cell ->
            val value = Wikitext.plain(cell.text).takeUnless { it == "-" || it == "–" || it.isBlank() }
            when (val c = columns.getOrNull(i) ?: return@forEachIndexed) {
                Column.Level -> Unit
                Column.Proficiency -> proficiency = value?.removePrefix("+")?.toIntOrNull() ?: 0
                Column.Features -> {
                    val parsed = featureList(cell.text, classTitle)
                    feat = parsed.feat
                    subclassChoice = subclassChoice || parsed.subclassChoice
                    features = parsed.features
                }
                Column.Subclass -> if (featureList(cell.text, classTitle).subclassChoice) subclassChoice = true
                Column.Cantrips -> cantrips = value?.toIntOrNull()
                Column.Spells -> spells = value?.toIntOrNull()
                is Column.Slot -> slots[c.spellLevel] = value?.toIntOrNull() ?: 0
                is Column.Res -> value?.toIntOrNull()?.let { resources += ResourceCount(c.resource, it) }
                is Column.Value -> if (value != null) values += LevelValue(c.key, value)
            }
        }
        val slotList = if (slots.isEmpty()) emptyList() else (1..slots.lastKey()).map { slots[it] ?: 0 }.dropLastWhile { it == 0 }
        return ClassLevel(level, proficiency, features, feat, subclassChoice, cantrips, spells, slotList, resources, values)
    }

    private data class ParsedFeatures(val features: List<LevelFeature>, val feat: Boolean, val subclassChoice: Boolean)

    /** Capacités d'une case « Features » : modèles de lien (SAI, Pass, Icon link…) et liens simples, séparés par des virgules. */
    private fun featureList(raw: String, classTitle: String): ParsedFeatures {
        var feat = false
        var subclass = false
        val out = mutableListOf<LevelFeature>()
        for (token in Wikitext.splitTopLevel(raw.replace(",", "|")).map(String::trim).filter(String::isNotEmpty)) {
            val (target, label) = linkOf(token) ?: continue
            val name = label.ifBlank { target }
            val lower = name.lowercase()
            when {
                lower == "feat" || lower == "feats" || target.equals("Feats", ignoreCase = true) -> feat = true
                lower.startsWith("choose a subclass") -> subclass = true
                lower.startsWith("subclass feature") -> Unit // détaillé par la sous-classe
                lower == "spellcasting" -> Unit // porté par les colonnes de sorts et la caractéristique d'incantation
                else -> {
                    val page = target.substringBefore('#').ifBlank { classTitle }
                    val id = Corpus.id("feature", page).takeIf { features.has(it) && !page.equals(classTitle, ignoreCase = true) }
                    // Sans fiche, le nom est résolu sous un identifiant de capacité dérivé de son nom anglais (liste de traduction).
                    out += LevelFeature(names.resolve(id ?: Corpus.id("feature", name), name), id)
                }
            }
        }
        return ParsedFeatures(out.distinct(), feat, subclass)
    }

    /** Cible et libellé d'un élément de la case : modèle de lien, ou lien wiki ; `null` pour une icône seule ou un tiret. */
    private fun linkOf(token: String): Pair<String, String>? {
        val templates = Wikitext.templates(token).filterNot { it.name == "icon" }
        for (t in templates) {
            when (t.name) {
                "sai", "smsai", "pass", "spellaction", "passive" -> t.arg(0)?.let { return it to (t.arg(1) ?: Wikitext.plain(it)) }
                "smiconlink", "iconlink" -> t.arg(1)?.let { return it to (t.arg(2) ?: it.substringAfter('#')) }
            }
        }
        val withoutIcons = token.replace(Regex("\\{\\{\\s*[Ii]con\\s*\\|[^{}]*}}"), "").trim()
        Wikitext.links(withoutIcons).firstOrNull { !it.target.startsWith("File:") }?.let { link ->
            val target = Regex("\\[\\[([^\\]|]+)").find(withoutIcons)?.groupValues?.get(1)?.trim() ?: link.target
            return target to link.label
        }
        val text = Wikitext.plain(withoutIcons)
        return if (text.isBlank() || text == "-") null else text to text
    }

    // ---------- Caractéristiques et maîtrises ----------

    /** Points de vie au niveau 1 et par niveau suivant (sans le modificateur de Constitution). */
    private fun hitPoints(text: String): Pair<Int, Int>? {
        val block = definition(text, "Hit points") ?: return null
        val first = Regex("(?i)at level 1[^\\n]*?InfoBlob\\|(\\d+)").find(block)?.groupValues?.get(1)?.toIntOrNull()
        val next = Regex("(?i)on level up[^\\n]*?InfoBlob\\|(\\d+)").find(block)?.groupValues?.get(1)?.toIntOrNull()
        return if (first != null && next != null) first to next else null
    }

    private fun startingProficiencies(text: String): Proficiencies {
        val equipment = definition(text, "Equipment proficiencies").orEmpty()
        val templates = Wikitext.templates(equipment)
        val armour = buildList {
            if (templates.any { it.name == "lightarmour" }) add(ArmourProficiency.LIGHT)
            if (templates.any { it.name == "mediumarmour" }) add(ArmourProficiency.MEDIUM)
            if (templates.any { it.name == "heavyarmour" }) add(ArmourProficiency.HEAVY)
            if (templates.any { it.name == "shields" }) add(ArmourProficiency.SHIELD)
        }
        val weapons = templates.filter { it.name == "weapontype" }.mapNotNull { t -> t.arg(0)?.let(WeaponType::byWikiName) }
        val (choices, skills) = skillChoice(text)
        return Proficiencies(
            armour = armour,
            simpleWeapons = templates.any { it.name == "simpleweaponsprof" },
            martialWeapons = templates.any { it.name == "martialweaponsprof" },
            weapons = weapons.distinct(),
            skillChoices = choices,
            skills = skills,
        )
    }

    /** Caractéristique d'incantation, à défaut de définition : celle que les « caractéristiques clés » réservent aux sorts. */
    private fun spellAbilityFromKeyAbilities(text: String): Ability? {
        val m = Regex("(?im)^;\\s*Key abilit[^\\n]*\\n((?:\\s*:[^\\n]*\\n?)+)").find(text) ?: return null
        return m.groupValues[1].lines().firstOrNull { it.contains("spell", ignoreCase = true) }?.let { abilities(it).firstOrNull() }
    }

    /** Écarts entre le tableau et les sections « Level N » (« Level 3 Spell Slots Unlocked: 4 »). */
    private fun slotMismatches(text: String, levels: List<ClassLevel>): List<String> {
        val byLevel = levels.associateBy { it.level }
        val sections = Regex("(?m)^===\\s*Level (\\d+)\\s*===\\s*$").findAll(text).toList()
        return sections.flatMapIndexed { i, m ->
            val level = m.groupValues[1].toInt()
            val body = text.substring(m.range.last, sections.getOrNull(i + 1)?.range?.first ?: text.length)
            Regex("Level (\\d) Spell Slots? Unlocked\\s*(?:&colon;|:)\\s*(\\d+)").findAll(body).mapNotNull { s ->
                val spellLevel = s.groupValues[1].toInt()
                val announced = s.groupValues[2].toInt()
                val table = byLevel[level]?.spellSlots?.getOrNull(spellLevel - 1) ?: 0
                if (table == announced) null else "niveau $level, emplacements de niveau $spellLevel : tableau $table, section $announced"
            }.toList()
        }
    }

    private fun skillChoice(text: String): Pair<Int, List<Skill>> {
        val m = Regex("(?im)^;\\s*Skills?[^\\n]*\\(Choose (\\d+)\\)[^\\n]*\\n((?::[^\\n]*\\n?)+)").find(text) ?: return 0 to emptyList()
        val skills = Wikitext.templates(m.groupValues[2]).filter { it.name == "skill" }.mapNotNull { t -> t.arg(0)?.let(::skill) }
        return m.groupValues[1].toInt() to skills.distinct()
    }

    /** Tableau « Multiclass proficiencies » de la page Classes : classe → maîtrises. */
    fun multiclassProficiencies(text: String): Map<String, Proficiencies> {
        val section = section(Wikitext.stripComments(text), Regex("^=+\\s*Multiclass proficiencies\\s*=+\\s*$", RegexOption.MULTILINE)) ?: return emptyMap()
        val table = WikiTables.parse(section).firstOrNull() ?: return emptyMap()
        return table.grid().filter { row -> row.size >= 2 && !row[0].header }.associate { row ->
            val name = Wikitext.plain(row[0].text)
            name to multiclassCell(Wikitext.plain(row[1].text))
        }
    }

    private fun multiclassCell(value: String): Proficiencies {
        val parts = value.replace("*", "").split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() && it != "none" }
        fun count(word: String) = when (word) { "one" -> 1; "two" -> 2; "three" -> 3; else -> word.toIntOrNull() ?: 0 }
        return Proficiencies(
            armour = buildList {
                if ("light armour" in parts) add(ArmourProficiency.LIGHT)
                if ("medium armour" in parts) add(ArmourProficiency.MEDIUM)
                if ("heavy armour" in parts) add(ArmourProficiency.HEAVY)
                if ("shields" in parts) add(ArmourProficiency.SHIELD)
            },
            simpleWeapons = "simple weapons" in parts,
            martialWeapons = "martial weapons" in parts,
            weapons = parts.mapNotNull(WeaponType::byWikiName),
            skillChoices = parts.firstOrNull { it.endsWith("skill") || it.endsWith("skills") }?.substringBefore(' ')?.let(::count) ?: 0,
            instruments = parts.firstOrNull { it.endsWith("instrument") || it.endsWith("instruments") }?.substringBefore(' ')?.let(::count) ?: 0,
        )
    }

    // ---------- Outils ----------

    /** Contenu d'une définition `; Terme` (même ligne et lignes `:` qui suivent). */
    private fun definition(text: String, term: String): String? {
        val m = Regex("(?im)^;\\s*(?:\\{\\{[^}]*}}\\s*)?" + Regex.escape(term) + "[^\\n]*\\n?((?:\\s*:[^\\n]*\\n?)*)").find(text) ?: return null
        val sameLine = m.value.lineSequence().first().substringAfter(term, "")
        return (sameLine + "\n" + m.groupValues[1]).trim()
    }

    private fun section(text: String, heading: Regex): String? {
        val m = heading.find(text) ?: return null
        val level = m.value.trim().takeWhile { it == '=' }.length
        val rest = text.substring(m.range.last + 1)
        val end = Regex("^={2,$level}[^=].*$", RegexOption.MULTILINE).find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun abilities(value: String?): List<Ability> =
        Wikitext.templates(value.orEmpty()).filter { it.name == "ability" }.mapNotNull { t -> t.arg(0)?.let(Parsing::abilityByName) } +
            Wikitext.links(value.orEmpty()).mapNotNull { Parsing.abilityByName(it.target) }.let { linked ->
                if (Wikitext.templates(value.orEmpty()).any { it.name == "ability" }) emptyList() else linked
            }

    private fun skill(name: String): Skill? = Skill.entries.firstOrNull { it.name.replace('_', ' ').equals(name.trim(), ignoreCase = true) }

    private fun plain(cell: TableCell) = Wikitext.plain(cell.text).lowercase().trim()

    private companion object {
        val ORDINAL = Regex("^(\\d)(?:st|nd|rd|th)$")
    }
}
