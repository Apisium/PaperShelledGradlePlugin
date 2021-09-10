# PaperShelled

A Paper plugin mixin development framework.

## Usage

Simple usage:

```groovy
import cn.apisium.papershelled.gradle.paperShelledJar

plugins {
    id "cn.apisium.papershelled"
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
```

### Complete example

[See also](./example)

```groovy
import cn.apisium.papershelled.gradle.paperShelledJar
import cn.apisium.papershelled.gradle.craftBukkitVersion

plugins {
    id "cn.apisium.papershelled"
    id 'com.github.johnrengelman.shadow' version '7.0.0'
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
    jarUrl = "https://papermc.io/api/v2/projects/paper/versions/1.17.1/builds/257/downloads/paper-1.17.1-257.jar"
    jarFile = "some/path/server.jar"
    reobfFile = "some/path/reobf.tiny"
    spigotMap = "spigot"
    mojangMap = "mojang+yarn"
    relocateCraftBukkit = false
}

shadowJar {
    dependencies {
        relocate('org.bukkit.craftbukkit', 'org.bukkit.craftbukkit.' + craftBukkitVersion())
    }
    archiveClassifier.set('')
    minimize()
}
build.dependsOn(shadowJar)
```

## License

[License](./LICENSE)
