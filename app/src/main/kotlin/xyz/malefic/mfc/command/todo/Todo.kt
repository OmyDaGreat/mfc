package xyz.malefic.mfc.command.todo

import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
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
import com.github.ajalt.mordant.terminal.warning
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.betterPrompt
import xyz.malefic.mfc.util.checkInteractive
import xyz.malefic.mfc.util.todo.TodoManager
import xyz.malefic.mfc.util.todo.TodoManager.TodoTask
import xyz.malefic.mfc.util.todo.TodoManager.tasks
import xyz.malefic.mfc.util.todo.getParsedDate
import java.time.LocalDate
import java.time.format.DateTimeParseException

class TodoCommand :
    CliktCommand(
        name = "todo",
        help =
            """
            Manage your todo list interactively or via subcommands.

            Interactive Mode:
            - Enter interactive mode by running `mfc todo` without any subcommands.
            - Type commands directly (e.g., `add`, `list`, `delete`, `complete`) or use `q` to exit.
            - Use `--help` after a command to see usage instructions.

            Subcommands:
            - `add`: Add a new todo item.
              Example: `mfc todo add Buy groceries --dueDate 2023-10-15`
            - `list`: List all todo items.
              Example: `mfc todo list`
            - `delete`: Delete a todo item.
              Example: `mfc todo delete`
            - `complete`: Mark a todo item as complete. Supports interactive selection.
              Example: `mfc todo complete Buy groceries`
            - `incomplete`: Mark a todo item as incomplete. Supports interactive selection.
              Example: `mfc todo incomplete`
            """.trimIndent(),
    ) {
    init {
        subcommands(
            AddTodoCommand(),
            ListTodoCommand(),
            DeleteTodoCommand(),
            CompleteTodoCommand(),
            IncompleteTodoCommand(),
        )
    }

    override val invokeWithoutSubcommand = true

    override fun run() =
        with(Terminal()) {
            if (currentContext.invokedSubcommands.isNotEmpty()) {
                return
            }

            if (checkInteractive()) return

            info("Entering interactive todo mode. Type commands prefixed with 'mfc todo' automatically, or 'q' to exit.")

            while (true) {
                try {
                    print(theme.info("Enter a command: "))
                    val input = readLine()?.trim() ?: continue

                    if (input.lowercase() == "q") {
                        success("Exiting interactive todo mode.")
                        break
                    }

                    val parts = input.split(" ")
                    val commandName = parts.firstOrNull()
                    val args = parts.drop(1)

                    fun CliktCommand.checkHelp() =
                        if (args.contains("--help")) {
                            info(help)
                        } else {
                            parse(args)
                        }

                    when (commandName) {
                        "help" -> println(currentContext.command.help(currentContext))
                        "add" -> AddTodoCommand().checkHelp()
                        "list" -> ListTodoCommand().checkHelp()
                        "delete" -> DeleteTodoCommand().checkHelp()
                        "complete" -> CompleteTodoCommand().checkHelp()
                        else -> danger("Unknown command: $commandName")
                    }
                } catch (e: Exception) {
                    warning("Something didn't quite work as expected: ${e.message}")
                }
            }
        }
}

class AddTodoCommand : CliktCommand("add", "Add a new todo item") {
    private val terminal = Terminal()
    private val task: List<String> by argument(help = "The task description").multiple(required = true)
    private val dueDate: String? by option(
        "--due",
        "--dueDate",
        "--due-date",
        help = "The due date for the task (format: YYYY-MM-DD or MM-DD)",
    )

    override fun run() =
        with(terminal) {
            try {
                val parsedDate: LocalDate?
                try {
                    parsedDate = dueDate.getParsedDate()
                } catch (_: DateTimeParseException) {
                    danger("Invalid date format. Use YYYY-MM-DD or MM-DD.")
                    return
                }
                val single = task.reduce { acc, string -> "$acc $string" }
                TodoManager.addTask(single, parsedDate)
                success("Added todo: $single${parsedDate?.let { " (Due: $it)" } ?: ""}")
            } catch (e: Exception) {
                danger("Failed to add todo: ${e.message}")
            }
        }
}

class ListTodoCommand : CliktCommand("list", "List all todo items") {
    private val terminal = Terminal()

    override fun run() =
        with(terminal) {
            val tasks = tasks
            if (tasks.isEmpty()) {
                muted("No todo items found.")
            } else {
                success("Todo list:")
                renderTable(tasks)
            }
        }

