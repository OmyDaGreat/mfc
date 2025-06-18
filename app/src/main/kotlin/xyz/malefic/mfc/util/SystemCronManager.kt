package xyz.malefic.mfc.util

import com.github.ajalt.mordant.rendering.BorderType.Companion.SQUARE_DOUBLE_SECTION_SEPARATOR
import com.github.ajalt.mordant.rendering.TextAlign.LEFT
import com.github.ajalt.mordant.rendering.TextAlign.RIGHT
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.Borders.ALL
import com.github.ajalt.mordant.table.Borders.BOTTOM
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Utility object for managing system scheduled tasks (cron jobs on Unix, Task Scheduler on Windows).
 *
 * This object detects the user's operating system and provides functions to list, add, and delete
 * scheduled tasks using the system's native scheduler.
 */
object SystemCronManager {
    private val osName: String by lazy { System.getProperty("os.name").lowercase() }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return `true` if the OS is Windows, `false` otherwise.
     */
    private fun isWindows(): Boolean = osName.contains("win")

    /**
     * Checks if the current operating system is Unix-like (Linux or Mac).
     *
     * @return `true` if the OS is Unix-like, `false` otherwise.
     */
    private fun isUnix(): Boolean = osName.contains("nix") || osName.contains("nux") || osName.contains("mac")

    /**
     * Lists the scheduled tasks for the current operating system.
     *
     * On Unix-like systems, retrieves the user's crontab entries.
     * On Windows, queries the Task Scheduler for all tasks.
     *
     * The tasks are rendered as a formatted table using the Mordant terminal library.
     *
     * @return A string containing the formatted table of scheduled tasks.
     * @throws UnsupportedOperationException if the OS is not supported.
     */
    fun tableTasks(): String {
        val terminal = Terminal()
        val tasks =
            if (isUnix()) {
                executeCommand("crontab -l")
            } else if (isWindows()) {
                executeCommand("schtasks /query /fo LIST")
            } else {
                throw UnsupportedOperationException("Unsupported operating system: $osName")
            }

        return terminal.render(
            table {
                borderType = SQUARE_DOUBLE_SECTION_SEPARATOR
                borderStyle = rgb("#4b25b9")
                align = RIGHT
                tableBorders = ALL
                header {
                    style = brightRed + bold on rgb("#f0f0f0")
                    row("Task Name", "Schedule", "Startup", "Command") { cellBorders = BOTTOM }
                }
                body {
                    style = green
                    column(0) {
                        align = LEFT
                        cellBorders = ALL
                        style = brightBlue
                    }
                    tasks.forEachIndexed { index, task ->
                        val parts = task.split("\t")
                        row(parts[0], parts[1], parts[2], parts[3]) {
                            style = if (index % 2 == 0) rgb("#e0e0e0") else rgb("#ffffff")
                        }
                    }
                }
                footer {
                    style = bold + brightBlue
                    row("Total Tasks: ${tasks.size}")
                }
            },
        )
    }

    /**
     * Lists all scheduled tasks for the current operating system.
     *
     * @return A list of strings representing the scheduled tasks.
     * @throws UnsupportedOperationException if the OS is not supported.
     */
    fun listTasks(): List<String> =
        if (isUnix()) {
            executeCommand("crontab -l")
        } else if (isWindows()) {
            executeCommand("schtasks /query /fo LIST")
        } else {
            throw UnsupportedOperationException("Unsupported operating system: $osName")
        }

    /**
     * Adds a scheduled task to the system scheduler.
     *
     * On Unix-like systems, adds a cron job to the user's crontab.
     * On Windows, creates a scheduled task using Task Scheduler.
     *
     * @param description The command or script to schedule.
     * @param schedule Optional custom schedule in the format "every:<duration>", e.g., "every:15m".
     * @param onStartup If true, schedules the task to run at system startup.
     *
     * @throws UnsupportedOperationException if the OS is not supported.
     */
    fun addTask(
        description: String,
        schedule: String? = null,
        onStartup: Boolean = true,
    ) {
        val duration = schedule?.let { parseCustomSchedule(it) }
        if (isUnix()) {
            val commands = mutableListOf<String>()
            if (onStartup) {
                commands.add("@reboot $description")
            }
            duration?.let {
                val cronSchedule = convertDurationToCron(it)
                commands.add("$cronSchedule $description")
            }
            val command = "(crontab -l; echo \"${commands.joinToString("\n")}\") | crontab -"
            executeCommand(command)
        } else if (isWindows()) {
            val command =
                if (onStartup && duration == null) {
                    "schtasks /create /tn \"$description\" /tr \"$description\" /sc ONSTART"
                } else if (onStartup) {
                    val windowsSchedule = convertDurationToWindows(duration!!)
                    "schtasks /create /tn \"$description\" /tr \"$description\" /sc ONSTART & schtasks /create /tn \"$description\" /tr \"$description\" $windowsSchedule"
                } else {
                    val windowsSchedule = convertDurationToWindows(duration!!)
                    "schtasks /create /tn \"$description\" /tr \"$description\" $windowsSchedule"
                }
            executeCommand(command)
        } else {
            throw UnsupportedOperationException("Unsupported operating system: $osName")
        }
    }

    /**
     * Parses a custom schedule string in the format "every:<duration>".
     *
     * @param schedule The schedule string to parse.
     * @return The parsed [Duration].
     * @throws IllegalArgumentException if the format is invalid.
     */
    private fun parseCustomSchedule(schedule: String): Duration {
        val parts = schedule.split(":")
        require(parts.size == 2 && parts[0] == "every") { "Invalid schedule format. Use 'every:<duration>'." }
        return Duration.parse(parts[1])
    }

    /**
     * Converts a [Duration] to a cron schedule string.
     *
     * @param duration The duration to convert.
     * @return A cron schedule string representing the interval.
     */
    private fun convertDurationToCron(duration: Duration): String {
        val minutes = duration.toInt(DurationUnit.MINUTES)
        return "*/$minutes * * * *"
    }

    /**
     * Converts a [Duration] to a Windows Task Scheduler schedule argument.
     *
     * @param duration The duration to convert.
     * @return A string representing the schedule for Windows Task Scheduler.
     */
    private fun convertDurationToWindows(duration: Duration): String {
        val minutes = duration.toInt(DurationUnit.MINUTES)
        return "/sc MINUTE /mo $minutes"
    }

    /**
     * Deletes a scheduled task for the current operating system.
     *
     * On Unix, removes lines containing the description from the user's crontab.
     * On Windows, deletes the scheduled task by name.
     *
     * @param description Description or name of the task to delete.
     * @throws UnsupportedOperationException if the OS is not supported.
     */
    fun deleteTask(description: String) {
        if (isUnix()) {
            val tasks = listTasks().filterNot { it.contains(description) }
            val command = "echo \"${tasks.joinToString("\n")}\" | crontab -"
            executeCommand(command)
        } else if (isWindows()) {
            val command = "schtasks /delete /tn \"$description\" /f"
            executeCommand(command)
        } else {
            throw UnsupportedOperationException("Unsupported operating system: $osName")
        }
    }

    /**
     * Executes a system command and returns the output as a list of strings.
     *
     * @param command The command to execute.
     * @return The output lines from the command.
     */
    private fun executeCommand(command: String): List<String> {
        val process =
            ProcessBuilder(*command.split(" ").toTypedArray())
                .redirectErrorStream(true)
                .start()
        return BufferedReader(InputStreamReader(process.inputStream)).readLines()
    }
}
