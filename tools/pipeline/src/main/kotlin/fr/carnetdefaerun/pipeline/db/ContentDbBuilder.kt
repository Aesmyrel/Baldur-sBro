package fr.carnetdefaerun.pipeline.db

import fr.carnetdefaerun.core.model.content.ConditionFacts
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentSchema
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.FeatureFacts
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.PlaceFacts
import fr.carnetdefaerun.core.model.content.SpellFacts
import fr.carnetdefaerun.core.model.search.Folding
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

/**
 * Crée `content.db` à partir des entrées validées, avec le schéma partagé [ContentSchema].
 * La base est entièrement reconstruite à chaque fois : elle ne contient que des données dérivées.
 */
class ContentDbBuilder(private val gameVersion: String) {

    fun build(entries: List<Entry>, out: Path) {
        Files.createDirectories(out.toAbsolutePath().parent)
        Files.deleteIfExists(out)
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:${out.toAbsolutePath()}").use { c ->
            c.autoCommit = false
            c.createStatement().use { st -> ContentSchema.CREATE.forEach(st::execute) }
            insertEntries(c, entries)
            insertLinks(c, entries)
            insertSearch(c, entries)
            insertMeta(c, entries)
            c.commit()
            c.autoCommit = true
            c.createStatement().use { st ->
                st.execute("PRAGMA user_version = ${ContentSchema.VERSION}")
                st.execute("VACUUM")
            }
        }
    }

    private fun insertEntries(c: Connection, entries: List<Entry>) {
        val e = ContentSchema.Entry
        val sql = "INSERT INTO ${e.TABLE} (${e.ID}, ${e.TYPE}, ${e.SUBTYPE}, ${e.NAME_FR}, ${e.NAME_EN}, ${e.NAME_STATUS}, ${e.SORT_KEY}, " +
            "${e.ACT}, ${e.LEVEL}, ${e.OTHER_WORKS}, ${e.TITLE_ACT}, ${e.TITLE_LEVEL}, ${e.TITLE_OTHER_WORKS}, ${e.PAYLOAD}) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
        c.prepareStatement(sql).use { ps ->
            for (entry in entries) {
                ps.setString(1, entry.id.value)
                ps.setString(2, entry.type.name)
                ps.setString(3, subtype(entry))
                ps.setString(4, entry.name.fr)
                ps.setString(5, entry.name.en)
                ps.setString(6, entry.name.status.name)
                ps.setString(7, Folding.fold(entry.name.fr))
                ps.setInt(8, entry.tag.act.ordinal)
                ps.setInt(9, entry.tag.level.ordinal)
                ps.setInt(10, entry.tag.otherWorks.ordinal)
                ps.setInt(11, entry.titleTag.act.ordinal)
                ps.setInt(12, entry.titleTag.level.ordinal)
                ps.setInt(13, entry.titleTag.otherWorks.ordinal)
                ps.setString(14, ContentFormat.strict.encodeToString(Entry.serializer(), entry))
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertLinks(c: Connection, entries: List<Entry>) {
        val l = ContentSchema.Link
        c.prepareStatement("INSERT INTO ${l.TABLE} VALUES (?,?,?,?,?,?)").use { ps ->
            for (entry in entries) for (link in entry.links) {
                ps.setString(1, entry.id.value)
                ps.setString(2, link.target.value)
                ps.setString(3, link.relation.name)
                ps.setInt(4, link.tag.act.ordinal)
                ps.setInt(5, link.tag.level.ordinal)
                ps.setInt(6, link.tag.otherWorks.ordinal)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertSearch(c: Connection, entries: List<Entry>) {
        val n = ContentSchema.SearchName
        c.prepareStatement("INSERT INTO ${n.TABLE} (${n.ENTRY_ID}, ${n.FORM}, ${n.KIND}) VALUES (?,?,?)").use { ps ->
            for (entry in entries) {
                val forms = listOf("fr" to entry.name.fr, "en" to entry.name.en) + entry.aliases.map { "alias" to it }
                forms.map { (kind, text) -> kind to Folding.fold(text) }.filter { it.second.isNotEmpty() }
                    .distinctBy { it.second }.forEach { (kind, form) ->
                        ps.setString(1, entry.id.value); ps.setString(2, form); ps.setString(3, kind); ps.addBatch()
                    }
            }
            ps.executeBatch()
        }
        c.prepareStatement("INSERT INTO ${ContentSchema.SearchText.TABLE} (entry_id, block_id, act, level, other_works, text) VALUES (?,?,?,?,?,?)").use { ps ->
            for (entry in entries) for (block in entry.blocks) {
                ps.setString(1, entry.id.value)
                ps.setString(2, block.id)
                ps.setInt(3, block.tag.act.ordinal)
                ps.setInt(4, block.tag.level.ordinal)
                ps.setInt(5, block.tag.otherWorks.ordinal)
                ps.setString(6, block.text)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun insertMeta(c: Connection, entries: List<Entry>) {
        val m = ContentSchema.Meta
        val rows = mapOf(
            m.SCHEMA_VERSION to ContentSchema.VERSION.toString(),
            m.GENERATED to LocalDate.now().toString(),
            m.GAME_VERSION to gameVersion,
            m.ENTRY_COUNT to entries.size.toString(),
        )
        c.prepareStatement("INSERT INTO ${m.TABLE} VALUES (?,?)").use { ps ->
            rows.forEach { (k, v) -> ps.setString(1, k); ps.setString(2, v); ps.addBatch() }
            ps.executeBatch()
        }
    }

    /** Sous-type pour les filtres de liste : école de sort, catégorie d'objet, sorte de capacité. */
    private fun subtype(entry: Entry): String? = when (val f = entry.facts) {
        is SpellFacts -> f.school?.name
        is ItemFacts -> f.category.name
        is FeatureFacts -> f.kind.name
        is PlaceFacts -> f.act?.name
        is ConditionFacts -> null
    }
}
