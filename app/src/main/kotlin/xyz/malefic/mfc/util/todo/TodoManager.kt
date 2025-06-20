package xyz.malefic.mfc.util.todo

import xyz.malefic.compose.prefs.collection.PersistentHashSet
import xyz.malefic.mfc.util.prefs
import java.io.Serializable
import java.time.LocalDate

/**
 * A singleton object that manages a list of todo tasks.
 * Provides functionality to add, list, delete, and mark tasks as complete.
 */
object TodoManager {
    /**
     * A mutable list of tasks managed by the TodoManager.
     */
    var tasks = PersistentHashSet<TodoTask>("tasks", prefs)
        private set

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
    ) : Serializable

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
     * Deletes a task from the todo list based on its description.
     *
     * @param description The description of the task to be deleted.
     */
    fun deleteTask(description: String) = tasks.removeIf { it.description == description }

    /**
     * Marks a task as completed based on its description.
     *
     * @param description The description of the task to be marked as complete.
     */
    fun completeTask(description: String) {
        tasks.find { it.description == description }?.completed = true
    }

    /**
     * Marks a task as uncompleted based on its description.
     *
     * @param description The description of the task to be marked as uncomplete.
     */
    fun uncompleteTask(description: String) {
        tasks.find { it.description == description }?.completed = false
    }
}
