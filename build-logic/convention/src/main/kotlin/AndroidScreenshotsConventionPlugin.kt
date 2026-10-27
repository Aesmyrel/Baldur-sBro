import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Captures d'écran sur JVM : Robolectric (graphismes natifs) + Roborazzi. */
class AndroidScreenshotsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.github.takahirom.roborazzi")
        dependencies {
            add("testImplementation", catalog.findLibrary("robolectric").get())
            add("testImplementation", catalog.findLibrary("roborazzi").get())
            add("testImplementation", catalog.findLibrary("roborazzi-compose").get())
            add("testImplementation", catalog.findLibrary("roborazzi-junit-rule").get())
            add("testImplementation", catalog.findLibrary("compose-ui-test-junit4").get())
            add("testImplementation", catalog.findLibrary("androidx-test-core").get())
            add("debugImplementation", catalog.findLibrary("compose-ui-test-manifest").get())
        }
    }
}
