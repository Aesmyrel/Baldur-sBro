package fr.carnetdefaerun.feature.tools

import fr.carnetdefaerun.core.rules.BonusDie
import fr.carnetdefaerun.core.rules.D20
import fr.carnetdefaerun.core.rules.RollKind
import fr.carnetdefaerun.core.rules.RollMode
import java.util.Locale

/** Ce que le joueur décrit de son jet. Les dés de bonus qui ne s'appliquent pas à ce type de jet sont ignorés. */
data class ProbabilityInput(
    val kind: RollKind = RollKind.ABILITY_CHECK,
    val modifier: Int = 3,
    val target: Int = 15,
    val mode: RollMode = RollMode.NORMAL,
    val bonusDice: Set<BonusDie> = emptySet(),
    val bardLevel: Int = 1,
    val halflingLuck: Boolean = false,
    /** Coup critique amélioré : nombre d'effets cumulés (attaques seulement). */
    val improvedCriticals: Int = 0,
    /** Sauvegarde de concentration ou de dialogue : les 1 et 20 naturels comptent. */
    val autoSaves: Boolean = false,
    val inspirationRerolls: Int = 0,
) {
    val appliedDice: List<BonusDie> get() = BonusDie.entries.filter { it in bonusDice && kind in it.kinds }
}

/** Ce qu'il faut au d20 seul (sans les dés de bonus) pour réussir. */
sealed interface Needed {
    /** Réussit sur [face] ou plus. */
    data class Face(val face: Int) : Needed
    /** Tout réussit, sauf un 1 naturel (attaque, test). */
    data object AllButOne : Needed
    /** Seul le haut du dé réussit : un 20 naturel, ou le seuil de critique d'une attaque ([face]). */
    data class OnlyNatural(val face: Int) : Needed
    /** Sauvegarde ordinaire : réussite certaine, ou échec certain (aucun naturel ne joue). */
    data object Always : Needed
    data object Never : Needed
}

data class ProbabilityResult(val success: Double, val critical: Double?, val needed: Needed)

/** Calcul exact du jet décrit, par le moteur de règles (énumération des faces, convolution des dés). */
fun ProbabilityInput.result(): ProbabilityResult {
    val crit = if (kind == RollKind.ATTACK) D20.critThreshold(improvedCriticals) else 20
    val success = D20.successChance(
        kind = kind,
        modifier = modifier,
        target = target,
        mode = mode,
        critThreshold = crit,
        halflingLuck = halflingLuck,
        autoSaves = autoSaves,
        bonusDice = appliedDice.map { it.sides(bardLevel) },
        inspirationRerolls = inspirationRerolls,
    )
    val critical = if (kind == RollKind.ATTACK) D20.criticalChance(mode, crit, halflingLuck) else null
    val natural = kind != RollKind.SAVING_THROW || autoSaves
    val face = target - modifier
    val needed = when {
        natural && kind == RollKind.ATTACK && face >= crit -> Needed.OnlyNatural(crit)
        natural && face >= 20 -> Needed.OnlyNatural(20)
        natural && face <= 2 -> Needed.AllButOne
        !natural && face <= 1 -> Needed.Always
        !natural && face > 20 -> Needed.Never
        else -> Needed.Face(face)
    }
    return ProbabilityResult(success, critical, needed)
}

/** Pourcentage affiché : sa valeur à la française, et s'il s'agit d'une borne (« moins de », « plus de »). */
data class Percent(val value: String, val bound: Bound = Bound.EXACT) {
    enum class Bound { EXACT, BELOW, ABOVE }
}

/**
 * Pourcentage à la française : une décimale sous 10 % et au-dessus de 90 %, sinon un entier. « 0 » et « 100 » ne
 * s'affichent que si l'échec ou la réussite sont certains, jugés sur la probabilité exacte et non sur l'arrondi ;
 * en deçà d'un millième, une borne (« moins de 0,1 », « plus de 99,9 »).
 */
fun percent(p: Double): Percent {
    val failure = 1 - p
    return when {
        p <= 0.0 -> Percent("0")
        failure <= 0.0 -> Percent("100")
        p < 0.0005 -> Percent(fr(0.1, 1), Percent.Bound.BELOW)
        failure < 0.0005 -> Percent(fr(99.9, 1), Percent.Bound.ABOVE)
        else -> {
            // Arrondi préalable au millionième de point : 0,575 × 100 vaut 57,4999… en virgule flottante, et doit s'afficher 58.
            val v = Math.round(p * 1e8) / 1e6
            if (v < 10 || v > 90) Percent(fr(v, 1)) else Percent(fr(v, 0))
        }
    }
}

private fun fr(v: Double, decimals: Int): String = String.format(Locale.FRANCE, "%.${decimals}f", v)
