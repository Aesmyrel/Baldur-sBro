package fr.carnetdefaerun.core.rules

import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ArmourProficiency

/** Armure de corps portée : sa classe d'armure de base, son poids et, pour quelques armures, la Dextérité entière. */
data class WornArmour(val baseClass: Int, val weight: ArmourProficiency, val fullDexterity: Boolean = false)

/** Formules d'un personnage sans armure (« Other formulas »). */
enum class UnarmouredFormula {
    /** Armure du mage ou Résilience draconique : 13 + Dextérité + bouclier. */
    MAGE_ARMOUR,

    /** Défense sans armure du barbare : 10 + Constitution + Dextérité + bouclier. */
    BARBARIAN,

    /** Défense sans armure du moine : 10 + Sagesse + Dextérité, sans bouclier. */
    MONK,
}

/**
 * Classe d'armure. Source : [RuleSources.ARMOUR_CLASS], sections « Formulas » et « Other formulas » :
 * - avec une armure : sa base + Dextérité, plafonnée à +2 en armure intermédiaire (+3 avec le don Maître des armures
 *   intermédiaires), ignorée en armure lourde ; quelques armures rares donnent la Dextérité entière (« Exotic Material ») ;
 * - sans armure : 10 + Dextérité, ou la plus haute des formules alternatives dont le personnage dispose ;
 *   celle du moine cesse de fonctionner avec un bouclier ;
 * - bouclier et autres bonus s'ajoutent.
 */
object ArmourClass {
    fun compute(
        dexterity: Int,
        armour: WornArmour? = null,
        shieldBonus: Int = 0,
        constitution: Int = 0,
        wisdom: Int = 0,
        unarmoured: Set<UnarmouredFormula> = emptySet(),
        mediumArmourMaster: Boolean = false,
        otherBonuses: Int = 0,
    ): Int {
        val body = if (armour != null) {
            val dex = when {
                armour.fullDexterity -> dexterity
                armour.weight == ArmourProficiency.HEAVY -> 0
                armour.weight == ArmourProficiency.MEDIUM -> minOf(dexterity, if (mediumArmourMaster) 3 else 2)
                else -> dexterity
            }
            armour.baseClass + dex + shieldBonus
        } else {
            val formulas = buildList {
                add(10 + dexterity + shieldBonus)
                if (UnarmouredFormula.MAGE_ARMOUR in unarmoured) add(13 + dexterity + shieldBonus)
                if (UnarmouredFormula.BARBARIAN in unarmoured) add(10 + constitution + dexterity + shieldBonus)
                if (UnarmouredFormula.MONK in unarmoured && shieldBonus == 0) add(10 + wisdom + dexterity)
            }
            formulas.max()
        }
        return body + otherBonuses
    }
}

/** Sorte de jet de d20 : elle décide des 1 et des 20 naturels. */
enum class RollKind { ATTACK, ABILITY_CHECK, SAVING_THROW }

/** Avantage et désavantage ne se cumulent pas et s'annulent. */
enum class RollMode {
    NORMAL, ADVANTAGE, DISADVANTAGE;

    companion object {
        fun of(advantage: Boolean, disadvantage: Boolean): RollMode = when {
            advantage == disadvantage -> NORMAL
            advantage -> ADVANTAGE
            else -> DISADVANTAGE
        }
    }
}

/**
 * Probabilités exactes d'un jet de d20 : chaque face est énumérée, rien n'est simulé.
 * Source : [RuleSources.DICE_ROLLS], sections « Natural 1s and 20s », « Advantage and disadvantage »,
 * « Critical hits » et « Critical misses ».
 * - Jet d'attaque et test de caractéristique : un 1 naturel échoue toujours, un 20 naturel réussit toujours.
 * - Jet de sauvegarde : ni l'un ni l'autre, sauf pour la concentration ou en dialogue ([autoSaves]).
 * - Attaque : un résultat naturel au moins égal au seuil de critique est un coup critique, qui touche toujours.
 * - Avantage : le meilleur de deux dés ; désavantage : le moins bon.
 * - Chance du halfelin : un 1 naturel est relancé. La page ne précise pas le cas d'un jet avec avantage ; on applique
 *   la relance à chaque dé, comme dans les règles de D&D (interprétation signalée, D-085).
 */
