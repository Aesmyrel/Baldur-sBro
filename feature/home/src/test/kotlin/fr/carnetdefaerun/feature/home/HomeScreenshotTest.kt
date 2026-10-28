package fr.carnetdefaerun.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.spoiler.Act
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Ma partie : thèmes, police à 200 %, écran étroit (360 dp) et pliable ouvert. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, mode: ThemeMode, act: Act, fontScale: Float = 1f, session: Boolean = true) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                CarnetTheme(mode = mode, reducedMotion = true) {
                    Box(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) {
                        HomeScreen(HomeUiState(act, Difficulty.TACTICIAN, sessionActive = session), {}, {}, {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun accueil_acte1_sombre() = shot("accueil_acte1_sombre", ThemeMode.SOMBRE, Act.ACT_1)
    @Test fun accueil_prologue_parchemin() = shot("accueil_prologue_parchemin", ThemeMode.PARCHEMIN, Act.PROLOGUE)
    @Test fun accueil_acte2_oled() = shot("accueil_acte2_oled", ThemeMode.NOIR_OLED, Act.ACT_2, session = false)
    @Test fun accueil_epilogue_police200() = shot("accueil_epilogue_police200", ThemeMode.SOMBRE, Act.EPILOGUE, fontScale = 2f)

    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    @Test fun accueil_w360() = shot("accueil_w360", ThemeMode.SOMBRE, Act.ACT_1)

    @Config(qualifiers = "w800dp-h840dp-xhdpi")
    @Test fun accueil_pliable() = shot("accueil_pliable", ThemeMode.PARCHEMIN, Act.ACT_3)
}
