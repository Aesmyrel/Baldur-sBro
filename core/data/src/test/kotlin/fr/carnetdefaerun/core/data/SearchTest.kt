package fr.carnetdefaerun.core.data

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.data.content.RevealIds
import fr.carnetdefaerun.core.data.search.SearchRepository
import fr.carnetdefaerun.core.model.content.Block
import fr.carnetdefaerun.core.model.content.BlockKind
import fr.carnetdefaerun.core.model.content.ConditionFacts
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.NameStatus
import fr.carnetdefaerun.core.model.search.MatchQuality
import fr.carnetdefaerun.core.model.search.NameKind
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.model.spoiler.SpoilerGate
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test

/** Recherche globale : verrou anti-spoiler sur chaque résultat (PLAN §2.5, point 4), classement, temps de réponse. */
class SearchTest {
    private val early = SpoilerTag(Act.ACT_1, SpoilerLevel.NONE, SpoilerLevel.NONE)
    private val late = SpoilerTag(Act.ACT_3, SpoilerLevel.NONE, SpoilerLevel.NONE)
    private val act1 = SpoilerProfile(Act.ACT_1)

    private fun entry(id: String, fr: String, en: String, tag: SpoilerTag, titleTag: SpoilerTag = tag, blocks: List<Block> = emptyList()) = Entry(
        id = ContentId(id), type = EntryType.CONDITION, name = Name(fr, en, NameStatus.UNOFFICIAL), tag = tag, titleTag = titleTag,
        licence = "carnet", facts = ConditionFacts(), blocks = blocks,
    )

    private val small = SearchRepository(
        FakeStore(
            listOf(
                entry("condition.lumiere", "Lumière vive", "Bright Light", early, blocks = listOf(Block("effet-1", BlockKind.EFFECT, "Révèle les créatures tapies", late))),
                entry("condition.lune", "Lueur lunaire", "Moon Glow", late),
                entry("condition.lumiere-noire", "Lumière noire", "Black Light", early),
                entry("condition.voile", "Voile", "Veil", early, blocks = listOf(Block("effet-1", BlockKind.EFFECT, "Attire les créatures lunaires", early))),
            ),
        ),
    )

    private fun ids(query: String, profile: SpoilerProfile = act1, revealed: Set<String> = emptySet()) =
        small.search(query, profile, revealed).hits.map { it.entry.id.value }

    @Test
    fun `un nom verrouillé, tapé tel quel, ne renvoie rien et n'est pas compté`() {
        val results = small.search("Lueur lunaire", act1)
        assertThat(results.hits.map { it.entry.id.value }).doesNotContain("condition.lune")
        assertThat(results.hidden).isEmpty()
        assertThat(ids("Moon Glow")).isEmpty()
    }

    @Test
    fun `chercher aussi dans le contenu masqué, des voiles opaques, que la révélation lève`() {
        val results = small.search("Lueur lunaire", act1, includeHidden = true)
        assertThat(results.hidden).containsExactly(RevealKey.of(RevealIds.title(ContentId("condition.lune"))))
        assertThat(results.hits.map { it.entry.id.value }).doesNotContain("condition.lune")
        val revealed = setOf(results.hidden.single().token)
        assertThat(ids("Lueur lunaire", revealed = revealed)).contains("condition.lune")
    }

    @Test
    fun `un passage verrouillé d'une fiche visible n'est pas trouvé`() {
        assertThat(ids("tapies")).isEmpty()
        val withHidden = small.search("tapies", act1, includeHidden = true)
        assertThat(withHidden.hits).isEmpty()
        assertThat(withHidden.hidden).containsExactly(RevealKey.of(RevealIds.block(ContentId("condition.lumiere"), "effet-1")))
        assertThat(ids("tapies", SpoilerProfile(Act.ACT_3))).containsExactly("condition.lumiere")
    }

    @Test
    fun `noms d'abord, puis passages, un résultat par entrée`() {
        val results = small.search("lumi", act1)
        assertThat(results.hits.map { it.entry.id.value }).containsExactly("condition.lumiere", "condition.lumiere-noire").inOrder()
        assertThat(results.hits.all { it.nameMatch == MatchQuality.PREFIX }).isTrue()
        val text = small.search("creatures", act1).hits.single()
        assertThat(text.entry.id.value).isEqualTo("condition.voile")
        assertThat(text.nameMatch).isNull()
        assertThat(text.excerpt).isEqualTo("Attire les créatures lunaires")
    }

