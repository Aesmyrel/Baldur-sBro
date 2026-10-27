plugins {
    alias(libs.plugins.carnet.android.application)
    alias(libs.plugins.carnet.android.compose)
    alias(libs.plugins.carnet.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.carnetdefaerun"
    defaultConfig {
        applicationId = "fr.carnetdefaerun"
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    listOf("home", "encyclopedia", "codex", "builds", "companions", "progress", "tools", "search", "notes", "settings")
        .forEach { implementation(project(":feature:$it")) }
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
}
