@file:Suppress("UnstableApiUsage", "unused")

package cn.apisium.papershelled.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ds = project.gradle.sharedServices.registerIfAbsent("downloader", DownloadService::class.java) {}
        // Add the 'template' extension object
        val extension = project.extensions.create("papershelled", Extension::class.java, project)

        project.tasks.register("download", DownloadTask::class.java) {
            it.jarUrl.set(extension.jarUrl)
            it.jarFile.set(extension.jarFile)
            it.downloader.set(ds)
        }

        project.tasks.register("generateMappedJar", GenerateMappedJarTask::class.java) {
            it.jarFile.set(extension.jarFile)
        }
    }
}