object D20 {
    /** Probabilité de chaque face (indices 1 à 20) d'un dé, puis du résultat retenu selon le mode. */
    fun faceProbabilities(mode: RollMode = RollMode.NORMAL, halflingLuck: Boolean = false): DoubleArray {
        val die = DoubleArray(21)
        for (f in 1..20) die[f] = 1.0 / 20
        if (halflingLuck) {
            // Le 1 est relancé une fois : il ne reste qu'avec probabilité 1/20 × 1/20.
            for (f in 1..20) die[f] = (if (f == 1) 0.0 else 1.0 / 20) + (1.0 / 20) * (1.0 / 20)
        }
        if (mode == RollMode.NORMAL) return die
        val kept = DoubleArray(21)
        for (a in 1..20) for (b in 1..20) {
            val face = if (mode == RollMode.ADVANTAGE) maxOf(a, b) else minOf(a, b)
            kept[face] += die[a] * die[b]
        }
        return kept
    }

    /**
     * Probabilité de réussir un jet de [modifier] contre [target] (DD ou classe d'armure).
     * [bonusDice] : faces des dés de bonus ajoutés au jet (4 pour Assistance ou Bénédiction, 6 à 10 pour
     * l'Inspiration bardique) ; leur somme est convoluée exactement avec le d20. Les 1 et 20 naturels et le coup
     * critique ne regardent que le d20. [inspirationRerolls] : relances d'Inspiration disponibles, chacune refaisant
     * le jet raté avec tous ses modificateurs ([RuleSources.INSPIRATION]).
     */
    fun successChance(
        kind: RollKind,
        modifier: Int,
        target: Int,
        mode: RollMode = RollMode.NORMAL,
        critThreshold: Int = 20,
        halflingLuck: Boolean = false,
        autoSaves: Boolean = false,
        bonusDice: List<Int> = emptyList(),
        inspirationRerolls: Int = 0,
    ): Double {
        val faces = faceProbabilities(mode, halflingLuck)
        val bonus = Dice.sumDistribution(bonusDice)
        val natural = kind != RollKind.SAVING_THROW || autoSaves
        var p = 0.0
        for (f in 1..20) {
            for ((b, pb) in bonus) {
                val success = when {
                    natural && f == 1 -> false
                    kind == RollKind.ATTACK && f >= critThreshold -> true
                    natural && f == 20 -> true
                    else -> f + modifier + b >= target
                }
                if (success) p += faces[f] * pb
            }
        }
        require(inspirationRerolls >= 0)
        // Chaque relance refait un jet indépendant : échouer à tous a pour probabilité (1 − p)^(n + 1).
        return 1 - Math.pow(1 - p, (inspirationRerolls + 1).toDouble())
    }

    /** Seuil de critique abaissé de 1 par effet de Coup critique amélioré, cumulable ([RuleSources.IMPROVED_CRITICAL_HIT]). */
    fun critThreshold(improvedCriticalHits: Int): Int = (20 - improvedCriticalHits).coerceIn(2, 20)

    /** Probabilité d'un coup critique (résultat naturel au moins égal au seuil). */
    fun criticalChance(mode: RollMode = RollMode.NORMAL, critThreshold: Int = 20, halflingLuck: Boolean = false): Double {
        val faces = faceProbabilities(mode, halflingLuck)
        return (critThreshold.coerceIn(2, 20)..20).sumOf { faces[it] }
    }

    /** Valeur passive d'une compétence : 10 + bonus, ±5 avec avantage ou désavantage. Source : [RuleSources.ADVANTAGE]. */
    fun passive(modifier: Int, mode: RollMode = RollMode.NORMAL): Int = 10 + modifier + when (mode) {
        RollMode.NORMAL -> 0
        RollMode.ADVANTAGE -> 5
        RollMode.DISADVANTAGE -> -5
    }
}

/** Dés de bonus de BG3 et jets auxquels ils s'ajoutent, d'après leurs pages. */
enum class BonusDie(val kinds: Set<RollKind>) {
    /** Assistance : +1d4 aux tests de caractéristique ([RuleSources.GUIDANCE]). */
    GUIDANCE(setOf(RollKind.ABILITY_CHECK)),

