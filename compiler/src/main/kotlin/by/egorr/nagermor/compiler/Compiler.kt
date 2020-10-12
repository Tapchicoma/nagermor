package by.egorr.nagermor.compiler

import by.egorr.nagermor.compiler.backend.JavacCompilerBackend
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
    ) {

    }

    enum class SourceFileState {
        ADDED, REMOVED, CHANGED, NOT_CHANGED;
    }
}
