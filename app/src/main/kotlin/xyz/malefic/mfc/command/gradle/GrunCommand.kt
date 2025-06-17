package xyz.malefic.mfc.command.gradle

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import xyz.malefic.mfc.util.SuspendingCliktCommand
import java.io.BufferedReader
import java.io.InputStreamReader

class GrunCommand : SuspendingCliktCommand("grun", "Run a Gradle task") {
    private val args: List<String> by argument(help = "Arguments to pass to the Gradle task").multiple()

    override suspend fun run() {
        val gradleCommand =
            buildString {
                append(".\\gradlew run")
                if (args.isNotEmpty()) {
                    append(" --args=\"${args.joinToString(" ")}\"")
                }
            }

        try {
            val process =
                ProcessBuilder("cmd.exe", "/c", gradleCommand)
                    .redirectErrorStream(true)
                    .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.lines().forEach { println(it) }

            process.waitFor()
        } catch (e: Exception) {
            echo("Failed to execute Gradle command: ${e.message}")
        }
    }
}
