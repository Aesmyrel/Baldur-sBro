package fr.carnetdefaerun

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.carnetdefaerun.navigation.rememberTabEntries
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Changer d'onglet puis revenir retrouve l'état sauvegardable de l'écran quitté (I6). */
@RunWith(AndroidJUnit4::class)
class TabEntriesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `l'état d'un onglet survit à un aller-retour vers un autre onglet`() {
        var tab by mutableStateOf("a")
        compose.setContent {
            val entries = rememberTabEntries(listOf("a", "b"), tab, { listOf("racine-$it") }) { t ->
                { key: String ->
                    NavEntry(key, contentKey = "$t/$key") {
                        var taps by rememberSaveable { mutableIntStateOf(0) }
                        Text("$key:$taps", Modifier.testTag("page").clickable { taps++ })
                    }
                }
            }
            NavDisplay(entries = entries, onBack = {})
        }
        repeat(3) { compose.onNodeWithTag("page").performClick() }
        compose.onNodeWithTag("page").assertTextEquals("racine-a:3")

        tab = "b"
        compose.waitForIdle()
        compose.onNodeWithTag("page").assertTextEquals("racine-b:0")

        tab = "a"
        compose.waitForIdle()
        compose.onNodeWithTag("page").assertTextEquals("racine-a:3")
    }
}
