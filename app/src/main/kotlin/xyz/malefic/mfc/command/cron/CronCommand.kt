package xyz.malefic.mfc.command.cron

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.muted
import com.github.ajalt.mordant.terminal.success
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.SystemCronManager
import xyz.malefic.mfc.util.betterPrompt

class CronCommand : CliktCommand("cron", "Manage scheduled tasks") {
    init {
        subcommands(ListCronCommand(), AddCronCommand(), DeleteTaskCommand())
    }

    override fun run() = Unit
}

class ListCronCommand : CliktCommand("list", "List all scheduled tasks") {
    private val terminal = Terminal()

    override fun run() =
        with(terminal) {
            try {
                val tasksTable = SystemCronManager.tableTasks()
                if (tasksTable.isEmpty()) {
                    muted("No scheduled tasks found.")
                } else {
                    success("Scheduled tasks:")
                    println(tasksTable)
                }
            } catch (e: Exception) {
                danger("Failed to retrieve tasks: ${e.message}")
            }
        }
}

class AddCronCommand : CliktCommand("add", "Add a scheduled task") {
    private val terminal = Terminal()
    private val command: String by argument(help = "The task to run")
    private val schedule: String? by option(help = "Schedule for the task with every:duration (e.g., 'every:5m' for every 5 minutes)")
    private val onStartup: Boolean by option("--on-startup", help = "Run the task at system startup").flag("--until-off", default = false)

    override fun run() =
        with(terminal) {
            try {
                SystemCronManager.addTask(command, schedule, onStartup)
                when {
                    onStartup && schedule == null -> success("Added startup-only task: $command")
                    onStartup -> success("Added startup task: $command with schedule: $schedule")
                    else -> success("Added task: $command with schedule: $schedule")
                }
            } catch (e: Exception) {
                danger("Failed to add task: ${e.message}")
            }
        }
}

class DeleteTaskCommand : CliktCommand("delete", "Delete a scheduled task") {
    private val terminal = Terminal()

    override fun run() =
        with(terminal) {
            val tasks = SystemCronManager.listTasks()
            if (tasks.isEmpty()) {
                muted("No tasks found.")
                return
            }

            val response = betterPrompt("Select a task to delete:", tasks, enableNoneOption = true)

            if (response == null) {
                muted("No valid selection made.")
                return
            }

            try {
                SystemCronManager.deleteTask(response)
                success("Deleted task: $response")
            } catch (e: Exception) {
                danger("Failed to delete task: ${e.message}")
            }
        }
}
