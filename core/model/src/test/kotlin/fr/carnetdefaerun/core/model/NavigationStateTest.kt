package fr.carnetdefaerun.core.model

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.NavigationState
import fr.carnetdefaerun.core.model.navigation.MAX_STACK_DEPTH
import fr.carnetdefaerun.core.model.navigation.TopLevel
import fr.carnetdefaerun.core.model.navigation.normalized
import kotlinx.serialization.json.Json
import org.junit.Test

class NavigationStateTest {

    @Test
    fun `chaque onglet garde sa pile`() {
        val s = NavigationState()
            .push(Destination.LoreEntry("lore.people.githyanki"))
            .select(TopLevel.BUILDS)
            .push(Destination.BuildEditor(3))
            .select(TopLevel.PARTIE)
            .select(TopLevel.BUILDS)
        assertThat(s.visible).containsExactly(Destination.Builds, Destination.BuildEditor(3)).inOrder()
        assertThat(s.stack(TopLevel.PARTIE)).containsExactly(Destination.Home, Destination.LoreEntry("lore.people.githyanki")).inOrder()
    }

    @Test
    fun `retour, puis racine, puis sortie`() {
        val s = NavigationState(current = TopLevel.CODEX).push(Destination.LoreEntry("lore.x.y"))
        val back1 = s.pop()!!
        assertThat(back1.visible).containsExactly(Destination.Codex)
        val back2 = back1.pop()!!
        assertThat(back2.current).isEqualTo(TopLevel.PARTIE)
        assertThat(back2.pop()).isNull()
    }

    @Test
    fun `retoucher l'onglet courant ramène à sa racine`() {
        val s = NavigationState().push(Destination.Settings).select(TopLevel.PARTIE)
        assertThat(s.visible).containsExactly(Destination.Home)
    }

    @Test
    fun `l'état survit à la sérialisation`() {
        val s = NavigationState(current = TopLevel.CODEX).push(Destination.LoreEntry("lore.people.githyanki", anchor = "s1"))
        val json = Json.encodeToString(NavigationState.serializer(), s)
        assertThat(Json.decodeFromString(NavigationState.serializer(), json)).isEqualTo(s)
    }

    @Test
    fun `un état relu est remis d'aplomb`() {
        val damaged = NavigationState(
            current = TopLevel.CODEX,
            stacks = mapOf(
                TopLevel.CODEX to listOf(Destination.LoreEntry("Pas Un Id"), Destination.Settings, Destination.Home),
                TopLevel.BUILDS to List(100) { Destination.BuildEditor(it + 1L) },
            ),
        ).normalized()
        assertThat(damaged.stack(TopLevel.CODEX)).containsExactly(Destination.Codex, Destination.Settings).inOrder()
        assertThat(damaged.stack(TopLevel.BUILDS).first()).isEqualTo(Destination.Builds)
        assertThat(damaged.stack(TopLevel.BUILDS)).hasSize(MAX_STACK_DEPTH)
        assertThat(damaged.stack(TopLevel.BUILDS).last()).isEqualTo(Destination.BuildEditor(100))
        assertThat(damaged.stack(TopLevel.PARTIE)).containsExactly(Destination.Home)
    }
}
