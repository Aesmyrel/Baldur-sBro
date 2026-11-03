package fr.carnetdefaerun.feature.search

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
import fr.carnetdefaerun.core.data.search.SearchRepository
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Recherche : résultats groupés, voiles du contenu masqué, invitation ; profil du Prologue. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class SearchScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val search by lazy { SearchRepository(InMemoryContentStore(GameData.entries)) }

    private fun state(query: String, includeHidden: Boolean = false): SearchUiState {
        val results = search.search(query, SpoilerProfile(Act.PROLOGUE), includeHidden = includeHidden, limit = 12)
        return SearchUiState(
            query, includeHidden,
            results.hits.groupBy { it.entry.type }.map { (type, hits) -> SearchGroup(type, hits.toImmutableList()) }.toImmutableList(),
            results.hidden.take(2).toImmutableList(),
        )
    }

    private fun shot(name: String, state: SearchUiState, mode: ThemeMode = ThemeMode.SOMBRE, fontScale: Float = 1f) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                CarnetTheme(mode = mode, reducedMotion = true) {
                    Box(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) {
                        SearchScreen(state, {}, {}, {}, { _, _ -> }, {}, autoFocus = false)
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun recherche_resultats_sombre() = shot("recherche_resultats_sombre", state("feu"))
    @Test fun recherche_fautes_parchemin() = shot("recherche_fautes_parchemin", state("bolue de feu"), ThemeMode.PARCHEMIN)
    @Test fun recherche_masques_sombre() = shot("recherche_masques_sombre", state("souffle", includeHidden = true))
    @Test fun recherche_invitation_police200() = shot("recherche_invitation_police200", state(""), fontScale = 2f)
}
