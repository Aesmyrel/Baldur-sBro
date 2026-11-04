package fr.carnetdefaerun.core.rules

/** Niveau maximal d'un personnage : les tableaux de progression des classes s'arrêtent à 12. */
const val MAX_CHARACTER_LEVEL = 12

/**
 * Bonus de maîtrise selon le niveau du personnage (pas de la classe) : +2, +3 au niveau 5, +4 au niveau 9.
 * Source : [RuleSources.PROFICIENCY], section « Proficiency bonus ».
 */
fun proficiencyBonus(characterLevel: Int): Int {
    require(characterLevel in 1..MAX_CHARACTER_LEVEL) { "Niveau hors bornes : $characterLevel" }
    return when {
        characterLevel >= 9 -> 4
        characterLevel >= 5 -> 3
        else -> 2
    }
}

/** DD de sauvegarde des sorts : 8 + modificateur d'incantation + bonus de maîtrise. Source : [RuleSources.PROFICIENCY]. */
fun spellSaveDc(spellcastingModifier: Int, proficiency: Int): Int = 8 + spellcastingModifier + proficiency

/**
 * Bonus aux jets d'attaque des sorts : modificateur d'incantation + bonus de maîtrise. Sources : [RuleSources.SPELLS]
 * (« Spellcasting ability ») et [RuleSources.PROFICIENCY] (le bonus s'applique aux attaques des sorts).
 */
fun spellAttackBonus(spellcastingModifier: Int, proficiency: Int): Int = spellcastingModifier + proficiency

/** Sorts préparés : modificateur d'incantation + niveau de classe, au moins 1. Source : [RuleSources.SPELLS], « Prepared spells ». */
fun preparedSpells(spellcastingModifier: Int, classLevel: Int): Int = maxOf(1, spellcastingModifier + classLevel)

/** Part d'une classe dans le niveau d'incantateur effectif. Source : [RuleSources.SPELLS], « Spellcasters ». */
enum class CasterProgression(val numerator: Int, val denominator: Int) {
    FULL(1, 1),
    HALF(1, 2),
    THIRD(1, 3),

    /** Magie de pacte : ses niveaux sont exclus du niveau d'incantateur effectif. */
    PACT(0, 1),
    NONE(0, 1),
}

/**
 * Une classe du personnage, vue par l'incantation : sa progression, son niveau et le niveau où elle obtient
 * la capacité Incantation (le paladin et le rôdeur au niveau 2, l'escroc arcanique et le chevalier occulte au niveau 3,
 * d'après leurs tableaux de progression). Avant ce niveau, elle ne compte pas.
 */
data class CasterLevels(val progression: CasterProgression, val level: Int, val spellcastingFrom: Int = 1, val classId: String? = null)

object SpellSlots {
    /**
     * Emplacements de l'Incantation par niveau d'incantateur effectif (indice 0 : emplacements de niveau 1).
     * Source : [RuleSources.SPELLS], table « Spell Slots per Spell Level ».
     */
    private val TABLE: List<List<Int>> = listOf(
        listOf(2),
        listOf(3),
        listOf(4, 2),
        listOf(4, 3),
        listOf(4, 3, 2),
        listOf(4, 3, 3),
        listOf(4, 3, 3, 1),
        listOf(4, 3, 3, 2),
        listOf(4, 3, 3, 3, 1),
        listOf(4, 3, 3, 3, 2),
        listOf(4, 3, 3, 3, 2, 1),
        listOf(4, 3, 3, 3, 2, 1),
    )

    /**
     * Niveau d'incantateur effectif. Source : [RuleSources.SPELLS], « Spellcasting » : une seule classe
     * d'incantation donne son niveau, ou sa moitié ou son tiers arrondis au-dessus ; plusieurs classes donnent la
     * somme de leurs parts arrondie au-dessous. Les niveaux d'occultiste (Magie de pacte) sont exclus.
     */
    fun effectiveSpellcasterLevel(classes: List<CasterLevels>): Int {
        val casting = classes.filter { it.progression.numerator > 0 && it.level >= it.spellcastingFrom }
        return when (casting.size) {
            0 -> 0
            1 -> casting.single().let { c -> ceilDiv(c.level * c.progression.numerator, c.progression.denominator) }
            // Somme exacte des fractions (dénominateur commun 6), arrondie au-dessous.
            else -> casting.sumOf { c -> c.level * c.progression.numerator * (6 / c.progression.denominator) } / 6
        }
    }

    /** Emplacements pour un niveau effectif (vide à 0). */
    fun forLevel(effectiveLevel: Int): List<Int> {
        require(effectiveLevel in 0..MAX_CHARACTER_LEVEL) { "Niveau effectif hors bornes : $effectiveLevel" }
        return if (effectiveLevel == 0) emptyList() else TABLE[effectiveLevel - 1]
    }

    fun forClasses(classes: List<CasterLevels>): List<Int> {
        // Exception propre à BG3 : un rôdeur de niveau 12 (forcément sans autre classe) gagne un emplacement de
        // niveau 3 de plus que la table. Source : [RuleSources.RANGER], section « Level 12 ».
        val only = classes.singleOrNull()
        if (only != null && only.classId == "class.ranger" && only.level == MAX_CHARACTER_LEVEL) return listOf(4, 3, 4)
        return forLevel(effectiveSpellcasterLevel(classes))
    }

    private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b
}
