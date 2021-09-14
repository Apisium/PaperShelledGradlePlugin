@file:Suppress("UnstableApiUsage", "unused")

package cn.apisium.papershelled.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import java.net.URI
import java.nio.file.Files

internal var lastJarTask: Jar? = null

abstract class Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ds = project.gradle.sharedServices.registerIfAbsent("downloader", DownloadService::class.java) {}

        val extension = project.extensions.create("paperShelled", Extension::class.java, project)

        val download = project.tasks.register("download", DownloadTask::class.java) {
            it.jarUrl.set(extension.jarUrl)
            it.jarFile.set(extension.jarFile)
            it.downloader.set(ds)
        }

        val gmj = project.tasks.register("generateMappedJar", GenerateMappedJarTask::class.java) {
            it.jarFile.set(extension.jarFile)
            it.reobfFile.set(extension.reobfFile)
            it.paperShelledJar.set(extension.paperShelledJar)
            if (!Files.exists(it.jarFile.get().asFile.toPath())) it.dependsOn(download)
        }

        val reobf = project.tasks.register("reobf", ReobfTask::class.java) {
            it.relocateCraftBukkit.set(extension.relocateCraftBukkit)
            it.reobfFile.set(extension.reobfFile)
            it.craftBukkitVersion.set(extension.craftBukkitVersion)
            it.paperShelledJar.set(extension.paperShelledJar)
            it.archiveClassifier.set(extension.archiveClassifier)
            if (!Files.exists(it.reobfFile.get().asFile.toPath()) ||
                !Files.exists(it.paperShelledJar.get().asFile.toPath())) it.dependsOn(gmj)
        }

        if (extension.reobfAfterJarTask.get()) project.tasks.withType(Jar::class.java) {
            lastJarTask = it
            it.finalizedBy(reobf)
        }

        if (extension.generateReferenceMap.get()) {
            val refMap = project.layout.tmp.resolve("refmap.json")
            if (Files.exists(refMap)) Files.delete(refMap)
            project.afterEvaluate {
                arrayOf(
                    "net.fabricmc:fabric-mixin-compile-extensions:0.4.6",
                    "org.apache.logging.log4j:log4j-core:2.14.1",
                    "org.ow2.asm:asm-commons:9.2"
                ).forEach { name ->
                    project.dependencies.add("annotationProcessor", project.dependencies.create(name))
                }
                project.repositories.maven { it.url = URI.create("https://maven.fabricmc.net/") }
                project.repositories.mavenCentral()
            }
            project.tasks.withType(JavaCompile::class.java) {
                it.options.compilerArgs.apply {
                    add("-AdefaultObfuscationEnv=named:intermediary")
                    add("-AinMapFileNamedIntermediary=" + extension.reobfFile.get().asFile.path)
                    add("-AoutRefMapFile=$refMap")
                }
            }
            project.tasks.withType(Jar::class.java) {
                it.from(project.file(refMap.toFile())) { c ->
                    c.rename { extension.referenceMapName.get() }
                }
            }
        }
    }
}
