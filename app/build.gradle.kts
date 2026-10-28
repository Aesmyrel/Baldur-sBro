import com.android.build.api.artifact.SingleArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.carnet.android.application)
    alias(libs.plugins.carnet.android.compose)
    alias(libs.plugins.carnet.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.carnetdefaerun"
    testOptions.unitTests.isIncludeAndroidResources = true
    // APK release volontairement limité à arm64 (Pixel 10 Pro XL).
    lint.disable += "ChromeOsAbiSupport"
    defaultConfig {
        applicationId = "fr.carnetdefaerun"
        versionCode = 1
        versionName = "0.1.0"
    }
    // Clé de signature locale, jamais versionnée : keystore.properties à la racine (voir docs/INSTALLER.md).
    val keystoreFile = rootProject.file("keystore.properties")
    val signing = keystoreFile.takeIf { it.exists() }?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    signingConfigs {
        if (signing != null) {
            create("release") {
                storeFile = rootProject.file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            // Pixel 10 Pro XL : arm64 uniquement.
            ndk { abiFilters += "arm64-v8a" }
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        tasks.register<VerifyPermissionsTask>("verify${name}Permissions") {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            allowed.set(listOf("android.permission.VIBRATE", "fr.carnetdefaerun.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"))
            report.set(layout.buildDirectory.file("reports/permissions-${variant.name}.txt"))
        }
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
    jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens", "java.base/java.io=ALL-UNNAMED")
}

tasks.named("check") {
    dependsOn("verifyReleasePermissions", "verifyDebugPermissions")
    // Tests des plugins de convention (dont la lecture des permissions du manifeste).
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
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
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
