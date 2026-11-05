package fr.carnetdefaerun.feature.encyclopedia

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
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import kotlinx.collections.immutable.toImmutableMap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Grimoire : accueil, listes et fiches, rendues à partir des vraies données filtrées par le vrai dépôt.
 * Profil du Prologue (ou du début de l'Acte I) : aucune capture ne montre de contenu au-delà.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class EncyclopediaScreenshotTest {
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

    /** Comme le fait la porte du contenu : listes triées sur le nom replié. */
    private fun list(type: EntryType) = repository.list(type, prologue).let { l -> l.copy(visible = l.visible.sortedBy { Folding.fold(it.name.fr) }) }

    private fun entryState(id: String, profile: SpoilerProfile = prologue, repo: ContentRepository = repository): EntryUiState =
        when (val gated = repo.entry(ContentId(id), profile)) {
            is Gated.Visible -> EntryUiState.Visible(gated.value, favorite = false)
            is Gated.Hidden -> EntryUiState.Hidden(gated.key, gated.act)
            null -> EntryUiState.Missing
        }

    @Test fun grimoire_sombre() = shot("grimoire_sombre") {
        val counts = GRIMOIRE_GROUPS.flatMap { it.second }.associateWith { repository.list(it, prologue).visible.size }
        EncyclopediaScreen(EncyclopediaUiState(counts.toImmutableMap())) {}
    }

    @Test fun liste_sorts_sombre() = shot("liste_sorts_sombre") {
        CategoryScreen(categoryState(EntryType.SPELL, list(EntryType.SPELL), null), {}, {}, { _, _ -> }, {})
    }

    @Test fun liste_sous_classes_parchemin() = shot("liste_sous_classes_parchemin", ThemeMode.PARCHEMIN) {
        CategoryScreen(categoryState(EntryType.SUBCLASS, list(EntryType.SUBCLASS), list(EntryType.CLASS)), {}, {}, { _, _ -> }, {})
    }

    /** Barre d'une fiche avec l'action « Ajouter une note » (feuillet gravé) à côté du signet. */
    @Test fun fiche_avec_note_sombre() = shot("fiche_avec_note_sombre") {
        EntryScreen(entryState("feature.dirty-trick-sand-toss"), EntryActions(onNote = {}))
    }

    @Test fun fiche_capacite_sombre() = shot("fiche_capacite_sombre") {
        EntryScreen(entryState("feature.dirty-trick-sand-toss"), EntryActions())
    }

    @Test fun fiche_capacite_voile_sombre() = shot("fiche_capacite_voile_sombre") {
        // Un des blocs passe à l'Acte III pour montrer le voile qui le remplace.
        val late = SpoilerTag(Act.ACT_3, SpoilerLevel.NONE, SpoilerLevel.NONE)
        val entries = GameData.entries.map { e -> if (e.id.value == "feature.dirty-trick-sand-toss") e.withLateBlock(late) else e }
        EntryScreen(entryState("feature.dirty-trick-sand-toss", repo = ContentRepository(InMemoryContentStore(entries))), EntryActions())
    }

    @Test fun fiche_classe_parchemin() = shot("fiche_classe_parchemin", ThemeMode.PARCHEMIN) {
        EntryScreen(entryState("class.barbarian"), EntryActions())
    }

    @Test fun fiche_objet_oled() = shot("fiche_objet_oled", ThemeMode.NOIR_OLED) {
        EntryScreen(entryState("item.safeguard-shield", SpoilerProfile(Act.ACT_1)), EntryActions())
    }

    @Test fun fiche_police200() = shot("fiche_police200", fontScale = 2f) {
        EntryScreen(entryState("feature.chalice-healing"), EntryActions())
    }

    @Test fun fiche_masquee_sombre() = shot("fiche_masquee_sombre") {
        EntryScreen(EntryUiState.Hidden(RevealKey.of("entry:feature.exemple"), act = null), EntryActions())
    }

    @Config(qualifiers = "w800dp-h840dp-xhdpi")
    @Test fun fiche_classe_pliable() = shot("fiche_classe_pliable") {
        EntryScreen(entryState("class.barbarian"), EntryActions())
    }

    private fun Entry.withLateBlock(tag: SpoilerTag): Entry = copy(blocks = blocks.mapIndexed { i, b -> if (i == 1) b.copy(tag = tag) else b })
}
