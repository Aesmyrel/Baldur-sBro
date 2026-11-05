package fr.carnetdefaerun.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.data.builds.RoomBuildRepository
import fr.carnetdefaerun.core.data.builds.RulesRepository
import fr.carnetdefaerun.core.database.user.BuildEntity
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Builds enregistrés dans la base du joueur, et profils de règles lus dans la base de contenu. */
@RunWith(AndroidJUnit4::class)
class BuildRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UserDatabase::class.java).allowMainThreadQueries().build()
    private val repository = RoomBuildRepository(db)

    @After fun close() = db.close()

    @Test
    fun `créer, modifier, lister et supprimer`() = runTest {
        val build = Build(name = "Rempart", majorBonus = Ability.STRENGTH, minorBonus = Ability.CONSTITUTION, levels = listOf(BuildLevel(ContentId("class.fighter"))))
        val id = repository.create(build)
        assertThat(repository.build(id).first()?.build).isEqualTo(build)
        val renamed = build.copy(name = "x".repeat(80))
        repository.save(id, renamed)
        assertThat(repository.build(id).first()?.build?.name).hasLength(60)
        val other = repository.create(build.copy(name = "Second"))
        assertThat(repository.builds.first().map { it.id }).containsExactly(id, other)
        repository.delete(id)
        assertThat(repository.builds.first().map { it.build.name }).containsExactly("Second")
    }

    @Test
    fun `un enregistrement illisible est ignoré`() = runTest {
        db.builds().insert(BuildEntity(name = "abîmé", payload = "{pas du json", createdAt = 1, updatedAt = 1))
        val id = repository.create(Build(name = "Sain"))
        assertThat(repository.builds.first().map { it.id }).containsExactly(id)
    }

    @Test
    fun `profils de règles tirés de la base de contenu`() = runTest {
        val rules = RulesRepository(InMemoryContentStore(GameData.entries)).data()
        assertThat(rules.classes).hasSize(12)
        assertThat(rules.subclasses).hasSize(58)
        val sheet = CharacterSheetCalculator.compute(
            Build(name = "Essai", majorBonus = Ability.CONSTITUTION, minorBonus = Ability.DEXTERITY, levels = listOf(BuildLevel(ContentId("class.wizard")))),
            rules,
        )
        assertThat(sheet.hitPoints.total).isEqualTo(6) // Constitution 8 + 2 = 10 : modificateur 0
    }
}
