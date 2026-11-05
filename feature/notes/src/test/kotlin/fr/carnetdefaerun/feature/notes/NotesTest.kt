package fr.carnetdefaerun.feature.notes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.data.notes.Note
import fr.carnetdefaerun.core.data.notes.NoteTarget
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Notes : le nom d'une fiche rattachée passe par le verrou ; captures de la liste et de l'éditeur. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class NotesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val visibleId = ContentId("spell.fireball")
    private val lockedId = ContentId("condition.blinded")
    private val late = SpoilerTag(Act.ACT_3, SpoilerLevel.MAJOR, SpoilerLevel.NONE)
    private val entries by lazy { GameData.entries.map { if (it.id == lockedId) it.copy(tag = late, titleTag = late) else it } }
    private val repository by lazy { ContentRepository(InMemoryContentStore(entries)) }
    private val labels by lazy {
        listOf(visibleId, lockedId, ContentId("spell.does-not-exist")).associateWith { labelOf(repository.entry(it, SpoilerProfile(Act.PROLOGUE))) }
    }
    private val notes = listOf(
        Note(3, "Acheter des parchemins avant de quitter le camp\nEt du pain", NoteTarget.OnAct(Act.ACT_1), 3),
        Note(2, "À essayer sur le prochain groupe d'ennemis", NoteTarget.OnEntry(visibleId), 2),
        Note(1, "Revoir cette fiche plus tard", NoteTarget.OnEntry(lockedId), 1),
        Note(0, "Idée de build", null, 0),
    )

    private fun texts(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() + node.children.flatMap(::texts)

    private fun shot(name: String, mode: ThemeMode = ThemeMode.SOMBRE, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                CarnetTheme(mode = mode, reducedMotion = true) { Box(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) { content() } }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun `une fiche masquée rattachée n'est jamais nommée`() {
        assertThat(labels[lockedId]).isEqualTo(EntryLabel.Hidden)
        assertThat(labels[ContentId("spell.does-not-exist")]).isEqualTo(EntryLabel.Missing)
        compose.setContent { CarnetTheme(reducedMotion = true) { NotesScreen(NotesUiState(notes, labels), {}, {}, {}) } }
        compose.waitForIdle()
        val shown = texts(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        val locked = GameData.entries.single { it.id == lockedId }.name
        assertThat(shown.none { it.contains(locked.fr) || it.contains(locked.en) }).isTrue()
        assertThat(shown).contains("Une fiche masquée")
    }

    @Test fun notes_sombre() = shot("notes_sombre") { NotesScreen(NotesUiState(notes, labels), {}, {}, {}) }
    @Test fun notes_vide_parchemin() = shot("notes_vide_parchemin", ThemeMode.PARCHEMIN) { NotesScreen(NotesUiState(emptyList(), emptyMap()), {}, {}, {}) }
    @Test fun note_editeur_sombre() = shot("note_editeur_sombre") {
        NoteEditorScreen(NoteEditorUiState(2, notes[1].text, notes[1].target, labels), {}, {}, {}, {}, {})
    }
    @Test fun note_nouvelle_police200() = shot("note_nouvelle_police200", fontScale = 2f) {
        NoteEditorScreen(NoteEditorUiState(null, "", NoteTarget.OnAct(Act.ACT_1)), {}, {}, {}, {}, {})
    }
}
