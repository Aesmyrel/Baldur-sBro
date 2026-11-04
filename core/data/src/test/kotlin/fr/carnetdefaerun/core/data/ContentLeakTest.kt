package fr.carnetdefaerun.core.data

import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.database.content.ContentStore
import fr.carnetdefaerun.core.database.content.EntryRow
import fr.carnetdefaerun.core.database.content.LinkRow
import fr.carnetdefaerun.core.database.content.NameRow
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Facts
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.LoreSection
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.SpoilerGate
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import org.junit.Test

/**
 * Tests de non-fuite (PLAN §2.5) sur toute la base : pour chaque profil de joueur, aucune liste,
 * aucune fiche, aucune source, aucun lien ni aucun identifiant ne laisse paraître un contenu verrouillé.
 */
class ContentLeakTest {
    private val entries: List<Entry> = GameData.entries
    private val byId = entries.associateBy { it.id }
    private val repository = ContentRepository(InMemoryContentStore(entries, GameData.references))

    private val profiles = Act.entries.flatMap { act -> Tolerance.entries.map { SpoilerProfile(act, it) } } +
        Tolerance.entries.map { SpoilerProfile(Act.ACT_1, Tolerance.LIGHT, it) }

    @Test
    fun `les listes ne montrent que des noms visibles, et tous les noms visibles`() {
        for (profile in profiles) for (type in EntryType.entries) {
            val list = repository.list(type, profile)
            val expected = entries.filter { it.type == type && SpoilerGate.isVisible(it.titleTag, profile) }.map { it.id }.toSet()
            assertWithMessage("$profile, $type").that(list.visible.map { it.id }.toSet()).isEqualTo(expected)
            assertWithMessage("$profile, $type : voile").that(list.someHidden).isEqualTo(expected.size < entries.count { it.type == type })
        }
    }

    @Test
    fun `aucune fiche ne laisse paraître une partie verrouillée`() {
        val idPattern = Regex("\"([a-z]+\\.[a-z0-9]+(?:-[a-z0-9]+)*)\"")
        for (profile in profiles) for (entry in entries) {
            val where = "${entry.id} pour $profile"
            when (val result = repository.entry(entry.id, profile)) {
                null -> error("$where : absente")
                is Gated.Hidden -> assertWithMessage(where).that(SpoilerGate.isVisible(entry.tag, profile)).isFalse()
                is Gated.Visible -> {
                    val view = result.value
                    assertWithMessage(where).that(SpoilerGate.isVisible(entry.tag, profile)).isTrue()
                    view.links.forEach { link ->
                        val target = byId.getValue(link.id)
                        val tag = entry.links.first { it.target == link.id }.tag
                        assertWithMessage("$where → ${link.id}").that(SpoilerGate.isVisible(tag, profile) && SpoilerGate.isVisible(target.titleTag, profile)).isTrue()
                    }
                    view.sources.forEach { assertWithMessage("$where : source").that(SpoilerGate.isVisible(it.tag, profile)).isTrue() }
                    // Chaque page attribuée est celle de la fiche ou d'une fiche liée visible.
                    val allowed = (entry.attribution + view.links.flatMap { byId.getValue(it.id).attribution }).map { it.url }.toSet()
                    view.attribution.forEach { assertWithMessage("$where : attribution ${it.title}").that(it.url in allowed).isTrue() }
                    // Un lieu d'obtention lié ne se montre que si une provenance visible y mène.
                    val sourcePlaces = (entry.facts as? ItemFacts)?.sources.orEmpty().mapNotNull { it.place }.toSet()
                    view.links.filter { it.relation == Relation.LOCATED_IN && it.id in sourcePlaces }.forEach { link ->
                        assertWithMessage("$where → ${link.id} : lieu sans provenance visible").that(view.sources.any { it.place == link.id }).isTrue()
                    }
                    view.blocks.filterIsInstance<Gated.Visible<fr.carnetdefaerun.core.model.content.Block>>().forEach {
                        assertWithMessage("$where : bloc").that(SpoilerGate.isVisible(it.value.tag, profile)).isTrue()
                    }
                    // Codex : chaque section visible l'est pour ce profil ; seules les références des sections visibles.
                    val visibleSections = view.sections.filterIsInstance<Gated.Visible<LoreSection>>().map { it.value }
                    visibleSections.forEach { assertWithMessage("$where : section ${it.id}").that(SpoilerGate.isVisible(it.tag, profile)).isTrue() }
                    val cited = visibleSections.flatMap { sec -> sec.paragraphs.flatMap { p -> p.refs.map { it.ref } } }.toSet()
                    view.references.forEach { assertWithMessage("$where : référence ${it.id}").that(it.id in cited).isTrue() }
                    (entry.facts as? LoreFacts)?.sections.orEmpty().filter { !SpoilerGate.isVisible(it.tag, profile) }.forEach { hidden ->
                        assertWithMessage("$where : section masquée ${hidden.id}").that(visibleSections.none { it.id == hidden.id }).isTrue()
                    }
                    // Aucun identifiant d'entrée masquée dans les faits transmis (un identifiant dérive d'un nom).
                    val facts = ContentFormat.strict.encodeToString(Facts.serializer(), view.facts)
                    idPattern.findAll(facts).map { it.groupValues[1] }.filter { ContentId.PATTERN.matches(it) }
                        .mapNotNull { byId[ContentId(it)] }.filter { it.id != entry.id }.forEach { referenced ->
                            assertWithMessage("$where : identifiant ${referenced.id} dans les faits")
                                .that(SpoilerGate.isVisible(referenced.titleTag, profile)).isTrue()
                        }
                }
            }
        }
    }

}
