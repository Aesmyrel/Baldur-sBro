package fr.carnetdefaerun.pipeline

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.BackgroundFacts
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.FeatFacts
import fr.carnetdefaerun.core.model.content.RaceFacts
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.content.SubraceFacts
import fr.carnetdefaerun.core.model.content.WeaponType
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test

/**
 * Instantané de référence des origines (`data/game/races.json`, `sous-races.json`, `historiques.json`, `dons.json`),
 * comparé à la main aux pages de synthèse de bg3.wiki : Races, Backgrounds, Feats (révisions dans les attributions).
 */
class OriginDataTest {
    private fun load(name: String): Map<String, Entry> {
        val file = File("../../data/game/$name.json").takeIf { it.exists() } ?: File("data/game/$name.json")
        return ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), file.readText()).associateBy { it.id.value }
    }

    private val races by lazy { load("races") }
    private val subraces by lazy { load("sous-races") }

    @Test
    fun `onze races, vingt-huit sous-races, chacune rattachée`() {
        assertThat(races).hasSize(11)
        assertThat(subraces).hasSize(28)
        subraces.values.forEach { assertThat((it.facts as SubraceFacts).parentRace.value).isIn(races.keys) }
    }

    @Test
    fun `humain, maîtrises de la milice et une compétence au choix`() {
        val h = races.getValue("race.human").facts as RaceFacts
        assertThat(h.speedMetres).isEqualTo(9.0)
        assertThat(h.proficiencies.weapons).containsExactly(WeaponType.SPEAR, WeaponType.PIKE, WeaponType.HALBERD, WeaponType.GLAIVE)
        assertThat(h.proficiencies.armour).containsExactly(ArmourProficiency.LIGHT, ArmourProficiency.SHIELD)
        assertThat(h.proficiencies.skillChoices).isEqualTo(1)
    }

    @Test
    fun `elfe, vitesse propre à chaque sous-race, elfe des bois plus rapide et furtif`() {
        assertThat((races.getValue("race.elf").facts as RaceFacts).speedMetres).isNull()
        val wood = subraces.getValue("subrace.wood-elf").facts as SubraceFacts
        assertThat(wood.speedMetres).isEqualTo(10.5)
        assertThat(wood.proficiencies.skills).containsExactly(Skill.STEALTH)
        assertThat((subraces.getValue("subrace.high-elf").facts as SubraceFacts).speedMetres).isEqualTo(9.0)
    }

    @Test
    fun `sorts raciaux rattachés aux sous-races`() {
        val spells = load("sorts")
        val sources = spells.values.flatMap { (it.facts as fr.carnetdefaerun.core.model.content.SpellFacts).learnedBy.map { l -> l.source } }.toSet()
        assertThat(sources).containsAtLeast(ContentId("subrace.high-elf"), ContentId("subrace.zariel-tiefling"), ContentId("subrace.duergar"))
        assertThat(sources).doesNotContain(ContentId("race.high-elf"))
    }

    @Test
    fun `douze historiques, deux compétences chacun`() {
        val backgrounds = load("historiques")
        assertThat(backgrounds).hasSize(12)
        backgrounds.values.forEach { assertThat((it.facts as BackgroundFacts).skills).hasSize(2) }
        assertThat((backgrounds.getValue("background.acolyte").facts as BackgroundFacts).skills).containsExactly(Skill.INSIGHT, Skill.RELIGION)
    }

    @Test
    fun `quarante et un dons, augmentations lues dans leurs descriptions`() {
        val feats = load("dons")
        assertThat(feats).hasSize(41)
        val asi = (feats.getValue("feat.ability-improvement").facts as FeatFacts).abilityIncrease!!
        assertThat(asi.amount).isEqualTo(2)
        assertThat(asi.splittable).isTrue()
        val athlete = (feats.getValue("feat.athlete").facts as FeatFacts).abilityIncrease!!
        assertThat(athlete.choices).containsExactly(Ability.STRENGTH, Ability.DEXTERITY)
        assertThat((feats.getValue("feat.alert").facts as FeatFacts).abilityIncrease).isNull()
    }
}
