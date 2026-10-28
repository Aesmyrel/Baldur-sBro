package fr.carnetdefaerun.navigation

import android.net.Uri
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.MAX_QUERY_LENGTH
import fr.carnetdefaerun.core.model.navigation.SECTION_ANCHOR
import fr.carnetdefaerun.core.model.navigation.isWellFormed

/**
 * Liens profonds `carnetdefaerun://…` : fiche/{id}, codex/{id}[#s{n}], recherche?q=…, build/{id}, reglages.
 * Toute valeur mal formée est refusée (le lien est ignoré) ; la recherche est tronquée à 200 caractères.
 * Un lien vers un contenu verrouillé ouvre la fiche, qui reste voilée (le verrou s'applique au rendu).
 */
object DeepLinks {
    const val SCHEME = "carnetdefaerun"

    fun parse(uri: Uri?): Destination? {
        if (uri == null || uri.scheme != SCHEME) return null
        val segments = listOfNotNull(uri.host) + uri.pathSegments
        return when (segments.firstOrNull()) {
            "fiche" -> segments.getOrNull(1)?.takeIf { ContentId.PATTERN.matches(it) }?.let { Destination.GameEntry(it) }
            "codex" -> segments.getOrNull(1)?.takeIf { ContentId.PATTERN.matches(it) }
                ?.let { Destination.LoreEntry(it, uri.fragment?.takeIf { f -> SECTION_ANCHOR.matches(f) }) }
            "recherche" -> Destination.Search(uri.getQueryParameter("q").orEmpty().take(MAX_QUERY_LENGTH))
            "build" -> segments.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }?.let { Destination.BuildEditor(it) }
            "reglages" -> Destination.Settings
            "partie" -> Destination.Home
            else -> null
        }?.takeIf { it.isWellFormed() }
    }
}
