package by.egorr.nagermor.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split

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

    override fun run() {
        echo("Test run")
        echo("Provided classpath: $classpath")
    }
}
