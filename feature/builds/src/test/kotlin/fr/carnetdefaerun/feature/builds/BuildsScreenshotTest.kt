package fr.carnetdefaerun.feature.builds

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
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Atelier des builds, rendu avec les vraies données et le vrai moteur de règles. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class BuildsScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val catalog by lazy { BuildFixtures.catalog() }
    private val rules = BuildFixtures.rules
    private val ids = listOf(5L, 4L, 3L, 2L, 1L)

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

    private fun editor(
        build: Build = BuildFixtures.battleMage,
        panel: Panel? = null,
        mode: ThemeMode = ThemeMode.SOMBRE,
        fontScale: Float = 1f,
        difficulty: Difficulty = Difficulty.BALANCED,
        name: String,
    ) = shot(name, mode, fontScale) {
        BuildEditorScreen(editorState(3L, build, rules, catalog, ids, difficulty), onEdit = {}, onBack = {}, initialPanel = panel)
    }

    @Test fun atelier_sombre() = editor(name = "atelier_sombre")
    @Test fun atelier_detail_pv_sombre() = editor(panel = Panel.HitPoints, name = "atelier_detail_pv_sombre")
    @Test fun atelier_parchemin() = editor(mode = ThemeMode.PARCHEMIN, name = "atelier_parchemin")
    @Test fun atelier_oled_detail_ca() = editor(panel = Panel.ArmourClass, mode = ThemeMode.NOIR_OLED, name = "atelier_oled_detail_ca")
    @Test fun atelier_police200() = editor(fontScale = 2f, name = "atelier_police200")
    @Test fun atelier_niveau_sombre() = editor(panel = Panel.Level(5), name = "atelier_niveau_sombre")
    @Test fun atelier_caracteristique_sombre() = editor(panel = Panel.AbilityPanel(Ability.INTELLIGENCE), name = "atelier_caracteristique_sombre")
    @Test fun atelier_partage_sombre() = editor(panel = Panel.Share, name = "atelier_partage_sombre")
    @Test fun atelier_pacte_parchemin() = editor(BuildFixtures.pact, panel = Panel.SpellDc, mode = ThemeMode.PARCHEMIN, name = "atelier_pacte_parchemin")
    @Test fun atelier_vide_sombre() = editor(Build(name = "Nouveau build"), name = "atelier_vide_sombre")
    @Test fun atelier_explorateur_detail_pv() = editor(panel = Panel.HitPoints, difficulty = Difficulty.EXPLORER, name = "atelier_explorateur_detail_pv")
    @Test fun atelier_competences_sombre() = editor(panel = Panel.Skills("class.fighter"), name = "atelier_competences_sombre")

    private fun item(id: Long, build: Build) = BuildItem(id, build, CharacterSheetCalculator.compute(build, rules))
    private fun party() = PartyUiState(
        slots = listOf(item(1, BuildFixtures.battleMage), null, item(2, BuildFixtures.pact), null),
        builds = listOf(item(1, BuildFixtures.battleMage), item(2, BuildFixtures.pact)),
        catalog = catalog,
    )

    @Test fun groupe_sombre() = shot("groupe_sombre") { PartyScreen(party(), onSet = { _, _ -> }, onBack = {}) }
    @Test fun groupe_parchemin_choix() = shot("groupe_parchemin_choix", ThemeMode.PARCHEMIN) { PartyScreen(party(), onSet = { _, _ -> }, onBack = {}, initialSlot = 1) }
    @Test fun groupe_police200() = shot("groupe_police200", fontScale = 2f) { PartyScreen(party(), onSet = { _, _ -> }, onBack = {}) }
    @Test fun builds_import_sombre() = shot("builds_import_sombre") {
        BuildsScreen(BuildsUiState(persistentListOf(), catalog), {}, {}, { false }, startImporting = true, message = R.string.builds_scan_unavailable)
    }

    @Config(qualifiers = "w800dp-h840dp-xhdpi")
    @Test fun atelier_pliable() = editor(name = "atelier_pliable")

    @Test fun builds_liste_sombre() = shot("builds_liste_sombre") {
        val items = listOf(BuildFixtures.battleMage, BuildFixtures.pact, Build(name = "Nouveau build"))
            .mapIndexed { i, b -> BuildItem(i + 1L, b, CharacterSheetCalculator.compute(b, rules)) }
        BuildsScreen(BuildsUiState(kotlinx.collections.immutable.persistentListOf(*items.toTypedArray()), catalog), {}, {}, { false })
    }

    @Test fun builds_liste_vide_parchemin() = shot("builds_liste_vide_parchemin", ThemeMode.PARCHEMIN) {
        BuildsScreen(BuildsUiState(persistentListOf(), catalog), {}, {}, { false })
    }
}
