package fr.carnetdefaerun.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Un en-tête de section ne coupe jamais un mot, ni du libellé ni de l'action (D-050), même étroit et à 200 %. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w320dp-h900dp-xxhdpi")
class SectionHeaderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val labels = listOf("Caractéristiques", "Statistiques dérivées", "Prochains objectifs", "Emblèmes des classes", "Personnalisation")

    private fun lines(node: androidx.compose.ui.semantics.SemanticsNode): List<String> {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.single()
        val content = layout.layoutInput.text.text
        return (0 until layout.lineCount).map { content.substring(layout.getLineStart(it), layout.getLineEnd(it)).trim() }
    }

    @Test
    fun `aucun mot coupé, à 320 dp et 200 %`() {
        var setting by mutableStateOf(320 to 1f)
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, setting.second)) {
                CarnetTheme(reducedMotion = true) {
                    Column(Modifier.width(setting.first.dp)) {
                        labels.forEachIndexed { i, label -> SectionHeader(label, actionLabel = if (i % 2 == 0) "Tout voir" else null, onAction = {}) }
                    }
                }
            }
        }
        for (width in listOf(320, 360)) for (scale in listOf(1f, 1.3f, 2f)) {
            setting = width to scale
            compose.waitForIdle()
            for (label in labels) {
                val words = label.split(' ').toSet()
                lines(compose.onNodeWithText(label).fetchSemanticsNode()).forEach { line ->
                    line.split(' ').forEach { part ->
                        assertWithMessage("%s dp, %s : « %s » coupé en « %s »", width, scale, label, part).that(part in words).isTrue()
                    }
                }
            }
            compose.onAllNodesWithText("Tout voir").fetchSemanticsNodes().forEach { node ->
                assertWithMessage("%s dp, %s : action sur une ligne", width, scale).that(lines(node)).hasSize(1)
            }
        }
    }
}
