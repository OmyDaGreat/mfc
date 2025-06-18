package xyz.malefic.mfc.util

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger

/**
 * Clears the console screen in a cross-platform manner.
 *
 * This extension function attempts to clear the terminal screen for both Windows and Unix-like systems.
 * - On Windows, it runs the `cls` command using `cmd.exe`.
 * - On Unix-like systems, it checks the `TERM` environment variable:
 *   - If the terminal type contains "xterm" or "vt", it prints the ANSI escape codes to clear the screen.
 *   - Otherwise, it runs the `clear` command.
 *
 * If clearing the console fails, an error is thrown with the exception message.
 *
 * @receiver The Mordant Terminal instance on which to perform the clear operation.
 */
fun Terminal.clearConsole() {
    try {
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            ProcessBuilder("cmd.exe", "/c", "cls").inheritIO().start().waitFor()
        } else {
            val terminal = System.getenv("TERM") ?: "unknown"
            if (terminal.contains("xterm") || terminal.contains("vt")) {
                print("\u001b[H\u001b[2J")
                System.out.flush()
            } else {
                ProcessBuilder("clear").inheritIO().start().waitFor()
            }
        }
    } catch (e: Exception) {
        danger("Failed to clear console: ${e.message}")
    }
}
