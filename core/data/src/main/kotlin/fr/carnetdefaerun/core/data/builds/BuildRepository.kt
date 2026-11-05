package fr.carnetdefaerun.core.data.builds

import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.user.BuildEntity
import fr.carnetdefaerun.core.database.user.UserDatabase
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.builds.BuildFormat
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.rules.RulesData
import fr.carnetdefaerun.core.rules.from
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Build enregistré dans la base du joueur. [damaged] : l'enregistrement est illisible (base abîmée) ; seul son nom
 * est repris, pour qu'il reste visible et qu'on puisse le supprimer.
 */
data class SavedBuild(val id: Long, val build: Build, val updatedAt: Long, val damaged: Boolean = false)

/** Builds du joueur. Un enregistrement illisible est montré comme abîmé plutôt que de bloquer la liste ou de disparaître. */
interface BuildRepository {
    val builds: Flow<List<SavedBuild>>
    fun build(id: Long): Flow<SavedBuild?>
    suspend fun create(build: Build): Long
    suspend fun save(id: Long, build: Build)
    suspend fun delete(id: Long)
}

@Singleton
class RoomBuildRepository @Inject constructor(private val db: UserDatabase) : BuildRepository {
    private fun now() = System.currentTimeMillis()

    override val builds: Flow<List<SavedBuild>> = db.builds().all().map { list -> list.map(::decode) }.distinctUntilChanged()

    override fun build(id: Long): Flow<SavedBuild?> = db.builds().byId(id).map { it?.let(::decode) }.distinctUntilChanged()

    override suspend fun create(build: Build): Long {
        val now = now()
        return db.builds().insert(BuildEntity(name = build.safeName, payload = encode(build), createdAt = now, updatedAt = now))
    }

    override suspend fun save(id: Long, build: Build) = db.builds().update(id, build.safeName, encode(build), now())

    override suspend fun delete(id: Long) = db.builds().delete(id)

    private val Build.safeName get() = BuildCode.cleanName(name)
    private fun encode(build: Build) = BuildFormat.json.encodeToString(Build.serializer(), build.copy(name = build.safeName))
    private fun decode(entity: BuildEntity): SavedBuild =
        runCatching { BuildFormat.json.decodeFromString(Build.serializer(), entity.payload) }.getOrNull()?.let { SavedBuild(entity.id, it, entity.updatedAt) }
            ?: SavedBuild(entity.id, Build(name = BuildCode.cleanName(entity.name)), entity.updatedAt, damaged = true)
}

/**
 * Profils de règles tirés de la base de contenu (classes, sous-classes, dons, races, sous-races, historiques), lus une fois.
 * Ce ne sont que des nombres et des identifiants : aucun texte n'en sort vers un écran, les noms passent par la
 * porte de contenu et son verrou.
 */
@Singleton
class RulesRepository @Inject internal constructor(private val store: ContentStore) {
    private val mutex = Mutex()
    private var cached: RulesData? = null

    suspend fun data(): RulesData = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val types = listOf(EntryType.CLASS, EntryType.SUBCLASS, EntryType.FEAT, EntryType.RACE, EntryType.SUBRACE, EntryType.BACKGROUND)
            RulesData.from(types.flatMap { type -> store.entries(type).map { it.entry() } })
        }.also { cached = it }
    }
}
