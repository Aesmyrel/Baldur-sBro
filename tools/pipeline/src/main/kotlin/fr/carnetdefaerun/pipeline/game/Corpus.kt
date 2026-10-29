package fr.carnetdefaerun.pipeline.game

import fr.carnetdefaerun.core.model.content.Attribution
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.pipeline.wiki.CachedPage
import fr.carnetdefaerun.pipeline.wiki.PageStore
import fr.carnetdefaerun.pipeline.wiki.Template
import fr.carnetdefaerun.pipeline.wiki.WikiClient
import fr.carnetdefaerun.pipeline.wiki.Wikitext

/** Collections de pages de bg3.wiki lues par le pipeline, et le modèle d'inclusion qui les définit. */
enum class Collection(val key: String, val template: String?) {
    FEATURES("feature", "Feature page"),
    PASSIVES("passive", "Passive feature page"),
    EQUIPMENT("equipment", "Equipment page"),
    WEAPONS("weapon", "WeaponPage"),
    CONDITIONS("condition", "Condition page"),
    LOCATIONS("location", "Location page"),
    MODULES("module", null),
    CLASSES("class", null),
}

/** Pages nommées, hors modèle d'inclusion. */
val NAMED_PAGES = mapOf(
    Collection.MODULES to listOf("Module:Resource/data"),
    Collection.CLASSES to CLASS_PAGES + CLASSES_OVERVIEW_PAGE,
)

/** Une page et son modèle principal. */
data class WikiPage(val title: String, val page: CachedPage, val template: Template) {
    val attribution get() = Attribution(SOURCE_BG3WIKI, title, page.rev, WikiClient.url(title))
}

const val SOURCE_BG3WIKI = "bg3wiki"
const val LICENCE_BG3WIKI = "cc-by-nc-sa-4.0"

/** Pages exclues : retirées du jeu, inaccessibles, ou d'une ancienne version. */
private val EXCLUDED_MARKERS = listOf("unobtainable", "inaccessible", "legacy", "removed")

class Corpus(private val store: PageStore) {
    private val cache = mutableMapOf<Collection, Map<String, CachedPage>>()

    fun raw(collection: Collection): Map<String, CachedPage> = cache.getOrPut(collection) { store.load(collection.key) }

    /** Pages d'une collection qui portent leur modèle principal et ne sont pas exclues. */
    fun pages(collection: Collection): List<WikiPage> {
        val template = checkNotNull(collection.template)
        return raw(collection).mapNotNull { (title, page) ->
            if (Wikitext.hasTemplate(page.text, *EXCLUDED_MARKERS.toTypedArray())) return@mapNotNull null
            Wikitext.pageTemplate(page.text, template)?.let { WikiPage(title, page, it) }
        }.sortedBy { it.title }
    }

    val resourceAliases: Map<String, String> by lazy {
        Parsing.resourceAliases(checkNotNull(raw(Collection.MODULES)["Module:Resource/data"]) { "Module:Resource/data absent du cache" }.text)
    }

    companion object {
        /** Titre de page sans qualificatif de désambiguïsation redondant : `Bane (spell)` → `Bane`. */
        fun baseTitle(title: String, vararg redundant: String): String {
            val m = Regex("^(.*?) \\(([^)]+)\\)$").matchEntire(title) ?: return title
            return if (redundant.any { it.equals(m.groupValues[2], ignoreCase = true) }) m.groupValues[1] else title
        }

        fun id(kind: String, name: String): ContentId = ContentFormat.id(kind, name)
    }
}
