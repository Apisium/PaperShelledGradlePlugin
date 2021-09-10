package cn.apisium.papershelled.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class Extension @Inject constructor(project: Project) {
    private val objects = project.objects

    val jarUrl: Property<String> = objects.property(String::class.java)
    val jarFile: Property<String> = objects.property(String::class.java)
}