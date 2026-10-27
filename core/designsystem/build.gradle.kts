plugins {
    alias(libs.plugins.carnet.android.library)
    alias(libs.plugins.carnet.android.compose)
    alias(libs.plugins.carnet.android.screenshots)
}

android {
    namespace = "fr.carnetdefaerun.core.designsystem"
}

dependencies {
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.animation)
    api(libs.compose.ui.text)
    implementation(libs.androidx.core.ktx)
}
