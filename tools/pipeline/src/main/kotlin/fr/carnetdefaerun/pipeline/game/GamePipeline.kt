package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.NameStatus
import fr.carnetdefaerun.pipeline.Paths
import fr.carnetdefaerun.pipeline.db.ContentDbBuilder
import fr.carnetdefaerun.pipeline.i18n.NameResolver
import fr.carnetdefaerun.pipeline.validate.Validator
import fr.carnetdefaerun.pipeline.wiki.PageStore
import fr.carnetdefaerun.pipeline.wiki.WikiClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Files
import java.time.LocalDate
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Ligne d'une liste de travail (non versionnée) : ce qu'il reste à rédiger ou à traduire. */
@Serializable
data class WorkItem(val id: String, val nameEn: String, val page: String, val revision: Long, val todo: String, val detail: String? = null)

/**
 * Version du jeu couverte par les données (docs/SOURCES.md, relevé du 27/10/2026 : dernière mise à jour
 * publiée le 29/09/2026, après le Patch 8 et le correctif n° 36).
 */
const val GAME_VERSION = "Patch 8 + correctifs, mise à jour du 29/09/2026 (4.1.1.7631656)"

/** Données de jeu issues de bg3.wiki : récupération, normalisation, validation. */
class GamePipeline(private val paths: Paths) {

    fun fetch(): Boolean {
        val store = PageStore(paths.wikiCache, WikiClient())
        for (collection in Collection.entries) {
            val refreshed = when (val template = collection.template) {
                null -> store.refresh(collection.key, NAMED_PAGES[collection].orEmpty())
                else -> store.refreshTemplate(collection.key, template)
            }
            println("  ${collection.key} : ${store.load(collection.key).size} pages, $refreshed mises à jour")
        }
        return true
    }

    fun normalize(): Boolean {
        val corpus = Corpus(PageStore(paths.wikiCache))
        val names = NameResolver(NameResolver.loadCurated(paths.curatedNames), Lexicons(paths).load())
        val places = PlaceNormalizer(corpus, names)
        val items = ItemNormalizer(corpus, names, places.acts)
        val assembler = Assembler(FeatureNormalizer(corpus, names), ConditionNormalizer(corpus, names))
        val entries = assembler.assemble(SpellNormalizer(corpus, names).normalize() + items.normalize() + places.normalize())

        Files.createDirectories(paths.gameData)
        // Un fichier par type, trié par identifiant : des différences lisibles d'une génération à l'autre.
        paths.gameData.listDirectoryEntries("*.json").forEach(Files::delete)
        entries.groupBy { it.type }.forEach { (type, list) ->
            paths.gameData.resolve(fileName(type)).writeText(ContentFormat.pretty.encodeToString(ListSerializer(Entry.serializer()), list) + "\n")
        }

        val work = buildList {
            items.pending.forEach { add(WorkItem(it.id.value, it.id.value, it.wikiTitle, it.revision, "effet", it.line)) }
            entries.filter { "effect_text" in it.unknown }.forEach {
                val a = it.attribution.first()
                add(WorkItem(it.id.value, it.name.en, a.title, a.revision, "effet"))
            }
            names.missing.forEach { id ->
                val e = entries.firstOrNull { it.id.value == id } ?: return@forEach
                val a = e.attribution.first()
                add(WorkItem(id, e.name.en, a.title, a.revision, "nom"))
            }
        }
        Files.createDirectories(paths.worklists)
        paths.worklists.resolve("a-faire.json").writeText(ContentFormat.pretty.encodeToString(ListSerializer(WorkItem.serializer()), work))
        paths.gameData.resolve("RAPPORT.md").writeText(report(entries, assembler.dangling.size, work))
        println("  ${entries.size} entrées écrites dans ${paths.root.relativize(paths.gameData)}, ${work.size} éléments à rédiger ou traduire")
        return true
    }

    fun validate(): Boolean {
        val entries = load()
        val problems = Validator().validate(entries)
        problems.forEach { println("  ✗ $it") }
        println("  ${entries.size} entrées, ${problems.size} problème(s)")
        return problems.isEmpty()
    }

    /** Base embarquée : uniquement à partir de données valides. */
    fun buildDb(out: java.nio.file.Path): Boolean {
        val entries = load()
        val problems = Validator().validate(entries)
        if (problems.isNotEmpty()) {
            System.err.println("  ${problems.size} problème(s) de validation : base non construite")
            return false
        }
        ContentDbBuilder(GAME_VERSION).build(entries, out)
        println("  ${entries.size} entrées → $out")
        return true
    }

    /** Relecture stricte des fichiers versionnés : un champ inconnu ou une étiquette absente est une erreur. */
    fun load(): List<Entry> {
        if (!paths.gameData.exists()) return emptyList()
        return paths.gameData.listDirectoryEntries("*.json").sorted().flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), it.readText())
        }
    }

    private fun fileName(type: EntryType) = when (type) {
        EntryType.SPELL -> "sorts.json"
        EntryType.ITEM -> "objets.json"
        EntryType.CONDITION -> "etats.json"
        EntryType.FEATURE -> "capacites.json"
        EntryType.PLACE -> "lieux.json"
        else -> "${type.name.lowercase()}.json"
    }

    /** Rapport de couverture, versionné : comptes, statuts des noms, champs inconnus. */
    private fun report(entries: List<Entry>, dangling: Int, work: List<WorkItem>): String = buildString {
        appendLine("# Données de jeu — rapport de génération")
        appendLine()
        appendLine("_Généré le ${LocalDate.now()} par `./gradlew :tools:pipeline:run --args=\"normalize\"` depuis bg3.wiki (CC BY-NC-SA 4.0)._")
        appendLine()
        appendLine("| Type | Entrées | VF BG3 | VF D&D | Non officielle | Avec champs inconnus |")
        appendLine("|---|---:|---:|---:|---:|---:|")
        entries.groupBy { it.type }.toSortedMap().forEach { (type, list) ->
            fun count(s: NameStatus) = list.count { it.name.status == s }
            appendLine("| ${fileName(type).removeSuffix(".json")} | ${list.size} | ${count(NameStatus.VF_BG3)} | ${count(NameStatus.VF_DND)} | ${count(NameStatus.UNOFFICIAL)} | ${list.count { it.unknown.isNotEmpty() }} |")
        }
        appendLine()
        appendLine("Champs inconnus (vides dans les données, signalés à l'écran) :")
        appendLine()
        entries.flatMap { it.unknown }.groupingBy { it }.eachCount().toSortedMap().forEach { (field, n) -> appendLine("- `$field` : $n") }
        appendLine()
        appendLine("Liens écartés faute de cible dans la base : $dangling. Éléments à rédiger ou traduire : ${work.size} (liste de travail locale, non versionnée).")
    }
}
