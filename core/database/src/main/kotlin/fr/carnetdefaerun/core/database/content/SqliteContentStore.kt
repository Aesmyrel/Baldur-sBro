package fr.carnetdefaerun.core.database.content

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.ContentSchema
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import java.io.File

/**
 * Base de contenu livrée dans l'APK : copiée une fois dans le stockage privé non sauvegardé
 * (et à chaque mise à jour de l'appli), puis ouverte en lecture seule. Aucun accès réseau.
 */
class SqliteContentStore(private val context: Context) : ContentStore {

    private val db: SQLiteDatabase by lazy { open() }

    private fun open(): SQLiteDatabase {
        val dir = File(context.noBackupFilesDir, "content").apply { mkdirs() }
        val stamp = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        val file = File(dir, "content-v${ContentSchema.VERSION}-$stamp.db")
        if (!file.exists()) {
            dir.listFiles()?.forEach { it.delete() }
            val tmp = File(dir, "${file.name}.tmp")
            context.assets.open(ContentSchema.FILE_NAME).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(file)) { "Copie de la base de contenu impossible" }
        }
        val database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        check(database.version == ContentSchema.VERSION) { "Base de contenu en version ${database.version}, attendue ${ContentSchema.VERSION}" }
        return database
    }

    override fun entry(id: ContentId): EntryRow? =
        db.query(E.TABLE, ENTRY_COLUMNS, "${E.ID} = ?", arrayOf(id.value), null, null, null).use { c ->
            if (c.moveToFirst()) c.entryRow() else null
        }

    override fun entries(type: EntryType): List<EntryRow> =
        db.query(E.TABLE, ENTRY_COLUMNS, "${E.TYPE} = ?", arrayOf(type.name), null, null, E.SORT_KEY).use { c ->
            buildList { while (c.moveToNext()) add(c.entryRow()) }
        }

    override fun linksFrom(id: ContentId): List<LinkRow> = links("${L.SOURCE} = ?", id)

    override fun linksTo(id: ContentId): List<LinkRow> = links("${L.TARGET} = ?", id)

    override fun names(): List<NameRow> =
        db.query(N.TABLE, arrayOf(N.ENTRY_ID, N.FORM, N.KIND), null, null, null, null, null).use { c ->
            buildList { while (c.moveToNext()) add(NameRow(ContentId(c.getString(0)), c.getString(1), c.getString(2))) }
        }

    override fun meta(): Map<String, String> =
        db.query(ContentSchema.Meta.TABLE, arrayOf(ContentSchema.Meta.KEY, ContentSchema.Meta.VALUE), null, null, null, null, null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
        }

    private fun links(where: String, id: ContentId): List<LinkRow> =
        db.query(L.TABLE, arrayOf(L.SOURCE, L.TARGET, L.RELATION, L.ACT, L.LEVEL, L.OTHER_WORKS), where, arrayOf(id.value), null, null, null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(LinkRow(ContentId(c.getString(0)), ContentId(c.getString(1)), Relation.valueOf(c.getString(2)), c.tag(3)))
                }
            }
        }

    private fun Cursor.entryRow() = EntryRow(
        id = ContentId(getString(0)),
        type = EntryType.valueOf(getString(1)),
        subtype = getString(2),
        nameFr = getString(3),
        nameEn = getString(4),
        tag = tag(5),
        titleTag = tag(8),
        payload = getString(11),
    )

    private fun Cursor.tag(first: Int) = SpoilerTag(Act.entries[getInt(first)], SpoilerLevel.entries[getInt(first + 1)], SpoilerLevel.entries[getInt(first + 2)])

    private companion object {
        val E = ContentSchema.Entry
        val L = ContentSchema.Link
        val N = ContentSchema.SearchName
        val ENTRY_COLUMNS = arrayOf(
            E.ID, E.TYPE, E.SUBTYPE, E.NAME_FR, E.NAME_EN,
            E.ACT, E.LEVEL, E.OTHER_WORKS, E.TITLE_ACT, E.TITLE_LEVEL, E.TITLE_OTHER_WORKS, E.PAYLOAD,
        )
    }
}
