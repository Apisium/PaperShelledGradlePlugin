import cn.apisium.papershelled.gradle.paperShelledJar

plugins {
    java
    id("cn.apisium.papershelled")
}

buildscript {
    repositories {
        maven("https://maven.fabricmc.net/")
    }
}

dependencies {
    compileOnly(paperShelledJar())
}

paperShelled {
    jarUrl.set("https://papermc.io/api/v2/projects/paper/versions/1.17.1/builds/257/downloads/paper-1.17.1-257.jar")
}
