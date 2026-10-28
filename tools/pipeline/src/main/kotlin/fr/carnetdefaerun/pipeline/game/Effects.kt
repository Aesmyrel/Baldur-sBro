package fr.carnetdefaerun.pipeline.game

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import fr.carnetdefaerun.core.model.content.Block
import fr.carnetdefaerun.core.model.content.BlockKind
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

/**
 * Effets rédigés en français par le projet (fichiers YAML de `data/effets`, un par lot d’écriture).
 * Style télégraphique et factuel, chiffres repris des données structurées, jamais une traduction
 * phrase à phrase du texte du jeu (docs/DECISIONS.md D-062).
 */
@Serializable
data class WrittenEffects(
    val effets: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    /** Étiquette plus stricte que celle de la fiche, si le texte en dit davantage (lieu, personnage). */
    val etiquette: SpoilerTag? = null,
    /** Révision de la page source consultée pour la rédaction. */
    val revision: Long,
)

class Effects(private val dir: Path) {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = true))

    fun load(): Map<String, WrittenEffects> {
        if (!dir.exists()) return emptyMap()
        val merged = linkedMapOf<String, WrittenEffects>()
        for (file in dir.listDirectoryEntries("*.yaml").sorted()) {
            yaml.decodeFromString(MapSerializer(String.serializer(), WrittenEffects.serializer()), file.readText()).forEach { (id, e) ->
                require(merged.put(id, e) == null) { "Effets en double pour $id (${file.fileName})" }
            }
        }
        return merged
    }

    /** Ajoute les blocs rédigés aux entrées ; renvoie aussi les identifiants inconnus et les rédactions périmées. */
    fun apply(entries: List<Entry>, written: Map<String, WrittenEffects>): Triple<List<Entry>, List<String>, List<String>> {
        val known = entries.associateBy { it.id.value }
        val unknownIds = written.keys.filterNot { it in known }
        val stale = mutableListOf<String>()
        val result = entries.map { entry ->
            val w = written[entry.id.value] ?: return@map entry
            if (entry.attribution.none { it.revision == w.revision }) stale += entry.id.value
            val tag = w.etiquette?.let { entry.tag.strictest(it) } ?: entry.tag
            val blocks = w.effets.mapIndexed { i, t -> Block("effet-${i + 1}", BlockKind.EFFECT, t.trim(), tag) } +
                w.notes.mapIndexed { i, t -> Block("note-${i + 1}", BlockKind.NOTE, t.trim(), tag) }
            entry.copy(
                blocks = entry.blocks + blocks,
                // Les effets rédigés comblent ce que les données structurées ne disaient pas.
                unknown = entry.unknown - setOf("effect_text", "effects"),
            )
        }
        return Triple(result, unknownIds, stale)
    }
}
