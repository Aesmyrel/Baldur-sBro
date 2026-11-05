plugins {
    alias(libs.plugins.carnet.android.feature)
    alias(libs.plugins.carnet.android.screenshots)
}

android {
    namespace = "fr.carnetdefaerun.feature.settings"
}

dependencies {
    // Sélecteur de documents du système pour l'export et l'import (aucune permission).
    implementation(libs.androidx.activity.compose)
}
