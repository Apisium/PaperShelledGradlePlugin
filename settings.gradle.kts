pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = ("papershelled")

include(":example")
includeBuild("plugin-build")
