package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.Area
import fr.carnetdefaerun.core.model.content.Damage
import fr.carnetdefaerun.core.model.content.Range
import fr.carnetdefaerun.core.model.content.Rest
import fr.carnetdefaerun.core.model.content.SaveEffect
import fr.carnetdefaerun.pipeline.wiki.Template

/**
 * Mécanique d'une action, telle que le modèle `Feature page` la donne pour un sort comme pour une capacité :
 * portée, zone, jet d'attaque, sauvegarde, dégâts, durée. Les champs illisibles sont ajoutés à `unknown`.
 */
data class Mechanics(
    val range: Range?,
    val area: Area?,
    val attackRoll: Boolean,
    val save: Ability?,
    val saveEffect: SaveEffect?,
    val damage: List<Damage>,
    val weaponDamage: Boolean,
    val durationTurns: Int?,
    val durationUntil: Rest?,
) {
    companion object {
        fun of(t: Template, unknown: MutableList<String>, durationRequired: Boolean = true): Mechanics {
            val range = Parsing.range(t["range"], t["range m"])
            if (range == null && (t["range"] != null || t["range m"] != null)) unknown += "range"
            val area = Parsing.area(t["aoe"], t["aoe m"])
            if (area == null && t["aoe"] != null) unknown += "area"

            val damage = mutableListOf<Damage>()
            var weaponDamage = false
            val damageKeys = listOf("damage" to "damage type") + (1..7).map { "damage $it" to "damage $it type" }
            for ((dk, tk) in damageKeys) {
                val dice = t[dk] ?: continue
                if (dice.trim().equals("weapon", ignoreCase = true)) { weaponDamage = true; continue }
                // « Weapon » comme type : les dés prennent le type de l'arme tenue (type laissé vide).
                val type = t[tk]?.takeUnless { it.trim().equals("weapon", ignoreCase = true) }
                Parsing.damage(dice, type)?.let { damage += it } ?: run { unknown += "damage" }
            }

            val save = t["save"]?.let { Parsing.ability(it) ?: run { unknown += "save"; null } }
            val saveEffect = t["on save"]?.lowercase()?.let {
                when {
                    "half" in it -> SaveEffect.HALF
                    "negated" in it || "no damage" in it -> SaveEffect.NEGATES
                    else -> null
                }
            }
            if (save != null && saveEffect == null && damage.isNotEmpty()) unknown += "save_effect"

            val raw = t["duration"] ?: t["condition duration"]
            val (turns, until) = Parsing.duration(raw) ?: (null to null).also { if (durationRequired || raw != null) unknown += "duration" }
            return Mechanics(range, area, t["attack roll"] != null, save, saveEffect, damage, weaponDamage, turns, until)
        }
    }
}
