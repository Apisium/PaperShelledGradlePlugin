package cn.apisium.papershelled.gradle

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import java.nio.file.Files
import javax.inject.Inject

abstract class Extension @Inject constructor(private val project: Project) {
    private val objects = project.objects

    val jarUrl: Property<String> = objects.property(String::class.java)
    val jarFile: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("server.jar"))
    val reobfFile: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("reobf.tiny"))
    val spigotMap: Property<String> = objects.property(String::class.java).convention("spigot")
    val mojangMap: Property<String> = objects.property(String::class.java).convention("mojang+yarn")
    val relocateCraftBukkit: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val paperShelledJar: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("out.jar"))
    val craftBukkitVersion: Property<String> = objects.property(String::class.java)
        .convention(project.provider {
            try {
                Files.readString(project.layout.cache.resolve("mappingVersion.txt"))
            } catch (ignored: Throwable) {
                ""
            }
        })

    @Suppress("unused")
    fun jar(): ConfigurableFileCollection = project.files(paperShelledJar.get().asFile)
}