    private fun Terminal.renderTable(tasks: Collection<TodoTask>) {
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

        println(table)
    }
}

class DeleteTodoCommand : CliktCommand("delete", "Delete a todo item") {
    private val terminal = Terminal()
    private val task by argument(help = "The task description").multiple().optional()

    override fun run() =
        with(terminal) {
            if (tasks.isEmpty()) {
                muted("No todo items found.")
                return
            }

            val todo = task?.reduce { acc, string -> "$acc $string" }

            todo?.let {
                handleSpecificTask(it)
            } ?: handleInteractiveCompletion()
        }

    private fun Terminal.handleSpecificTask(task: String) {
        if (TodoManager.deleteTask(task)) {
            success("Deleted todo: $task")
        } else {
            muted("Task $task doesn't exist")
        }
    }

    private fun Terminal.handleInteractiveCompletion() {
        if (checkInteractive()) return

        try {
            val response =
                betterPrompt(
                    "Select a todo to delete:",
                    tasks.map { it.description },
                    enableNoneOption = true,
                    "Exit",
                )

            if (response == null || response == "Exit") {
                muted("No selection made.")
                return
            }

            handleSpecificTask(response)
        } catch (e: Exception) {
            danger("Failed to delete todo: ${e.message}")
        }
    }
}

class CompleteTodoCommand : CliktCommand("complete", "Mark a todo item as complete") {
    private val terminal = Terminal()
    private val task by argument(help = "The task description").multiple().optional()

    override fun run() =
        with(terminal) {
            if (tasks.none { !it.completed }) {
                muted("No incomplete todo items found.")
                return
            }

            val todo = task?.reduce { acc, string -> "$acc $string" }

            todo?.let {
                handleSpecificTask(it)
            } ?: handleInteractiveCompletion()
        }

    private fun Terminal.handleSpecificTask(task: String) {
        val todo = findTask(task)
        if (todo == null) {
            danger("Todo item not found: $task")
            return
        }
        if (todo.completed) {
            muted("Todo item is already marked as complete: $task")
            return
        }
        markTaskAsComplete(task)
    }

    private fun Terminal.handleInteractiveCompletion() {
        if (checkInteractive()) return

        val response = betterPrompt("Select a todo to mark as complete:", tasks.map { it.description }, true, "Exit")
        if (response == null || response == "Exit") {
            muted("No selection made.")
            return
        }

        markTaskAsComplete(response)
    }

    private fun findTask(description: String): TodoTask? = tasks.find { it.description == description }

    private fun Terminal.markTaskAsComplete(task: String) {
        try {
            TodoManager.completeTask(task)
            success("Marked todo as complete: $task")
        } catch (e: Exception) {
            danger("Failed to mark todo as complete: ${e.message}")
        }
    }
}

class IncompleteTodoCommand : CliktCommand("incomplete", "Mark a todo item as incomplete") {
    private val terminal = Terminal()
    private val task by argument(help = "The task description").multiple().optional()

    override fun run() =
        with(terminal) {
            if (tasks.none { it.completed }) {
                muted("No complete todo items found.")
                return
            }

            val todo = task?.reduce { acc, string -> "$acc $string" }

            todo?.let {
                handleSpecificTask(it)
            } ?: handleInteractiveCompletion()
        }

    private fun Terminal.handleSpecificTask(task: String) {
        val todo = findTask(task)
        if (todo == null) {
            danger("Todo item not found: $task")
            return
        }
        if (!todo.completed) {
            muted("Todo item is already marked as complete: $task")
            return
        }
        markTaskAsincomplete(task)
    }

    private fun Terminal.handleInteractiveCompletion() {
        if (checkInteractive()) return

        val response = betterPrompt("Select a todo to mark as incomplete:", tasks.map { it.description }, true, "Exit")
        if (response == null || response == "Exit") {
            muted("No selection made.")
            return
        }

        markTaskAsincomplete(response)
    }

    private fun findTask(description: String): TodoTask? = tasks.find { it.description == description }

    private fun Terminal.markTaskAsincomplete(task: String) {
        try {
            TodoManager.uncompleteTask(task)
            success("Marked todo as incomplete: $task")
        } catch (e: Exception) {
            danger("Failed to mark todo as incomplete: ${e.message}")
        }
    }
}
