package fr.carnetdefaerun.core.data.notes

import fr.carnetdefaerun.core.database.user.NoteEntity
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.spoiler.Act
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** À quoi une note est rattachée : un acte ou une fiche (du jeu ou du codex). */
sealed interface NoteTarget {
    data class OnAct(val act: Act) : NoteTarget
    data class OnEntry(val id: ContentId) : NoteTarget
}

data class Note(val id: Long, val text: String, val target: NoteTarget?, val updatedAt: Long)

/** Notes du joueur. Le texte est le sien, jamais filtré ; le nom d'une fiche rattachée passe, lui, par le verrou. */
interface NoteRepository {
    val notes: Flow<List<Note>>
    fun note(id: Long): Flow<Note?>
    suspend fun create(text: String, target: NoteTarget?): Long
    suspend fun save(id: Long, text: String, target: NoteTarget?)
    suspend fun delete(id: Long)

    companion object {
        const val MAX_LENGTH = 10_000
    }
}

@Singleton
class RoomNoteRepository @Inject constructor(private val db: UserDatabase) : NoteRepository {
    private fun now() = System.currentTimeMillis()

    override val notes: Flow<List<Note>> = db.notes().all().map { list -> list.map(::toNote) }.distinctUntilChanged()
    override fun note(id: Long): Flow<Note?> = db.notes().byId(id).map { it?.let(::toNote) }.distinctUntilChanged()

    override suspend fun create(text: String, target: NoteTarget?): Long {
        val (kind, value) = encode(target)
        val now = now()
        return db.notes().insert(NoteEntity(text = text.take(NoteRepository.MAX_LENGTH), targetKind = kind, targetId = value, createdAt = now, updatedAt = now))
    }

    override suspend fun save(id: Long, text: String, target: NoteTarget?) {
        val (kind, value) = encode(target)
        db.notes().update(id, text.take(NoteRepository.MAX_LENGTH), kind, value, now())
    }

    override suspend fun delete(id: Long) = db.notes().delete(id)

    companion object {
        internal fun encode(target: NoteTarget?): Pair<String?, String?> = when (target) {
            null -> null to null
            is NoteTarget.OnAct -> "act" to target.act.name
            is NoteTarget.OnEntry -> "entry" to target.id.value
        }

        /** Une cible illisible (acte inconnu, identifiant mal formé) est oubliée : la note reste, sans rattachement. */
        internal fun decode(kind: String?, value: String?): NoteTarget? = when (kind) {
            "act" -> Act.entries.firstOrNull { it.name == value }?.let(NoteTarget::OnAct)
            "entry" -> value?.takeIf { ContentId.PATTERN.matches(it) }?.let { NoteTarget.OnEntry(ContentId(it)) }
            else -> null
        }

        internal fun toNote(e: NoteEntity) = Note(e.id, e.text, decode(e.targetKind, e.targetId), e.updatedAt)
    }
}
