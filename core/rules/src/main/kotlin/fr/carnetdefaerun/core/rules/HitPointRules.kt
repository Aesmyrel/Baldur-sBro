package fr.carnetdefaerun.core.rules

/** Points de vie d'une classe : au premier niveau du personnage, puis à chaque niveau suivant. */
data class HitDie(val atFirstLevel: Int, val perLevel: Int)

/**
 * Points de vie maximaux. Source : [RuleSources.HIT_POINTS], sections « Determining a character's hit points » et
 * « Changing the maximum » :
 * - le premier niveau du personnage donne la valeur de départ de sa première classe, chaque niveau suivant le gain
 *   de la classe prise à ce niveau ;
 * - le modificateur de Constitution s'ajoute à chaque niveau, rétroactivement (le modificateur actuel compte pour
 *   tous les niveaux) ;
 * - Robustesse naine : +1 par niveau du personnage ; Résilience draconique : +1 par niveau d'ensorceleur.
 */
object HitPoints {
    /** [levels] : la classe de chaque niveau, dans l'ordre où ils ont été pris (le premier est la classe de départ). */
    fun maximum(
        levels: List<HitDie>,
        constitutionModifier: Int,
        dwarvenToughness: Boolean = false,
        draconicResilienceSorcererLevels: Int = 0,
    ): Int {
        require(levels.size in 1..MAX_CHARACTER_LEVEL) { "Nombre de niveaux hors bornes : ${levels.size}" }
        val classes = levels.first().atFirstLevel + levels.drop(1).sumOf { it.perLevel }
        val constitution = constitutionModifier * levels.size
        val features = (if (dwarvenToughness) levels.size else 0) + draconicResilienceSorcererLevels
        return classes + constitution + features
    }
}
