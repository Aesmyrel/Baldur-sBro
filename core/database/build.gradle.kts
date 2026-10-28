plugins {
    alias(libs.plugins.carnet.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.carnetdefaerun.core.database"
}

room {
    // Base du joueur (user.db) : schéma exporté et versionné, migrations testées.
    schemaDirectory("$projectDir/schemas")
}

// Base de contenu : générée à la compilation par tools:pipeline à partir de data/ (jamais versionnée).
val pipeline: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

abstract class BuildContentDb : JavaExec() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}

val buildContentDb = tasks.register<BuildContentDb>("buildContentDb") {
    description = "Construit content.db à partir des données validées (data/)."
    classpath = pipeline
    mainClass.set("fr.carnetdefaerun.pipeline.MainKt")
    workingDir = rootDir
    inputs.dir(rootDir.resolve("data")).withPathSensitivity(PathSensitivity.RELATIVE)
    outputDir.set(layout.buildDirectory.dir("generated/contentDb"))
    val out = outputDir.file("content.db")
    argumentProviders.add(CommandLineArgumentProvider { listOf("build-db", "--out", out.get().asFile.absolutePath) })
}

androidComponents {
    onVariants { variant -> variant.sources.assets?.addGeneratedSourceDirectory(buildContentDb, BuildContentDb::outputDir) }
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)
    pipeline(project(":tools:pipeline"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
    jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens", "java.base/java.io=ALL-UNNAMED")
}
