plugins {
    alias(libs.plugins.carnet.android.feature)
    alias(libs.plugins.carnet.android.screenshots)
}

android {
    namespace = "fr.carnetdefaerun.feature.notes"
}

dependencies {
    // Retour système intercepté par l'éditeur pour enregistrer la note.
    implementation(libs.androidx.activity.compose)
}
