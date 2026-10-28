package fr.carnetdefaerun.pipeline.i18n

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.NameStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Path
import java.text.Normalizer
import kotlin.io.path.exists
import kotlin.io.path.readText

/** Nom français choisi à la main (`data/i18n/noms.fr.yaml`), prioritaire sur tout lexique. */
@Serializable
data class CuratedName(
    val fr: String,
    val statut: CuratedStatus,
    val source: String? = null,
    val note: String? = null,
)

@Serializable
enum class CuratedStatus(val status: NameStatus) {
    @SerialName("VF_BG3") VF_BG3(NameStatus.VF_BG3),
    @SerialName("VF_DND") VF_DND(NameStatus.VF_DND),
    @SerialName("NON_OFFICIELLE") NON_OFFICIELLE(NameStatus.UNOFFICIAL),
}

/** Correspondance de vocabulaire anglais → français d'une source (jamais de texte, seulement des noms). */
class Lexicon(val source: String, private val entries: Map<String, String>) {
    fun lookup(english: String): String? = entries[key(english)]

    val size get() = entries.size

    companion object {
        /**
         * Clé de comparaison : sans accents ni ponctuation, et orthographe britannique ramenée à
         * l'américaine (BG3 écrit « Armour », les livres de D&D « Armor »). La même règle s'applique
         * des deux côtés : elle ne crée pas de correspondance, elle en retrouve.
         */
        fun key(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase()
            .replace(Regex("['’]"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
            .split(' ').joinToString(" ") { word ->
                word.replace(Regex("our$"), "or").replace(Regex("ise$"), "ize").replace(Regex("yse$"), "yze").replace(Regex("tre$"), "ter")
                    .let { if (it == "grey") "gray" else it }
            }

        /**
         * Liste filtrable d'AideDD : chaque ligne porte le nom VF dans la cellule `item`
         * et le nom VO dans la cellule `colVO`.
         */
        fun parseAideDd(html: String, source: String): Lexicon {
            val row = Regex("<tr>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
            val vf = Regex("<td class=\"item\"><a [^>]*>(.*?)</a>")
            val vo = Regex("<td class=\"colVO\">(.*?)</td>")
            val map = linkedMapOf<String, String>()
            row.findAll(html).forEach { r ->
                val fr = vf.find(r.value)?.groupValues?.get(1)?.let(::unescape)?.trim()
                val en = vo.find(r.value)?.groupValues?.get(1)?.let(::unescape)?.trim()
                if (!fr.isNullOrEmpty() && !en.isNullOrEmpty()) map.putIfAbsent(key(en), fr)
            }
            return Lexicon(source, map)
        }

        private fun unescape(s: String) = s.replace(Regex("<[^>]+>"), "")
            .replace("&#039;", "'").replace("&apos;", "'").replace("&amp;", "&").replace("&quot;", "\"")
    }
}

/**
 * Résout le nom affiché d'une entrée : nom choisi à la main, sinon lexique officiel D&D,
 * sinon nom anglais marqué « traduction non officielle » et signalé à traduire.
 */
class NameResolver(
    private val curated: Map<String, CuratedName>,
    /** Lexiques par sorte d'entrée (`spell`, `item`…) : un nom de sort ne sert jamais à un objet. */
    private val lexicons: Map<String, List<Lexicon>>,
) {
    val missing = sortedSetOf<String>()

    fun resolve(id: ContentId, english: String): Name {
        curated[id.value]?.let { return Name(typography(it.fr), english, it.statut.status, it.source) }
        for (lexicon in lexicons[id.kind].orEmpty()) {
            lexicon.lookup(english)?.let { return Name(typography(it), english, NameStatus.VF_DND, lexicon.source) }
        }
        missing += id.value
        return Name(typography(english), english, NameStatus.UNOFFICIAL, null)
    }

    companion object {
        private val yaml = Yaml(configuration = YamlConfiguration(strictMode = true))

        fun loadCurated(file: Path): Map<String, CuratedName> =
            if (!file.exists()) emptyMap()
            else yaml.decodeFromString(MapSerializer(String.serializer(), CuratedName.serializer()), file.readText())

        /** Apostrophe typographique dans les noms français. */
        fun typography(name: String) = name.replace('\'', '’')
    }
}
