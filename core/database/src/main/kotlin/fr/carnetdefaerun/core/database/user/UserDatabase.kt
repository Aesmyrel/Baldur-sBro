package fr.carnetdefaerun.core.database.user

import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Contenu dévoilé par le joueur, sous forme de jeton opaque (`RevealKey.token`) : la base ne contient
 * aucun identifiant de contenu en clair pour ce qui était masqué.
 */
@Entity(tableName = "revealed")
data class RevealedEntity(@PrimaryKey val token: String, val revealedAt: Long)

/** Nom français corrigé par le joueur (prioritaire sur la donnée embarquée, CLAUDE.md règle 6). */
@Entity(tableName = "name_correction")
data class NameCorrectionEntity(@PrimaryKey val entryId: String, val fr: String, val correctedAt: Long)

@Entity(tableName = "favorite")
data class FavoriteEntity(@PrimaryKey val entryId: String, val addedAt: Long)

@Dao
interface RevealedDao {
    @Query("SELECT token FROM revealed")
    fun tokens(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: RevealedEntity)

    /** Tout remasquer (réglages). */
    @Query("DELETE FROM revealed")
    suspend fun clear()
}

@Dao
interface NameCorrectionDao {
    @Query("SELECT * FROM name_correction")
    fun all(): Flow<List<NameCorrectionEntity>>

    @Upsert
    suspend fun upsert(entity: NameCorrectionEntity)

    @Query("DELETE FROM name_correction WHERE entryId = :entryId")
    suspend fun delete(entryId: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT entryId FROM favorite ORDER BY addedAt DESC")
    fun ids(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE entryId = :entryId")
    suspend fun delete(entryId: String)
}

/**
 * Build enregistré : les choix du joueur en JSON (`Build`, core:model), jamais les valeurs calculées.
 * Les références au contenu passent par les identifiants stables.
 */
@Entity(tableName = "build")
data class BuildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val payload: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface BuildDao {
    @Query("SELECT * FROM build ORDER BY updatedAt DESC, id DESC")
    fun all(): Flow<List<BuildEntity>>

    @Query("SELECT * FROM build WHERE id = :id")
    fun byId(id: Long): Flow<BuildEntity?>

    @Insert
    suspend fun insert(entity: BuildEntity): Long

    @Query("UPDATE build SET name = :name, payload = :payload, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, name: String, payload: String, updatedAt: Long)

    @Query("DELETE FROM build WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * Note du joueur, rattachée ou non : à un acte (`targetKind` = « act », `targetId` = nom de l'acte) ou à une fiche
 * (`targetKind` = « entry », `targetId` = identifiant stable). Le texte est celui du joueur, jamais filtré.
 */
@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val targetKind: String? = null,
    val targetId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY updatedAt DESC, id DESC")
    fun all(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE id = :id")
    fun byId(id: Long): Flow<NoteEntity?>

    @Insert
    suspend fun insert(entity: NoteEntity): Long

    @Query("UPDATE note SET text = :text, targetKind = :targetKind, targetId = :targetId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, text: String, targetKind: String?, targetId: String?, updatedAt: Long)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: Long)
}

/** Tout le contenu de la base du joueur, pour l'export et l'import d'une sauvegarde. */
data class UserDataSnapshot(
    val revealed: List<RevealedEntity>,
    val names: List<NameCorrectionEntity>,
    val favorites: List<FavoriteEntity>,
    val builds: List<BuildEntity>,
    val notes: List<NoteEntity>,
)

/** Lecture et remplacement de toute la base du joueur, en une transaction : un import réussit entier ou ne change rien. */
@Dao
abstract class BackupDao {
    @Query("SELECT * FROM revealed") protected abstract suspend fun revealed(): List<RevealedEntity>
    @Query("SELECT * FROM name_correction") protected abstract suspend fun names(): List<NameCorrectionEntity>
    @Query("SELECT * FROM favorite") protected abstract suspend fun favorites(): List<FavoriteEntity>
    @Query("SELECT * FROM build ORDER BY id") protected abstract suspend fun builds(): List<BuildEntity>
    @Query("SELECT * FROM note ORDER BY id") protected abstract suspend fun notes(): List<NoteEntity>

    @Query("DELETE FROM revealed") protected abstract suspend fun clearRevealed()
    @Query("DELETE FROM name_correction") protected abstract suspend fun clearNames()
    @Query("DELETE FROM favorite") protected abstract suspend fun clearFavorites()
    @Query("DELETE FROM build") protected abstract suspend fun clearBuilds()
    @Query("DELETE FROM note") protected abstract suspend fun clearNotes()

    @Insert protected abstract suspend fun insertRevealed(rows: List<RevealedEntity>)
    @Insert protected abstract suspend fun insertNames(rows: List<NameCorrectionEntity>)
    @Insert protected abstract suspend fun insertFavorites(rows: List<FavoriteEntity>)
    @Insert protected abstract suspend fun insertBuild(row: BuildEntity): Long
    @Insert protected abstract suspend fun insertNotes(rows: List<NoteEntity>)

    @Transaction
    open suspend fun snapshot(): UserDataSnapshot = UserDataSnapshot(revealed(), names(), favorites(), builds(), notes())

    /** Remplace tout ; renvoie les nouveaux identifiants des builds, dans l'ordre de [UserDataSnapshot.builds]. */
    @Transaction
    open suspend fun replaceAll(data: UserDataSnapshot): List<Long> {
        clearRevealed(); clearNames(); clearFavorites(); clearBuilds(); clearNotes()
        insertRevealed(data.revealed)
        insertNames(data.names)
        insertFavorites(data.favorites)
        val ids = data.builds.map { insertBuild(it.copy(id = 0)) }
        insertNotes(data.notes.map { it.copy(id = 0) })
        return ids
    }
}

/**
 * Base du joueur (`user.db`) : séparée de la base de contenu, sauvegardée, jamais remplacée par une mise à jour
 * des données (les références au contenu passent par les identifiants stables). Schéma exporté dans `schemas/`.
 */
@Database(
    entities = [RevealedEntity::class, NameCorrectionEntity::class, FavoriteEntity::class, BuildEntity::class, NoteEntity::class],
    version = 3,
    exportSchema = true,
    // Version 2 : table des builds (phase 4) ; version 3 : notes (phase 6). Migrations automatiques, vérifiées par
    // UserDatabaseMigrationTest.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun revealed(): RevealedDao
    abstract fun nameCorrections(): NameCorrectionDao
    abstract fun favorites(): FavoriteDao
    abstract fun builds(): BuildDao
    abstract fun notes(): NoteDao
    abstract fun backup(): BackupDao

    companion object {
        const val FILE_NAME = "user.db"
    }
}
