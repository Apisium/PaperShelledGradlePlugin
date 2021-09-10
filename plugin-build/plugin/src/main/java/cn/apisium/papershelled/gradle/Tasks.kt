package cn.apisium.papershelled.gradle

import me.lucko.jarrelocator.JarRelocator
import me.lucko.jarrelocator.Relocation
import net.fabricmc.tinyremapper.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.workers.WorkerExecutor
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

private const val GROUP = "papershelled"

abstract class SetupTask : DefaultTask() {
    init {
        description = "Setup"
        group = GROUP
    }

    @TaskAction
    fun run() = Unit
}

abstract class DownloadTask : DefaultTask() {
    init {
        description = "Download server jar"
        group = GROUP
    }

    @get:Input
    @get:Option(option = "jarUrl", description = "The url to download server jar")
    abstract val jarUrl: Property<String>

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Internal
    abstract val downloader: Property<DownloadService>

    @TaskAction
    fun run() {
        logger.lifecycle("Downloading jar file: ${jarUrl.get()}")
        workerExecutor.noIsolation().submit(DownloadWorker::class.java) {
            it.downloader.set(downloader)
            it.source.set(jarUrl)
            it.target.set(project.layout.cache.resolve("server.jar").toFile())
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
    abstract val jarFile: RegularFileProperty

    @get:Input
    @get:Option(option = "reobfFile", description = "The file path of reobf map")
    @get:Optional
    abstract val reobfFile: RegularFileProperty

    @get:Input
    @get:Option(option = "spigotMap", description = "The map name of Spigot")
    @get:Optional
    abstract val spigotMap: Property<String>

    @get:Input
    @get:Option(option = "mojangMap", description = "The map name of Mojang")
    @get:Optional
    abstract val mojangMap: Property<String>

    @ExperimentalPathApi
    @TaskAction
    fun run() {
        val reobf = reobfFile.asFile.get().toPath()
        val path = URLClassLoader(arrayOf(jarFile.get().asFile.toURI().toURL())).use {
            val m = it.loadClass("io.papermc.paperclip.Paperclip").getDeclaredMethod("setupEnv")
            m.isAccessible = true
            m.invoke(null) as Path
        }
        if (Files.exists(reobf)) Files.delete(reobf)
        val obcVersion = FileSystems.newFileSystem(path, null as ClassLoader?).use { fs ->
            Files.copy(fs.getPath("/META-INF/mappings/reobf.tiny"), reobf)
            Files.list(fs.getPath("/org/bukkit/craftbukkit"))
                .map { it.fileName.name }
                .filter { it.startsWith("v") }
                .findFirst().get()
        }
        logger.lifecycle("Found org.bukkit.craftbukkit: $obcVersion")
        Files.writeString(project.layout.cache.resolve("mappingVersion.txt"), obcVersion)
        val temp = project.layout.cache.resolve("temp.jar")
        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(reobf, spigotMap.get(), mojangMap.get()))
            .ignoreConflicts(true)
            .fixPackageAccess(true)
            .rebuildSourceFilenames(true)
            .renameInvalidLocals(true)
            .threads(-1)
            .build()
        try {
            OutputConsumerPath.Builder(temp).build().use {
                it.addNonClassFiles(path, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(path)
                remapper.apply(it)
            }
        } finally {
            remapper.finish()
        }
        JarRelocator(temp.toFile(), project.layout.cache.resolve("out.jar").toFile(), listOf(
            Relocation("org.bukkit.craftbukkit.$obcVersion", "org.bukkit.craftbukkit"))).run()
    }
}

abstract class ReobfTask : DefaultTask() {
    init {
        description = "Generate mapped jar file"
        group = GROUP
    }

    @get:Input
    @get:Option(option = "relocateCraftBukkit", description = "Should relocate craftbukkit packets")
    @get:Optional
    abstract val relocateCraftBukkit: Property<Boolean>

    @get:Input
    @get:Option(option = "reobfFile", description = "The file path of reobf map")
    @get:Optional
    abstract val reobfFile: RegularFileProperty

    @get:Input
    @get:Option(option = "spigotMap", description = "The map name of Spigot")
    @get:Optional
    abstract val spigotMap: Property<String>

    @get:Input
    @get:Option(option = "mojangMap", description = "The map name of Mojang")
    @get:Optional
    abstract val mojangMap: Property<String>

    @ExperimentalPathApi
    @TaskAction
    fun run() {
        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(reobfFile.asFile.get().toPath(),
                mojangMap.get(), spigotMap.get()))
            .ignoreConflicts(true)
            .fixPackageAccess(true)
            .rebuildSourceFilenames(true)
            .renameInvalidLocals(true)
            .threads(-1)
            .build()

        val needRelocate = relocateCraftBukkit.get()
        val path = lastJarTask!!.archiveFile.get().asFile.toPath()
        val noSuffix = path.fileName.toString().removeSuffix(".jar")
        val out = path.parent.resolve("$noSuffix-reobf.jar")
        val temp = if (needRelocate) path.parent.resolve("$noSuffix.tmp.jar") else out
        try {
            OutputConsumerPath.Builder(temp).build().use {
                it.addNonClassFiles(path, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(path)
                remapper.readClassPath(project.layout.cache.resolve("out.jar"))
                remapper.apply(it)
            }
        } finally {
            remapper.finish()
        }
        if (needRelocate) {
            JarRelocator(temp.toFile(), out.toFile(), listOf(Relocation("org.bukkit.craftbukkit",
                "org.bukkit.craftbukkit." + craftBukkitVersion()))).run()
            Files.delete(temp)
        }
    }
}
