package by.egorr.nagermor.compiler.backend

import by.egorr.nagermor.compiler.CompilerBackend
import by.egorr.nagermor.compiler.deleteDirectoryWithContent
import java.nio.file.Files
import java.nio.file.Path

class JavacCompilerBackend : CompilerBackend {
    override fun compile(
        outputDir: Path,
        classPath: Set<Path>,
        compiledClassFiles: Set<Path>,
        sourceFiles: Set<Path>
    ): Int {
        val commands = mutableListOf(
            "javac"
        )

        val tempOutputDir = outputDir.resolve("tmp")

        commands.addAll(prepareClassPathCommandOption(
            classPath,
            compiledClassFiles,
            tempOutputDir,
            outputDir
        ))
        commands.add("-d")
        commands.add(outputDir.toAbsolutePath().toString())

        sourceFiles.forEach {
            commands.add(it.toAbsolutePath().toString())
        }

        val processBuilder = ProcessBuilder()
            .command(commands)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = processBuilder.start()
        return try {
            process.waitFor()
        } finally {
            tempOutputDir.deleteDirectoryWithContent()
        }
    }

    private fun prepareClassPathCommandOption(
        classPath: Set<Path>,
        compiledClassFiles: Set<Path>,
        tempOutputDir: Path,
        outputDir: Path
    ): List<String> {
        val additionalClassPath = preparePreCompiledClasses(
            compiledClassFiles,
            tempOutputDir,
            outputDir
        )

        return if (classPath.isNotEmpty() || additionalClassPath != null) {
            val mergedClassPath = if (additionalClassPath != null) classPath + setOf(additionalClassPath) else classPath
            listOf(
                "-cp",
                mergedClassPath.joinToString(":") {
                    it.toAbsolutePath().toString()
                }
            )
        } else {
            emptyList()
        }
    }

    private fun preparePreCompiledClasses(
        compiledClassFiles: Set<Path>,
        tempOutputDir: Path,
        outputDir: Path
    ): Path? = if (compiledClassFiles.isNotEmpty()) {
        Files.createDirectory(tempOutputDir)
        compiledClassFiles.forEach {
            val tempFileOutputDir = tempOutputDir.resolve(outputDir.relativize(it))
            if (Files.notExists(tempFileOutputDir.parent)) {
                Files.createDirectories(tempFileOutputDir.parent)
            }
            Files.copy(it, tempFileOutputDir)
        }
        tempOutputDir
    } else {
        null
    }
}
