package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.AbilityBonus
import fr.carnetdefaerun.core.model.content.ArmourClassBonus
import fr.carnetdefaerun.core.model.content.ArmourFacts
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.CheckAdvantage
import fr.carnetdefaerun.core.model.content.CriticalHitImmunity
import fr.carnetdefaerun.core.model.content.DamageResistance
import fr.carnetdefaerun.core.model.content.Effect
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Grants
import fr.carnetdefaerun.core.model.content.Handedness
import fr.carnetdefaerun.core.model.content.ItemCategory
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.ItemSource
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.Rarity
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.content.SaveAdvantage
import fr.carnetdefaerun.core.model.content.SavingThrowBonus
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.content.SkillBonus
import fr.carnetdefaerun.core.model.content.SourceMethod
import fr.carnetdefaerun.core.model.content.WeaponFacts
import fr.carnetdefaerun.core.model.content.WeaponProperty
import fr.carnetdefaerun.core.model.content.WeaponType
import fr.carnetdefaerun.core.model.content.WearerCondition
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.pipeline.i18n.NameResolver
import fr.carnetdefaerun.pipeline.wiki.Template
import fr.carnetdefaerun.pipeline.wiki.Wikitext

/** Ligne d'effet que le pipeline n'a pas su structurer : à rédiger en français par le projet. */
data class PendingEffect(val id: ContentId, val wikiTitle: String, val revision: Long, val line: String)

/**
 * Équipements et armes, de rareté Inhabituelle et au-delà, plus les objets d'histoire (PLAN §8).
 * Les lieux donnent l'acte ; le détail de l'obtention est étiqueté plus strictement que l'objet.
 */
