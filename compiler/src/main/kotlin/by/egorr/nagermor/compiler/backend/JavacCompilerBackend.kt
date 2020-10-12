package by.egorr.nagermor.compiler.backend

import by.egorr.nagermor.compiler.CompilerBackend
import java.nio.file.Path

class JavacCompilerBackend : CompilerBackend {
    override fun compile(
        outputDir: Path,
        classPath: List<Path>,
        sourceFiles: List<Path>
    ): Int {
        val commands = mutableListOf(
            "javac"
        )

        if (classPath.isNotEmpty()) {
            commands.add("-cp")
            commands.add(
                classPath.joinToString(":") {
                    it.toAbsolutePath().toString()
                }
            )
        }

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
        return process.waitFor()
    }
}
