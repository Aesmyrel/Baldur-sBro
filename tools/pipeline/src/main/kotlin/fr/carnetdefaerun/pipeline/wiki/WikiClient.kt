package fr.carnetdefaerun.pipeline.wiki

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Page récupérée : texte source et révision exacte, pour l'attribution. */
@Serializable
data class CachedPage(val rev: Long, val ts: String, val text: String)

/**
 * Client de l'API MediaWiki de bg3.wiki : lecture seule, une requête par seconde au plus,
 * reprise sur erreur temporaire, `maxlag` pour céder la place quand le serveur est chargé.
 * Les requêtes Cargo arbitraires sont refusées par le wiki : on lit le wikitexte des pages.
 */
class WikiClient(
    private val api: String = "https://bg3.wiki/w/api.php",
    private val minInterval: Duration = Duration.ofSeconds(1),
) {
    private val http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private var last = 0L

    private fun get(params: Map<String, String>): JsonObject {
        val query = (params + mapOf("format" to "json", "formatversion" to "2", "maxlag" to "5"))
            .entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        val request = HttpRequest.newBuilder(URI.create("$api?$query"))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(60))
            .GET().build()
        var attempt = 0
        while (true) {
            val wait = last + minInterval.toMillis() - System.currentTimeMillis()
            if (wait > 0) Thread.sleep(wait)
            last = System.currentTimeMillis()
            val response = runCatching { http.send(request, HttpResponse.BodyHandlers.ofString()) }
            val body = response.getOrNull()
            val retryable = body == null || body.statusCode() == 429 || body.statusCode() >= 500 ||
                body.body().contains("\"code\":\"maxlag\"")
            if (!retryable) {
                check(body!!.statusCode() == 200) { "HTTP ${body.statusCode()} pour $query" }
                val obj = json.parseToJsonElement(body.body()).jsonObject
                obj["error"]?.let { error("Erreur de l'API : $it") }
                return obj
            }
            attempt++
            check(attempt <= 5) { "Échec après 5 tentatives : $query (${response.exceptionOrNull() ?: body?.statusCode()})" }
            Thread.sleep(2000L shl attempt)
        }
    }

    /** Pages de l'espace principal qui incluent le modèle [template]. */
    fun transclusions(template: String): List<String> {
        val titles = mutableListOf<String>()
        var cont = emptyMap<String, String>()
        do {
            val res = get(
                mapOf("action" to "query", "list" to "embeddedin", "eititle" to "Template:$template", "eilimit" to "500", "einamespace" to "0") + cont,
            )
            res["query"]!!.jsonObject["embeddedin"]!!.jsonArray.forEach { titles += it.jsonObject["title"]!!.jsonPrimitive.content }
            cont = res["continue"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
        } while (cont.isNotEmpty())
        return titles
    }

    /** Dernière révision de chaque page, par lots de 50. */
    fun lastRevisions(titles: List<String>): Map<String, Long> = titles.chunked(50).flatMap { batch ->
        val res = get(mapOf("action" to "query", "prop" to "info", "titles" to batch.joinToString("|")))
        res["query"]!!.jsonObject["pages"]!!.jsonArray.mapNotNull { page ->
            val p = page.jsonObject
            p["lastrevid"]?.jsonPrimitive?.long?.let { p["title"]!!.jsonPrimitive.content to it }
        }
    }.toMap()

    /** Wikitexte et révision de chaque page, par lots de 50. */
    fun pages(titles: List<String>): Map<String, CachedPage> = titles.chunked(50).flatMap { batch ->
        val res = get(
            mapOf(
                "action" to "query", "prop" to "revisions", "rvprop" to "ids|timestamp|content",
                "rvslots" to "main", "titles" to batch.joinToString("|"),
            ),
        )
        res["query"]!!.jsonObject["pages"]!!.jsonArray.mapNotNull { page ->
            val p = page.jsonObject
            val rev = p["revisions"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@mapNotNull null
            p["title"]!!.jsonPrimitive.content to CachedPage(
                rev = rev["revid"]!!.jsonPrimitive.long,
                ts = rev["timestamp"]!!.jsonPrimitive.content,
                text = rev["slots"]!!.jsonObject["main"]!!.jsonObject["content"]!!.jsonPrimitive.content,
            )
        }
    }.toMap()

    companion object {
        const val USER_AGENT = "CarnetDeFaerun-pipeline/0.1 (projet personnel non commercial ; lecture seule, hors ligne)"
        const val PAGE_URL = "https://bg3.wiki/wiki/"

        fun url(title: String) = PAGE_URL + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
            .replace("%2F", "/").replace("%3A", ":").replace("%28", "(").replace("%29", ")")

        private fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)
    }
}

/**
 * Cache local des pages (non versionné : il contient le texte du jeu cité par le wiki).
 * Une collection par modèle d'inclusion, mise à jour seulement pour les pages modifiées.
 */
class PageStore(private val dir: Path, private val client: WikiClient? = null) {
    private val json = Json { prettyPrint = false }
    private val SERIALIZER = MapSerializer(String.serializer(), CachedPage.serializer())

    fun load(collection: String): Map<String, CachedPage> {
        val file = dir.resolve("$collection.json")
        if (!file.exists()) return emptyMap()
        return json.decodeFromString(SERIALIZER, file.readText())
    }

    private fun save(collection: String, pages: Map<String, CachedPage>) {
        Files.createDirectories(dir)
        val sorted: Map<String, CachedPage> = LinkedHashMap(pages.toSortedMap())
        dir.resolve("$collection.json").writeText(json.encodeToString(SERIALIZER, sorted))
    }

    /** Synchronise la collection des pages qui incluent [template] ; renvoie le nombre de pages rafraîchies. */
    fun refreshTemplate(collection: String, template: String): Int {
        val wiki = checkNotNull(client) { "Aucun client réseau : mode hors ligne" }
        return refresh(collection, wiki.transclusions(template))
    }

    /** Synchronise une collection de pages nommées (pages de classes, modules…). */
    fun refresh(collection: String, titles: List<String>): Int {
        val wiki = checkNotNull(client) { "Aucun client réseau : mode hors ligne" }
        val cached = load(collection)
        val revisions = wiki.lastRevisions(titles)
        val stale = titles.filter { cached[it]?.rev != revisions[it] }
        val fresh = wiki.pages(stale)
        save(collection, cached.filterKeys { it in titles.toSet() } + fresh)
        return fresh.size
    }
}
