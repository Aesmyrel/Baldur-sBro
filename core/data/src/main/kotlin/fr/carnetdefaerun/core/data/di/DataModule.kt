package fr.carnetdefaerun.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.SqliteContentStore
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.data.navigation.DataStoreNavigationStateStore
import fr.carnetdefaerun.core.data.navigation.NavigationStateStore
import fr.carnetdefaerun.core.data.preferences.DataStoreUserPreferencesRepository
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import java.io.File
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {
    @Binds abstract fun preferences(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository
    @Binds abstract fun navigation(impl: DataStoreNavigationStateStore): NavigationStateStore
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    /** Préférences du joueur : incluses dans la sauvegarde Android (voir les règles d'extraction de l'appli). */
    @Provides @Singleton
    fun preferencesStore(@ApplicationContext context: Context): DataStore<Preferences> =
        resilientStore { context.preferencesDataStoreFile("preferences") }

    /** Base de contenu embarquée : lue seulement à travers [ContentRepository]. */
    @Provides @Singleton
    fun contentStore(@ApplicationContext context: Context): ContentStore = SqliteContentStore(context)

    @Provides @Singleton
    internal fun contentRepository(store: ContentStore): ContentRepository = ContentRepository(store)

    @Provides @Singleton @Named("navigation")
    fun navigationStore(@ApplicationContext context: Context): DataStore<Preferences> =
        resilientStore { context.preferencesDataStoreFile("navigation") }
}

/**
 * Un fichier corrompu (sauvegarde restaurée, écriture interrompue) est remplacé par un fichier vide :
 * l'appli repart des valeurs par défaut au lieu de planter à chaque démarrage.
 */
fun resilientStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
    produceFile = produceFile,
)
