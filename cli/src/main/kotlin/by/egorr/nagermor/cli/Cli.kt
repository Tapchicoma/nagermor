package by.egorr.nagermor.cli

import by.egorr.nagermor.compiler.Compiler
import by.egorr.nagermor.fscaching.FileSystemChangesDetector
import by.egorr.nagermor.fscaching.hash.HashHelper
import by.egorr.nagermor.fscaching.hash.HashHelper.Companion.toHex
import by.egorr.nagermor.fscaching.hash.Sha256HashHelper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import java.nio.file.Files
import java.nio.file.Path
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

    private val outputDir by option(
        "--output", "-o",
        help = "Provide non-default output directory to put compiled sources"
    )
        .file(
            mustExist = true,
            canBeFile = false,
            canBeSymlink = false
        )

    private val sourcesDir by argument(
        name = "path_to_sources",
        help = "Provide path to sources files"
    )
        .file(
            mustExist = true,
            mustBeReadable = true,
            canBeSymlink = false
        )

    private val cacheDir = Paths.get(
        System.getProperty("user.home"),
        ".nagermor",
        "caches",
    )

    private val hashHelper: HashHelper = Sha256HashHelper()

    private val fsChangesDetector = FileSystemChangesDetector(
        cacheDir.resolve("filesystem"),
        hashHelper
    )

    private val compiler by lazy { Compiler(debug = debug) }

    override fun run() {
        debugEcho("Provided classpath: $classpath")
        debugEcho("Provided path to sources files: $sourcesDir")

        val sourcesPath = sourcesDir.toPath()
        val classPath = classpath.map { it.toPath() }
        val outputPath = outputDir?.toPath() ?: sourcesPath.defaultOutputPath()
        val incrementalCacheFile = incrementalCacheFile(sourcesPath, outputPath)

        val isClasspathChanged = fsChangesDetector.isClassPathChanged(
            sourcesPath,
            outputPath,
            classPath
        )
        debugEcho("Provided classpath is changed since previous compilation: $isClasspathChanged")

        val sourceFilesWithState = fsChangesDetector.getSourceStatus(sourcesPath, outputPath)
        debugEcho("Found following source files: $sourceFilesWithState")

        if (sourceFilesWithState.isEmpty()) {
            echo(
                message = "Could not find any java sources under $sourcesDir",
                err = true
            )
            exitProcess(1)
        }

        val result = compiler.compileSources(
            classPath = classPath.toSet(),
            isClassPathChanged = isClasspathChanged,
            outputDir = outputPath,
            incrementalCacheFile = incrementalCacheFile,
            sourceFilesWithState
        )

        if (result != 0) fsChangesDetector.clearCache(sourcesPath, outputPath)

        exitProcess(result)
    }

    private fun debugEcho(message: String) {
        if (debug) echo(message)
    }

    private fun Path.defaultOutputPath() = resolveSibling("$fileName-output").apply {
        if (!Files.exists(this)) Files.createDirectory(this)
    }

    private fun incrementalCacheFile(
        sourcesPath: Path,
        outputPath: Path
    ): Path {
        val incrementalCacheDir = cacheDir.resolve("incremental")
        if (Files.notExists(incrementalCacheDir)) Files.createDirectories(incrementalCacheDir)

        return incrementalCacheDir.resolve(
            "${hashHelper.hashString("$sourcesPath$outputPath").toHex()}.bin"
        )
    }
}
