plugins {
    `kotlin-dsl`
}

group = "fr.carnetdefaerun.buildlogic"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "carnet.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "carnet.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "carnet.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "carnet.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "carnet.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidScreenshots") {
            id = "carnet.android.screenshots"
            implementationClass = "AndroidScreenshotsConventionPlugin"
        }
        register("jvmLibrary") {
            id = "carnet.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
