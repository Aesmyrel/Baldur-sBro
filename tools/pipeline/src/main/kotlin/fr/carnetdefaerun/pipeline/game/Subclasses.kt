package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.Attribution
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.GrantKind
import fr.carnetdefaerun.core.model.content.GrantedSpell
import fr.carnetdefaerun.core.model.content.LevelChoice
import fr.carnetdefaerun.core.model.content.LevelFeature
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.Proficiencies
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.ResourceCount
import fr.carnetdefaerun.core.model.content.SubclassFacts
import fr.carnetdefaerun.core.model.content.SubclassLevel
import fr.carnetdefaerun.core.model.content.WeaponType
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.pipeline.i18n.NameResolver
import fr.carnetdefaerun.pipeline.wiki.CachedPage
import fr.carnetdefaerun.pipeline.wiki.Template
import fr.carnetdefaerun.pipeline.wiki.WikiClient
import fr.carnetdefaerun.pipeline.wiki.WikiTables
import fr.carnetdefaerun.pipeline.wiki.Wikitext

/**
 * Sous-classes : sections « Level N » de chaque page (capacités, choix, sorts accordés, maîtrises,
 * ressources, emplacements des tiers de lanceur). Comme pour les classes, seuls des faits sont retenus.
 */
class SubclassNormalizer(
    private val corpus: Corpus,
    private val names: NameResolver,
    private val features: FeatureNormalizer,
    private val spells: Set<ContentId>,
) {

    fun normalize(): List<Entry> {
        val pages = corpus.raw(Collection.SUBCLASSES)
        val overview = corpus.raw(Collection.CLASSES)[CLASSES_OVERVIEW_PAGE]?.text.orEmpty()
        val titles = subclassPages(overview)
        val entries = titles.mapNotNull { title -> pages[title]?.let { entry(title, it) } }
        // Deux sous-classes de même nom (« Wild Magic ») : identifiant suffixé par la classe parente.
        val clashes = entries.groupBy { it.id }.filterValues { it.size > 1 }.keys
        return crossCheckSlots(entries).map { e ->
            if (e.id !in clashes) e
            else e.copy(id = ContentId("${e.id.value}-${(e.facts as SubclassFacts).parentClass.value.substringAfter('.')}"))
        }.map { e -> e.copy(name = names.resolve(e.id, e.name.en)) }
    }

    private fun entry(title: String, page: CachedPage): Entry? {
        // Tableaux partagés inclus par `{{:Page}}` (ascendance draconique) : remplacés par le texte de la page.
        val pages = corpus.raw(Collection.SUBCLASSES)
        val text = Wikitext.stripComments(page.text).replace(Regex("\\{\\{:([^{}|]+)}}")) { m -> pages[m.groupValues[1].trim()]?.text?.let(::includable) ?: m.value }
        val parent = parentClass(text) ?: return null
        val id = subclassId(title)
        val unknown = mutableListOf<String>()
        val sections = Regex("(?m)^===\\s*Level (\\d+)\\s*===\\s*$").findAll(text).toList()
        if (sections.isEmpty()) unknown += "levels"
        var proficiencies = Proficiencies()
        val slots = sortedMapOf<Int, Int>()
        val levels = sections.mapIndexed { i, m ->
            val level = m.groupValues[1].toInt()
            val end = sections.getOrNull(i + 1)?.range?.first ?: nextHeading(text, m.range.last)
            val body = text.substring(m.range.last + 1, end)
            proficiencies = merge(proficiencies, proficienciesIn(body))
            slotsIn(body).forEach { (spellLevel, n) -> slots[spellLevel] = n }
            val slotList = if (slots.isEmpty()) emptyList() else (1..slots.lastKey()).map { slots[it] ?: 0 }
            level(level, body, if (Regex("Spell Slots? Unlocked").containsMatchIn(body)) slotList else emptyList(), text)
        }
        val tag = SpoilerTag(Act.PROLOGUE, SpoilerLevel.NONE, SpoilerLevel.NONE)
        // Liens vers tout ce que la sous-classe accorde : capacités, options et ce qu'elles apportent, sorts.
        val featureIds = levels.flatMap { l ->
            val options = l.features + l.choices.flatMap { it.options }
            options.mapNotNull { it.id } + options.flatMap { it.grants } + l.spells.map { it.spell }
        }
        return Entry(
            id = id,
            type = EntryType.SUBCLASS,
            name = names.resolve(id, Corpus.baseTitle(title, *QUALIFIERS)),
            tag = tag,
            titleTag = tag,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(Attribution(SOURCE_BG3WIKI, title, page.rev, WikiClient.url(title))),
            facts = SubclassFacts(parent, proficiencies, levels),
            links = (featureIds.distinct().map { Link(it, Relation.GRANTS) } + Link(parent, Relation.SUBCLASS_OF)).distinct(),
            unknown = unknown,
        )
    }

    /**
     * Les tiers de lanceur suivent la même progression d'emplacements : là où leurs pages divergent au même niveau,
     * les emplacements de ce niveau restent vides et signalés, faute de pouvoir trancher (D-067).
     */
    private fun crossCheckSlots(entries: List<Entry>): List<Entry> {
        val casters = entries.filter { e -> (e.facts as SubclassFacts).levels.any { it.spellSlots.isNotEmpty() } }
        val conflicts = casters.flatMap { e -> (e.facts as SubclassFacts).levels.filter { it.spellSlots.isNotEmpty() } }
            .groupBy { it.level }.filterValues { levels -> levels.map { it.spellSlots }.distinct().size > 1 }.keys
        if (conflicts.isEmpty()) return entries
        System.err.println("  ! emplacements des tiers de lanceur divergents aux niveaux $conflicts : laissés vides")
        return entries.map { e ->
            val f = e.facts as SubclassFacts
            if (e !in casters) e
            else e.copy(
                facts = f.copy(levels = f.levels.map { l -> if (l.level in conflicts) l.copy(spellSlots = emptyList()) else l }),
                unknown = (e.unknown + "spell_slots").distinct(),
            )
        }
    }

    /** Bloc d'une section de niveau : une définition (`; Terme` et ses lignes), ou une ligne isolée. */
    private data class Block(val term: String?, val lines: MutableList<String> = mutableListOf()) {
        val text get() = lines.joinToString("\n")
    }

    private fun blocks(body: String): List<Block> {
        val out = mutableListOf<Block>()
        var current: Block? = null
        var lastDefinition: Block? = null // un tableau séparé de sa définition par une ligne vide lui reste rattaché
        var tables = 0 // profondeur dans un tableau ({| … |}), rattaché au bloc en cours
        var braces = 0 // accolades ouvertes d'une boîte {{Flexbox …}}, rattachée au bloc en cours
        var dl = 0 // profondeur dans une liste de définitions HTML (<dl><dt>Terme</dt><dd>…</dd></dl>)
        fun attach(line: String) = (current ?: lastDefinition ?: Block(null).also { out += it }).also { current = it }.lines.add(line)
        for (line in body.lines()) {
            val t = line.trim()
            if (tables > 0 || braces > 0) {
                current?.lines?.add(line)
                if (tables > 0) tables += (if (t.trimStart(':').startsWith("{|")) 1 else 0) - (if (t.startsWith("|}")) 1 else 0)
                if (braces > 0) braces += Regex("\\{\\{").findAll(t).count() - Regex("}}").findAll(t).count()
                continue
            }
            val dt = Regex("<dt>(.*?)</dt>").find(t)?.groupValues?.get(1)
            if (dt != null) { current = Block(dt).also { out += it; lastDefinition = it } }
            if (dl > 0 || t.startsWith("<dl") || dt != null) {
                dl += Regex("<dl").findAll(t).count() - Regex("</dl>").findAll(t).count()
                attach(line)
                if (dl <= 0) { dl = 0; current = null }
                continue
            }
            // Ligne en gras servant d'intitulé (« '''Choose 3 [[Elemental Disciplines]]''': ») : une définition.
            val bold = Regex("^'''(.+?)'''\\s*:?\\s*$").find(t)?.groupValues?.get(1)
            when {
                bold != null -> current = Block(bold).also { out += it; it.lines += t; lastDefinition = it }
                t.startsWith(";") -> current = Block(t.removePrefix(";").trim()).also { out += it; it.lines += t; lastDefinition = it }
                t.trimStart(':').startsWith("{|") -> { attach(line); tables = 1 }
                t.startsWith("{{Flexbox", ignoreCase = true) -> {
                    attach(line)
                    braces = Regex("\\{\\{").findAll(t).count() - Regex("}}").findAll(t).count()
                }
                t.startsWith(":") || t.startsWith("*") || t.startsWith("{{div col", ignoreCase = true) ->
                    (current ?: Block(null).also { current = it; out += it }).lines += line
                t.isBlank() -> current = null
                t.startsWith("{{HorizontalRuleImage", ignoreCase = true) -> { current = null; lastDefinition = null }
                else -> { out += Block(null, mutableListOf(line)); current = null }
            }
        }
        return out
    }

    private fun level(level: Int, body: String, slots: List<Int>, page: String): SubclassLevel {
        val features = mutableListOf<LevelFeature>()
        val granted = mutableListOf<GrantedSpell>()
        val resources = mutableListOf<ResourceCount>()
        val choices = mutableListOf<LevelChoice>()

        for (block in blocks(body)) {
            val text = block.text
            block.lines.firstOrNull()?.let { resourceIn(it.trim()) }?.let { resources += it; continue }
            val name = block.term?.let(::termName) ?: WikiTables.parse(text).firstOrNull()?.caption?.let(::termName)
            val kind = block.term?.let(::spellListKind)
            // « Choose » dans l'intitulé ou sa description, jamais dans les cellules d'un tableau (« choose to… »).
            val prose = withoutTables(text)
            val chooses = Regex("(?i)\\b(choose|select)\\b").containsMatchIn(prose)
            val count = Regex("(?i)\\b(?:choose|select) (\\d+)").find(prose)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val tables = WikiTables.parse(text)
            val flexbox = Wikitext.templates(text).firstOrNull { it.name == "flexbox" }
            when {
                kind == SpellList.SKIP -> Unit
                // Boîte d'options, ou tableau présenté comme un choix (et non une table de tirage, comme la magie sauvage) :
                // un choix, nommé par le terme qui le précède.
                flexbox != null || (tables.isNotEmpty() && (chooses || tables.any { Regex("(?i)option|choice").containsMatchIn(it.caption.orEmpty()) })) -> {
                    val options = tables.flatMap(::tableOptions) + (flexbox?.let(::flexOptions) ?: emptyList())
                    if (options.isNotEmpty()) choices += LevelChoice(choiceName(name), options.distinctBy { it.name.en }, count)
                }
                // Liste où l'on choisit (« Choose an additional Cantrip ») : chaque élément est une option.
                tables.isNotEmpty() -> Unit
                chooses && kind != SpellList.EXPANDED -> {
                    // Options listées ailleurs sur la page (« Choose 3 [[#Manoeuvres|manoeuvres]] ») : celles de cette section.
                    val anchor = Regex("\\[\\[#([^\\]|]+)").find(text)?.groupValues?.get(1)?.trim()
                    val elsewhere = anchor?.let { sectionText(page, it) }
                    val options = when {
                        elsewhere != null && WikiTables.parse(elsewhere).isNotEmpty() -> WikiTables.parse(elsewhere).flatMap(::tableOptions)
                        else -> linkTemplates(elsewhere ?: text).mapNotNull { (target, label) -> optionOf(target, label) }
                    }
                    if (options.size >= 2) choices += LevelChoice(choiceName(name ?: anchor), options.distinctBy { it.name.en }, count)
                }
                kind != null -> linkTemplates(text).forEach { (target, _) ->
                    spellId(target)?.let { granted += GrantedSpell(it, if (kind == SpellList.EXPANDED) GrantKind.EXPANDED_LIST else GrantKind.ALWAYS_PREPARED) }
                }
                // Capacité décrite par une simple définition, sans fiche ni lien (« ; Abjuration Savant ») : son nom seul.
                name != null && block.term != null && linkTemplates(text).isEmpty() && !Regex("(?i)\\b(choose|select)\\b").containsMatchIn(prose) &&
                    name.lowercase() !in GENERIC_TERMS && !Regex("\\{\\{\\s*R(esource)?\\s*\\|", RegexOption.IGNORE_CASE).containsMatchIn(block.term) ->
                    features += LevelFeature(names.resolve(Corpus.id("feature", name), name))
                else -> for (tpl in Wikitext.templates(text)) {
                    if (tpl.name !in FEATURE_TEMPLATES) continue
                    val target = linkTarget(tpl) ?: continue
                    val spell = spellId(target)
                    when {
                        feature(target, tpl) != null -> features += feature(target, tpl)!!
                        // Sort accordé comme action (« Spell action|Speak with Animals ») : acquis à ce niveau.
                        spell != null && tpl.name == "spellaction" -> granted += GrantedSpell(spell, GrantKind.ALWAYS_PREPARED)
                    }
                }
            }
        }
        // Un même choix décrit deux fois (liste puis tableau détaillé) : une seule entrée, options les plus détaillées.
        val merged = choices.groupBy { it.name.en }.map { (_, same) ->
            same.first().copy(options = same.flatMap { it.options }.groupBy { it.name.en }.map { (_, o) -> o.maxBy { it.grants.size } })
        }
        val optionNames = merged.flatMap { c -> c.options.map { it.name.en } }.toSet()
        return SubclassLevel(level, features.distinct().filterNot { it.name.en in optionNames }, merged, granted.distinct(), resources.distinctBy { it.resource }, slots)
    }

    /** Texte d'une page incluse : ses parties `<onlyinclude>` s'il y en a, sinon tout. */
    private fun includable(text: String): String {
        val parts = Regex("<onlyinclude>(.*?)</onlyinclude>", RegexOption.DOT_MATCHES_ALL).findAll(text).map { it.groupValues[1] }.toList()
        val included = if (parts.isEmpty()) text.replace(Regex("<noinclude>.*?</noinclude>", RegexOption.DOT_MATCHES_ALL), "") else parts.joinToString("\n")
        return Wikitext.stripComments(included)
    }

    /** Corps de la section de niveau 2 intitulée [title] (« == Manoeuvres == »), ou `null`. */
    private fun sectionText(page: String, title: String): String? {
        val m = Regex("(?m)^==\\s*" + Regex.escape(title.trim()) + "\\s*==\\s*$", RegexOption.IGNORE_CASE).find(page) ?: return null
        val rest = page.substring(m.range.last + 1)
        return rest.substring(0, Regex("(?m)^==[^=]").find(rest)?.range?.first ?: rest.length)
    }

    /** Texte sans ses tableaux. */
    private fun withoutTables(text: String): String {
        var depth = 0
        return text.lines().filter { line ->
            val t = line.trim().trimStart(':')
            if (t.startsWith("{|")) depth++
            val keep = depth == 0
            if (t.startsWith("|}")) depth = maxOf(0, depth - 1)
            keep
        }.joinToString("\n")
    }

    /** Terme d'une définition sans ancres ni balises : « Defensive Tactics{{Anchor|…}} » → « Defensive Tactics ». */
    private fun termName(term: String): String =
        Wikitext.plain(term.replace(Regex("\\{\\{\\s*[Aa]nchor\\s*\\|[^{}]*}}"), "").replace(Regex("\\[\\[File:[^\\]]*]]"), "").replace(Regex("</?nowiki>"), ""))
            .replace(Regex("\\((?i:choose) \\d+\\)"), "").replace(Regex("^(?i:choose|select) \\d+ "), "")
            .substringBefore("&colon;").trim().trimEnd(':').trim()

    private fun choiceName(name: String?): fr.carnetdefaerun.core.model.content.Name {
        val label = name?.takeIf { it.isNotBlank() } ?: "Choice"
        return names.resolve(Corpus.id("feature", label), label)
    }

    /** Cibles et libellés des modèles de lien d'un texte, dans l'ordre. */
    private fun linkTemplates(text: String): List<Pair<String, String>> =
        Wikitext.templates(text).mapNotNull { tpl -> linkTarget(tpl)?.let { it to (tpl.arg(1)?.takeIf { a -> !a.contains('=') } ?: Wikitext.plain(it)) } }

    /** Options d'un tableau : une par ligne (première cellule), ou une par colonne si l'en-tête porte les options. */
    private fun tableOptions(table: fr.carnetdefaerun.pipeline.wiki.WikiTable): List<LevelFeature> {
        val grid = table.grid()
        if (grid.isEmpty()) return emptyList()
        val header = grid.first()
        val byColumn = header.size >= 2 && header.all { it.header } && header.all { optionLabel(it.text) != null } && grid.drop(1).none { it.firstOrNull()?.header == true }
        return if (byColumn) {
            header.mapIndexedNotNull { c, cell ->
                val (target, label) = optionLabel(cell.text) ?: return@mapIndexedNotNull null
                optionOf(target, label, grid.drop(1).mapNotNull { it.getOrNull(c)?.text }.joinToString("\n"))
            }
        } else {
            grid.drop(1).mapNotNull { row ->
                val (target, label) = row.firstOrNull()?.text?.let(::optionLabel) ?: return@mapNotNull null
                optionOf(target, label, row.drop(1).joinToString("\n") { it.text })
            }
        }
    }

    /** Options d'une boîte {{Flexbox}} : chaque segment commence par `; Option` et liste ce qu'il accorde. */
    private fun flexOptions(flexbox: Template): List<LevelFeature> =
        flexbox.positional.mapNotNull { segment ->
            val head = segment.lines().firstOrNull { it.trim().startsWith(";") } ?: return@mapNotNull null
            val (target, label) = optionLabel(head.trim().removePrefix(";")) ?: return@mapNotNull null
            optionOf(target, label, segment.lines().filter { it.trim().startsWith(":") }.joinToString("\n"))
        }

    /** Libellé d'une option : premier modèle de lien, sinon premier lien wiki, sinon une image légendée. */
    private fun optionLabel(cell: String): Pair<String, String>? {
        Wikitext.templates(cell).firstOrNull { linkTarget(it) != null }?.let { t -> return linkTarget(t)!! to (t.arg(1)?.takeIf { !it.contains('=') } ?: Wikitext.plain(linkTarget(t)!!)) }
        Wikitext.templates(cell).firstOrNull { it.name == "imagelink" }?.arg(1)?.let { return it to it }
        Wikitext.links(cell).firstOrNull { !it.target.startsWith("File:") }?.let { return it.target to it.label }
        // Option sans lien (« ! Commander's Strike ») : son texte.
        return Wikitext.plain(cell).takeIf { it.isNotBlank() && it.length <= 60 }?.let { it to it }
    }

    /** Une option : sa fiche (capacité ou sort) quand elle existe, et ce qu'elle accorde (liens de [grantsText]). */
    private fun optionOf(target: String, label: String, grantsText: String = ""): LevelFeature? {
        if (label.isBlank()) return null
        val page = target.substringBefore('#')
        val id = Corpus.id("feature", page).takeIf(features::has) ?: spellId(page)
        val grants = linkTemplates(grantsText).mapNotNull { (t, _) -> Corpus.id("feature", t.substringBefore('#')).takeIf(features::has) ?: spellId(t) }
        return LevelFeature(names.resolve(id ?: Corpus.id("feature", label), label), id, grants.distinct())
    }

    private enum class SpellList { ALWAYS, EXPANDED, SKIP }

    private fun spellListKind(term: String): SpellList? {
        val t = Wikitext.plain(term).lowercase()
        return when {
            t.startsWith("expanded spell list") -> SpellList.EXPANDED
            t.contains("domain spells") || t.contains("oath spells") || t.contains("subclass spells") || t.contains("circle of the land spells") ||
                t.startsWith("bonus cantrip") || t.startsWith("gain a cantrip") -> SpellList.ALWAYS
            // Listes où l'on choisit parmi les sorts de la classe : déjà portées par les fiches de sorts.
            Regex("^new .*(cantrips|spells) available").containsMatchIn(t) || t.startsWith("new level") ||
                t.startsWith("replacement spell") || t.startsWith("known spells") || t.startsWith("spellcasting") -> SpellList.SKIP
            else -> null
        }
    }

    private fun feature(target: String, tpl: Template): LevelFeature? {
        val page = target.substringBefore('#')
        val id = Corpus.id("feature", page).takeIf(features::has)
        if (id == null && spellId(target) != null) return null
        val label = tpl.arg(1)?.takeIf { tpl.name !in setOf("passive", "reaction", "spellaction") && !it.contains('=') } ?: Wikitext.plain(page)
        return LevelFeature(names.resolve(id ?: Corpus.id("feature", label), label), id)
    }

    private fun linkTarget(t: Template): String? = when (t.name) {
        "sai", "smsai", "mdsai", "lgsai", "pass", "lgpass", "passive", "reaction", "spellaction", "action", "bonusaction" -> t.arg(0)?.trim()
        else -> null
    }

    private fun spellId(target: String): ContentId? =
        Corpus.id("spell", Corpus.baseTitle(target.substringBefore('#'), "spell", "cantrip")).takeIf { it in spells }

    private fun resourceIn(raw: String): ResourceCount? {
        val line = raw.replace(Regex("\\{\\{\\s*[Aa]nchor\\s*\\|[^{}]*}}"), "")
        val m = Regex("^;\\s*\\{\\{\\s*R(?:esource)?\\s*\\|([^|}]+)[^}]*}}\\s*(?:&colon;|:)\\s*(\\d+)").find(line) ?: return null
        val code = m.groupValues[1].trim()
        val canonical = corpus.resourceAliases[code.lowercase()] ?: corpus.resourceAliases[code] ?: code
        return Resource.byWikiName(canonical)?.let { ResourceCount(it, m.groupValues[2].toInt()) }
    }

    /** Emplacements annoncés (« Level 1 Spell Slots Unlocked: 3 ») : totaux atteints à ce niveau. */
    private fun slotsIn(body: String): List<Pair<Int, Int>> =
        Regex("Level (\\d) Spell Slots? Unlocked\\s*(?:&colon;|:)\\s*(\\d+)").findAll(body).map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }.toList()

    /** Maîtrises accordées : modèles d'armure et d'arme sur les lignes « Gain … proficiency ». */
    private fun proficienciesIn(body: String): Proficiencies {
        val lines = body.lines().filter { it.contains("proficien", ignoreCase = true) }.joinToString("\n")
        val templates = Wikitext.templates(lines)
        return Proficiencies(
            armour = buildList {
                if (templates.any { it.name == "lightarmour" }) add(ArmourProficiency.LIGHT)
                if (templates.any { it.name == "mediumarmour" }) add(ArmourProficiency.MEDIUM)
                if (templates.any { it.name == "heavyarmour" }) add(ArmourProficiency.HEAVY)
                if (templates.any { it.name == "shields" }) add(ArmourProficiency.SHIELD)
            },
            simpleWeapons = templates.any { it.name == "simpleweaponsprof" },
            martialWeapons = templates.any { it.name == "martialweaponsprof" },
            weapons = templates.filter { it.name == "weapontype" }.mapNotNull { t -> t.arg(0)?.let(WeaponType::byWikiName) },
        )
    }

    private fun merge(a: Proficiencies, b: Proficiencies) = Proficiencies(
        armour = (a.armour + b.armour).distinct(),
        simpleWeapons = a.simpleWeapons || b.simpleWeapons,
        martialWeapons = a.martialWeapons || b.martialWeapons,
        weapons = (a.weapons + b.weapons).distinct(),
    )

    /** Classe parente : modèle `{{SubclassNote|Classe}}`, sinon la description de la page (« a subclass of X »). */
    private fun parentClass(text: String): ContentId? {
        val name = Wikitext.pageTemplate(text, "SubclassNote")?.arg(0)
            ?: Regex("subclass(?:es)? of \\[?\\[?([A-Z][a-z]+)").find(text)?.groupValues?.get(1)
        return name?.trim()?.takeIf { it in CLASS_PAGES }?.let { Corpus.id("class", it) }
    }

    private fun nextHeading(text: String, from: Int): Int =
        Regex("(?m)^==?[^=]").find(text, from)?.range?.first ?: text.length

    companion object {
        private val QUALIFIERS = arrayOf("barbarian subclass", "sorcerer subclass")
        /** Intitulés qui décrivent une règle de la classe, pas une capacité de la sous-classe. */
        private val GENERIC_TERMS = setOf("prepared spells", "spellcasting", "spellcasting ability")
        private val FEATURE_TEMPLATES = setOf("passive", "reaction", "spellaction", "action", "bonusaction", "lgpass", "pass", "sai", "lgsai", "mdsai", "smsai")

        /** Identifiant d'une sous-classe : nom de la page sans qualificatif (`Giant (barbarian subclass)` → `subclass.giant`). */
        fun subclassId(title: String): ContentId = Corpus.id("subclass", Corpus.baseTitle(title, *QUALIFIERS))
    }
}
