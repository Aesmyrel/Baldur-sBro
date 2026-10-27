import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Module d'écran : bibliothèque Android + Compose + Hilt + socle commun. Pas de dépendance entre features. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("carnet.android.library")
        pluginManager.apply("carnet.android.compose")
        pluginManager.apply("carnet.android.hilt")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", catalog.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", catalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", catalog.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
            add("implementation", catalog.findLibrary("androidx-navigation3-runtime").get())
            add("implementation", catalog.findLibrary("kotlinx-collections-immutable").get())
            add("testImplementation", project(":core:testing"))
            add("testImplementation", catalog.findLibrary("kotlinx-coroutines-test").get())
            add("testImplementation", catalog.findLibrary("turbine").get())
        }
    }
}
