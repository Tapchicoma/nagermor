package by.egorr.nagermor.cli

import com.github.ajalt.clikt.core.CliktCommand

fun main(args: Array<String>) = Compile().main(args)

class Compile : CliktCommand() {
    override fun run() {
        echo("Test run")
    }
}
