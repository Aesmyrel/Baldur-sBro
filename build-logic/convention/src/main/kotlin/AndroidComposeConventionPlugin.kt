import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val android = extensions.getByName("android") as CommonExtension
        android.buildFeatures.compose = true
        dependencies {
            val bom = catalog.findLibrary("compose-bom").get()
            add("implementation", platform(bom))
            add("testImplementation", platform(bom))
            add("implementation", catalog.findLibrary("compose-ui").get())
            add("implementation", catalog.findLibrary("compose-foundation").get())
            add("implementation", catalog.findLibrary("compose-material3").get())
            add("implementation", catalog.findLibrary("compose-ui-tooling-preview").get())
            add("debugImplementation", catalog.findLibrary("compose-ui-tooling").get())
        }
    }
}
