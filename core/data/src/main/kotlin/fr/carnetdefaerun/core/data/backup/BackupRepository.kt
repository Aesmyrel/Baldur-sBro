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
import fr.carnetdefaerun.core.data.notes.RoomNoteRepository
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
    /** Builds illisibles sur l'appareil d'origine, gardés tels quels. */
    val damagedBuilds: List<DamagedBuildRow> = emptyList(),
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

/** Build illisible, gardé tel quel d'un appareil à l'autre : il reste visible, marqué abîmé, et supprimable. */
@Serializable data class DamagedBuildRow(val name: String, val payload: String, val createdAt: Long, val updatedAt: Long)
@Serializable data class NoteRow(val text: String, val targetKind: String? = null, val targetId: String? = null, val createdAt: Long, val updatedAt: Long)

/** En-tête lu avant tout le reste : une sauvegarde plus récente se reconnaît même si sa structure a changé. */
@Serializable private data class CarnetHeader(val format: String = "", val version: Int = 0)

/** Issue d'un import. [Done.partial] : la base est remplacée, mais les réglages ou le groupe n'ont pas pu l'être. */
sealed interface ImportResult {
    data class Done(val builds: Int, val notes: Int, val skipped: Int, val partial: Boolean = false) : ImportResult
    data object NotACarnet : ImportResult
    data object TooNew : ImportResult
    data object TooLarge : ImportResult
}

