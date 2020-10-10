package by.egorr.nagermor.cli

import by.egorr.nagermor.fscaching.FileSystemChangesDetector
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import java.nio.file.Paths

fun main(args: Array<String>) = Compile().main(args)

private class Compile : CliktCommand() {
    private val classpath by option(
        "--classpath", "-cp",
        help = """
            Specify paths whether to find user class files.
            Paths should be separated by ':' character. 
        """.trimMargin()
    )
        .split(":")
        .default(emptyList())

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

    override fun run() {
        echo("Provided classpath: $classpath")
        echo("Provided path to sources files: $sourcesDir")

        val sourcesPath = sourcesDir.toPath()

        val isClasspathChanged = fsChangesDetector.isClassPathChanged(
            sourcesPath,
            classpath
        )
        echo("Provided classpath is changed since previous compilation: $isClasspathChanged")
        val sourceFilesWithState = fsChangesDetector.getSourceStatus(sourcesPath)
        echo("Found following source files: $sourceFilesWithState")
    }
}
