import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Captures d'écran sur JVM : Robolectric (graphismes natifs) + Roborazzi. */
class AndroidScreenshotsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")
        tasks.withType<Test>().configureEach {
            // Robolectric télécharge ses images Android à la volée : on passe par le miroir de Maven Central.
            systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
            maxHeapSize = "3g"
            // Les captures de référence sont des entrées : en changer une relance la comparaison.
            inputs.files(fileTree("src/test/screenshots")).withPropertyName("referenceScreenshots").withPathSensitivity(PathSensitivity.RELATIVE)
            jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens", "java.base/java.io=ALL-UNNAMED")
        }
        // `check` compare les captures aux références : un écart fait échouer le build (D-036).
        tasks.named("check").configure { dependsOn("verifyRoborazziDebug") }
        dependencies {
            add("testImplementation", catalog.findLibrary("robolectric").get())
            add("testImplementation", catalog.findLibrary("roborazzi").get())
            add("testImplementation", catalog.findLibrary("roborazzi-compose").get())
            add("testImplementation", catalog.findLibrary("roborazzi-junit-rule").get())
            add("testImplementation", catalog.findLibrary("compose-ui-test-junit4").get())
            add("testImplementation", catalog.findLibrary("androidx-test-core").get())
            add("testImplementation", catalog.findLibrary("androidx-test-ext-junit").get())
            add("testImplementation", catalog.findLibrary("androidx-activity-compose").get())
            add("debugImplementation", catalog.findLibrary("compose-ui-test-manifest").get())
        }
    }
}
