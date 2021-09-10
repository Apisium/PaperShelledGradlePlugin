@file:Suppress("UnstableApiUsage", "unused")

package cn.apisium.papershelled.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

lateinit var outJar: FileCollection

fun paperShelledJar() = outJar

abstract class Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        outJar = project.layout.getCaches("out.jar")
        val ds = project.gradle.sharedServices.registerIfAbsent("downloader", DownloadService::class.java) {}

        val extension = project.extensions.create("paperShelled", Extension::class.java, project)

        val download = project.tasks.register("download", DownloadTask::class.java) {
            it.jarUrl.set(extension.jarUrl)
            it.downloader.set(ds)
        }

        val gmj = project.tasks.register("generateMappedJar", GenerateMappedJarTask::class.java) {
            it.jarFile.set(extension.jarFile)
            it.reobfFile.set(extension.reobfFile)
            it.spigotMap.set(extension.spigotMap)
            it.mojangMap.set(extension.mojangMap)
        }

        project.tasks.register("setupPaperShelled", SetupTask::class.java) {
            it.dependsOn(download, gmj)
        }
    }
}
