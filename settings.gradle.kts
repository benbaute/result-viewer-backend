pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.24"
    }
}
rootProject.name = "backend"
include("common")
include("rides")
include("osmPlanet")
include("valhalla")
