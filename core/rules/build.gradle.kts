plugins {
    alias(libs.plugins.carnet.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
