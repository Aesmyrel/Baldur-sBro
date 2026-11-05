package fr.carnetdefaerun.core.data.backup

import fr.carnetdefaerun.core.data.builds.PartyRepository
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.database.user.BuildEntity
import fr.carnetdefaerun.core.database.user.FavoriteEntity
import fr.carnetdefaerun.core.database.user.NameCorrectionEntity
import fr.carnetdefaerun.core.database.user.NoteEntity
import fr.carnetdefaerun.core.database.user.RevealedEntity
import fr.carnetdefaerun.core.database.user.UserDataSnapshot
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.data.notes.NoteRepository
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.builds.BuildFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.settings.UserPreferences
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Fichier de sauvegarde `.carnet` : JSON compressé (gzip), versionné. Il contient tout ce qui appartient au joueur :
 * réglages, révélations (jetons opaques), noms corrigés, favoris, builds, groupe, notes. Jamais la base de contenu.
 */
@Serializable
data class CarnetFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val preferences: UserPreferences,
    val revealed: List<RevealedRow> = emptyList(),
    val names: List<NameRow> = emptyList(),
    val favorites: List<FavoriteRow> = emptyList(),
    val builds: List<BuildRow> = emptyList(),
    /** Groupe de quatre : rang de chaque membre dans [builds], ou null pour un emplacement libre. */
    val party: List<Int?> = emptyList(),
    val notes: List<NoteRow> = emptyList(),
) {
    companion object {
        const val FORMAT = "carnet-de-faerun"
        const val VERSION = 1
    }
}

@Serializable data class RevealedRow(val token: String, val at: Long)
@Serializable data class NameRow(val id: String, val fr: String, val at: Long)
@Serializable data class FavoriteRow(val id: String, val at: Long)
@Serializable data class BuildRow(val build: Build, val createdAt: Long, val updatedAt: Long)
@Serializable data class NoteRow(val text: String, val targetKind: String? = null, val targetId: String? = null, val createdAt: Long, val updatedAt: Long)

/** Issue d'un import : réussite (avec ce qui a été repris) ou refus, sans rien changer. */
sealed interface ImportResult {
    data class Done(val builds: Int, val notes: Int, val skipped: Int) : ImportResult
    data object NotACarnet : ImportResult
    data object TooNew : ImportResult
    data object TooLarge : ImportResult
}

/**
 * Export et import de la sauvegarde. L'import lit un fichier non fiable : tailles bornées (fichier et contenu
 * décompressé), format et version vérifiés, chaque ligne validée (identifiants, jetons, longueurs) ; ce qui est
 * invalide est écarté et compté. Le remplacement se fait en une transaction : tout ou rien.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val db: UserDatabase,
    private val preferences: UserPreferencesRepository,
    private val party: PartyRepository,
) {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    suspend fun export(now: Long = System.currentTimeMillis()): ByteArray {
        val snapshot = db.backup().snapshot()
        val builds = snapshot.builds.mapNotNull { e ->
            runCatching { BuildFormat.json.decodeFromString(Build.serializer(), e.payload) }.getOrNull()?.let { e.id to BuildRow(it, e.createdAt, e.updatedAt) }
        }
        val rank = builds.mapIndexed { i, (id, _) -> id to i }.toMap()
        val file = CarnetFile(
            exportedAt = now,
            preferences = preferences.preferences.first(),
            revealed = snapshot.revealed.map { RevealedRow(it.token, it.revealedAt) },
            names = snapshot.names.map { NameRow(it.entryId, it.fr, it.correctedAt) },
            favorites = snapshot.favorites.map { FavoriteRow(it.entryId, it.addedAt) },
            builds = builds.map { it.second },
            party = party.slots.first().map { id -> id?.let(rank::get) },
            notes = snapshot.notes.map { NoteRow(it.text, it.targetKind, it.targetId, it.createdAt, it.updatedAt) },
        )
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(json.encodeToString(CarnetFile.serializer(), file).toByteArray(Charsets.UTF_8)) }
        return out.toByteArray()
    }

    suspend fun import(bytes: ByteArray): ImportResult {
        if (bytes.size > MAX_FILE_BYTES) return ImportResult.TooLarge
        val text = runCatching { inflate(bytes) }.getOrElse { e -> return if (e is TooLarge) ImportResult.TooLarge else ImportResult.NotACarnet }
        val file = runCatching { json.decodeFromString(CarnetFile.serializer(), text) }.getOrNull() ?: return ImportResult.NotACarnet
        if (file.format != CarnetFile.FORMAT) return ImportResult.NotACarnet
        if (file.version > CarnetFile.VERSION) return ImportResult.TooNew

        var skipped = 0
        fun <T> keep(rows: List<T>, valid: (T) -> Boolean) = rows.filter { valid(it).also { ok -> if (!ok) skipped++ } }
        val revealed = keep(file.revealed) { TOKEN.matches(it.token) }.distinctBy { it.token }
        val names = keep(file.names) { ContentId.PATTERN.matches(it.id) && it.fr.isNotBlank() && it.fr.length <= 120 }.distinctBy { it.id }
        val favorites = keep(file.favorites) { ContentId.PATTERN.matches(it.id) }.distinctBy { it.id }
        val builds = keep(file.builds) { it.build.levels.size <= 12 }.map { it.copy(build = it.build.copy(name = BuildCode.cleanName(it.build.name))) }
        val notes = keep(file.notes) { it.text.length <= NoteRepository.MAX_LENGTH }

        val ids = db.backup().replaceAll(
            UserDataSnapshot(
                revealed = revealed.map { RevealedEntity(it.token, it.at) },
                names = names.map { NameCorrectionEntity(it.id, it.fr.trim(), it.at) },
                favorites = favorites.map { FavoriteEntity(it.id, it.at) },
                builds = builds.map { BuildEntity(name = it.build.name, payload = BuildFormat.json.encodeToString(Build.serializer(), it.build), createdAt = it.createdAt, updatedAt = it.updatedAt) },
                notes = notes.map { NoteEntity(text = it.text, targetKind = it.targetKind, targetId = it.targetId, createdAt = it.createdAt, updatedAt = it.updatedAt) },
            ),
        )
        // Réglages repris, sauf le mode session (propre à l'appareil) ; une taille de lecture hors de la liste revient à 100 %.
        val scale = file.preferences.readingScale.takeIf { it in UserPreferences.READING_SCALES } ?: 1f
        preferences.update { file.preferences.copy(sessionActive = false, readingScale = scale) }
        for (slot in 0 until PartyRepository.SIZE) party.set(slot, file.party.getOrNull(slot)?.let(ids::getOrNull))
        return ImportResult.Done(builds = builds.size, notes = notes.size, skipped = skipped)
    }

    private object TooLarge : RuntimeException()

    private fun inflate(bytes: ByteArray): String {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(8_192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_JSON_BYTES) throw TooLarge
            }
        }
        return out.toString(Charsets.UTF_8)
    }

    companion object {
        const val MAX_FILE_BYTES = 8 * 1024 * 1024
        const val MAX_JSON_BYTES = 32 * 1024 * 1024
        const val EXTENSION = "carnet"
        private val TOKEN = Regex("^[0-9a-f]{24}$")
    }
}