/**
 * Export et import de la sauvegarde. L'import lit un fichier non fiable :
 * - tailles bornées (fichier, contenu décompressé, chaque ligne, nombre de lignes), en-tête lu d'abord ;
 * - chaque ligne validée (identifiants bornés puis vérifiés, jetons, textes nettoyés, builds vraisemblables, cibles
 *   des notes relues) ; ce qui est invalide est écarté et compté ;
 * - la base du joueur est remplacée en une transaction (tout ou rien) ; réglages et groupe suivent, le groupe en une
 *   seule écriture ; s'ils échouent, l'import est dit partiel.
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
        val decoded = snapshot.builds.map { e -> e to runCatching { BuildFormat.json.decodeFromString(Build.serializer(), e.payload) }.getOrNull() }
        val builds = decoded.mapNotNull { (e, b) -> b?.let { e.id to BuildRow(it, e.createdAt, e.updatedAt) } }
        val damaged = decoded.filter { it.second == null }.map { (e, _) -> DamagedBuildRow(e.name, e.payload, e.createdAt, e.updatedAt) }
        val rank = builds.mapIndexed { i, (id, _) -> id to i }.toMap()
        val file = CarnetFile(
            exportedAt = now,
            preferences = preferences.preferences.first(),
            revealed = snapshot.revealed.map { RevealedRow(it.token, it.revealedAt) },
            names = snapshot.names.map { NameRow(it.entryId, it.fr, it.correctedAt) },
            favorites = snapshot.favorites.map { FavoriteRow(it.entryId, it.addedAt) },
            builds = builds.map { it.second },
            damagedBuilds = damaged,
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
        val header = runCatching { json.decodeFromString(CarnetHeader.serializer(), text) }.getOrNull() ?: return ImportResult.NotACarnet
        if (header.format != CarnetFile.FORMAT) return ImportResult.NotACarnet
        if (header.version > CarnetFile.VERSION) return ImportResult.TooNew
        val file = runCatching { json.decodeFromString(CarnetFile.serializer(), text) }.getOrNull() ?: return ImportResult.NotACarnet

        var skipped = 0
        fun <T> keep(rows: List<T>, max: Int, valid: (T) -> Boolean): List<T> {
            val ok = rows.filter(valid)
            skipped += rows.size - ok.size
            if (ok.size > max) skipped += ok.size - max
            return ok.take(max)
        }
        val revealed = keep(file.revealed, MAX_REVEALED) { TOKEN.matches(it.token) }.distinctBy { it.token }
        val names = keep(file.names, MAX_ROWS) { ContentId.isValid(it.id) && cleanText(it.fr, MAX_NAME).isNotEmpty() }.distinctBy { it.id }
        val favorites = keep(file.favorites, MAX_ROWS) { ContentId.isValid(it.id) }.distinctBy { it.id }
        // Builds : rang dans le fichier gardé, pour recaler le groupe sur ceux qui restent.
        val builds = keep(file.builds.withIndex().toList(), MAX_BUILDS) { (_, row) ->
            BuildCode.isPlausible(row.build) && BuildFormat.json.encodeToString(Build.serializer(), row.build).length <= MAX_PAYLOAD
        }.map { (i, row) -> i to row.copy(build = row.build.copy(name = BuildCode.cleanName(row.build.name))) }
        // Un build « abîmé » devenu lisible doit être vraisemblable comme les autres ; illisible, il est gardé tel quel.
        val damaged = keep(file.damagedBuilds, MAX_BUILDS) { row ->
            row.payload.length <= MAX_PAYLOAD && row.name.length <= MAX_NAME &&
                (runCatching { BuildFormat.json.decodeFromString(Build.serializer(), row.payload) }.getOrNull()?.let(BuildCode::isPlausible) ?: true)
        }
        val notes = keep(file.notes, MAX_NOTES) { it.text.length <= NoteRepository.MAX_LENGTH }

        val ids = db.backup().replaceAll(
            UserDataSnapshot(
                revealed = revealed.map { RevealedEntity(it.token, it.at) },
                names = names.map { NameCorrectionEntity(it.id, cleanText(it.fr, MAX_NAME), it.at) },
                favorites = favorites.map { FavoriteEntity(it.id, it.at) },
                builds = builds.map { (_, row) ->
                    BuildEntity(name = row.build.name, payload = BuildFormat.json.encodeToString(Build.serializer(), row.build), createdAt = row.createdAt, updatedAt = row.updatedAt)
                } + damaged.map { BuildEntity(name = BuildCode.cleanName(it.name), payload = it.payload, createdAt = it.createdAt, updatedAt = it.updatedAt) },
                notes = notes.map { n ->
                    // Cible relue : une cible invalide est oubliée, la note reste.
                    val (kind, id) = RoomNoteRepository.encode(RoomNoteRepository.decode(n.targetKind?.take(16), n.targetId?.take(ContentId.MAX_LENGTH + 1)))
                    NoteEntity(text = n.text, targetKind = kind, targetId = id, createdAt = n.createdAt, updatedAt = n.updatedAt)
                },
            ),
        )
        val newId = builds.mapIndexed { k, (fileIndex, _) -> fileIndex to ids[k] }.toMap()
        // Réglages repris, sauf le mode session (propre à l'appareil) ; une taille de lecture hors de la liste revient à 100 %.
        val partial = runCatching {
            val scale = file.preferences.readingScale.takeIf { it in UserPreferences.READING_SCALES } ?: 1f
            preferences.update { file.preferences.copy(sessionActive = false, readingScale = scale) }
            party.setAll(List(PartyRepository.SIZE) { slot -> file.party.getOrNull(slot)?.let(newId::get) })
        }.isFailure
        return ImportResult.Done(builds = builds.size + damaged.size, notes = notes.size, skipped = skipped, partial = partial)
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
        private const val MAX_NAME = 120
        private const val MAX_PAYLOAD = 16_384
        private const val MAX_REVEALED = 100_000
        private const val MAX_ROWS = 20_000
        private const val MAX_BUILDS = 1_000
        private const val MAX_NOTES = 10_000

        /** Texte reçu : sans caractère de contrôle ni d'inversion bidirectionnelle, espaces resserrés, borné. */
        internal fun cleanText(text: String, max: Int): String =
            text.take(max * 4).replace(Regex("[\\p{Cc}\\p{Cf}]"), "").replace(Regex("\\s+"), " ").trim().take(max)
    }
}
