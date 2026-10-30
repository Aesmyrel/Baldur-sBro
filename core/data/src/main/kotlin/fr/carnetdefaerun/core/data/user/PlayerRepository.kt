package fr.carnetdefaerun.core.data.user

import fr.carnetdefaerun.core.database.user.FavoriteEntity
import fr.carnetdefaerun.core.database.user.NameCorrectionEntity
import fr.carnetdefaerun.core.database.user.RevealedEntity
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Ce que le joueur a fait du contenu : révélations, noms corrigés, favoris (base du joueur). */
interface PlayerRepository {
    /** Jetons des contenus dévoilés. */
    val revealed: Flow<Set<String>>
    /** Noms français corrigés par le joueur, prioritaires sur la donnée embarquée. */
    val nameCorrections: Flow<Map<ContentId, String>>
    val favorites: Flow<Set<ContentId>>

    suspend fun reveal(key: RevealKey)
    suspend fun hideAgain()
    suspend fun correctName(id: ContentId, fr: String)
    suspend fun resetName(id: ContentId)
    suspend fun setFavorite(id: ContentId, favorite: Boolean)
}

@Singleton
class RoomPlayerRepository @Inject constructor(private val db: UserDatabase) : PlayerRepository {
    private fun now() = System.currentTimeMillis()

    override val revealed: Flow<Set<String>> = db.revealed().tokens().map { it.toSet() }.distinctUntilChanged()
    override val nameCorrections: Flow<Map<ContentId, String>> =
        db.nameCorrections().all().map { list -> list.associate { ContentId(it.entryId) to it.fr } }.distinctUntilChanged()
    override val favorites: Flow<Set<ContentId>> = db.favorites().ids().map { ids -> ids.map(::ContentId).toSet() }.distinctUntilChanged()

    override suspend fun reveal(key: RevealKey) = db.revealed().insert(RevealedEntity(key.token, now()))
    override suspend fun hideAgain() = db.revealed().clear()

    override suspend fun correctName(id: ContentId, fr: String) {
        val trimmed = fr.trim()
        if (trimmed.isEmpty()) db.nameCorrections().delete(id.value) else db.nameCorrections().upsert(NameCorrectionEntity(id.value, trimmed, now()))
    }

    override suspend fun resetName(id: ContentId) = db.nameCorrections().delete(id.value)

    override suspend fun setFavorite(id: ContentId, favorite: Boolean) {
        if (favorite) db.favorites().insert(FavoriteEntity(id.value, now())) else db.favorites().delete(id.value)
    }
}
