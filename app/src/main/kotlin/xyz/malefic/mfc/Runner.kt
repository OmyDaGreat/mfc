package xyz.malefic.mfc

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import xyz.malefic.mfc.command.gradle.GrunCommand
import xyz.malefic.mfc.command.rss.AddCommand
import xyz.malefic.mfc.command.rss.DeleteCommand
import xyz.malefic.mfc.command.rss.FetchCommand
import xyz.malefic.mfc.command.rss.RssCommand
import xyz.malefic.mfc.util.CliktCommand

/**
 * The main function to run the application.
 * @param args The command-line arguments.
 */
fun main(args: Array<String>) =
    Runner()
        .context {
            terminal = Terminal(ansiLevel = AnsiLevel.TRUECOLOR, interactive = true)
        }.subcommands(GrunCommand(), RssCommand().subcommands(FetchCommand(), AddCommand(), DeleteCommand()))
        .main(args)

class Runner : CliktCommand() {
    override fun run() = Unit
}
