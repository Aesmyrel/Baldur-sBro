package fr.carnetdefaerun.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.spoiler.Act
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, mode: ThemeMode, act: Act) {
        compose.setContent {
            CarnetTheme(mode = mode, reducedMotion = true) {
                Box(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) {
                    HomeScreen(HomeUiState(act, Difficulty.TACTICIAN, sessionActive = true), {}, {}, {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun accueil_acte1_sombre() = shot("accueil_acte1_sombre", ThemeMode.SOMBRE, Act.ACT_1)
    @Test fun accueil_prologue_parchemin() = shot("accueil_prologue_parchemin", ThemeMode.PARCHEMIN, Act.PROLOGUE)
}
