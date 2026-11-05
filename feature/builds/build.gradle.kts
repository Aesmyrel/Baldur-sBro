plugins {
    alias(libs.plugins.carnet.android.feature)
    alias(libs.plugins.carnet.android.screenshots)
}

android {
    namespace = "fr.carnetdefaerun.feature.builds"
}

dependencies {
    // Génération du QR code de partage, sans réseau (bibliothèque Java pure, Apache 2.0).
    implementation(libs.zxing.core)
}
