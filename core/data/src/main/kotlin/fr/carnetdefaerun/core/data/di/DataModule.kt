package fr.carnetdefaerun.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.carnetdefaerun.core.data.navigation.DataStoreNavigationStateStore
import fr.carnetdefaerun.core.data.navigation.NavigationStateStore
import fr.carnetdefaerun.core.data.preferences.DataStoreUserPreferencesRepository
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import javax.inject.Named
import javax.inject.Singleton

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
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("preferences") }

    @Provides @Singleton @Named("navigation")
    fun navigationStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("navigation") }
}