    @Test
    fun `favori et nom anglais`() {
        val favored = small.search("lumiere", act1, favorites = setOf(ContentId("condition.lumiere-noire"))).hits
        assertThat(favored.first().entry.id.value).isEqualTo("condition.lumiere-noire")
        val english = small.search("black", act1).hits.single()
        assertThat(english.matchedKind).isEqualTo(NameKind.EN)
    }

    @Test
    fun `un nom corrigé se cherche comme un nom français`() {
        val corrections = mapOf(ContentId("condition.voile") to "Brume pâle")
        assertThat(small.search("brume", act1, corrections = corrections).hits.map { it.entry.id.value }).containsExactly("condition.voile")
        // Le nom corrigé d'une fiche verrouillée reste verrouillé.
        assertThat(small.search("halo", act1, corrections = mapOf(ContentId("condition.lune") to "Halo")).hits).isEmpty()
    }

    // --- Base complète ---

    private val entries: List<Entry> by lazy {
        File("../../data/game").listFiles { f -> f.extension == "json" }!!.sorted().flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), it.readText())
        }
    }
    private val full by lazy { SearchRepository(FakeStore(entries)) }
    private val byId by lazy { entries.associateBy { it.id } }

    private val allProfiles = Act.entries.flatMap { act -> Tolerance.entries.map { SpoilerProfile(act, it) } } +
        Tolerance.entries.map { SpoilerProfile(Act.ACT_1, Tolerance.LIGHT, it) }
    private val referenceProfiles = listOf(SpoilerProfile(Act.PROLOGUE), SpoilerProfile(Act.ACT_1), SpoilerProfile(Act.ACT_2, Tolerance.LIGHT))

    @Test
    fun `base complète, chaque nom verrouillé, tapé tel quel, ne renvoie pas sa fiche`() {
        for (profile in referenceProfiles) {
            val locked = entries.filter { !SpoilerGate.isVisible(it.titleTag, profile) }
            for (entry in locked) for (name in setOf(entry.name.fr, entry.name.en)) {
                val results = full.search(name, profile, limit = Int.MAX_VALUE)
                assertWithMessage("${entry.id} « $name » pour $profile").that(results.hits.map { it.entry.id }).doesNotContain(entry.id)
                assertWithMessage("${entry.id} pour $profile : voiles sans demande").that(results.hidden).isEmpty()
            }
        }
    }

    @Test
    fun `base complète, chaque résultat est visible, pour chaque profil`() {
        val queries = ('a'..'z').map { it.toString() } + listOf("feu", "lum", "arme", "sort", "degats", "jet de sauvegarde", "classe d armure", "avantage", "tour", "magie")
        for (profile in allProfiles) for (query in queries) {
            for (hit in full.search(query, profile, limit = Int.MAX_VALUE).hits) {
                val entry = byId.getValue(hit.entry.id)
                val where = "« $query » → ${entry.id} pour $profile"
                assertWithMessage(where).that(SpoilerGate.isVisible(entry.titleTag, profile)).isTrue()
                if (hit.excerpt != null) {
                    assertWithMessage("$where : fiche").that(SpoilerGate.isVisible(entry.tag, profile)).isTrue()
                    val block = entry.blocks.first { it.text == hit.excerpt }
                    assertWithMessage("$where : passage").that(SpoilerGate.isVisible(block.tag, profile)).isTrue()
                }
            }
        }
    }

    @Test
    fun `base complète, moins de 50 ms par recherche`() {
        val profile = SpoilerProfile(Act.ACT_2, Tolerance.LIGHT)
        val queries = listOf("b", "bo", "boule de feu", "boule", "bolue de feu", "fireball", "benediction", "epee longue", "arme a deux mains", "resistance", "degats de feu", "jet de sauvegarde de dexterite", "xyz")
        full.warmUp()
        repeat(5) { queries.forEach { q -> full.search(q, profile) } }
        for (query in queries) {
            val times = (1..7).map { val t = System.nanoTime(); full.search(query, profile); (System.nanoTime() - t) / 1_000_000.0 }.sorted()
            val median = times[times.size / 2]
            assertWithMessage("« $query » : $median ms").that(median).isLessThan(50.0)
        }
        assertThat(full.search("boule de feu", profile).hits.first().entry.id.value).isEqualTo("spell.fireball")
        assertThat(full.search("bolue de feu", profile).hits.first().entry.id.value).isEqualTo("spell.fireball")
    }
}
