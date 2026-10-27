package fr.carnetdefaerun.navigation

import android.net.Uri
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.navigation.Destination

/**
 * Liens profonds `carnetdefaerun://…` : fiche/{id}, codex/{id}[#ancre], recherche?q=…, build/{id}, reglages.
 * Un lien vers un contenu verrouillé ouvre la fiche, qui reste voilée (le verrou s'applique au rendu).
 */
object DeepLinks {
    const val SCHEME = "carnetdefaerun"

    fun parse(uri: Uri?): Destination? {
        if (uri == null || uri.scheme != SCHEME) return null
        val segments = listOfNotNull(uri.host) + uri.pathSegments
        return when (segments.firstOrNull()) {
            "fiche" -> segments.getOrNull(1)?.takeIf { ContentId.PATTERN.matches(it) }?.let { Destination.GameEntry(it) }
            "codex" -> segments.getOrNull(1)?.takeIf { ContentId.PATTERN.matches(it) }?.let { Destination.LoreEntry(it, uri.fragment) }
            "recherche" -> Destination.Search(uri.getQueryParameter("q").orEmpty())
            "build" -> segments.getOrNull(1)?.toLongOrNull()?.let { Destination.BuildEditor(it) }
            "reglages" -> Destination.Settings
            "partie" -> Destination.Home
            else -> null
        }
    }
}
