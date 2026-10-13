pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RoamMate"
include(
    ":app",
    ":backend",
    ":core:database",
    ":feature:cloud",
    ":feature:planner",
    ":feature:poi",
    ":feature:sensor",
)
