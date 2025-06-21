package xyz.malefic.mfc.util

import com.github.ajalt.clikt.core.parse
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.success
import com.github.ajalt.mordant.terminal.warning

/**
 * Provides an interactive mode for a base command, allowing users to execute subcommands interactively.
 *
 * @receiver Terminal The terminal instance used for input/output operations.
 * @param cliktCommand The base CliktCommand for which the interactive mode is being implemented.
 * @param subcommands A map of subcommand names to their corresponding CliktCommand instances.
 */
fun Terminal.interactiveBaseCommand(
    cliktCommand: CliktCommand,
    subcommands: Map<String, CliktCommand>,
) = with(cliktCommand) {
    if (currentContext.invokedSubcommands.isNotEmpty() || checkNonInteractive()) return

    info("Entering interactive $name mode. Type commands like ${subcommands.keys.joinToString(", ")}, or 'q' to exit.")

    while (true) {
        try {
            print(theme.info("Enter a command: "))
            val input = readLine()?.trim() ?: continue

            if (input.lowercase() == "q" || input.lowercase() == "exit") {
                success("Exiting interactive $name mode.")
                break
            }

            val parts = input.split(" ")
            val command = parts.firstOrNull()
            val args = parts.drop(1)

            fun CliktCommand.checkHelp() =
                when {
                    args.contains("--help") -> info(help)
                    else -> parse(args)
                }

            when (command) {
                "help" -> info(help)
                in subcommands.keys -> subcommands[command]?.checkHelp()
                else -> danger("Unknown command: $command")
            }
        } catch (e: Exception) {
            warning("Something didn't quite work as expected: ${e.message}")
        }
    }
}
