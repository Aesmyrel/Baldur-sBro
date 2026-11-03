package fr.carnetdefaerun.core.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.database.content.SqliteContentStore
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.ContentSchema
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.SpellFacts
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test
import org.junit.runner.RunWith

/** La base générée à la compilation s'ouvre avec le SQLite d'Android et contient exactement les données validées. */
@RunWith(AndroidJUnit4::class)
class ContentStoreTest {
    private val store = SqliteContentStore(ApplicationProvider.getApplicationContext())

    private val source: List<Entry> by lazy {
        listOf("../../data/game", "../../data/lore").flatMap { dir -> File(dir).listFiles { f -> f.extension == "json" }.orEmpty().toList() }.flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), it.readText())
        }
    }

    @Test
    fun `version du schéma et nombre d'entrées`() {
        val meta = store.meta()
        assertThat(meta[ContentSchema.Meta.SCHEMA_VERSION]).isEqualTo(ContentSchema.VERSION.toString())
        assertThat(meta[ContentSchema.Meta.ENTRY_COUNT]).isEqualTo(source.size.toString())
        assertThat(EntryType.entries.sumOf { store.entries(it).size }).isEqualTo(source.size)
    }

    @Test
    fun `une fiche relue est identique à la donnée validée`() {
        val expected = source.first { it.id.value == "spell.fireball" }
        val row = store.entry(ContentId("spell.fireball"))!!
        assertThat(row.entry()).isEqualTo(expected)
        assertThat(row.tag).isEqualTo(expected.tag)
        assertThat((row.entry().facts as SpellFacts).level).isEqualTo(3)
        assertThat(store.linksFrom(expected.id).map { it.target }).containsExactlyElementsIn(expected.links.map { it.target })
    }

    @Test
    fun `listes triées par nom français et index des noms`() {
        val spells = store.entries(EntryType.SPELL)
        assertThat(spells.map { it.name.fr }).isNotEmpty()
        assertThat(store.names().any { it.form == "boule de feu" && it.entryId.value == "spell.fireball" }).isTrue()
    }

    @Test
    fun `en-têtes lus sans la fiche, nom et source compris`() {
        val headers = store.headers()
        assertThat(headers).hasSize(source.size)
        val bySource = source.associateBy { it.id }
        headers.forEach { h ->
            val e = bySource.getValue(h.id)
            assertThat(h.name).isEqualTo(e.name)
            assertThat(h.titleTag).isEqualTo(e.titleTag)
        }
    }

    @Test
    fun `texte intégral, accents ignorés, chaque passage avec l'étiquette de son bloc`() {
        val hits = store.searchText(listOf("degats", "feu"), 50)
        assertThat(hits).isNotEmpty()
        val bySource = source.associateBy { it.id }
        hits.forEach { hit ->
            val block = bySource.getValue(hit.entryId).blocks.single { it.id == hit.blockId }
            assertThat(hit.tag).isEqualTo(block.tag)
            assertThat(fr.carnetdefaerun.core.model.search.Folding.fold(block.text)).contains("feu")
            assertThat(hit.excerpt).isNotEmpty()
        }
        assertThat(store.searchText(listOf("zzqxw"), 10)).isEmpty()
    }
}
