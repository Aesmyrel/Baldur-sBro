import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Contrôle bloquant du manifeste fusionné : toute permission absente de la liste blanche fait échouer
 * le build. Garantit en particulier l'absence d'INTERNET (docs/DECISIONS.md D-021).
 */
abstract class VerifyPermissionsTask : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val mergedManifest: RegularFileProperty
    @get:Input abstract val allowed: ListProperty<String>
    @get:OutputFile abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val found = ManifestPermissions.declared(mergedManifest.get().asFile.readText())
        val forbidden = ManifestPermissions.forbidden(found, allowed.get())
        report.get().asFile.writeText(found.joinToString("\n"))
        if (forbidden.isNotEmpty()) throw GradleException("Permissions non autorisées dans le manifeste fusionné : ${forbidden.joinToString()}")
    }
}

/** Lecture des permissions d'un manifeste, sous toutes leurs balises (y compris `uses-permission-sdk-23`). */
object ManifestPermissions {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private val TAGS = listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")

    fun declared(manifestXml: String): List<String> {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifestXml.byteInputStream())
        return TAGS.flatMap { tag ->
            val nodes = doc.getElementsByTagName(tag)
            (0 until nodes.length).map { nodes.item(it).attributes.getNamedItemNS(ANDROID_NS, "name").nodeValue }
        }.distinct()
    }

    fun forbidden(declared: List<String>, allowed: List<String>) = declared.filterNot { it in allowed }
}
