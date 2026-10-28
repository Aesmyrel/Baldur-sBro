package fr.carnetdefaerun.core.model.spoiler

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel.LIGHT
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel.MAJOR
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel.NONE
import org.junit.Test

/**
 * Table de vérité écrite à la main (docs/PLAN.md §2.3, D-028), indépendante de l'implémentation,
 * puis comparaison exhaustive sur les 5 × 3 × 5 × 3 × 3 × 3 = 2 025 combinaisons.
 */
class SpoilerGateTest {

    private enum class Relation { BEFORE, SAME, AFTER }

    /** Axe BG3 : [position de l'acte du bloc][niveau du bloc] → visible pour (aucun, léger, complet). */
    private val bg3Table: Map<Pair<Relation, SpoilerLevel>, List<Boolean>> = mapOf(
        //                                 aucun  léger  complet
        (Relation.BEFORE to NONE) to listOf(true, true, true),
        (Relation.BEFORE to LIGHT) to listOf(true, true, true),
        (Relation.BEFORE to MAJOR) to listOf(true, true, true),
        (Relation.SAME to NONE) to listOf(true, true, true),
        (Relation.SAME to LIGHT) to listOf(false, true, true),
        (Relation.SAME to MAJOR) to listOf(false, false, true),
        (Relation.AFTER to NONE) to listOf(false, true, true),
        (Relation.AFTER to LIGHT) to listOf(false, false, true),
        (Relation.AFTER to MAJOR) to listOf(false, false, true),
    )

    /** Axe « autres œuvres » : niveau du bloc → visible pour (aucun, léger, complet). */
    private val otherWorksTable: Map<SpoilerLevel, List<Boolean>> = mapOf(
        NONE to listOf(true, true, true),
        LIGHT to listOf(false, true, true),
        MAJOR to listOf(false, false, true),
    )

    @Test
    fun `toutes les combinaisons respectent la table de vérité`() {
        var cases = 0
        for (blockAct in Act.entries) for (level in SpoilerLevel.entries) for (otherWorks in SpoilerLevel.entries)
            for (current in Act.entries) for (tolerance in Tolerance.entries) for (otherTolerance in Tolerance.entries) {
                val relation = when {
                    blockAct < current -> Relation.BEFORE
                    blockAct == current -> Relation.SAME
                    else -> Relation.AFTER
                }
                val expected = bg3Table.getValue(relation to level)[tolerance.ordinal] &&
                    otherWorksTable.getValue(otherWorks)[otherTolerance.ordinal]
                val tag = SpoilerTag(blockAct, level, otherWorks)
                val profile = SpoilerProfile(current, tolerance, otherTolerance)
                assertWithMessage("$tag avec $profile").that(SpoilerGate.isVisible(tag, profile)).isEqualTo(expected)
                cases++
            }
        assertThat(cases).isEqualTo(2025)
    }

    @Test
    fun `profil par défaut, seul le contenu du prologue sans spoiler est visible`() {
        val profile = SpoilerProfile()
        assertThat(SpoilerGate.isVisible(SpoilerTag.SAFE, profile)).isTrue()
        assertThat(SpoilerGate.isVisible(SpoilerTag(Act.PROLOGUE, LIGHT, NONE), profile)).isFalse()
        assertThat(SpoilerGate.isVisible(SpoilerTag(Act.ACT_1, NONE, NONE), profile)).isFalse()
    }

    @Test
    fun `un rebondissement de l'acte courant se lève au passage à l'acte suivant`() {
        val twist = SpoilerTag(Act.ACT_1, MAJOR, NONE)
        assertThat(SpoilerGate.isVisible(twist, SpoilerProfile(Act.ACT_1))).isFalse()
        assertThat(SpoilerGate.isVisible(twist, SpoilerProfile(Act.ACT_2))).isTrue()
    }

    @Test
    fun `la révélation explicite l'emporte toujours`() {
        val tag = SpoilerTag(Act.EPILOGUE, MAJOR, MAJOR)
        assertThat(SpoilerGate.isVisible(tag, SpoilerProfile(), revealed = true)).isTrue()
    }

    @Test
    fun `un bloc masqué ne transporte ni contenu ni acte, sauf réglage explicite`() {
        val tag = SpoilerTag(Act.ACT_3, MAJOR, NONE)
        val hidden = "secret".gated("b1", tag, SpoilerProfile())
        assertThat(hidden).isEqualTo(Gated.Hidden(RevealKey.of("b1"), act = null))
        val withAct = "secret".gated("b1", tag, SpoilerProfile(), showActOfHidden = true)
        assertThat(withAct).isEqualTo(Gated.Hidden(RevealKey.of("b1"), act = Act.ACT_3))
        assertThat("visible".gated("b2", SpoilerTag.SAFE, SpoilerProfile())).isEqualTo(Gated.Visible("visible"))
    }

    @Test
    fun `strictest et protectsAtLeast`() {
        val a = SpoilerTag(Act.ACT_2, LIGHT, NONE)
        val b = SpoilerTag(Act.ACT_1, MAJOR, LIGHT)
        val s = a.strictest(b)
        assertThat(s).isEqualTo(SpoilerTag(Act.ACT_2, MAJOR, LIGHT))
        assertThat(s.protectsAtLeast(a)).isTrue()
        assertThat(s.protectsAtLeast(b)).isTrue()
        assertThat(a.protectsAtLeast(b)).isFalse()
    }

    @Test
    fun `l'ordre des actes est chronologique`() {
        assertThat(Act.entries).containsExactly(Act.PROLOGUE, Act.ACT_1, Act.ACT_2, Act.ACT_3, Act.EPILOGUE).inOrder()
    }

    @Test
    fun `sans révélation, le verrou est exactement la règle de visibilité`() {
        for (act in Act.entries) for (level in SpoilerLevel.entries) for (tolerance in Tolerance.entries) {
            val tag = SpoilerTag(act, level, NONE)
            val profile = SpoilerProfile(Act.ACT_1, tolerance)
            assertThat(SpoilerGate.isVisible(tag, profile, revealed = false)).isEqualTo(SpoilerGate.isVisible(tag, profile))
            assertThat(SpoilerGate.isVisible(tag, profile, revealed = true)).isTrue()
        }
    }

    @Test
    fun `une étiquette absente ou mal nommée est une erreur, jamais un contenu visible`() {
        val json = kotlinx.serialization.json.Json
        assertThrows { json.decodeFromString(SpoilerTag.serializer(), "{}") }
        assertThrows { json.decodeFromString(SpoilerTag.serializer(), """{"acte":"acte3","niveau":"majeur","otherWorks":"aucun"}""") }
        assertThat(json.decodeFromString(SpoilerTag.serializer(), """{"act":"acte3","level":"majeur","otherWorks":"aucun"}"""))
            .isEqualTo(SpoilerTag(Act.ACT_3, MAJOR, NONE))
    }

    @Test
    fun `la poignée d'un bloc masqué ne laisse pas lire son identifiant`() {
        val key = RevealKey.of("lore.deity.selune#section-2")
        assertThat(key.token).doesNotContain("selune")
        assertThat(key.token).matches("[0-9a-f]{24}")
        assertThat(RevealKey.of("lore.deity.selune#section-2")).isEqualTo(key)
    }

    private fun assertThrows(block: () -> Unit) {
        val failure = runCatching(block).exceptionOrNull()
        assertWithMessage("une exception était attendue").that(failure).isNotNull()
    }
}
