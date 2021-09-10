package cn.apisium.papershelled.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkerExecutor
import java.net.URLClassLoader
import java.nio.file.Paths
import javax.inject.Inject

private const val GROUP = "papershelled"

abstract class DownloadTask : DefaultTask() {
    init {
        description = "Download server jar"
        group = GROUP
    }

    @get:Input
    @get:Option(option = "jarUrl", description = "The url to download server jar")
    abstract val jarUrl: Property<String>

    @get:Input
    @get:Option(option = "jarFile", description = "The file path of server jar")
    @get:Optional
    abstract val jarFile: Property<String>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val downloader: Property<DownloadService>

    @TaskAction
    fun run() {
        logger.lifecycle("Downloading jar file: ${jarUrl.get()}")
        val jarFile2 = jarFile.orNull
        val file = if (jarFile2 == null) project.layout.cache.resolve("server.jar") else Paths.get(jarFile2)
        workerExecutor.noIsolation().submit(DownloadWorker::class.java) {
            it.downloader.set(downloader)
            it.source.set(jarUrl)
            it.target.set(file.toFile())
        }
    }
}

abstract class GenerateMappedJarTask : DefaultTask() {
    init {
        description = "Generate mapped jar file"
        group = GROUP
    }

    @get:Input
    @get:Option(option = "jarFile", description = "The file path of server jar")
    @get:Optional
    abstract val jarFile: Property<String>

    @TaskAction
    fun run() {
        val jarFile2 = jarFile.orNull
        val file = if (jarFile2 == null) project.layout.cache.resolve("server.jar") else Paths.get(jarFile2)
        try {
            URLClassLoader(arrayOf(file.toUri().toURL())).use {
                val m = it.loadClass("io.papermc.paperclip.Paperclip").getDeclaredMethod("setupEnv")
                m.isAccessible = true
                logger.lifecycle(m.invoke(null).toString())
            }
        } catch (e: Throwable) {
            logger.warn(e.message)
        }
    }
}
