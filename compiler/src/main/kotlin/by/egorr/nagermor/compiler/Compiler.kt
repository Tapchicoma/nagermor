package by.egorr.nagermor.compiler

import by.egorr.nagermor.compiler.backend.JavacCompilerBackend
import java.nio.file.Files
import java.nio.file.Path

/**
 * Performs actual incremental compilation of passed source files.
 *
 * @param debug enable debug logging
 */
class Compiler(
    private val compilerBackend: CompilerBackend = JavacCompilerBackend(),
    private val debug: Boolean = false
) {
    /**
     * Incrementally compile passed sources.
     */
    fun compileSources(
        classPath: List<Path>,
        isClassPathChanged: Boolean,
        sourceDir: Path,
        sourceFilesWithState: Map<Path, SourceFileState>
    ): Int {
        if (isClassPathChanged) {
            if (debug) println("Classpath was changed, doing full recompile for $sourceFilesWithState")
            return compilerBackend.compile(
                sourceDir.outputDir(),
                classPath,
                sourceFilesWithState.keys.toList()
            )
        }

        if (sourceFilesWithState
                .values
                .filterNot { it == SourceFileState.NOT_CHANGED }
                .isEmpty()
        ) {
            return 0
        }

        return 1
    }

    private fun Path.outputDir() = resolveSibling("$fileName-output").apply {
        if (!Files.exists(this)) Files.createDirectory(this)
    }

    enum class SourceFileState {
        ADDED, REMOVED, CHANGED, NOT_CHANGED;
    }
}
