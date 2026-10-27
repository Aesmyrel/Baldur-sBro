plugins {
    alias(libs.plugins.carnet.android.library)
    alias(libs.plugins.carnet.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.carnetdefaerun.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:rules"))
    implementation(project(":core:database"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
