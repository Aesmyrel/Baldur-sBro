package fr.carnetdefaerun.feature.encyclopedia

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.SpoilerGate
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Test de fuite sur l'écran (PLAN §2.5, point 3) : pour chaque profil de référence, chaque fiche visible qui a
 * une partie verrouillée est rendue, et aucun texte de cette partie (bloc, nom d'une fiche liée masquée, marchand
 * d'une provenance masquée) n'apparaît dans l'arbre sémantique.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h917dp")
class EntryScreenLeakTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val entries = GameData.entries
    private val byId = entries.associateBy { it.id }
    private val repository = ContentRepository(InMemoryContentStore(entries))
    private val profiles = listOf(SpoilerProfile(Act.PROLOGUE), SpoilerProfile(Act.ACT_1), SpoilerProfile(Act.ACT_2, Tolerance.LIGHT))

    /** Textes que [profile] doit cacher dans la fiche de [entry] (sa fiche étant visible). */
    private fun lockedTexts(entry: Entry, profile: SpoilerProfile, visibleNames: Set<String>): Pair<List<String>, List<String>> {
        val blocks = entry.blocks.filter { !SpoilerGate.isVisible(it.tag, profile) }.map { it.text }
        val names = entry.links.mapNotNull { byId[it.target] }.filter { !SpoilerGate.isVisible(it.titleTag, profile) }
            .flatMap { listOf(it.name.fr, it.name.en) }.filter { it !in visibleNames && it != entry.name.fr && it != entry.name.en }
        val vendors = (entry.facts as? ItemFacts)?.sources.orEmpty().filter { !SpoilerGate.isVisible(it.tag, profile) }.mapNotNull { it.vendor }
        return blocks to (names + vendors).distinct()
    }

    private fun texts(node: SemanticsNode): List<String> =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
            node.children.flatMap(::texts)

    @Test
    fun `aucune partie verrouillée d'une fiche visible n'apparaît à l'écran`() {
        var state by mutableStateOf<EntryUiState>(EntryUiState.Loading)
        compose.setContent { CarnetTheme(reducedMotion = true) { EntryScreen(state, EntryActions()) } }
        var checked = 0
        for (profile in profiles) {
            // Un nom porté aussi par une fiche visible peut s'afficher légitimement (homonymes).
            val visibleNames = entries.filter { SpoilerGate.isVisible(it.titleTag, profile) }.flatMap { listOf(it.name.fr, it.name.en) }.toSet()
            val candidates = entries.filter { SpoilerGate.isVisible(it.tag, profile) }
                .map { it to lockedTexts(it, profile, visibleNames) }
                .filter { (_, locked) -> locked.first.isNotEmpty() || locked.second.isNotEmpty() }
            for ((entry, locked) in candidates) {
                val view = (repository.entry(entry.id, profile) as Gated.Visible).value
                state = EntryUiState.Visible(view, favorite = false)
                compose.waitForIdle()
                val shown = texts(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
                val where = "${entry.id} pour $profile"
                for (block in locked.first) assertWithMessage("$where : bloc verrouillé affiché").that(shown.none { it.contains(block) }).isTrue()
                for (name in locked.second) assertWithMessage("$where : « $name » affiché").that(shown.none { it == name }).isTrue()
                checked++
            }
        }
        assertWithMessage("fiches contrôlées").that(checked).isGreaterThan(0)
    }
}
