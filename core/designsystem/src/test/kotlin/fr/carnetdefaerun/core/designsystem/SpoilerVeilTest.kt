package fr.carnetdefaerun.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.designsystem.component.SpoilerVeil
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Le voile ne se lève que sur un geste confirmé, et ne dit rien de ce qu'il cache (PLAN §2.4, §2.5). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class SpoilerVeilTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var reveals = 0

    private fun setVeil(actLabel: String? = null) {
        compose.setContent {
            CarnetTheme(reducedMotion = true) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SpoilerVeil(onReveal = { reveals++ }, modifier = Modifier.testTag("voile"), actLabel = actLabel)
                    Spacer(Modifier.height(2000.dp))
                }
            }
        }
    }

    @Test
    fun `un défilement commencé sur le voile ne révèle rien`() {
        setVeil()
        compose.onNodeWithTag("voile").performTouchInput { swipeUp() }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertThat(reveals).isEqualTo(0)
    }

    @Test
    fun `un toucher confirmé révèle, une seule fois`() {
        setVeil()
        compose.onNodeWithTag("voile").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithTag("voile").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertThat(reveals).isEqualTo(1)
    }

    @Test
    fun `l'accessibilité annonce un voile générique, sans titre ni niveau`() {
        setVeil()
        val node = compose.onNodeWithTag("voile")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Section masquée, au-delà de votre progression")))
        node.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
        node.assert(SemanticsMatcher("action « Révéler »") { it.config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label == "Révéler" })
    }

    @Test
    fun `l'acte n'apparaît que sur demande explicite`() {
        setVeil(actLabel = "Acte II")
        compose.onNodeWithTag("voile")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Section masquée, au-delà de votre progression, Acte II")))
    }
}
