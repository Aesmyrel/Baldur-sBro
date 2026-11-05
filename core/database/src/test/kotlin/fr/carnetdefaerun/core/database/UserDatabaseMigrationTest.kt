package fr.carnetdefaerun.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.database.user.BuildEntity
import fr.carnetdefaerun.core.database.user.UserDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** La base du joueur passe de la version 1 à la version 2 sans rien perdre : révélations, noms, favoris restent. */
@RunWith(AndroidJUnit4::class)
class UserDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), UserDatabase::class.java)

    @Test
    fun `migration 1 vers 2, données conservées et table des builds créée`() = runTest {
        helper.createDatabase(NAME, 1).apply {
            execSQL("INSERT INTO revealed (token, revealedAt) VALUES ('jeton', 1)")
            execSQL("INSERT INTO name_correction (entryId, fr, correctedAt) VALUES ('spell.fireball', 'Boule de feu', 2)")
            execSQL("INSERT INTO favorite (entryId, addedAt) VALUES ('spell.fireball', 3)")
            close()
        }
        helper.runMigrationsAndValidate(NAME, 2, true).close()

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), UserDatabase::class.java, NAME).allowMainThreadQueries().build()
        try {
            assertThat(db.revealed().tokens().first()).containsExactly("jeton")
            assertThat(db.nameCorrections().all().first().single().fr).isEqualTo("Boule de feu")
            assertThat(db.favorites().ids().first()).containsExactly("spell.fireball")
            val id = db.builds().insert(BuildEntity(name = "Essai", payload = "{}", createdAt = 4, updatedAt = 4))
            assertThat(db.builds().byId(id).first()?.name).isEqualTo("Essai")
        } finally {
            db.close()
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
