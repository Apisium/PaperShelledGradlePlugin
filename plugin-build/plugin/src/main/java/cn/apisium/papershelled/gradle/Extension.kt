package cn.apisium.papershelled.gradle

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class Extension @Inject constructor(project: Project) {
    private val objects = project.objects

    val jarUrl: Property<String> = objects.property(String::class.java)
    val jarFile: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("server.jar"))
    val reobfFile: RegularFileProperty = objects.fileProperty().convention(project.layout.getCache("reobf.tiny"))
    val spigotMap: Property<String> = objects.property(String::class.java).convention("spigot")
    val mojangMap: Property<String> = objects.property(String::class.java).convention("mojang+yarn")
}
