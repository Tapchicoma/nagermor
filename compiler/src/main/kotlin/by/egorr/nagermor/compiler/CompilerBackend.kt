package by.egorr.nagermor.compiler

import java.nio.file.Path

/**
 * Implementations should invoke actual programming language compiler.
 */
interface CompilerBackend {
    /**
     * Perform [sourceFiles] compilation into [outputDir] using given [classPath] and optionally [compiledClassFiles].
     *
     * @return compiler exit code. `0` means compilation finished successfully.
     */
    fun compile(
        outputDir: Path,
        classPath: Set<Path>,
        compiledClassFiles: Set<Path>,
        sourceFiles: Set<Path>
    ): Int
}
