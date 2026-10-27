import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            configureAndroidCommon(this)
            testOptions.unitTests.isIncludeAndroidResources = true
            testOptions.unitTests.isReturnDefaultValues = true
        }
        // Hilt/KSP génère des sources de test même sans test : on n'échoue pas sur un module sans test.
        tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }
        dependencies {
            add("testImplementation", catalog.findLibrary("junit").get())
            add("testImplementation", catalog.findLibrary("truth").get())
        }
    }
}
