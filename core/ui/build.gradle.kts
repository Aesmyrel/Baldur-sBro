plugins {
    alias(libs.plugins.carnet.android.library)
    alias(libs.plugins.carnet.android.compose)
    alias(libs.plugins.carnet.android.screenshots)
}

android {
    namespace = "fr.carnetdefaerun.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
    implementation(libs.kotlinx.collections.immutable)
}
