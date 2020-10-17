package by.egorr.nagermor.compiler

import by.egorr.nagermor.abi.AbiReader
import by.egorr.nagermor.abi.JavaAbiReader
import by.egorr.nagermor.compiler.backend.JavacCompilerBackend
import by.egorr.nagermor.compiler.incremental.IncrementalCompilationSolver
import by.egorr.nagermor.compiler.incremental.getJavaSourceFileTopElementName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Performs actual incremental compilation of passed source files.
 *
 * @param debug enable debug logging
 */
class Compiler(
    private val compilerBackend: CompilerBackend = JavacCompilerBackend(),
    private val abiReader: AbiReader = JavaAbiReader(),
    private val outputFileExtension: String = "class",
    private val debug: Boolean = false
) {
    /**
     * Incrementally compile passed sources.
     */
    fun compileSources(
        classPath: Set<Path>,
        isClassPathChanged: Boolean,
        outputDir: Path,
        incrementalCacheFile: Path,
        sourceFilesWithState: Map<Path, SourceFileState>
    ): Int {
        val incrementalCompilationSolver = IncrementalCompilationSolver(
            incrementalCacheFile = incrementalCacheFile,
            outputFileExtension = outputFileExtension,
            abiReader = abiReader
        )

        return when {
            isClassPathChanged -> doFullRecompilation(
                outputDir,
                classPath,
                sourceFilesWithState,
                incrementalCompilationSolver
            )
            sourceFilesWithState
                    .values
                    .filterNot { it == SourceFileState.NOT_CHANGED }
                    .isEmpty() -> return 0
            else -> compileIncrementally(
                sourceFilesWithState,
                incrementalCompilationSolver,
                outputDir,
                classPath
            )
        }
    }

    private fun doFullRecompilation(
        outputDir: Path,
        classPath: Set<Path>,
        sourceFilesWithState: Map<Path, SourceFileState>,
        incrementalCompilationSolver: IncrementalCompilationSolver
    ): Int {
        outputDir.deleteDirectoryWithContent()
        Files.createDirectories(outputDir)

        return compilerBackend.compile(
            outputDir,
            classPath,
            emptySet(),
            sourceFilesWithState.keys.toSet()
        ).also {
            if (it == 0) {
                incrementalCompilationSolver.resetInternalState()
                incrementalCompilationSolver.updateSourceFilesAbi(outputDir, sourceFilesWithState.keys)
                incrementalCompilationSolver.saveState()
            }
        }
    }

    private fun compileIncrementally(
        sourceFilesWithState: Map<Path, SourceFileState>,
        incrementalCompilationSolver: IncrementalCompilationSolver,
        outputDir: Path,
        classPath: Set<Path>
    ): Int {
        val sourceFilesToRecompile = sourceFilesWithState
            .filterNot { it.value == SourceFileState.NOT_CHANGED }
            .flatMap {
                when (it.value) {
                    SourceFileState.ADDED -> setOf(it.key)
                    SourceFileState.REMOVED -> incrementalCompilationSolver.sourceFileChanged(it.key)
                    SourceFileState.CHANGED -> incrementalCompilationSolver.sourceFileChanged(it.key) + setOf(it.key)
                    SourceFileState.NOT_CHANGED -> emptySet()
                }
            }
            .toSet()

        if (debug) println("Recompiling following source files: $sourceFilesToRecompile")

        val compiledClasses = sourceFilesWithState
            .filterNot { it.value == SourceFileState.REMOVED }
            .keys
            .subtract(sourceFilesToRecompile)
            .map {
                it.getJavaSourceFileTopElementName { packageName, className ->
                    if (packageName != null) {
                        val packageDir = packageName.replace('.', File.separatorChar)
                        "$packageDir${File.separator}$className.$outputFileExtension"
                    } else {
                        "$className.$outputFileExtension"
                    }
                }
            }
            .map { outputDir.resolve(it) }
            .toSet()
        if (debug) println("Adding following compiled classes to classpath: $compiledClasses")

        return compilerBackend.compile(
            outputDir,
            classPath,
            compiledClasses,
            sourceFilesToRecompile.toSet()
        ).also { exitCode ->
            if (exitCode == 0) {
                val removedSourceFiles = sourceFilesWithState.filter { it.value == SourceFileState.REMOVED }.keys
                incrementalCompilationSolver.removeSourceFiles(
                    outputDir,
                    removedSourceFiles
                )
                incrementalCompilationSolver.updateSourceFilesAbi(
                    outputDir,
                    sourceFilesToRecompile.subtract(removedSourceFiles)
                )
                incrementalCompilationSolver.saveState()
            }
        }
    }

    enum class SourceFileState {
        ADDED, REMOVED, CHANGED, NOT_CHANGED;
    }
}
