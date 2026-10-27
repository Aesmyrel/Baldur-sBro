package fr.carnetdefaerun.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class SettingsScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reglages_sombre() {
        compose.setContent {
            CarnetTheme(reducedMotion = true) {
                Box(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) {
                    SettingsScreen(UserPreferences(currentAct = Act.ACT_1, tolerance = Tolerance.LIGHT), {}, {}, {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/reglages_sombre.png")
    }
}
