package cn.apisium.papershelled.gradle

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import java.nio.file.Files
import javax.inject.Inject

abstract class Extension @Inject constructor(private val project: Project) {
    private val objects = project.objects

    val jarUrl: Property<String> = objects.property(String::class.java)
    val paperShelledVersion: Property<String> = objects.property(String::class.java).convention("1.0.0")
    val annotationsVersion: Property<String> = objects.property(String::class.java).convention("0.0.0")
    val jarFile: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("server.jar"))
    val reobfFile: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("reobf.tiny"))
    val paperShelledJar: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("out.jar"))
    val paperShelledLib: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("libs"))
    val relocateCraftBukkit: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val reobfAfterJarTask: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val generateReferenceMap: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val addJarToDependencies: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val archiveClassifier: Property<String> = objects.property(String::class.java).convention("-reobf")
    val referenceMapName: Property<String> = objects.property(String::class.java).convention(project.name + ".refmap.json")
    val mixinVersion: Property<String> = objects.property(String::class.java).convention("0.8.5")
    val craftBukkitVersion: Property<String> = objects.property(String::class.java)
        .convention(
            project.provider {
                try {
                    Files.readString(project.layout.cache.resolve("mappingVersion.txt"))
                } catch (ignored: Throwable) {
                    ""
                }
            }
        )

    @Suppress("unused")
    fun jar(): ConfigurableFileCollection = project.files(paperShelledJar.get().asFile)
    fun lib(): ConfigurableFileTree = project.fileTree(paperShelledLib.get().asFile)
}
