package fr.carnetdefaerun.core.model.content

import kotlinx.serialization.json.Json
import java.text.Normalizer

/** Format JSON des données de contenu, partagé par le pipeline et l'appli. */
object ContentFormat {
    /** Lecture stricte (pipeline, tests) : un champ inconnu est une erreur. */
    val strict = Json {
        classDiscriminator = "@type"
        encodeDefaults = false
        explicitNulls = false
    }

    /** Écriture des fichiers versionnés : lisible et stable d'une génération à l'autre. */
    val pretty = Json(strict) {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** Lecture tolérante dans l'appli : une base plus récente peut porter des champs ignorés. */
    val lenient = Json(strict) {
        ignoreUnknownKeys = true
    }

    /**
     * Partie d'identifiant dérivée d'un nom anglais : minuscules, sans accents ni apostrophes,
     * mots séparés par des tirets. `Tasha's Hideous Laughter` → `tashas-hideous-laughter`.
     */
    fun slug(englishName: String): String {
        val folded = Normalizer.normalize(englishName, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("['’‘`]"), "")
            .replace("&", " and ")
        return folded.replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }

    fun id(kind: String, englishName: String) = ContentId("$kind.${slug(englishName)}")
}
