package xyz.malefic.mfc.util.todo

import java.time.LocalDate

/**
 * A singleton object that manages a list of todo tasks.
 * Provides functionality to add, list, delete, and mark tasks as complete.
 */
object TodoManager {
    /**
     * A mutable list of tasks managed by the TodoManager.
     */
    val tasks = mutableListOf<TodoTask>()

    /**
     * Represents a single todo task.
     *
     * @property description The description of the task.
     * @property completed Indicates whether the task is completed.
     */
    data class TodoTask(
        val description: String,
        var completed: Boolean = false,
        val dueDate: LocalDate? = null,
    )

    /**
     * Adds a new task to the todo list.
     *
     * @param description The description of the task to be added.
     * @param dueDate The optional date the task is due.
     */
    fun addTask(
        description: String,
        dueDate: LocalDate? = null,
    ) {
        tasks.add(TodoTask(description, dueDate = dueDate))
    }

    /**
     * Lists all tasks in the todo list.
     *
     * @return A list of all tasks.
     */
    fun listTasks(): List<TodoTask> = tasks

    /**
     * Deletes a task from the todo list based on its description.
     *
     * @param description The description of the task to be deleted.
     */
    fun deleteTask(description: String) {
        tasks.removeIf { it.description == description }
    }

    /**
     * Marks a task as completed based on its description.
     *
     * @param description The description of the task to be marked as complete.
     */
    fun completeTask(description: String) {
        tasks.find { it.description == description }?.completed = true
    }
}
