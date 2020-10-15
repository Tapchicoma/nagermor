package by.egorr.nagermor.compiler.incremental

import by.egorr.nagermor.abi.AbiReader
import by.egorr.nagermor.abi.JavaAbiReader
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class IncrementalCompilationSolver(
    private val incrementalCacheFile: Path,
    private val abiReader: AbiReader = JavaAbiReader(),
) {
    private val graph = AbiDependencyGraph.deserialize(incrementalCacheFile)

    fun sourceFileRemoved(
        outputDir: Path,
        sourceFile: Path
    ): Set<Path> = emptySet()

    fun sourceFileChanged(
        outputDir: Path,
        sourceFile: Path
    ): Set<Path> = emptySet()

    fun updateSourceFilesAbi(
        outputDir: Path,
        sourceFiles: Set<Path>
    ) {

    }

    fun removeSourceFiles(
        outputDir: Path,
        sourceFiles: Set<Path>
    ) {

    }

    /**
     * Resets incremental cache state.
     */
    fun resetInternalState() {
        Files.write(
            incrementalCacheFile,
            ByteArray(0),
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }
}
