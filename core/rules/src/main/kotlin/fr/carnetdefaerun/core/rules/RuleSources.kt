package fr.carnetdefaerun.core.rules

/**
 * Pages de bg3.wiki d'où chaque règle est tirée, à la révision lue (docs/DECISIONS.md D-084).
 * Aucune valeur de ce module n'est écrite de mémoire : chacune renvoie à l'une de ces pages.
 */
object RuleSources {
    /** « Character creation », section « Ability scores » : budget, bornes et coût de l'achat de points, bonus +2 et +1. */
    const val CHARACTER_CREATION = "https://bg3.wiki/w/index.php?title=Character_creation&oldid=416995"

    /** « Abilities », section « Ability score modifiers » : calcul du modificateur et plafond de +10. */
    const val ABILITIES = "https://bg3.wiki/w/index.php?title=Abilities&oldid=439175"

    /** « Proficiency », section « Proficiency bonus » : +2, +3 au niveau 5, +4 au niveau 9 ; DD des sorts. */
    const val PROFICIENCY = "https://bg3.wiki/w/index.php?title=Proficiency&oldid=439299"

    /** « Spells » : catégories d'incantateurs, niveau d'incantateur effectif, table des emplacements, sorts préparés. */
    const val SPELLS = "https://bg3.wiki/w/index.php?title=Spells&oldid=433193"

    /** « Ranger », section « Level 12 » : un emplacement de niveau 3 de plus, écart voulu par BG3 (D-066). */
    const val RANGER = "https://bg3.wiki/w/index.php?title=Ranger&oldid=422145"

    /** « Armour Class » : formule par défaut, plafonds de Dextérité, formules sans armure. */
    const val ARMOUR_CLASS = "https://bg3.wiki/w/index.php?title=Armour_Class&oldid=439529"

    /** « Dice rolls » : 1 et 20 naturels selon le jet, avantage et désavantage, coups critiques. */
    const val DICE_ROLLS = "https://bg3.wiki/w/index.php?title=Dice_rolls&oldid=439285"

    /** « Advantage » : valeur passive d'une compétence, 10 + bonus, ±5 avec avantage ou désavantage. */
    const val ADVANTAGE = "https://bg3.wiki/w/index.php?title=Advantage&oldid=415831"

    /** « Hit points » : points de vie par classe, modificateur de Constitution rétroactif, capacités passives. */
    const val HIT_POINTS = "https://bg3.wiki/w/index.php?title=Hit_points&oldid=425339"

    /** « Classes », section « Multiclassing » : la multiclasse n'accorde jamais de maîtrise de jet de sauvegarde ; niveau 12 au total. */
    const val CLASSES = "https://bg3.wiki/w/index.php?title=Classes&oldid=417100"

    /** « Feats » : augmentations de caractéristique plafonnées à 20 ; Résilient donne la maîtrise de la sauvegarde choisie. */
    const val FEATS = "https://bg3.wiki/w/index.php?title=Feats&oldid=439281"

    /** « Tough » : +2 points de vie maximaux par niveau du personnage, rétroactivement, niveau 1 compris. */
    const val TOUGH = "https://bg3.wiki/w/index.php?title=Tough&oldid=438958"

    /** « Guidance » : +1d4 aux tests de caractéristique. */
    const val GUIDANCE = "https://bg3.wiki/w/index.php?title=Guidance&oldid=413520"

    /** « Bless » : +1d4 aux jets d'attaque et de sauvegarde. */
    const val BLESS = "https://bg3.wiki/w/index.php?title=Bless&oldid=404078"

    /** « Bardic Inspiration (class action) » : +1d6 à une attaque, un test ou une sauvegarde ; d8 au niveau 5 de barde, d10 au niveau 10. */
    const val BARDIC_INSPIRATION = "https://bg3.wiki/w/index.php?title=Bardic_Inspiration_(class_action)&oldid=416309"

    /** « Improved Critical Hit » : le résultat nécessaire au coup critique baisse de 1, effet cumulable. */
    const val IMPROVED_CRITICAL_HIT = "https://bg3.wiki/w/index.php?title=Improved_Critical_Hit&oldid=432678"

    /** « Inspiration » : un jet raté éligible se relance avec tous ses modificateurs, tant qu'il reste des points (4 au plus). */
    const val INSPIRATION = "https://bg3.wiki/w/index.php?title=Inspiration&oldid=419845"

    /** « Difficulty » : effets des modes sur les personnages du joueur (maîtrise, points de vie, multiclasse). */
    const val DIFFICULTY = "https://bg3.wiki/w/index.php?title=Difficulty&oldid=417706"

    /** « Attacks » (page « Attack roll ») : caractéristique des attaques d'arme (corps à corps, distance, finesse, lancer) et maîtrise. */
    const val ATTACKS = "https://bg3.wiki/w/index.php?title=Attacks&oldid=416178"
}
