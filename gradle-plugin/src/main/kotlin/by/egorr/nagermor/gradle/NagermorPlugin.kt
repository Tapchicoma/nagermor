package by.egorr.nagermor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer

open class NagermorPlugin : Plugin<Project> {
    override fun apply(
        target: Project
    ) {
        target.pluginManager.apply(JavaPlugin::class.java)

        createCompileTask(target)
    }

    private fun createCompileTask(target: Project) {
        target.extensions.configure(SourceSetContainer::class.java) {
            it.all { sourceSet ->
                target.tasks.register(
                    "compile${sourceSet.name}WithNagermor",
                    CompileTask::class.java
                ) { task ->
                    task.classpath.plus(sourceSet.compileClasspath)
                    task.javaSources.plus(sourceSet.java.sourceDirectories)
                }
            }
        }
    }
}