class ItemNormalizer(
    private val corpus: Corpus,
    private val names: NameResolver,
    private val places: Map<String, Act?>,
) {
    val pending = mutableListOf<PendingEffect>()

    private val keptRarities = setOf(Rarity.UNCOMMON, Rarity.RARE, Rarity.VERY_RARE, Rarity.LEGENDARY, Rarity.STORY)

    /** Pages d'actions et de sorts, pour savoir si une action accordée est un sort ou une capacité. */
    private val featureTypes: Map<String, String> by lazy {
        corpus.pages(Collection.FEATURES).associate { Corpus.baseTitle(it.title, "spell", "cantrip").lowercase() to (it.template["type"] ?: "") }
    }

    fun normalize(): List<Entry> =
        (corpus.pages(Collection.EQUIPMENT).map { it to false } + corpus.pages(Collection.WEAPONS).map { it to true })
            .mapNotNull { (page, weapon) -> entry(page, weapon) }

    private fun rarity(value: String?): Rarity? = when (value?.trim()?.lowercase()) {
        "common" -> Rarity.COMMON
        "uncommon" -> Rarity.UNCOMMON
        "rare" -> Rarity.RARE
        "very rare" -> Rarity.VERY_RARE
        "legendary" -> Rarity.LEGENDARY
        "story" -> Rarity.STORY
        else -> null
    }

    private fun entry(page: WikiPage, isWeapon: Boolean): Entry? {
        val t = page.template
        val rarity = rarity(t["rarity"]) ?: return null
        if (rarity !in keptRarities) return null
        val unknown = mutableListOf<String>()
        val englishName = t["name"]?.let(Wikitext::plain) ?: page.title
        val id = Corpus.id("item", page.title)

        val category = if (isWeapon) ItemCategory.WEAPON else ItemCategory.byWikiName(t["type"].orEmpty()) ?: return null
        val enchantment = t["enchantment"]?.let { Parsing.int(it) }
        val weapon = if (isWeapon) weapon(t, unknown) else null
        val armour = if (isWeapon) null else armour(t, category)

        val links = mutableListOf<Link>()
        val effects = effects(page, unknown)
        effects.filterIsInstance<Grants>().forEach { links += Link(it.target, Relation.GRANTS) }
        effects.filterIsInstance<WearerCondition>().forEach { links += Link(it.condition, Relation.APPLIES) }
        weapon?.weaponActions?.forEach { links += Link(it, Relation.GRANTS) }

        val sources = sources(t, unknown)
        sources.mapNotNull { it.place }.distinct().forEach { links += Link(it, Relation.LOCATED_IN) }

        // Acte de l'objet : la plus précoce de ses sources ; inconnue → Acte III par prudence (signalé).
        val act = sources.mapNotNull { it.act }.minOrNull() ?: Act.ACT_3.also { unknown += "act" }
        val nameLevel = if (rarity == Rarity.STORY) SpoilerLevel.LIGHT else SpoilerLevel.NONE
        val titleTag = SpoilerTag(act, nameLevel, otherWorks = SpoilerLevel.NONE)

        return Entry(
            id = id,
            type = EntryType.ITEM,
            name = names.resolve(id, englishName),
            tag = titleTag,
            titleTag = titleTag,
            licence = LICENCE_BG3WIKI,
            attribution = listOf(page.attribution),
            facts = ItemFacts(
                category = category,
                rarity = rarity,
                enchantment = enchantment,
                weightKg = Parsing.number(t["weight kg"]),
                priceGold = Parsing.int(t["price"]),
                weapon = weapon,
                armour = armour,
                effects = effects,
                sources = sources,
                gameUid = t["uid"],
            ),
            links = links.distinct(),
            unknown = unknown.distinct(),
        )
    }

    private fun weapon(t: Template, unknown: MutableList<String>): WeaponFacts? {
        val type = WeaponType.byWikiName(Wikitext.plain(t["type"].orEmpty())) ?: run { unknown += "weapon_type"; return null }
        val handedness = when (t["handedness"]?.lowercase()) {
            "one-handed" -> Handedness.ONE_HANDED
            "two-handed" -> Handedness.TWO_HANDED
            "versatile" -> Handedness.VERSATILE
            else -> { unknown += "handedness"; Handedness.ONE_HANDED }
        }
        val damage = listOfNotNull(
            Parsing.damage(t["damage"], t["damage type"]) ?: run { unknown += "damage"; null },
            t["extra damage"]?.let { Parsing.damage(it, t["extra damage type"]) ?: run { unknown += "damage"; null } },
            t["extra damage 2"]?.let { Parsing.damage(it, t["extra damage 2 type"]) ?: run { unknown += "damage"; null } },
        )
        val properties = buildSet {
            if (t.flag("finesse")) add(WeaponProperty.FINESSE)
            if (t.flag("heavy")) add(WeaponProperty.HEAVY)
            if (t.flag("light")) add(WeaponProperty.LIGHT)
            if (t.flag("reach")) add(WeaponProperty.REACH)
            if (t.flag("thrown")) add(WeaponProperty.THROWN)
            if (t.flag("dippable")) add(WeaponProperty.DIPPABLE)
        }
        val actions = listOfNotNull(t["weapon actions"], t["special weapon actions"])
            .flatMap { it.split(',') }.map { Wikitext.plain(it) }.filter { it.isNotBlank() }
            .map { Corpus.id("feature", it) }
        return WeaponFacts(
            type = type,
            martial = t["category"]?.lowercase() == "martial",
            ranged = t["melee or ranged"]?.lowercase() == "ranged",
            handedness = handedness,
            damage = damage,
            versatileDamage = t["versatile damage"]?.let { Parsing.damage(it, t["damage type"]) },
            properties = properties,
            rangeMetres = Parsing.number(t["range m"]),
            weaponActions = actions.distinct(),
        )
    }

    private fun armour(t: Template, category: ItemCategory): ArmourFacts? {
        val proficiency = when (t["proficiency"]?.lowercase()) {
            "light armour" -> ArmourProficiency.LIGHT
            "medium armour" -> ArmourProficiency.MEDIUM
            "heavy armour" -> ArmourProficiency.HEAVY
            "shields" -> ArmourProficiency.SHIELD
            "musical instruments" -> ArmourProficiency.INSTRUMENT
            else -> null
        }
        val ac = Parsing.int(t["armour class"])
        val bonus = Parsing.int(t["armour class bonus"])
        val isArmour = category in setOf(ItemCategory.LIGHT_ARMOUR, ItemCategory.MEDIUM_ARMOUR, ItemCategory.HEAVY_ARMOUR, ItemCategory.SHIELD)
        if (!isArmour && ac == null && bonus == null && proficiency == null) return null
        return ArmourFacts(ac, bonus, proficiency, t.flag("stealth disadvantage"))
    }

    /** Effets structurés de `special` et `passives` ; le reste part dans [pending]. */
    private fun effects(page: WikiPage, unknown: MutableList<String>): List<Effect> {
        val t = page.template
        val id = Corpus.id("item", page.title)
        val effects = mutableListOf<Effect>()
        t["passives"]?.split(',')?.map { Wikitext.plain(it) }?.filter { it.isNotBlank() }
            ?.forEach { effects += Grants(Corpus.id("feature", it)) }
        val special = Wikitext.stripComments(t["special"].orEmpty())
        for (raw in special.lines()) {
            val line = raw.trim().trimStart('*', ';', ':', ' ').trim()
            if (line.isEmpty() || line.startsWith("{{HorizontalRuleImage")) continue
            val parsed = EffectLine.parse(line) { grantTarget(it) }
            if (parsed != null) effects += parsed
            else {
                unknown += "effects"
                pending += PendingEffect(id, page.title, page.page.rev, line)
            }
        }
        return effects.distinct()
    }

    /** Une action accordée par un objet : sort s'il a une page de sort, capacité sinon. */
    private fun grantTarget(name: String): ContentId {
        val base = Corpus.baseTitle(name, "spell", "cantrip")
        return if (featureTypes[base.lowercase()] == "spell") Corpus.id("spell", base) else Corpus.id("feature", name)
    }

    private fun sources(t: Template, unknown: MutableList<String>): List<ItemSource> {
        val keys = t.named.keys.mapNotNull { Regex("^where to find(\\d*)$").matchEntire(it)?.groupValues?.get(1) }
        return keys.mapNotNull { n ->
            val text = t["where to find$n"] ?: return@mapNotNull null
            val location = t["where to find$n location"]?.let(Wikitext::plain)
            val placeAct = location?.let { places[it] ?: places[Wikitext.plain(it)] }
            if (location != null && location !in places) unknown += "place"
            val method = method(text)
            val vendor = if (method == SourceMethod.VENDOR) vendorName(text) else null
            // Le lieu et la méthode sont descriptifs ; qui vend, porte ou récompense est un indice.
            val level = when (method) {
                SourceMethod.VENDOR, SourceMethod.CARRIED, SourceMethod.REWARD -> SpoilerLevel.LIGHT
                else -> SpoilerLevel.NONE
            }
            ItemSource(
                act = placeAct,
                place = location?.takeIf { it in places }?.let { Corpus.id("place", it) },
                method = method,
                vendor = vendor,
                tag = SpoilerTag(placeAct ?: Act.ACT_3, level, otherWorks = SpoilerLevel.NONE),
            )
        }
    }

    private fun method(text: String): SourceMethod {
        val s = Wikitext.plain(text).lowercase()
        return when {
            Regex("^(sold|can be bought|purchas)").containsMatchIn(s) || s.contains("sold by") -> SourceMethod.VENDOR
            Regex("reward|given by|received from|obtained from completing").containsMatchIn(s) -> SourceMethod.REWARD
            Regex("^(carried|worn|wielded|equipped|dropped|looted|held)").containsMatchIn(s) -> SourceMethod.CARRIED
            Regex("chest|crate|barrel|container|bag|backpack|sarcophag|coffin|box|urn|cabinet|wardrobe|drawer|pouch|safe\\b|trunk|stash").containsMatchIn(s) -> SourceMethod.CONTAINER
            Regex("^(on|in|found|lying|near|at|inside|under|behind)\\b").containsMatchIn(s) -> SourceMethod.GROUND
            else -> SourceMethod.UNKNOWN
        }
    }

    private fun vendorName(text: String): String? =
        Wikitext.templates(text).firstOrNull { it.name in setOf("charlink", "smcharlink", "mdcharlink") }?.arg(0)
            ?: Wikitext.links(text).firstOrNull()?.target
}

