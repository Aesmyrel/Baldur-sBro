package fr.carnetdefaerun.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.data.user.RoomPlayerRepository
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.model.content.Block
import fr.carnetdefaerun.core.model.content.BlockKind
import fr.carnetdefaerun.core.model.content.ConditionFacts
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.NameStatus
import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.core.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Révélations, noms corrigés et favoris : enregistrés dans la base du joueur et appliqués par la porte de contenu. */
@RunWith(AndroidJUnit4::class)
class PlayerGatewayTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UserDatabase::class.java).allowMainThreadQueries().build()
    private val player = RoomPlayerRepository(db)
    private val prefs = FakeUserPreferencesRepository(UserPreferences(currentAct = Act.ACT_1))

    private val early = SpoilerTag(Act.ACT_1, SpoilerLevel.NONE, SpoilerLevel.NONE)
    private val late = SpoilerTag(Act.ACT_3, SpoilerLevel.MAJOR, SpoilerLevel.NONE)
    private fun entry(id: String, fr: String, tag: SpoilerTag, blocks: List<Block> = emptyList()) = Entry(
        id = ContentId(id), type = EntryType.CONDITION, name = Name(fr, fr, NameStatus.UNOFFICIAL), tag = tag, titleTag = tag,
        licence = "carnet", facts = ConditionFacts(), blocks = blocks,
    )
    private val entries = listOf(
        entry("condition.visible", "Visible", early, listOf(Block("effet-1", BlockKind.EFFECT, "Texte de l’acte III", late))),
        entry("condition.cachee", "Cachée", late),
    )
    private val gateway = ContentGateway(ContentRepository(FakeStore(entries)), prefs, player)

    @After fun close() = db.close()

    @Test
    fun `révéler un bloc le rend visible, et seul lui`() = runTest {
        val before = gateway.entry(ContentId("condition.visible")).first() as Gated.Visible
        val hidden = before.value.blocks.single() as Gated.Hidden
        gateway.reveal(hidden.key)
        val after = gateway.entry(ContentId("condition.visible")).first() as Gated.Visible
        assertThat(after.value.blocks.single()).isInstanceOf(Gated.Visible::class.java)
        // La base du joueur ne contient que le jeton opaque, pas l'identifiant du bloc.
        assertThat(db.revealed().tokens().first()).containsExactly(hidden.key.token)
        assertThat(gateway.entry(ContentId("condition.cachee")).first()).isInstanceOf(Gated.Hidden::class.java)
    }

    @Test
    fun `un nom corrigé prime, partout, avec le statut corrigé`() = runTest {
        gateway.correctName(ContentId("condition.visible"), "  Bien visible ")
        val name = (gateway.entry(ContentId("condition.visible")).first() as Gated.Visible).value.name
        assertThat(name.fr).isEqualTo("Bien visible")
        assertThat(name.status).isEqualTo(NameStatus.CORRECTED)
        assertThat(gateway.list(EntryType.CONDITION).first().visible.single().name.fr).isEqualTo("Bien visible")
        gateway.resetName(ContentId("condition.visible"))
        assertThat(gateway.list(EntryType.CONDITION).first().visible.single().name.status).isEqualTo(NameStatus.UNOFFICIAL)
    }

    @Test
    fun `la liste suit le réglage d'acte, et un voile générique signale le reste`() = runTest {
        val act1 = gateway.list(EntryType.CONDITION).first()
        assertThat(act1.visible.map { it.id.value }).containsExactly("condition.visible")
        assertThat(act1.someHidden).isTrue()
        prefs.update { it.copy(currentAct = Act.EPILOGUE, tolerance = fr.carnetdefaerun.core.model.spoiler.Tolerance.FULL) }
        assertThat(gateway.list(EntryType.CONDITION).first().visible).hasSize(2)
    }

    @Test
    fun `favoris ajoutés et retirés`() = runTest {
        gateway.setFavorite(ContentId("condition.visible"), true)
        assertThat(gateway.favorites.first()).containsExactly(ContentId("condition.visible"))
        gateway.setFavorite(ContentId("condition.visible"), false)
        assertThat(gateway.favorites.first()).isEmpty()
    }
}
