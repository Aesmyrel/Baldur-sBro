package fr.carnetdefaerun.core.model

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import org.junit.Test

class BuildCodeTest {
    private val build = Build(
        name = "L’Évocateur en harnois",
        race = ContentId("race.elf"),
        subrace = ContentId("subrace.high-elf"),
        background = ContentId("background.sage"),
        baseScores = Ability.entries.associateWith { 8 } + mapOf(Ability.INTELLIGENCE to 15, Ability.CONSTITUTION to 15, Ability.DEXTERITY to 14),
        majorBonus = Ability.INTELLIGENCE,
        minorBonus = Ability.CONSTITUTION,
        levels = List(2) { BuildLevel(ContentId("class.fighter")) } +
            BuildLevel(ContentId("class.wizard")) + BuildLevel(ContentId("class.wizard"), ContentId("subclass.evocation-school")) +
            BuildLevel(ContentId("class.wizard")) +
            BuildLevel(ContentId("class.wizard"), feat = FeatChoice(ContentId("feat.ability-improvement"), mapOf(Ability.INTELLIGENCE to 2))) +
            List(6) { BuildLevel(ContentId("class.wizard")) },
    )

    @Test
    fun `aller-retour exact, code court`() {
        val code = BuildCode.encode(build)
        assertThat(code).startsWith(BuildCode.PREFIX)
        assertThat(code.length).isLessThan(400) // tient dans un petit QR code
        assertThat(code).matches("[A-Za-z0-9._-]+")
        assertThat(BuildCode.decode(code)).isEqualTo(build)
        assertThat(BuildCode.decode("  $code\n")).isEqualTo(build)
    }

    @Test
    fun `codes mal formés ou hostiles refusés`() {
        assertThat(BuildCode.decode("")).isNull()
        assertThat(BuildCode.decode("bonjour")).isNull()
        assertThat(BuildCode.decode(BuildCode.PREFIX + "!!!")).isNull()
        assertThat(BuildCode.decode(BuildCode.PREFIX + "AAAA")).isNull()
        val code = BuildCode.encode(build)
        assertThat(BuildCode.decode(code.dropLast(6))).isNull() // tronqué
        // Identifiant de contenu mal formé, nom trop long, trop de niveaux.
        assertThat(BuildCode.decode(raw("""{"name":"x","levels":[{"classId":"../etc"}]}"""))).isNull()
        assertThat(BuildCode.decode(raw("""{"name":"${"x".repeat(61)}"}"""))).isNull()
        val thirteen = build.copy(levels = List(13) { BuildLevel(ContentId("class.fighter")) })
        assertThat(BuildCode.decode(BuildCode.encode(thirteen))).isNull()
        // Bombe de décompression : un million d'espaces tient en quelques centaines d'octets.
        assertThat(BuildCode.decode(raw("""{"name":"x"}""" + " ".repeat(1_000_000)))).isNull()
    }

    private fun raw(json: String): String {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true).apply { setInput(json.toByteArray()); finish() }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4_096)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        return BuildCode.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }
}
