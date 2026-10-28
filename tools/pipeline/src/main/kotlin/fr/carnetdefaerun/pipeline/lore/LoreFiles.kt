package fr.carnetdefaerun.pipeline.lore

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import fr.carnetdefaerun.core.model.content.CanonNature
import fr.carnetdefaerun.core.model.content.Citation
import fr.carnetdefaerun.core.model.content.Consultation
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Edition
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.Link
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.LoreParagraph
import fr.carnetdefaerun.core.model.content.LoreSection
import fr.carnetdefaerun.core.model.content.LoreSectionType
import fr.carnetdefaerun.core.model.content.LoreTheme
import fr.carnetdefaerun.core.model.content.Name
import fr.carnetdefaerun.core.model.content.Reference
import fr.carnetdefaerun.core.model.content.ReferenceKind
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import fr.carnetdefaerun.pipeline.i18n.CuratedStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.streams.toList

/** Licence des textes rédigés pour le carnet. */
const val LICENCE_CARNET = "carnet"

// Format d'écriture d'une fiche de lore (content/lore/<thème>/<fiche>.yaml), clés en français.

@Serializable
data class LoreNameFile(val fr: String, val en: String, val statut: CuratedStatus, val source: String? = null)

@Serializable
data class ParagraphFile(
    val texte: String,
    val nature: CanonNature,
    val edition: Edition? = null,
    val refs: List<Citation>,
    val variantes: String? = null,
)

@Serializable
data class SectionFile(
    val type: LoreSectionType,
    val titre: String,
    val etiquette: SpoilerTag,
    val paragraphes: List<ParagraphFile>,
)

@Serializable
data class RelationFile(val cible: String, val type: Relation, val etiquette: SpoilerTag? = null)

@Serializable
data class LoreFile(
    val id: String,
    val theme: LoreTheme,
    val nom: LoreNameFile,
    val alias: List<String> = emptyList(),
    @SerialName("etiquette_titre") val titleTag: SpoilerTag,
    /** Étiquette de la fiche ouverte (accroche), au moins celle du titre. */
    val etiquette: SpoilerTag,
    val resume: String,
    val sections: List<SectionFile>,
    val relations: List<RelationFile> = emptyList(),
)

@Serializable
data class ReferenceFile(
    val type: ReferenceKind,
    @SerialName("titre_vo") val titleOriginal: String,
    @SerialName("titre_vf") val titleFrench: String? = null,
    val editeur: String? = null,
    val annee: Int? = null,
    val edition: Edition? = null,
    val url: String? = null,
    val revision: Long? = null,
    val isbn: String? = null,
    val licence: String? = null,
    val consultation: Consultation,
    val via: String? = null,
)

/** Lecture du codex : registre des références et fiches, converties au format commun des entrées. */
class LoreFiles(private val root: Path) {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = true))

    /**
     * Registre des références : `references.yaml` et les fichiers de `references/` (un par lot de rédaction).
     * Une même référence peut figurer dans deux fichiers si elle y est décrite à l'identique.
     */
    fun references(): Map<String, Reference> {
        val dir = root.resolve("references")
        val files = listOf(root.resolve("references.yaml")).filter { it.exists() } +
            (if (dir.exists()) Files.list(dir).use { s -> s.filter { it.extension == "yaml" }.sorted().toList() } else emptyList())
        val merged = linkedMapOf<String, Reference>()
        for (file in files) {
            yaml.decodeFromString(MapSerializer(String.serializer(), ReferenceFile.serializer()), file.readText()).forEach { (id, r) ->
                val ref = Reference(id, r.type, r.titleOriginal, r.titleFrench, r.editeur, r.annee, r.edition, r.url, r.revision, r.isbn, r.licence, r.consultation, r.via)
                val previous = merged.put(id, ref)
                require(previous == null || previous == ref) { "Référence « $id » décrite différemment dans ${file.fileName}" }
            }
        }
        return merged
    }

    /** Fiches et chemin relatif de leur fichier (pour les messages). */
    fun files(): List<Pair<String, LoreFile>> {
        val dir = root.resolve("lore")
        if (!dir.exists()) return emptyList()
        return Files.walk(dir).use { s -> s.filter { it.extension == "yaml" }.sorted().toList() }.map { path ->
            root.relativize(path).toString() to yaml.decodeFromString(LoreFile.serializer(), path.readText())
        }
    }

    fun entry(file: LoreFile): Entry {
        val sections = file.sections.mapIndexed { i, s ->
            LoreSection(
                id = "s${i + 1}",
                type = s.type,
                title = s.titre.trim(),
                tag = s.etiquette,
                paragraphs = s.paragraphes.map { p -> LoreParagraph(p.texte.trim(), p.nature, p.edition, p.refs, p.variantes) },
            )
        }
        val words = (listOf(file.resume) + sections.flatMap { s -> s.paragraphs.map { it.text } }).sumOf { it.split(Regex("\\s+")).size }
        return Entry(
            id = ContentId(file.id),
            type = EntryType.LORE,
            name = Name(file.nom.fr, file.nom.en, file.nom.statut.status, file.nom.source),
            aliases = file.alias,
            tag = file.etiquette,
            titleTag = file.titleTag,
            licence = LICENCE_CARNET,
            facts = LoreFacts(file.theme, file.resume.trim(), sections, readingMinutes = maxOf(1, (words + 199) / 200)),
            links = file.relations.map { Link(ContentId(it.cible), it.type, it.etiquette ?: SpoilerTag.LOCKED) },
        )
    }
}
