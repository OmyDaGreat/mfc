package xyz.malefic.mfc.util

import com.github.ajalt.mordant.input.InputReceiver.Status.Continue
import com.github.ajalt.mordant.input.InputReceiver.Status.Finished
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.muted

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
 * @return The selected option as a String, or `null` if "None" is selected or the prompt is canceled.
 */
fun Terminal.betterPrompt(
    promptQuestion: String = "Please select an option:",
    choices: List<String>,
    enableNoneOption: Boolean = false,
): String? {
    val options = if (enableNoneOption) choices + "None" else choices
    var currentIndex = 0

    fun displayOptions() {
        clearConsole()
        info(promptQuestion)
        options.forEachIndexed { index, option ->
            if (index == currentIndex) {
                info("> $option")
            } else {
                muted("  $option")
            }
        }
    }

    displayOptions()

    return receiveKeyEvents { event ->
        when (event.key) {
            "UpArrow" -> {
                if (currentIndex > 0) currentIndex--
            }
            "DownArrow" -> {
                if (currentIndex < options.size - 1) currentIndex++
            }
            "Enter" -> {
                return@receiveKeyEvents Finished(options[currentIndex].takeUnless { it == "None" })
            }
            "q" -> {
                muted("Prompt canceled.")
                return@receiveKeyEvents Finished(null)
            }
        }
        displayOptions()
        Continue
    }
}
