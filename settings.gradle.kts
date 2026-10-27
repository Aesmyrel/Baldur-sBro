pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        // Miroir officiel de Maven Central hébergé par Google (moins sujet aux erreurs 429).
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Miroir officiel de Maven Central hébergé par Google (moins sujet aux erreurs 429).
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
        mavenCentral()
    }
}

rootProject.name = "CarnetDeFaerun"

include(":app")
include(":core:model")
include(":core:rules")
include(":core:database")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")
include(":feature:home")
include(":feature:encyclopedia")
include(":feature:codex")
include(":feature:builds")
include(":feature:companions")
include(":feature:progress")
include(":feature:tools")
include(":feature:search")
include(":feature:notes")
include(":feature:settings")
include(":tools:pipeline")
