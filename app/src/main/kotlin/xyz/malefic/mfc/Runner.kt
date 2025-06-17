package xyz.malefic.mfc

import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.AnsiLevel.TRUECOLOR
import com.github.ajalt.mordant.terminal.Terminal
import xyz.malefic.mfc.command.gradle.GrunCommand
import xyz.malefic.mfc.command.rss.AddCommand
import xyz.malefic.mfc.command.rss.DeleteCommand
import xyz.malefic.mfc.command.rss.FetchCommand
import xyz.malefic.mfc.command.rss.RssCommand
import xyz.malefic.mfc.util.SuspendingCliktCommand

/**
 * The main function to run the application.
 * @param args The command-line arguments.
 */
suspend fun main(args: Array<String>) =
    Runner()
        .context {
            terminal = Terminal(TRUECOLOR, interactive = true)
        }.subcommands(GrunCommand(), RssCommand().subcommands(FetchCommand(), AddCommand(), DeleteCommand()))
        .main(args)

class Runner : SuspendingCliktCommand() {
    override suspend fun run() = Unit
}
