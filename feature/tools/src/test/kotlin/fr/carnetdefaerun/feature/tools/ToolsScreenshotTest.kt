package fr.carnetdefaerun.feature.tools

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.rules.BonusDie
import fr.carnetdefaerun.core.rules.RollKind
import fr.carnetdefaerun.core.rules.RollMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class ToolsScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, mode: ThemeMode = ThemeMode.SOMBRE, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                CarnetTheme(mode = mode, reducedMotion = true) {
                    Box(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private val attack = ProbabilityInput(RollKind.ATTACK, modifier = 7, target = 16, mode = RollMode.ADVANTAGE, bonusDice = setOf(BonusDie.BLESS), improvedCriticals = 1)
    private val check = ProbabilityInput(RollKind.ABILITY_CHECK, modifier = 3, target = 15, bonusDice = setOf(BonusDie.GUIDANCE, BonusDie.BARDIC_INSPIRATION), bardLevel = 5, inspirationRerolls = 1)

    @Test fun outils_sombre() = shot("outils_sombre") { ToolsRoute {} }
    @Test fun proba_attaque_sombre() = shot("proba_attaque_sombre") { ProbabilityScreen(attack, {}, {}) }
    @Test fun proba_test_parchemin() = shot("proba_test_parchemin", ThemeMode.PARCHEMIN) { ProbabilityScreen(check, {}, {}) }
    @Test fun proba_sauvegarde_oled() = shot("proba_sauvegarde_oled", ThemeMode.NOIR_OLED) {
        ProbabilityScreen(ProbabilityInput(RollKind.SAVING_THROW, modifier = 2, target = 14, mode = RollMode.DISADVANTAGE), {}, {})
    }
    private val pool = DicePool(mapOf(6 to 2, 8 to 1), modifier = 3)
    private val roll = DiceRoll(pool, listOf(4, 6, 2))
    @Test fun des_sombre() = shot("des_sombre") { DiceScreen(pool, roll, listOf(roll, DiceRoll(DicePool(mapOf(20 to 1)), listOf(17))), {}, {}, {}) }
    @Test fun des_parchemin() = shot("des_parchemin", ThemeMode.PARCHEMIN) { DiceScreen(DicePool(mapOf(20 to 1, 4 to 2)), null, emptyList(), {}, {}, {}) }
    @Test fun des_police200() = shot("des_police200", fontScale = 2f) { DiceScreen(pool, roll, emptyList(), {}, {}, {}) }
    @Test fun proba_police200() = shot("proba_police200", fontScale = 2f) { ProbabilityScreen(check, {}, {}) }
}
