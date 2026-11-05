package fr.carnetdefaerun.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.data.backup.BackupRepository
import fr.carnetdefaerun.core.data.backup.CarnetFile
import fr.carnetdefaerun.core.data.backup.ImportResult
import fr.carnetdefaerun.core.data.builds.DataStorePartyRepository
import fr.carnetdefaerun.core.data.builds.RoomBuildRepository
import fr.carnetdefaerun.core.data.di.resilientStore
import fr.carnetdefaerun.core.data.notes.NoteTarget
import fr.carnetdefaerun.core.data.notes.RoomNoteRepository
import fr.carnetdefaerun.core.data.user.RoomPlayerRepository
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.testing.FakeUserPreferencesRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Sauvegarde `.carnet` : export puis import sur un appareil vierge sans perte ; fichiers hostiles refusés sans dégât. */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val dir = createTempDirectory("backup").toFile()
    private val opened = mutableListOf<UserDatabase>()

    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UserDatabase::class.java)
        .allowMainThreadQueries().build().also { opened += it }

    private inner class Device(scope: TestScope, name: String, prefs: UserPreferences = UserPreferences()) {
        val db = database()
        val preferences = FakeUserPreferencesRepository(prefs)
        val party = DataStorePartyRepository(resilientStore(scope.backgroundScope) { File(dir, "$name.preferences_pb") })
        val builds = RoomBuildRepository(db)
        val notes = RoomNoteRepository(db)
        val player = RoomPlayerRepository(db)
        val backup = BackupRepository(db, preferences, party)
    }

    @After fun close() = opened.forEach { it.close() }

    @Test
    fun `export puis import sur un appareil vierge, sans perte`() = runTest(UnconfinedTestDispatcher()) {
        val source = Device(this, "source", UserPreferences(currentAct = Act.ACT_2, readingScale = 1.25f))
        source.player.reveal(RevealKey.of("bloc-1"))
        source.player.correctName(ContentId("spell.fireball"), "Boule de feu")
        source.player.setFavorite(ContentId("spell.fireball"), true)
        val first = source.builds.create(Build(name = "Rempart", levels = listOf(BuildLevel(ContentId("class.fighter")))))
        val second = source.builds.create(Build(name = "Pacte"))
        source.party.set(2, second)
        source.party.set(0, first)
        source.notes.create("Penser au sceau", NoteTarget.OnAct(Act.ACT_1))
        source.notes.create("Fiche à relire", NoteTarget.OnEntry(ContentId("spell.fireball")))

        val bytes = source.backup.export()
        val target = Device(this, "cible")
        target.builds.create(Build(name = "À écraser"))
        val result = target.backup.import(bytes)

        assertThat(result).isEqualTo(ImportResult.Done(builds = 2, notes = 2, skipped = 0))
        assertThat(target.player.revealed.first()).isEqualTo(source.player.revealed.first())
        assertThat(target.player.nameCorrections.first()).isEqualTo(source.player.nameCorrections.first())
        assertThat(target.player.favorites.first()).isEqualTo(source.player.favorites.first())
        assertThat(target.builds.builds.first().map { it.build }.toSet()).isEqualTo(source.builds.builds.first().map { it.build }.toSet())
        val byName = target.builds.builds.first().associate { it.build.name to it.id }
        assertThat(target.party.slots.first()).containsExactly(byName["Rempart"], null, byName["Pacte"], null).inOrder()
        assertThat(target.notes.notes.first().map { it.text to it.target }.toSet()).isEqualTo(source.notes.notes.first().map { it.text to it.target }.toSet())
        assertThat(target.preferences.preferences.first().currentAct).isEqualTo(Act.ACT_2)
        assertThat(target.preferences.preferences.first().readingScale).isEqualTo(1.25f)
    }

    @Test
    fun `fichiers hostiles ou étrangers refusés, rien n'est touché`() = runTest(UnconfinedTestDispatcher()) {
        val device = Device(this, "seul")
        val kept = device.builds.create(Build(name = "À garder"))
        assertThat(device.backup.import("bonjour".toByteArray())).isEqualTo(ImportResult.NotACarnet)
        assertThat(device.backup.import(gzip("""{"format":"autre","version":1,"exportedAt":0,"preferences":{}}"""))).isEqualTo(ImportResult.NotACarnet)
        assertThat(device.backup.import(gzip("""{"format":"${CarnetFile.FORMAT}","version":99,"exportedAt":0,"preferences":{}}"""))).isEqualTo(ImportResult.TooNew)
        // Bombe de décompression : 40 Mo d'espaces tiennent en quelques dizaines de Ko.
        assertThat(device.backup.import(gzip(" ".repeat(40 * 1024 * 1024)))).isEqualTo(ImportResult.TooLarge)
        assertThat(device.backup.import(ByteArray(BackupRepository.MAX_FILE_BYTES + 1))).isEqualTo(ImportResult.TooLarge)
        assertThat(device.builds.builds.first().map { it.id }).containsExactly(kept)
    }

    @Test
    fun `lignes invalides écartées et comptées`() = runTest(UnconfinedTestDispatcher()) {
        val device = Device(this, "tri")
        val file = """{"format":"${CarnetFile.FORMAT}","version":1,"exportedAt":0,"preferences":{"readingScale":9.0},
            "revealed":[{"token":"pas-un-jeton","at":1},{"token":"0123456789abcdef01234567","at":2}],
            "names":[{"id":"../etc","fr":"x","at":1},{"id":"spell.fireball","fr":"Boule de feu","at":1}],
            "favorites":[{"id":"Spell Fireball","at":1}],
            "builds":[{"build":{"name":"Ok\u202E"},"createdAt":1,"updatedAt":1}],
            "notes":[{"text":"${"x".repeat(10_001)}","createdAt":1,"updatedAt":1}]}"""
        val result = device.backup.import(gzip(file))
        assertThat(result).isEqualTo(ImportResult.Done(builds = 1, notes = 0, skipped = 4))
        assertThat(device.builds.builds.first().single().build.name).isEqualTo("Ok")
        assertThat(device.preferences.preferences.first().readingScale).isEqualTo(1f)
    }

    private fun gzip(text: String): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray()) }
        return out.toByteArray()
    }
}
