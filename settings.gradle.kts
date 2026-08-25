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

rootProject.name = "EveryCue"

include(":app")
include(":core:navigation")
include(":core:designsystem")
include(":core:database")
include(":core:security")
include(":core:extraction")
include(":core:attachments")
include(":core:vision")
include(":feature:track")
include(":feature:pack")
include(":feature:renew")
