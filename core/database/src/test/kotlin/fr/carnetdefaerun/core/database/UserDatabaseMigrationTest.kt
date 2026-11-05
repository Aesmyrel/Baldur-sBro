package fr.carnetdefaerun.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.database.user.BuildEntity
import fr.carnetdefaerun.core.database.user.NoteEntity
import fr.carnetdefaerun.core.database.user.UserDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La base du joueur passe de la version 1 à la version 3 sans rien perdre : révélations, noms, favoris restent. */
@RunWith(AndroidJUnit4::class)
class UserDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), UserDatabase::class.java)

    @Test
    fun `migrations 1, 2, 3, données conservées, tables des builds et des notes créées`() = runTest {
        helper.createDatabase(NAME, 1).apply {
            execSQL("INSERT INTO revealed (token, revealedAt) VALUES ('jeton', 1)")
            execSQL("INSERT INTO name_correction (entryId, fr, correctedAt) VALUES ('spell.fireball', 'Boule de feu', 2)")
            execSQL("INSERT INTO favorite (entryId, addedAt) VALUES ('spell.fireball', 3)")
            close()
        }
        helper.runMigrationsAndValidate(NAME, 2, true).apply {
            // Un build enregistré en version 2 doit traverser la migration vers la version 3.
            execSQL("INSERT INTO build (name, payload, createdAt, updatedAt) VALUES ('Ancien', '{\"name\":\"Ancien\"}', 1, 1)")
            close()
        }
        helper.runMigrationsAndValidate(NAME, 3, true).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), UserDatabase::class.java, NAME).allowMainThreadQueries().build()
        try {
            assertThat(db.revealed().tokens().first()).containsExactly("jeton")
            assertThat(db.nameCorrections().all().first().single().fr).isEqualTo("Boule de feu")
            assertThat(db.favorites().ids().first()).containsExactly("spell.fireball")
            assertThat(db.builds().all().first().map { it.name }).containsExactly("Ancien")
            val id = db.builds().insert(BuildEntity(name = "Essai", payload = "{}", createdAt = 4, updatedAt = 4))
            assertThat(db.builds().byId(id).first()?.name).isEqualTo("Essai")
            val note = db.notes().insert(NoteEntity(text = "Penser au sceau", targetKind = "act", targetId = "ACT_1", createdAt = 5, updatedAt = 5))
            assertThat(db.notes().byId(note).first()?.text).isEqualTo("Penser au sceau")
        } finally {
            db.close()
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
