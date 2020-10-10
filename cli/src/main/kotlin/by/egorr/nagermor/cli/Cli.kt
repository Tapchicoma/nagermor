package by.egorr.nagermor.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file

fun main(args: Array<String>) = Compile().main(args)

class Compile : CliktCommand() {
    private val classpath by option(
        "--classpath", "-cp",
        help = """
            Specify paths whether to find user class files.
            Paths should be separated by ':' character. 
        """.trimMargin()
    )
        .split(":")
        .default(emptyList())

    private val sourcesPath by argument(
        name = "path_to_sources",
        help = "Provide path to sources files"
    )
        .file(
            mustExist = true,
            mustBeReadable = true,
            canBeSymlink = false
        )

    override fun run() {
        echo("Test run")
        echo("Provided classpath: $classpath")
        echo("Provided path to sources files: $sourcesPath")
    }
}
