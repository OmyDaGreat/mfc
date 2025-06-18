package xyz.malefic.mfc.command.cron

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.muted
import com.github.ajalt.mordant.terminal.success
import xyz.malefic.compose.prefs.collection.PersistentHashSet
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.betterPrompt
import xyz.malefic.mfc.util.prefs
import java.io.Serializable

class CronCommand : CliktCommand("cron", "Manage scheduled tasks") {
    private val terminal = Terminal()
    override val invokeWithoutSubcommand = true

    init {
        subcommands(AddCronCommand(), DeleteTaskCommand())
    }

    override fun run() {
        if (tasks.isEmpty()) {
            terminal.muted("No tasks found.")
            return
        }

        terminal.success("Scheduled tasks:")
        tasks.forEach { task ->
            terminal.println("Description: ${task.description}, Schedule: ${task.schedule}")
        }
    }
}

class AddCronCommand : CliktCommand("add", "Add a scheduled task") {
    private val terminal = Terminal()
    private val description: String by argument(help = "Description of the task")
    private val schedule: String by argument(
        help = "Schedule for the task in cron format (e.g., '*/5 * * * *' for every 5 minutes)",
    )

    override fun run() {
        try {
            tasks.add(Task(description, schedule))
            terminal.success("Added task: $description with schedule: $schedule")
        } catch (e: Exception) {
            terminal.danger("Failed to add task: ${e.message}")
        }
    }
}

class DeleteTaskCommand : CliktCommand("delete", "Delete a scheduled task") {
    private val terminal = Terminal()

    override fun run() {
        if (tasks.isEmpty()) {
            terminal.muted("No tasks found.")
            return
        }

        val response =
            terminal.betterPrompt(
                "Select a task to delete:",
                tasks.map { it.description },
                enableNoneOption = true,
            )

        if (response == null) {
            terminal.muted("No valid selection made.")
            return
        }

        val taskToDelete = tasks.find { it.description == response }
        if (taskToDelete != null && tasks.remove(taskToDelete)) {
            terminal.success("Deleted task: ${taskToDelete.description}")
        } else {
            terminal.danger("Task not found: $response")
        }
    }
}

data class Task(
    val description: String,
    val schedule: String,
) : Serializable

val tasks = PersistentHashSet<Task>("tasks", prefs)
