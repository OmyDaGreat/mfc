package xyz.malefic.mfc

import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import xyz.malefic.mfc.command.cron.CronCommand
import xyz.malefic.mfc.command.rss.RssCommand
import xyz.malefic.mfc.command.single.GrunCommand
import xyz.malefic.mfc.command.todo.TodoCommand
import xyz.malefic.mfc.util.CliktCommand

/**
 * The main function to run the application.
 * @param args The command-line arguments.
 */
fun main(args: Array<String>) =
    Runner()
        .context {
            terminal = Terminal(ansiLevel = AnsiLevel.TRUECOLOR, interactive = true)
        }.subcommands(GrunCommand(), RssCommand(), CronCommand(), TodoCommand())
        .main(args)

class Runner : CliktCommand() {
    override fun run() = Unit
}
