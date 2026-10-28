package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.pipeline.Paths
import fr.carnetdefaerun.pipeline.i18n.Lexicon
import fr.carnetdefaerun.pipeline.wiki.WikiClient
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Lexiques VO → VF de la terminologie officielle de D&D 5e (2014), lus sur les listes filtrables
 * d'AideDD. Usage : vocabulaire seulement (des noms), jamais de texte (docs/SOURCES.md §2.4).
 * Le cache n'est pas versionné ; seuls les noms retenus entrent dans les données.
 */
class Lexicons(private val paths: Paths) {
    /** Fichier de cache → (adresse, sorte d'entrée à laquelle il sert). */
    private val pages = mapOf(
        "aidedd-sorts" to ("https://www.aidedd.org/dnd-filters/sorts.php" to "spell"),
        "aidedd-objets" to ("https://www.aidedd.org/dnd-filters/objets-magiques.php" to "item"),
    )

    fun fetch(): Boolean {
        Files.createDirectories(paths.lexiconCache)
        val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).followRedirects(HttpClient.Redirect.NORMAL).build()
        for ((name, page) in pages) {
            val url = page.first
            val request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", WikiClient.USER_AGENT).GET().build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() == 200) { "HTTP ${response.statusCode()} pour $url" }
            paths.lexiconCache.resolve("$name.html").writeText(response.body())
            println("  $name : ${Lexicon.parseAideDd(response.body(), "aidedd").size} noms")
            Thread.sleep(2_000)
        }
        return true
    }

    /** Lexiques disponibles dans le cache, par sorte d'entrée. */
    fun load(): Map<String, List<Lexicon>> = pages.mapNotNull { (name, page) ->
        val file = paths.lexiconCache.resolve("$name.html")
        if (file.exists()) page.second to Lexicon.parseAideDd(file.readText(), "aidedd") else null
    }.groupBy({ it.first }, { it.second })
}
