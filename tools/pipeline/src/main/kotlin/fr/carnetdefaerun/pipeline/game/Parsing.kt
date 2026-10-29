package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.Area
import fr.carnetdefaerun.core.model.content.AreaShape
import fr.carnetdefaerun.core.model.content.Cost
import fr.carnetdefaerun.core.model.content.Damage
import fr.carnetdefaerun.core.model.content.DamageModifier
import fr.carnetdefaerun.core.model.content.DamageType
import fr.carnetdefaerun.core.model.content.Range
import fr.carnetdefaerun.core.model.content.RangeKind
import fr.carnetdefaerun.core.model.content.Recharge
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.Rest
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.pipeline.wiki.Wikitext

/**
 * Lecture des valeurs de paramètres de bg3.wiki. Toute valeur non reconnue renvoie null :
 * l'appelant la range dans les champs inconnus de l'entrée, jamais une valeur devinée.
 */
object Parsing {
    fun ability(value: String?): Ability? = when (value?.trim()?.lowercase()?.take(3)) {
        "str" -> Ability.STRENGTH
        "dex" -> Ability.DEXTERITY
        "con" -> Ability.CONSTITUTION
        "int" -> Ability.INTELLIGENCE
        "wis" -> Ability.WISDOM
        "cha" -> Ability.CHARISMA
        else -> null
    }

    fun abilityByName(value: String): Ability? = Ability.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

    fun damageType(value: String?): DamageType? =
        DamageType.entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }

    fun number(value: String?): Double? = value?.trim()?.replace(',', '.')?.toDoubleOrNull()

    fun int(value: String?): Int? = value?.trim()?.removePrefix("+")?.toIntOrNull()

    /** Acte d'une page de lieu (`prologue`, `one`, `two`, `three`). */
    fun act(value: String?): Act? = when (value?.trim()?.lowercase()) {
        "prologue" -> Act.PROLOGUE
        "one", "1" -> Act.ACT_1
        "two", "2" -> Act.ACT_2
        "three", "3" -> Act.ACT_3
        else -> null
    }

    fun recharge(value: String?): Recharge? = when (value?.trim()?.lowercase()) {
        "per turn", "once per turn", "turn" -> Recharge.PER_TURN
        "per battle", "per combat", "once per combat", "once per battle" -> Recharge.PER_COMBAT
        "short rest" -> Recharge.SHORT_REST
        "long rest", "until long rest" -> Recharge.LONG_REST
        else -> null
    }

    fun range(kind: String?, metres: String?): Range? {
        val m = number(metres)
        return when (kind?.trim()?.lowercase()) {
            null, "" -> m?.let { Range(RangeKind.DISTANCE, it) }
            "self" -> Range(RangeKind.SELF, m)
            "melee" -> Range(RangeKind.MELEE, m)
            "ranged" -> Range(RangeKind.RANGED, m)
            "weapon" -> Range(RangeKind.WEAPON, m)
            else -> null
        }
    }

    fun area(shape: String?, metres: String?): Area? {
        val s = when (shape?.trim()?.lowercase()) {
            "radius" -> AreaShape.RADIUS
            "sphere" -> AreaShape.SPHERE
            "circle" -> AreaShape.CIRCLE
            "cone" -> AreaShape.CONE
            "line" -> AreaShape.LINE
            "cube" -> AreaShape.CUBE
            "wall" -> AreaShape.WALL
            else -> return null
        }
        return Area(s, number(metres))
    }

    private val DICE_PART = Regex("^\\s*(\\d+d\\d+|\\d+)((?:\\s*[+-]\\s*\\d+(?:d\\d+)?)*)\\s*(?:\\+\\s*(spellcasting mod(?:ifier)?|spell mod|finesse mod))?\\s*$", RegexOption.IGNORE_CASE)

    /** `8d6`, `1d8 + 1`, `1d4 + Spellcasting mod`. */
    /** Dégâts écrits en un seul champ, type en dernier mot : `1d4 Cold`, `3 Poison` (dégâts supplémentaires des armes). */
    fun damageWithType(value: String?): Damage? {
        val raw = value?.let(Wikitext::plain)?.trim().orEmpty()
        val m = Regex("^(.*\\S)\\s+([A-Za-z]+)$").matchEntire(raw) ?: return null
        val type = damageType(m.groupValues[2]) ?: return null
        return damage(m.groupValues[1], type.name)
    }

    fun damage(dice: String?, type: String?): Damage? {
        val raw = dice?.let(Wikitext::plain)?.takeIf { it.isNotBlank() } ?: return null
        val m = DICE_PART.matchEntire(raw) ?: return null
        val normalized = (m.groupValues[1] + m.groupValues[2]).replace(" ", "")
        val modifier = when {
            m.groupValues[3].isEmpty() -> null
            m.groupValues[3].lowercase().startsWith("finesse") -> DamageModifier.FINESSE
            else -> DamageModifier.SPELLCASTING
        }
        val healing = type?.trim()?.equals("healing", ignoreCase = true) == true
        val damageType = damageType(type)
        if (!healing && damageType == null && !type.isNullOrBlank()) return null
        return Damage(normalized, damageType, healing, modifier)
    }

    /**
     * Coûts au format du modèle `{{Resource}}` : `action, spell3`, `ki:2`, `movement m:3`.
     * [aliases] vient de `Module:Resource/data` (alias → nom canonique).
     */
    fun costs(value: String?, aliases: Map<String, String>): List<Cost>? {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(',').filter { it.isNotBlank() }.map { term ->
            val name = term.substringBefore(':').trim().lowercase()
            val count = term.substringAfter(':', "").trim()
            val canonical = aliases[name] ?: return null
            val amount = if (count.isEmpty()) 1 else count.toIntOrNull() ?: return null
            val slot = Regex("^Level (\\d) (Warlock |Shadow )?Spell Slot$").matchEntire(canonical)
            when {
                slot != null -> Cost(
                    resource = when (slot.groupValues[2].trim()) {
                        "Warlock" -> Resource.PACT_SLOT
                        "Shadow" -> Resource.SHADOW_SPELL_SLOT
                        else -> Resource.SPELL_SLOT
                    },
                    amount = amount,
                    level = slot.groupValues[1].toInt(),
                )
                else -> Cost(Resource.byWikiName(canonical) ?: return null, amount)
            }
        }
    }

    /** Durée en tours, ou jusqu'au prochain repos. */
    fun duration(value: String?): Pair<Int?, Rest?>? {
        val v = value?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null to null
        v.toIntOrNull()?.let { return it to null }
        return when (v) {
            "until long rest", "long rest" -> null to Rest.LONG
            "until short rest", "short rest" -> null to Rest.SHORT
            else -> null
        }
    }

    /**
     * Table des alias de ressources, lue dans `Module:Resource/data` :
     * `{ aliases = {"bonus", "ba"}, name = "Bonus Action", … }`.
     */
    fun resourceAliases(lua: String): Map<String, String> {
        val block = Regex("aliases\\s*=\\s*\\{([^}]*)}\\s*,\\s*name\\s*=\\s*\"([^\"]+)\"")
        return block.findAll(lua).flatMap { m ->
            Regex("\"([^\"]+)\"").findAll(m.groupValues[1]).map { it.groupValues[1].lowercase() to m.groupValues[2] }
        }.toMap()
    }
}