    /** Bénédiction : +1d4 aux jets d'attaque et de sauvegarde ([RuleSources.BLESS]). */
    BLESS(setOf(RollKind.ATTACK, RollKind.SAVING_THROW)),

    /** Inspiration bardique : +1d6, 1d8 ou 1d10 selon le niveau du barde, à une attaque, un test ou une sauvegarde ([RuleSources.BARDIC_INSPIRATION]). */
    BARDIC_INSPIRATION(setOf(RollKind.ATTACK, RollKind.ABILITY_CHECK, RollKind.SAVING_THROW));

    /** Faces du dé ; pour l'Inspiration bardique, selon le niveau de barde de celui qui l'accorde. */
    fun sides(bardLevel: Int = 1): Int = when (this) {
        GUIDANCE, BLESS -> 4
        BARDIC_INSPIRATION -> when {
            bardLevel >= 10 -> 10
            bardLevel >= 5 -> 8
            else -> 6
        }
    }
}

/** Distributions exactes de sommes de dés, par convolution (aucune simulation). */
object Dice {
    /** Probabilité de chaque somme de [sides] (un dé par entrée) ; somme nulle avec certitude sans dé. */
    fun sumDistribution(sides: List<Int>): Map<Int, Double> {
        var dist = mapOf(0 to 1.0)
        for (n in sides) {
            require(n >= 1) { "Dé sans face : $n" }
            val next = HashMap<Int, Double>()
            for ((sum, p) in dist) for (face in 1..n) next.merge(sum + face, p / n, Double::plus)
            dist = next
        }
        return dist
    }
}

/** Propriétés d'une arme qui décident de sa caractéristique. */
data class WeaponTraits(val ranged: Boolean = false, val finesse: Boolean = false, val thrown: Boolean = false)

/** Façon d'attaquer avec l'arme : au corps à corps, en tirant (arme à distance), en la lançant. */
enum class AttackMode { MELEE, RANGED, THROWN }

/**
 * Attaques d'arme. Source : [RuleSources.ATTACKS], section « Weapons » :
 * - corps à corps : Force ; tir avec une arme à distance : Dextérité ;
 * - arme de finesse : la plus haute valeur de Force ou de Dextérité ;
 * - arme lancée : Force, au corps à corps comme au lancer ; une arme de finesse lancée garde la Force pour le jet
 *   d'attaque mais prend la plus haute des deux pour les dégâts (écart à D&D signalé par la page, probable oubli) ;
 * - le bonus de maîtrise s'ajoute si l'attaquant maîtrise l'arme.
 */
object WeaponAttack {
    fun attackAbility(traits: WeaponTraits, mode: AttackMode, strength: Int, dexterity: Int): Ability = when {
        mode == AttackMode.THROWN -> Ability.STRENGTH
        mode == AttackMode.RANGED && traits.ranged -> Ability.DEXTERITY
        traits.finesse -> higher(strength, dexterity)
        else -> Ability.STRENGTH
    }

    fun damageAbility(traits: WeaponTraits, mode: AttackMode, strength: Int, dexterity: Int): Ability = when {
        traits.finesse -> higher(strength, dexterity)
        else -> attackAbility(traits, mode, strength, dexterity)
    }

    /** Bonus au jet d'attaque : modificateur de la caractéristique retenue, plus la maîtrise de l'arme, plus [otherBonuses]. */
    fun attackBonus(traits: WeaponTraits, mode: AttackMode, scores: Map<Ability, Int>, proficient: Boolean, proficiency: Int, otherBonuses: Int = 0): Int {
        val ability = attackAbility(traits, mode, scores.getValue(Ability.STRENGTH), scores.getValue(Ability.DEXTERITY))
        return abilityModifier(scores.getValue(ability)) + (if (proficient) proficiency else 0) + otherBonuses
    }

    /** À égalité de valeur, le modificateur est le même : la Force est retenue. */
    private fun higher(strength: Int, dexterity: Int) = if (dexterity > strength) Ability.DEXTERITY else Ability.STRENGTH
}
