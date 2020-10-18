package by.egorr.nagermor.gradle

import by.egorr.nagermor.compiler.Compiler
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges

@Suppress("UnstableApiUsage")
open class CompileTask(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : SourceTask() {
    @get:Classpath
    internal val classpath: FileCollection = objectFactory.fileCollection()

    @get:InputFiles
    @get:SkipWhenEmpty
    internal val javaSources: FileCollection = objectFactory.fileCollection()

    @get:OutputDirectory
    val outputDir: DirectoryProperty = objectFactory.directoryProperty().convention(
        projectLayout.buildDirectory.dir("$BUILD_OUTPUT_DIR/out")
    )

    @get:OutputFile
    internal val incrementalCacheFile: RegularFileProperty = objectFactory.fileProperty().convention(
        projectLayout.buildDirectory.file("$BUILD_OUTPUT_DIR/cache/incrementalCompilationCache.bin")
    )

    @TaskAction
    fun compileSources(inputChanges: InputChanges) {
        val compiler = Compiler()

        val filesToCompile = inputChanges
            .getFileChanges(javaSources)
            .filter { it.fileType == FileType.FILE }
            .associate {
                it.file.toPath() to it.changeType.toCompilerSourceFileState()
            }

        val nonChangedSources = javaSources
            .files
            .map { it.toPath() }
            .subtract(filesToCompile.keys)
            .associateWith { Compiler.SourceFileState.NOT_CHANGED }

        compiler.compileSources(
            classpath.map { it.toPath() }.toSet(),
            inputChanges.isIncremental.not(),
            outputDir.get().asFile.toPath(),
            incrementalCacheFile.get().asFile.toPath(),
            filesToCompile + nonChangedSources
        )
    }

    private fun ChangeType.toCompilerSourceFileState(): Compiler.SourceFileState =
        when (this) {
            ChangeType.ADDED -> Compiler.SourceFileState.ADDED
            ChangeType.MODIFIED -> Compiler.SourceFileState.CHANGED
            ChangeType.REMOVED -> Compiler.SourceFileState.REMOVED
        }

    companion object {
        const val BUILD_OUTPUT_DIR = "nagermor"
    }
}
