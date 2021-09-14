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
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name

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
    abstract val jarFile: RegularFileProperty

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
            it.target.set(jarFile)
        }
    }
}

private val LEFT = arrayOf("named", "mojang+yarn")
private val RIGHT = arrayOf("intermediary", "spigot")

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
    @get:Option(option = "paperShelledJar", description = "The file path of out jar")
    @get:Optional
    abstract val paperShelledJar: RegularFileProperty

    @ExperimentalPathApi
    @TaskAction
    fun run() {
        Files.createDirectories(project.layout.tmp)
        Files.createDirectories(project.layout.cache)
        val reobf = reobfFile.get().asFile.toPath()
        val path = URLClassLoader(arrayOf(jarFile.get().asFile.toURI().toURL())).use {
            val m = it.loadClass("io.papermc.paperclip.Paperclip").getDeclaredMethod("setupEnv")
            m.isAccessible = true
            m.invoke(null) as Path
        }
        if (Files.exists(reobf)) Files.delete(reobf)
        val obcVersion = FileSystems.newFileSystem(path, null as ClassLoader?).use { fs ->
            val data = Files.readAllBytes(fs.getPath("/META-INF/mappings/reobf.tiny")).toString(StandardCharsets.UTF_8)
            Files.write(reobf, data.replaceFirst(LEFT[1], LEFT[0]).replaceFirst(RIGHT[1], RIGHT[0])
                .toByteArray(StandardCharsets.UTF_8))
            Files.list(fs.getPath("/org/bukkit/craftbukkit"))
                .map { it.fileName.name }
                .filter { it.startsWith("v") }
                .findFirst().get()
        }
        logger.lifecycle("Found org.bukkit.craftbukkit: $obcVersion")
        Files.write(project.layout.cache.resolve("mappingVersion.txt"), obcVersion.toByteArray(StandardCharsets.UTF_8))
        val temp = project.layout.tmp.resolve("temp.jar")
        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(reobf as Path, RIGHT[0], LEFT[0]))
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
        JarRelocator(temp.toFile(), paperShelledJar.get().asFile, listOf(
            Relocation("org.bukkit.craftbukkit.$obcVersion", "org.bukkit.craftbukkit"))).run()
        Files.delete(temp)
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
    @get:Option(option = "archiveClassifier", description = "The classifier of output jar")
    @get:Optional
    abstract val archiveClassifier: Property<String>

    @get:Input
    @get:Option(option = "craftBukkitVersion", description = "The version of craft bukkit")
    @get:Optional
    abstract val craftBukkitVersion: Property<String>

    @get:Input
    @get:Option(option = "paperShelledJar", description = "The file path of out jar")
    @get:Optional
    abstract val paperShelledJar: RegularFileProperty

    @ExperimentalPathApi
    @TaskAction
    fun run() {
        Files.createDirectories(project.layout.tmp)
        Files.createDirectories(project.layout.cache)
        val remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(reobfFile.get().asFile.toPath(), LEFT[0], RIGHT[0]))
            .ignoreConflicts(true)
            .fixPackageAccess(true)
            .rebuildSourceFilenames(true)
            .renameInvalidLocals(true)
            .threads(-1)
            .build()

        val needRelocate = relocateCraftBukkit.get()
        val path = lastJarTask!!.archiveFile.get().asFile.toPath()
        val noSuffix = path.fileName.toString().removeSuffix(".jar")
        val temp = project.layout.tmp.resolve(noSuffix + archiveClassifier.get() + ".temp1.jar")
        val out = path.parent.resolve(noSuffix + archiveClassifier.get() + ".jar")
        try {
            OutputConsumerPath.Builder(temp).build().use {
                it.addNonClassFiles(path, NonClassCopyMode.FIX_META_INF, remapper)
                remapper.readInputs(path)
                remapper.readClassPath(paperShelledJar.get().asFile.toPath())
                remapper.apply(it)
            }
        } finally {
            remapper.finish()
        }
        val tempOut = if (needRelocate) {
            val tmp = project.layout.tmp.resolve(noSuffix + archiveClassifier.get() + ".temp2.jar")
            JarRelocator(temp.toFile(), tmp.toFile(), listOf(Relocation("org.bukkit.craftbukkit",
                "org.bukkit.craftbukkit." + craftBukkitVersion.get()))).run()
            Files.delete(temp)
            tmp
        } else temp
        if (Files.exists(out)) Files.delete(out)
        Files.move(tempOut, out)
    }
}
