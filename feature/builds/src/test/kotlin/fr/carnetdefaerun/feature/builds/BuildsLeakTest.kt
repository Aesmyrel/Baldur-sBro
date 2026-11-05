package fr.carnetdefaerun.feature.builds

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.core.testing.GameData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Fuite par l'atelier : un build (importé d'un code, par exemple) peut nommer une option que le verrou masque.
 * Ni son nom français ni son nom anglais n'apparaissent, sur la fiche comme dans ses feuilles ; un « choix voilé »
 * les remplace.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h917dp")
class BuildsLeakTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val lockedIds = setOf("subclass.evocation-school", "subrace.high-elf", "background.sage", "feat.ability-improvement")
    private val late = SpoilerTag(Act.ACT_3, SpoilerLevel.MAJOR, SpoilerLevel.NONE)
    private val entries = GameData.entries.map { e -> if (e.id.value in lockedIds) e.copy(tag = late, titleTag = late) else e }

    private fun texts(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            node.config.getOrNull(SemanticsProperties.EditableText)?.text.let { listOfNotNull(it) } +
            node.children.flatMap(::texts)

    @Test
    fun `une option masquée n'est jamais nommée`() {
        val catalog = BuildFixtures.catalog(entries)
        val state = editorState(1L, BuildFixtures.battleMage, BuildFixtures.rules, catalog, listOf(1L))
        val locked = GameData.entries.filter { it.id.value in lockedIds }.flatMap { listOf(it.name.fr, it.name.en) }
        val panels = listOf(null, Panel.HitPoints, Panel.Level(3), Panel.Level(5), Panel.Race, Panel.Subrace, Panel.Background)
        var panel by mutableStateOf<Panel?>(null)
        var shown: List<String>
        // La feuille ouverte au départ est mémorisée par l'écran : la clé le recrée à chaque feuille.
        compose.setContent { CarnetTheme(reducedMotion = true) { key(panel) { BuildEditorScreen(state, onEdit = {}, onBack = {}, initialPanel = panel) } } }
        for (p in panels) {
            panel = p
            compose.waitForIdle()
            shown = texts(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
            for (name in locked) {
                // Mot entier : « Sage » (l'historique) ne doit pas être confondu avec « Sagesse ».
                val word = Regex("(?<!\\p{L})" + Regex.escape(name) + "(?!\\p{L})")
                assertWithMessage("« $name » affiché (feuille $p)").that(shown.none { word.containsMatchIn(it) }).isTrue()
            }
        }
        panel = null
        compose.waitForIdle()
        shown = texts(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        assertThat(shown.any { it.contains("choix voilé") }).isTrue()
    }

    @Test
    fun `le groupe ne nomme pas non plus une option masquée`() {
        val catalog = BuildFixtures.catalog(entries)
        val item = BuildItem(1L, BuildFixtures.battleMage, fr.carnetdefaerun.core.rules.CharacterSheetCalculator.compute(BuildFixtures.battleMage, BuildFixtures.rules))
        val state = PartyUiState(listOf(item, null, null, null), listOf(item), catalog)
        val locked = GameData.entries.filter { it.id.value in lockedIds }.flatMap { listOf(it.name.fr, it.name.en) }
        compose.setContent { CarnetTheme(reducedMotion = true) { PartyScreen(state, onSet = { _, _ -> }, onBack = {}) } }
        compose.waitForIdle()
        val shown = texts(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        for (name in locked) {
            val word = Regex("(?<!\\p{L})" + Regex.escape(name) + "(?!\\p{L})")
            assertWithMessage("« $name » affiché dans le groupe").that(shown.none { word.containsMatchIn(it) }).isTrue()
        }
    }
}
