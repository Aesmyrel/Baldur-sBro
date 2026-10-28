package fr.carnetdefaerun.pipeline

import fr.carnetdefaerun.pipeline.game.GamePipeline
import fr.carnetdefaerun.pipeline.game.Lexicons
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Pipeline de données du Carnet de Faerûn (hors de l'appli, jamais à l'exécution) :
 * `fetch` (bg3.wiki → cache local), `lexicon` (lexiques VO → VF), `normalize` (→ fichiers JSON de data/game),
 * `validate`, `build-db` (→ content.db), `all`. À lancer depuis la racine du dépôt : `./gradlew :tools:pipeline:run --args="all"`.
 */
fun main(args: Array<String>) {
    val root = Path.of("").toAbsolutePath()
    val paths = Paths(root)
    val out = args.indexOf("--out").takeIf { it >= 0 }?.let { Path.of(args[it + 1]) } ?: paths.cache.resolve("content.db")
    val commands = args.toList().filterIndexed { i, a -> a != "--out" && (i == 0 || args[i - 1] != "--out") }.ifEmpty { listOf("normalize", "validate") }
    val steps = commands.flatMap { if (it == "all") listOf("fetch", "lexicon", "normalize", "validate", "build-db") else listOf(it) }
    var ok = true
    for (step in steps) {
        println("— $step")
        ok = when (step) {
            "fetch" -> GamePipeline(paths).fetch()
            "lexicon" -> Lexicons(paths).fetch()
            "normalize" -> GamePipeline(paths).normalize()
            "validate" -> GamePipeline(paths).validate()
            "build-db" -> GamePipeline(paths).buildDb(out)
            else -> {
                System.err.println("Commande inconnue : $step (fetch, lexicon, normalize, validate, build-db [--out fichier], all)")
                false
            }
        } && ok
        if (!ok) break
    }
    exitProcess(if (ok) 0 else 1)
}

/** Emplacements du pipeline, relatifs à la racine du dépôt. */
class Paths(val root: Path) {
    val cache: Path = root.resolve("tools/cache")
    val wikiCache: Path = cache.resolve("bg3wiki")
    val lexiconCache: Path = cache.resolve("lexiques")
    val worklists: Path = cache.resolve("worklists")
    val gameData: Path = root.resolve("data/game")
    val curatedNames: Path = root.resolve("data/i18n/noms.fr.yaml")
    val writtenEffects: Path = root.resolve("data/effets")
}
