package fr.carnetdefaerun.core.testing

import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.Entry
import java.io.File
import kotlinx.serialization.builtins.ListSerializer

/** Données validées du dépôt (`data/game`), lues depuis le dossier d'un module (`core/data`, `feature/…`). */
object GameData {
    val entries: List<Entry> by lazy {
        File("../../data/game").listFiles { f -> f.extension == "json" }!!.sorted().flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), it.readText())
        }
    }
}
