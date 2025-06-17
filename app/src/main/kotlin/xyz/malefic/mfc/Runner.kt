package xyz.malefic.mfc

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.AnsiLevel.TRUECOLOR
import com.github.ajalt.mordant.terminal.Terminal
import xyz.malefic.mfc.command.rss.RssCommand

/**
 * The main function to run the application.
 * @param args The command-line arguments.
 */
fun main(args: Array<String>) =
    Runner()
        .context {
            terminal = Terminal(TRUECOLOR, interactive = true)
        }.subcommands(RssCommand())
        .main(args)

class Runner : CliktCommand() {
    override val printHelpOnEmptyArgs = true

    override fun run() = Unit
}
