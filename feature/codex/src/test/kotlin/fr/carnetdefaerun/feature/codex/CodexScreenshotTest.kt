package fr.carnetdefaerun.feature.codex

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
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.LoreTheme
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import kotlinx.collections.immutable.toImmutableMap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Codex : accueil, thème et fiches de lore, rendus depuis le codex assemblé, filtré par le vrai dépôt. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class CodexScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val prologue = SpoilerProfile(Act.PROLOGUE)
    private val repository by lazy { ContentRepository(InMemoryContentStore(GameData.entries, GameData.references)) }

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

    private fun lore(id: String, profile: SpoilerProfile = prologue): LoreUiState = when (val g = repository.entry(ContentId(id), profile)) {
        is Gated.Visible -> LoreUiState.Visible(g.value, favorite = false)
        is Gated.Hidden -> LoreUiState.Hidden(g.key, g.act)
        null -> LoreUiState.Missing
    }

    private fun list() = repository.list(EntryType.LORE, prologue).let { l -> l.copy(visible = l.visible.sortedBy { Folding.fold(it.name.fr) }) }

    @Test fun codex_sombre() = shot("codex_sombre") {
        val visible = list().visible
        CodexScreen(CodexUiState(LoreTheme.entries.associateWith { t -> visible.count { it.subtype == t.name } }.toImmutableMap())) {}
    }

    @Test fun theme_peuples_parchemin() = shot("theme_peuples_parchemin", ThemeMode.PARCHEMIN) {
        ThemeScreen(themeState(LoreTheme.PEOPLES, list()), {}, {})
    }

    @Test fun lore_tieffelin_sombre() = shot("lore_tieffelin_sombre") {
        // Au Prologue, la section de l'Acte I reste voilée.
        LoreEntryScreen(lore("lore.peuples.tiefling"), null, {}, {}, { _, _ -> }, {})
    }

    @Test fun lore_averne_parchemin() = shot("lore_averne_parchemin", ThemeMode.PARCHEMIN) {
        LoreEntryScreen(lore("lore.cosmologie.avernus"), null, {}, {}, { _, _ -> }, {})
    }

    @Test fun lore_police200() = shot("lore_police200", fontScale = 2f) {
        LoreEntryScreen(lore("lore.bestiaire.imp"), null, {}, {}, { _, _ -> }, {})
    }

    /** Fiche entière, pour relire le bas de page : section voilée, références, relations. */
    @Config(qualifiers = "w412dp-h6000dp-mdpi")
    @Test fun lore_tieffelin_complet() = shot("lore_tieffelin_complet") {
        LoreEntryScreen(lore("lore.peuples.tiefling"), null, {}, {}, { _, _ -> }, {})
    }

    @Config(qualifiers = "w800dp-h840dp-xhdpi")
    @Test fun lore_pliable_oled() = shot("lore_pliable_oled", ThemeMode.NOIR_OLED) {
        LoreEntryScreen(lore("lore.personnages.halaster-blackcloak"), null, {}, {}, { _, _ -> }, {})
    }
}
