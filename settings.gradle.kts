pluginManagement {
    repositories {
        maven("https://mcentral.firstdark.dev/releases")
        gradlePluginPortal()
    }
}

rootProject.name = "Mod Template"

include("common")
include("fabric")
include("forge")
include("neoforge")
project(":neoforge").name = "neoForge"