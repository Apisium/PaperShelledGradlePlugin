# PaperShelled

A Paper plugin mixin development framework.

## Usage

Simple usage:

```groovy
buildscript {
    repositories {
        maven { url "https://maven.fabricmc.net/" }
    }
}

plugins {
    id 'java'
    id 'cn.apisium.papershelled' version '1.0.3'
}

paperShelled {
    jarUrl = "https://papermc.io/api/v2/projects/paper/versions/1.17.1/builds/257/downloads/paper-1.17.1-257.jar"
}

dependencies {
    compileOnly paperShelled.jar()
}

```

### Complete example

[See also](./example)

```groovy
buildscript {
    repositories {
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id 'cn.apisium.papershelled' version '1.0.3'
    id 'com.github.johnrengelman.shadow' version '7.0.0'
}

dependencies {
    compileOnly paperShelled.jar()
}

paperShelled {
    jarUrl = 'https://papermc.io/api/v2/projects/paper/versions/1.17.1/builds/257/downloads/paper-1.17.1-257.jar'
    jarFile = 'some/path/server.jar'
    reobfFile = 'some/path/reobf.tiny'
    spigotMap = 'spigot'
    mojangMap = 'mojang+yarn'
    archiveClassifier = "-reobf"
    relocateCraftBukkit = true
    reobfAfterJarTask = true
}

shadowJar {
    dependencies {
        relocate('org.bukkit.craftbukkit', 'org.bukkit.craftbukkit.' + paperShelled.craftBukkitVersion.get())
    }
    archiveClassifier.set('')
    minimize()
}
build.dependsOn(shadowJar)
```

## License

[License](./LICENSE)
