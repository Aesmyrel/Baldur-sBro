plugins {
    alias(libs.plugins.carnet.android.library)
}

android {
    namespace = "fr.carnetdefaerun.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(libs.junit)
    api(libs.truth)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