/** Lecture des lignes d'effet courantes de bg3.wiki, avec les modèles en place. */
object EffectLine {
    private val number = "([+-]\\d+)"

    fun parse(line: String, grant: (String) -> ContentId): Effect? {
        val templates = Wikitext.templates(line)
        val names = templates.map { it.name }
        val rest = line.replace(Regex("\\{\\{[^{}]*}}"), "§").replace(Regex("\\[\\[(?:[^\\[\\]|]*\\|)?([^\\[\\]]+)]]"), "$1").trim().trimEnd('.')

        // {{SpellAction|Misty Step|recharge=Short Rest}} seul sur la ligne
        if (names.firstOrNull() == "spellaction" && rest == "§") {
            val action = templates.first()
            val name = action.arg(0) ?: return null
            return Grants(grant(Wikitext.plain(name)), Parsing.recharge(action["recharge"]))
        }
        if (names.firstOrNull() == "passive" && rest == "§") {
            return Grants(Corpus.id("feature", Wikitext.plain(templates.first().arg(0) ?: return null)))
        }
        // {{Skill|Performance}} +1
        Regex("^§ $number$").matchEntire(rest)?.let { m ->
            val delta = m.groupValues[1].toInt()
            val t = templates.first()
            return when (t.name) {
                "skill" -> skill(t.arg(0))?.let { SkillBonus(it, delta) }
                "armourclass" -> ArmourClassBonus(delta)
                "savingthrow" -> SavingThrowBonus(delta)
                "ability" -> Parsing.abilityByName(Wikitext.plain(t.arg(0).orEmpty()))?.let { AbilityBonus(it, delta) }
                else -> null
            }
        }
        // {{SavingThrow}}s +1
        Regex("^§s $number$").matchEntire(rest)?.let { m ->
            if (names.first() == "savingthrow") return SavingThrowBonus(m.groupValues[1].toInt())
        }
        // [[Strength]] {{SavingThrow}}s +1
        Regex("^(Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma) §s? $number$").matchEntire(rest)?.let { m ->
            if (names.singleOrNull() == "savingthrow") return SavingThrowBonus(m.groupValues[2].toInt(), Parsing.abilityByName(m.groupValues[1]))
        }
        // [[Strength]] +2 (up to 22)
        Regex("^(Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma) $number(?: \\(up to (\\d+)\\))?$").matchEntire(rest)?.let { m ->
            if (templates.isEmpty()) return AbilityBonus(Parsing.abilityByName(m.groupValues[1])!!, m.groupValues[2].toInt(), cap = m.groupValues[3].toIntOrNull())
        }
        // {{Ability|Strength}} +2 (up to 23) — ou « +2, up to 20 »
        Regex("^§ $number(?:,? \\(?up to (\\d+)\\)?)?$").matchEntire(rest)?.let { m ->
            val t = templates.singleOrNull()?.takeIf { it.name == "ability" }
            val ability = t?.arg(0)?.let { Parsing.abilityByName(Wikitext.plain(it)) }
            if (ability != null) return AbilityBonus(ability, m.groupValues[1].toInt(), cap = m.groupValues[2].toIntOrNull())
        }
        // {{Advantage}} on [[Sleight of Hand]] Checks
        Regex("^§ on ([A-Za-z ]+) Checks$", RegexOption.IGNORE_CASE).matchEntire(rest)?.let { m ->
            if (names == listOf("advantage")) return skill(m.groupValues[1])?.let { CheckAdvantage(skill = it) }
                ?: Parsing.abilityByName(m.groupValues[1])?.let { CheckAdvantage(ability = it) }
        }
        // {{Cond|High Spirits}} seul : état appliqué au porteur
        if (rest == "§" && names.singleOrNull() in setOf("cond", "smcond", "condition")) {
            val name = templates.first().arg(0)?.let(Wikitext::plain) ?: return null
            return WearerCondition(Corpus.id("condition", Corpus.baseTitle(name, "condition")))
        }
        // {{Advantage}} on [[Constitution]] {{Saving Throw}}s
        Regex("^§ on (Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma) §s?$").matchEntire(rest)?.let { m ->
            if (names == listOf("advantage", "savingthrow")) return SaveAdvantage(Parsing.abilityByName(m.groupValues[1])!!)
        }
        // Grants [[Resistance]] to {{DamageType|Fire}} damage
        Regex("^(?:Grants )?Resistance to § damage$", RegexOption.IGNORE_CASE).matchEntire(rest)?.let {
            if (names.singleOrNull() == "damagetype") return Parsing.damageType(templates.first().arg(0))?.let(::DamageResistance)
        }
        if (templates.isEmpty() && Regex("^Attackers can(not|'t|’t) land critical hits on the wearer$", RegexOption.IGNORE_CASE).matches(rest)) {
            return CriticalHitImmunity
        }
        return null
    }

    private fun skill(name: String?): Skill? {
        val key = name?.let(Wikitext::plain)?.replace(" ", "_")?.lowercase() ?: return null
        return Skill.entries.firstOrNull { it.name.lowercase() == key }
    }
}
