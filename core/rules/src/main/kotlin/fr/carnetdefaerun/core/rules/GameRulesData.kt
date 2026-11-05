package fr.carnetdefaerun.core.rules

import fr.carnetdefaerun.core.model.content.BackgroundFacts
import fr.carnetdefaerun.core.model.content.ClassFacts
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.FeatFacts
import fr.carnetdefaerun.core.model.content.RaceFacts
import fr.carnetdefaerun.core.model.content.SubclassFacts
import fr.carnetdefaerun.core.model.content.SubraceFacts

/**
 * Profils de règles tirés des fiches de jeu (classes, sous-classes, dons, sous-races). Rien n'est recopié à la main :
 * points de vie, sauvegardes, incantation, niveaux de don et de sous-classe viennent des tableaux de progression.
 * Les capacités qui changent un calcul sont reconnues par leur identifiant stable.
 */
fun RulesData.Companion.from(entries: Iterable<Entry>): RulesData {
    val classes = mutableMapOf<String, ClassProfile>()
    val subclasses = mutableMapOf<String, SubclassProfile>()
    val feats = mutableMapOf<String, FeatProfile>()
    val dwarven = mutableSetOf<String>()
    val origins = mutableMapOf<String, OriginProfile>()
    for (entry in entries) {
        val id = entry.id.value
        when (val facts = entry.facts) {
            is ClassFacts -> classes[id] = classProfile(id, facts)
            is SubclassFacts -> {
                val firstSlots = facts.levels.firstOrNull { it.spellSlots.isNotEmpty() }?.level
                subclasses[id] = SubclassProfile(
                    id = id,
                    parentClass = facts.parentClass.value,
                    progression = if (firstSlots != null) CasterProgression.THIRD else CasterProgression.NONE,
                    spellcastingFrom = firstSlots ?: 1,
                    draconicResilience = facts.levels.any { l -> l.features.any { it.id?.value == DRACONIC_RESILIENCE } },
                )
            }
            is FeatFacts -> feats[id] = FeatProfile(
                id = id,
                abilityIncrease = facts.abilityIncrease,
                tough = facts.features.any { it.id?.value == TOUGH },
                resilient = id == RESILIENT,
            )
            is SubraceFacts -> {
                if (facts.features.any { it.id?.value == DWARVEN_TOUGHNESS }) dwarven += id
                origins[id] = OriginProfile(id, facts.proficiencies.skills.toSet(), facts.proficiencies.skillChoices)
            }
            is RaceFacts -> origins[id] = OriginProfile(id, emptySet(), facts.proficiencies.skillChoices).let { o ->
                // Une race qui laisse choisir n'impose rien : sa liste éventuelle est celle du choix.
                if (facts.proficiencies.skillChoices > 0) o.copy(skillList = facts.proficiencies.skills) else o.copy(skills = facts.proficiencies.skills.toSet())
            }
            is BackgroundFacts -> origins[id] = OriginProfile(id, facts.skills.toSet())
            else -> Unit
        }
    }
    return RulesData(classes, subclasses, feats, dwarven, origins)
}

private fun classProfile(id: String, facts: ClassFacts): ClassProfile {
    // Catégorie d'incantateur (Spells, « Spellcasters ») : la Magie de pacte à part ; sinon un lanceur complet a des
    // emplacements dès le niveau 1, un demi-lanceur à partir du niveau 2.
    val firstSlots = facts.levels.firstOrNull { it.spellSlots.isNotEmpty() }?.level
    val progression = when {
        facts.pactMagic -> CasterProgression.PACT
        firstSlots == 1 -> CasterProgression.FULL
        firstSlots == 2 -> CasterProgression.HALF
        else -> CasterProgression.NONE
    }
    val firstLevel = facts.levels.firstOrNull { it.level == 1 }?.features.orEmpty().mapNotNull { it.id?.value }
    return ClassProfile(
        id = id,
        hitDie = HitDie(facts.hitPointsAtFirstLevel, facts.hitPointsPerLevel),
        savingThrows = facts.savingThrows.toSet(),
        spellcastingAbility = facts.spellcastingAbility,
        progression = progression,
        spellcastingFrom = firstSlots ?: 1,
        pactSlots = if (facts.pactMagic) facts.levels.sortedBy { it.level }.map { it.spellSlots } else emptyList(),
        unarmoured = when {
            UNARMOURED_BARBARIAN in firstLevel -> UnarmouredFormula.BARBARIAN
            UNARMOURED_MONK in firstLevel -> UnarmouredFormula.MONK
            else -> null
        },
        featLevels = facts.levels.filter { it.feat }.map { it.level }.toSet(),
        subclassLevel = facts.subclassLevel,
        skillList = facts.proficiencies.skills,
        skillChoices = facts.proficiencies.skillChoices,
        multiclassSkillChoices = facts.multiclassProficiencies.skillChoices,
        cantripsKnown = facts.levels.sortedBy { it.level }.map { it.cantripsKnown },
        spellsKnown = facts.levels.sortedBy { it.level }.map { it.spellsKnown },
        prepares = id in PREPARED_CASTERS,
    )
}

/** Lanceurs qui préparent leurs sorts : liste de la page Spells, section « Prepared spells » ([RuleSources.SPELLS]). */
private val PREPARED_CASTERS = setOf("class.cleric", "class.druid", "class.paladin", "class.wizard")

/** Capacités et dons qui changent un calcul (identifiants stables des fiches). */
private const val UNARMOURED_BARBARIAN = "feature.unarmoured-defence-barbarian"
private const val UNARMOURED_MONK = "feature.unarmoured-defence-monk"
private const val DRACONIC_RESILIENCE = "feature.draconic-resilience"
private const val DWARVEN_TOUGHNESS = "feature.dwarven-toughness"
private const val TOUGH = "feature.tough"
private const val RESILIENT = "feat.resilient"
