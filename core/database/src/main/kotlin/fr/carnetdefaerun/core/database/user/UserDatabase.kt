package fr.carnetdefaerun.core.database.user

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
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
 * Base du joueur (`user.db`) : séparée de la base de contenu, sauvegardée, jamais remplacée par une mise à jour
 * des données (les références au contenu passent par les identifiants stables). Schéma exporté dans `schemas/`.
 */
@Database(entities = [RevealedEntity::class, NameCorrectionEntity::class, FavoriteEntity::class], version = 1, exportSchema = true)
abstract class UserDatabase : RoomDatabase() {
    abstract fun revealed(): RevealedDao
    abstract fun nameCorrections(): NameCorrectionDao
    abstract fun favorites(): FavoriteDao

    companion object {
        const val FILE_NAME = "user.db"
    }
}
