pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Repos extra útiles
        maven { url = uri("https://jitpack.io") }
        maven("https://maven.google.com")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repos extra útiles
        maven { url = uri("https://jitpack.io") }
        maven("https://maven.google.com")
    }
}

rootProject.name = "taller_3_icm"
include(":app")
