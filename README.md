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
    id 'cn.apisium.papershelled' version '1.2.0'
}

paperShelled {
    jarUrl = 'https://papermc.io/api/v2/projects/paper/versions/1.18.1/builds/175/downloads/paper-1.18.1-175.jar'
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
    id 'cn.apisium.papershelled' version '1.2.0'
    id 'com.github.johnrengelman.shadow' version '7.0.0'
}

dependencies {
    compileOnly paperShelled.jar()
}

paperShelled {
    jarUrl = 'https://papermc.io/api/v2/projects/paper/versions/1.18.1/builds/175/downloads/paper-1.18.1-175.jar'
    version = 'master-SNAPSHOT'
    annotationsVersion = 'master-SNAPSHOT'
    mixinVersion = '0.8.4'
    jarFile = 'some/path/server.jar'
    reobfFile = 'some/path/reobf.tiny'
    archiveClassifier = '-reobf'
    referenceMapName = 'ProjectName.refmap.json'
    relocateCraftBukkit = true
    reobfAfterJarTask = true
    generateReferenceMap = true
    addJarToDependencies = true
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
