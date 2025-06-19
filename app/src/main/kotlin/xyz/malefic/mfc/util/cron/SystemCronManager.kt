package xyz.malefic.mfc.util.cron

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.warning
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
     * Represents a Unix-like cron job entry.
     *
     * @property schedule The cron schedule expression.
     * @property command The command to execute.
     */
    data class UnixTask(
        val schedule: String,
        val command: String,
    )

    /**
     * Represents a Windows Task Scheduler entry.
     *
     * @property folder The folder containing the task.
     * @property hostName The host name where the task is registered.
     * @property taskName The name of the scheduled task.
     * @property nextRunTime The next scheduled run time.
     * @property status The current status of the task.
     * @property logonMode The logon mode for the task.
     */
    data class WindowsTask(
        val folder: String = "",
        val hostName: String = "",
        val taskName: String,
        val nextRunTime: String,
        val status: String,
        val logonMode: String,
    )

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
    fun tableTasks(): String =
        when {
            isWindows() -> {
                val terminal = Terminal()
                val tasks = executeCommand("schtasks /query /fo LIST")
                val parsedTasks = parseWindowsTasks(tasks, terminal)
                renderWindowsTasksTable(parsedTasks, terminal)
            }
            isUnix() -> {
                val terminal = Terminal()
                val tasks = executeCommand("crontab -l")
                val parsedTasks = parseUnixTasks(tasks, terminal)
                renderUnixTasksTable(parsedTasks, terminal)
            }
            else -> throw UnsupportedOperationException("Unsupported operating system: $osName")
        }

    /**
     * Parses the output of Windows Task Scheduler (`schtasks /query /fo LIST`) into a list of [WindowsTask] objects.
     *
     * @param tasks The list of output lines from the Task Scheduler command.
     * @param terminal The Mordant [Terminal] for logging warnings or errors.
     * @return A list of parsed [WindowsTask] objects.
     */
    private fun parseWindowsTasks(
        tasks: List<String>,
        terminal: Terminal,
    ): List<WindowsTask> =
        tasks
            .fold(mutableListOf<MutableList<String>>()) { acc, line ->
                if (line.isEmpty()) {
                    if (acc.lastOrNull()?.isNotEmpty() == true) acc.add(mutableListOf())
                } else {
                    acc.lastOrNull()?.add(line) ?: acc.add(mutableListOf(line))
                }
                acc
            }.map { group ->
                group
                    .mapNotNull { line ->
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                    }.toMap()
            }.mapNotNull { task ->
                try {
                    WindowsTask(
                        folder = task["Folder"] ?: "",
                        hostName = task["HostName"] ?: "",
                        taskName = task["TaskName"] ?: throw IllegalArgumentException("Missing TaskName"),
                        nextRunTime = task["Next Run Time"] ?: throw IllegalArgumentException("Missing Next Run Time"),
                        status = task["Status"] ?: throw IllegalArgumentException("Missing Status"),
                        logonMode = task["Logon Mode"] ?: throw IllegalArgumentException("Missing Logon Mode"),
                    )
                } catch (e: IllegalArgumentException) {
                    if (task["INFO"] == "There are no scheduled tasks presently available at your access level.") {
                        terminal.warning("You don't have access to the task in folder ${task["Folder"]}")
                    } else {
                        terminal.danger("Failed to map task: ${e.message} for task $task")
                    }
                    null
                }
            }

    /**
     * Renders a formatted table of Windows scheduled tasks using the Mordant terminal library.
     *
     * @param tasks The list of [WindowsTask] objects to display.
     * @param terminal The Mordant [Terminal] used for rendering.
     * @return A string containing the formatted table.
     */
    private fun renderWindowsTasksTable(
        tasks: List<WindowsTask>,
        terminal: Terminal,
    ): String =
        terminal.render(
            table {
                borderType = BorderType.Companion.ROUNDED
                borderStyle = TextColors.Companion.rgb("#4b25b9")
                align = TextAlign.RIGHT
                tableBorders = Borders.ALL
                header {
                    style = TextColors.brightRed + TextStyles.bold
                    row("Folder", "Host Name", "Task Name", "Next Run Time", "Status", "Logon Mode") {
                        cellBorders = Borders.BOTTOM
                    }
                }
                body {
                    style = TextColors.green
                    column(0) {
                        align = TextAlign.LEFT
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue
                    }
                    tasks.forEachIndexed { index, task ->
                        row(
                            task.folder,
                            task.hostName,
                            task.taskName,
                            task.nextRunTime,
                            task.status,
                            task.logonMode,
                        ) {
                            style =
                                if (index % 2 == 0) TextColors.Companion.rgb("#008080") else TextColors.Companion.rgb("#00ced1")
                        }
                    }
                }
                captionBottom("Total Tasks: ${tasks.size}")
            },
        )

    /**
     * Parses the output of the Unix `crontab -l` command into a list of [UnixTask] objects.
     *
     * @param tasks The list of crontab lines.
     * @param terminal The Mordant [Terminal] for logging errors.
     * @return A list of parsed [UnixTask] objects.
     */
    private fun parseUnixTasks(
        tasks: List<String>,
        terminal: Terminal,
    ): List<UnixTask> =
        tasks.mapNotNull { line ->
            val parts = line.split(" ", limit = 6)
            if (parts.size == 6) {
                UnixTask(
                    schedule = "${parts[0]} ${parts[1]} ${parts[2]} ${parts[3]} ${parts[4]}",
                    command = parts[5].takeUnless { parts[5].contains(" ") } ?: "\"${parts[5]}\"",
                )
            } else {
                terminal.danger("Failed to parse cron job: $line")
                null
            }
        }

    /**
     * Renders a formatted table of Unix cron jobs using the Mordant terminal library.
     *
     * @param tasks The list of [UnixTask] objects to display.
     * @param terminal The Mordant [Terminal] used for rendering.
     * @return A string containing the formatted table.
     */
    private fun renderUnixTasksTable(
        tasks: List<UnixTask>,
        terminal: Terminal,
    ): String =
        terminal.render(
            table {
                borderType = BorderType.Companion.ROUNDED
                borderStyle = TextColors.Companion.rgb("#4b25b9")
                align = TextAlign.RIGHT
                tableBorders = Borders.ALL
                header {
                    style = TextColors.brightRed + TextStyles.bold
                    row("Schedule", "Command") { cellBorders = Borders.BOTTOM }
                }
                body {
                    style = TextColors.green
                    column(0) {
                        align = TextAlign.LEFT
                        cellBorders = Borders.ALL
                        style = TextColors.brightBlue
                    }
                    tasks.forEachIndexed { index, task ->
                        row(
                            task.schedule,
                            task.command,
                        ) {
                            style = if (index % 2 == 0) TextColors.Companion.rgb("#008080") else TextColors.Companion.rgb("#00ced1")
                        }
                    }
                }
                footer {
                    style = TextStyles.bold + TextColors.brightBlue
                    row("Total Tasks: ${tasks.size}")
                }
            },
        )

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
     * @param command The command or script to schedule.
     * @param schedule Optional custom schedule in the format "every:<duration>", e.g., "every:15m".
     * @param onStartup If true, schedules the task to run at system startup.
     *
     * @throws UnsupportedOperationException if the OS is not supported.
     */
    fun addTask(
        command: String,
        schedule: String? = null,
        onStartup: Boolean = true,
    ) {
        val duration = schedule?.let { parseCustomSchedule(it) }
        if (isUnix()) {
            val commands = mutableListOf<String>()
            if (onStartup) {
                commands.add("@reboot $command")
            }
            duration?.let {
                val cronSchedule = convertDurationToCron(it)
                commands.add("$cronSchedule $command")
            }
            val cronCmd = "(crontab -l; echo \"${commands.joinToString("\n")}\") | crontab -"
            executeCommand(cronCmd)
        } else if (isWindows()) {
            val cronCmd =
                if (onStartup && duration == null) {
                    "schtasks /create /tn \"$command\" /tr \"$command\" /sc ONSTART"
                } else if (onStartup) {
                    val windowsSchedule = convertDurationToWindows(duration!!)
                    "schtasks /create /tn \"$command\" /tr \"$command\" /sc ONSTART & schtasks /create /tn \"$command\" /tr \"$command\" $windowsSchedule"
                } else {
                    val windowsSchedule = convertDurationToWindows(duration!!)
                    "schtasks /create /tn \"$command\" /tr \"$command\" $windowsSchedule"
                }
            executeCommand(cronCmd)
        } else {
            throw UnsupportedOperationException("Unsupported operating system: $osName")
        }
    }

    /**
     * Parses a custom schedule string in the format "every:<duration>".
     *
     * @param schedule The schedule string to parse.
     * @return The parsed [kotlin.time.Duration].
     * @throws IllegalArgumentException if the format is invalid.
     */
    private fun parseCustomSchedule(schedule: String): Duration {
        val parts = schedule.split(":")
        require(parts.size == 2 && parts[0] == "every") { "Invalid schedule format. Use 'every:<duration>'." }
        return Duration.Companion.parse(parts[1])
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
