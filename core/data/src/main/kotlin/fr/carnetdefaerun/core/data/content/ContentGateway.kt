package fr.carnetdefaerun.core.data.content

import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.data.user.PlayerRepository
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.NameStatus
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Porte du contenu pour les écrans : le dépôt filtré par le verrou, nourri en continu par le profil de spoilers,
 * les révélations et les noms corrigés du joueur. Tout changement de réglage ou de révélation se propage aussitôt.
 */
@Singleton
class ContentGateway @Inject internal constructor(
    private val repository: ContentRepository,
    private val preferences: UserPreferencesRepository,
    private val player: PlayerRepository,
) {
    private val context: Flow<Context> = combine(preferences.preferences, player.revealed, player.nameCorrections) { prefs, revealed, names ->
        Context(prefs.spoilerProfile, prefs.showActOfHidden, revealed, names)
    }.distinctUntilChanged()

    fun list(type: EntryType): Flow<EntryList> = context.map { c ->
        val list = repository.list(type, c.profile, c.revealed)
        list.copy(visible = list.visible.map { it.copy(name = c.correct(it.id, it.name)) }.sortedBy { Folding.fold(it.name.fr) })
    }.flowOn(Dispatchers.IO)

    fun entry(id: ContentId): Flow<Gated<EntryView>?> = context.map { c ->
        when (val gated = repository.entry(id, c.profile, c.revealed, c.showActOfHidden)) {
            is Gated.Visible -> Gated.Visible(
                gated.value.copy(name = c.correct(id, gated.value.name), links = gated.value.links.map { it.copy(name = c.correct(it.id, it.name)) }),
            )
            else -> gated
        }
    }.flowOn(Dispatchers.IO)

    val favorites: Flow<Set<ContentId>> = player.favorites

    suspend fun reveal(key: RevealKey) = player.reveal(key)
    suspend fun correctName(id: ContentId, fr: String) = player.correctName(id, fr)
    suspend fun resetName(id: ContentId) = player.resetName(id)
    suspend fun setFavorite(id: ContentId, favorite: Boolean) = player.setFavorite(id, favorite)

    private data class Context(
        val profile: SpoilerProfile,
        val showActOfHidden: Boolean,
        val revealed: Set<String>,
        val names: Map<ContentId, String>,
    ) {
        /** Nom corrigé par le joueur : il prime, avec le statut « corrigé par vous ». */
        fun correct(id: ContentId, name: Name): Name = names[id]?.let { name.copy(fr = it, status = NameStatus.CORRECTED, source = null) } ?: name
    }
}
