package fr.carnetdefaerun.pipeline

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.AbilityBonus
import fr.carnetdefaerun.core.model.content.AreaShape
import fr.carnetdefaerun.core.model.content.CheckAdvantage
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Cost
import fr.carnetdefaerun.core.model.content.CriticalHitImmunity
import fr.carnetdefaerun.core.model.content.Damage
import fr.carnetdefaerun.core.model.content.DamageModifier
import fr.carnetdefaerun.core.model.content.DamageResistance
import fr.carnetdefaerun.core.model.content.DamageType
import fr.carnetdefaerun.core.model.content.Grants
import fr.carnetdefaerun.core.model.content.Recharge
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.SaveAdvantage
import fr.carnetdefaerun.core.model.content.SavingThrowBonus
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.content.SkillBonus
import fr.carnetdefaerun.pipeline.game.EffectLine
import fr.carnetdefaerun.pipeline.game.Parsing
import fr.carnetdefaerun.pipeline.i18n.Lexicon
import org.junit.Test

class ParsingTest {
    // Extrait de la structure de Module:Resource/data (alias → nom canonique).
    private val aliases = Parsing.resourceAliases(
        """
        { aliases = {"action"}, name = "Action", },
        { aliases = {"bonus", "ba"}, name = "Bonus Action", },
        { aliases = {"spell3", "s3"}, name = "Level 3 Spell Slot", },
        { aliases = {"wspell2"}, name = "Level 2 Warlock Spell Slot", },
        { aliases = {"ki"}, name = "Ki Point", },
        """.trimIndent(),
    )

    @Test
    fun `coûts au format du modèle Resource`() {
        assertThat(Parsing.costs("action, spell3", aliases)).containsExactly(Cost(Resource.ACTION), Cost(Resource.SPELL_SLOT, level = 3)).inOrder()
        assertThat(Parsing.costs("ba, ki:2", aliases)).containsExactly(Cost(Resource.BONUS_ACTION), Cost(Resource.KI_POINT, amount = 2)).inOrder()
        assertThat(Parsing.costs("wspell2", aliases)).containsExactly(Cost(Resource.PACT_SLOT, level = 2))
        // Alias inconnu : rien n'est deviné.
        assertThat(Parsing.costs("action, mystère", aliases)).isNull()
    }

    @Test
    fun `dés de dégâts`() {
        assertThat(Parsing.damage("8d6", "Fire")).isEqualTo(Damage("8d6", DamageType.FIRE))
        assertThat(Parsing.damage("1d8 + 1", "Slashing")).isEqualTo(Damage("1d8+1", DamageType.SLASHING))
        assertThat(Parsing.damage("1d4 + Spellcasting mod", "Healing")).isEqualTo(Damage("1d4", healing = true, modifier = DamageModifier.SPELLCASTING))
        assertThat(Parsing.damage("beaucoup", "Fire")).isNull()
        assertThat(Parsing.damage("1d6", "Inconnu")).isNull()
        // Dégâts supplémentaires d'arme : type dans le même champ.
        assertThat(Parsing.damageWithType("1d4 Cold")).isEqualTo(Damage("1d4", DamageType.COLD))
        assertThat(Parsing.damageWithType("3 Poison")).isEqualTo(Damage("3", DamageType.POISON))
        assertThat(Parsing.damageWithType("1d4 Glacial")).isNull()
    }

    @Test
    fun `zones, portées, durées`() {
        assertThat(Parsing.area("Radius", "4")!!.shape).isEqualTo(AreaShape.RADIUS)
        assertThat(Parsing.range(null, "18")!!.metres).isEqualTo(18.0)
        assertThat(Parsing.duration("10")).isEqualTo(10 to null)
        assertThat(Parsing.duration("n'importe quoi")).isNull()
        assertThat(Parsing.recharge("Short Rest")).isEqualTo(Recharge.SHORT_REST)
    }

    @Test
    fun `lignes d'effet courantes`() {
        val grant = { name: String -> ContentId("spell.${name.lowercase().replace(' ', '-')}") }
        assertThat(EffectLine.parse("{{Skill|Sleight of Hand}} +1", grant)).isEqualTo(SkillBonus(Skill.SLEIGHT_OF_HAND, 1))
        assertThat(EffectLine.parse("{{Armour Class}} +1", grant)).isEqualTo(fr.carnetdefaerun.core.model.content.ArmourClassBonus(1))
        assertThat(EffectLine.parse("{{SavingThrow}} +1", grant)).isEqualTo(SavingThrowBonus(1))
        assertThat(EffectLine.parse("[[Strength]] {{SavingThrow}}s +1", grant)).isEqualTo(SavingThrowBonus(1, Ability.STRENGTH))
        assertThat(EffectLine.parse("{{Ability|Strength}} +2 (up to 23)", grant)).isEqualTo(AbilityBonus(Ability.STRENGTH, 2, cap = 23))
        assertThat(EffectLine.parse("{{Advantage}} on [[Constitution]] {{Saving Throw}}s", grant)).isEqualTo(SaveAdvantage(Ability.CONSTITUTION))
        assertThat(EffectLine.parse("{{Advantage}} on [[Sleight of Hand]] Checks.", grant)).isEqualTo(CheckAdvantage(skill = Skill.SLEIGHT_OF_HAND))
        assertThat(EffectLine.parse("Grants [[Resistance]] to {{DamageType|Fire}} damage.", grant)).isEqualTo(DamageResistance(DamageType.FIRE))
        assertThat(EffectLine.parse("Attackers cannot land [[critical hit]]s on the wearer.", grant)).isEqualTo(CriticalHitImmunity)
        assertThat(EffectLine.parse("{{SpellAction|Misty Step|level=2|recharge=Short Rest}}", grant))
            .isEqualTo(Grants(ContentId("spell.misty-step"), Recharge.SHORT_REST))
        // Une phrase libre n'est jamais structurée : elle part à la rédaction.
        assertThat(EffectLine.parse("Once per turn, something clever happens.", grant)).isNull()
    }

    @Test
    fun `lexique AideDD et orthographe britannique`() {
        val html = """<table><tr><td class="item"><a href="x">Armure de mage</a></td><td class="colVO">Mage Armor</td></tr>
            <tr><td class="item"><a href="y">Boule de feu</a></td><td class="colVO">Fireball</td></tr></table>"""
        val lexicon = Lexicon.parseAideDd(html, "aidedd")
        assertThat(lexicon.lookup("Mage Armour")).isEqualTo("Armure de mage")
        assertThat(lexicon.lookup("FIREBALL")).isEqualTo("Boule de feu")
        assertThat(lexicon.lookup("Fire Bolt")).isNull()
    }
}
