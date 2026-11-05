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
    // Lecture des QR codes sans permission caméra, par Google Play services. Ses permissions réseau sont retirées
    // du manifeste fusionné de l'appli (D-021).
    implementation(libs.play.services.code.scanner)
    implementation(libs.androidx.activity.compose)
}
