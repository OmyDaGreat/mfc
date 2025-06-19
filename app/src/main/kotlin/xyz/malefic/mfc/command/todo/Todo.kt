package xyz.malefic.mfc.command.todo

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.BorderType.Companion.ROUNDED
import com.github.ajalt.mordant.rendering.TextAlign.LEFT
import com.github.ajalt.mordant.rendering.TextAlign.RIGHT
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.brightRed
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.table.Borders.ALL
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.muted
import com.github.ajalt.mordant.terminal.success
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.betterPrompt
import xyz.malefic.mfc.util.todo.TodoManager
import xyz.malefic.mfc.util.todo.TodoManager.TodoTask
import xyz.malefic.mfc.util.todo.TodoManager.tasks
import java.time.LocalDate
import java.time.format.DateTimeParseException

class TodoCommand : CliktCommand("todo", "Manage your todo list") {
    init {
        subcommands(AddTodoCommand(), ListTodoCommand(), DeleteTodoCommand(), CompleteTodoCommand())
    }

    override fun run() = Unit
}

class AddTodoCommand : CliktCommand("add", "Add a new todo item") {
    private val terminal = Terminal()
    private val task: String by argument(help = "The task description")
    private val dueDate: String? by option(help = "The due date for the task (format: YYYY-MM-DD or MM-DD)")

    override fun run() {
        try {
            val parsedDate =
                dueDate?.let {
                    try {
                        if (it.matches(Regex("\\d{2}-\\d{2}"))) {
                            val currentYear = LocalDate.now().year
                            LocalDate.parse("$currentYear-$it")
                        } else {
                            LocalDate.parse(it)
                        }
                    } catch (_: DateTimeParseException) {
                        terminal.danger("Invalid date format. Use YYYY-MM-DD or MM-DD.")
                        return
                    }
                }
            TodoManager.addTask(task, parsedDate)
            terminal.success("Added todo: $task${if (parsedDate != null) " (Due: $parsedDate)" else ""}")
        } catch (e: Exception) {
            terminal.danger("Failed to add todo: ${e.message}")
        }
    }
}

class ListTodoCommand : CliktCommand("list", "List all todo items") {
    private val terminal = Terminal()

    override fun run() =
        with(terminal) {
            val tasks = TodoManager.listTasks()
            if (tasks.isEmpty()) {
                muted("No todo items found.")
            } else {
                success("Todo list:")
                renderTable(tasks)
            }
        }

    private fun Terminal.renderTable(tasks: List<TodoTask>) {
        val table =
            table {
                borderType = ROUNDED
                borderStyle = rgb("#4b25b9")
                align = RIGHT
                tableBorders = ALL
                header {
                    style = brightRed + bold
                    row("Status", "Description", "Due Date")
                }
                body {
                    style = green
                    column(0) {
                        align = LEFT
                        cellBorders = ALL
                        style = brightBlue
                    }
                    tasks.forEachIndexed { index, task ->
                        row(
                            if (task.completed) "✔" else " ",
                            task.description,
                            task.dueDate?.toString() ?: "",
                        ) {
                            style = if (index % 2 == 0) rgb("#008080") else rgb("#00ced1")
                        }
                    }
                }
            }
        info(table)
    }
}

class DeleteTodoCommand : CliktCommand("delete", "Delete a todo item") {
    private val terminal = Terminal()

    override fun run() =
        with(terminal) {
            val tasks = TodoManager.listTasks()
            if (tasks.isEmpty()) {
                muted("No todo items found.")
                return
            }

            val response = betterPrompt("Select a todo to delete:", tasks.map { it.description }, enableNoneOption = true)
            if (response == null) {
                muted("No valid selection made.")
                return
            }

            try {
                TodoManager.deleteTask(response)
                success("Deleted todo: $response")
            } catch (e: Exception) {
                danger("Failed to delete todo: ${e.message}")
            }
        }
}

class CompleteTodoCommand : CliktCommand("complete", "Mark a todo item as complete") {
    private val terminal = Terminal()
    private val task by argument(help = "The task description").optional()

    override fun run() =
        with(terminal) {
            task?.let { task ->
                val todo = tasks.find { it.description == task }
                if (todo == null) {
                    danger("Todo item not found: $task")
                    return
                }
                if (todo.completed) {
                    muted("Todo item is already marked as complete: $task")
                    return
                }
                try {
                    TodoManager.completeTask(task)
                    success("Marked todo as complete: $task")
                } catch (e: Exception) {
                    danger("Failed to mark todo as complete: ${e.message}")
                }
            } ?: run {
                if (tasks.isEmpty()) {
                    muted("No incomplete todo items found.")
                    return
                }

                val response = betterPrompt("Select a todo to mark as complete:", tasks.map { it.description })
                if (response == null) {
                    muted("No valid selection made.")
                    return
                }

                try {
                    TodoManager.completeTask(response)
                    success("Marked todo as complete: $response")
                } catch (e: Exception) {
                    danger("Failed to mark todo as complete: ${e.message}")
                }
            }
        }
}
