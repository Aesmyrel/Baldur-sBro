package fr.carnetdefaerun.core.testing

import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.Reference
import java.io.File
import kotlinx.serialization.builtins.ListSerializer

/**
 * Données validées du dépôt, lues depuis le dossier d'un module (`core/data`, `feature/…`) : données de jeu
 * (`data/game`) et codex assemblé (`data/lore`), exactement ce que contient la base embarquée.
 */
object GameData {
    val entries: List<Entry> by lazy {
        listOf("../../data/game", "../../data/lore").flatMap { dir -> File(dir).listFiles { f -> f.extension == "json" && f.name != REFERENCES }.orEmpty().sorted() }.flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), it.readText())
        }
    }

    /** Registre des références du codex (`data/lore/references.json`). */
    val references: Map<String, Reference> by lazy {
        val file = File("../../data/lore/$REFERENCES")
        if (!file.exists()) emptyMap()
        else ContentFormat.strict.decodeFromString(ListSerializer(Reference.serializer()), file.readText()).associateBy { it.id }
    }

    private const val REFERENCES = "references.json"
}
