package by.egorr.nagermor.cli

import by.egorr.nagermor.compiler.Compiler
import by.egorr.nagermor.fscaching.FileSystemChangesDetector
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) = Compile().main(args)

private class Compile : CliktCommand() {
    private val classpath by option(
        "--classpath", "-cp",
        help = """
            Specify paths whether to find user class files.
            Paths should be separated by ':' character. 
        """.trimMargin()
    )
        .file(
            mustExist = true,
            mustBeReadable = true,
            canBeSymlink = false
        )
        .split(":")
        .default(emptyList())

    private val debug by option(
        "--debug",
        help = "Enable additional debug output."
    )
        .flag()

    private val sourcesDir by argument(
        name = "path_to_sources",
        help = "Provide path to sources files"
    )
        .file(
            mustExist = true,
            mustBeReadable = true,
            canBeSymlink = false
        )

    private val fsChangesDetector = FileSystemChangesDetector(
        Paths.get(
            System.getProperty("user.home"),
            ".nagermor",
            "caches",
            "filesystem"
        )
    )

    private val compiler by lazy { Compiler(debug = debug) }

    override fun run() {
        debugEcho("Provided classpath: $classpath")
        debugEcho("Provided path to sources files: $sourcesDir")

        val sourcesPath = sourcesDir.toPath()

        val classPath = classpath.map { it.toPath() }
        val isClasspathChanged = fsChangesDetector.isClassPathChanged(
            sourcesPath,
            classPath
        )
        debugEcho("Provided classpath is changed since previous compilation: $isClasspathChanged")

        val sourceFilesWithState = fsChangesDetector.getSourceStatus(sourcesPath)
        debugEcho("Found following source files: $sourceFilesWithState")

        if (sourceFilesWithState.isEmpty()) {
            echo(
                message = "Could not find any java sources under $sourcesDir",
                err = true
            )
            exitProcess(1)
        }

        val result = compiler.compileSources(
            classPath = classPath,
            isClassPathChanged = isClasspathChanged,
            sourceDir = sourcesPath,
            sourceFilesWithState
        )

        if (result != 0) fsChangesDetector.clearCache(sourcesPath)

        exitProcess(result)
    }

    private fun debugEcho(message: String) {
        if (debug) echo(message)
    }
}
