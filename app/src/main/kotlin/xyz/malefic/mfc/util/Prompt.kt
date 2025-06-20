package xyz.malefic.mfc.util

import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.terminal.Terminal

/**
 * Displays an interactive prompt in the terminal for the user to select an option from a list.
 *
 * The user can navigate the options using the up and down arrow keys, and select an option by pressing Enter.
 * Optionally, a "None" choice can be added to the list. If "None" is selected, `null` is returned.
 * The prompt can be canceled by pressing 'q', which also returns `null`.
 *
 * @receiver The Mordant Terminal instance used for displaying the prompt and handling input.
 * @param promptQuestion The question or message to display above the list of choices.
 * @param choices The list of selectable options.
 * @param enableNoneOption If true, adds a "None" option to the end of the choices.
 * @param customNoneOption An optional custom label for the "None" option. If not provided, defaults to "None".
 * @return The selected option as a String, or `null` if "None" is selected or the prompt is canceled.
 */
fun Terminal.betterPrompt(
    promptQuestion: String = "Please select an option:",
    choices: List<String>,
    enableNoneOption: Boolean = false,
    customNoneOption: String? = null,
): String? =
    interactiveSelectList {
        title(promptQuestion)
        choices.forEach { addEntry(it) }
        if (enableNoneOption) {
            addEntry(customNoneOption ?: "None")
        }
    }
