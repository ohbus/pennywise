pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "pennywise"
include(":app:accounts", ":app:expense-core", ":app:notifications", ":app:bff")
include(":libs:db", ":libs:security", ":libs:observability", ":libs:test-support", ":libs:errors", ":libs:ids")
