import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Réglages partagés par tous les modules (docs/PLAN.md §1.1). */
object ProjectConfig {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 33 // AGSL (RuntimeShader) disponible sans version dégradée
    val JAVA = JavaVersion.VERSION_17
}

internal val Project.catalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK
        defaultConfig.minSdk = ProjectConfig.MIN_SDK
        compileOptions.sourceCompatibility = ProjectConfig.JAVA
        compileOptions.targetCompatibility = ProjectConfig.JAVA
        lint.abortOnError = true
        // Aucun avertissement toléré (D-036) ; les exceptions sont listées dans `lint.disable`, avec leur raison.
        lint.warningsAsErrors = true
        lint.checkDependencies = false
    }
}
