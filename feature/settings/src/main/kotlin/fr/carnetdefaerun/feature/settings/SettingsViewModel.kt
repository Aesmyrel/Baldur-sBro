package fr.carnetdefaerun.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.backup.BackupRepository
import fr.carnetdefaerun.core.data.backup.ImportResult
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.model.settings.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Message de la section Données, après un export ou un import. */
enum class DataMessage { EXPORTED, EXPORT_FAILED, IMPORTED, IMPORT_NOT_CARNET, IMPORT_TOO_NEW, IMPORT_TOO_LARGE, IMPORT_FAILED }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val backup: BackupRepository,
) : ViewModel() {
    val message = MutableStateFlow<DataMessage?>(null)

    /** Nombre de lignes écartées au dernier import (lignes invalides du fichier). */
    val skipped = MutableStateFlow(0)

    /** Écrit la sauvegarde avec [write] (flux du fichier choisi) ; tout échec est dit, rien n'est perdu. */
    fun export(write: suspend (ByteArray) -> Unit) = viewModelScope.launch {
        message.value = runCatching { write(withContext(Dispatchers.IO) { backup.export() }) }
            .fold({ DataMessage.EXPORTED }, { DataMessage.EXPORT_FAILED })
    }

    fun import(read: suspend () -> ByteArray?) = viewModelScope.launch {
        val bytes = runCatching { read() }.getOrNull()
        if (bytes == null) { message.value = DataMessage.IMPORT_FAILED; return@launch }
        message.value = when (val result = withContext(Dispatchers.IO) { backup.import(bytes) }) {
            is ImportResult.Done -> { skipped.value = result.skipped; DataMessage.IMPORTED }
            ImportResult.NotACarnet -> DataMessage.IMPORT_NOT_CARNET
            ImportResult.TooNew -> DataMessage.IMPORT_TOO_NEW
            ImportResult.TooLarge -> DataMessage.IMPORT_TOO_LARGE
        }
    }

    /** null tant que les réglages ne sont pas relus. */
    val preferences: StateFlow<UserPreferences?> =
        repository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (UserPreferences) -> UserPreferences) = viewModelScope.launch { repository.update(transform) }
}
