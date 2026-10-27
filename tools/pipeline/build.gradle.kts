plugins {
    alias(libs.plugins.carnet.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("fr.carnetdefaerun.pipeline.MainKt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)
    implementation(libs.sqlite.jdbc)
}
