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
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(mergedManifest.get().asFile)
        val nodes = doc.getElementsByTagName("uses-permission")
        val found = (0 until nodes.length).map { nodes.item(it).attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "name").nodeValue }
        val forbidden = found.filterNot { it in allowed.get() }
        report.get().asFile.writeText(found.joinToString("\n"))
        if (forbidden.isNotEmpty()) throw GradleException("Permissions non autorisées dans le manifeste fusionné : ${forbidden.joinToString()}")
    }
}
