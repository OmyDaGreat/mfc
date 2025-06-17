package xyz.malefic.mfc.util

import com.github.ajalt.clikt.core.BaseCliktCommand

fun <T : BaseCliktCommand<T>> BaseCliktCommand<T>.clearConsole() {
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
        echo("Failed to clear console: ${e.message}")
    }
}
