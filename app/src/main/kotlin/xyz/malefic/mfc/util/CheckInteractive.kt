package xyz.malefic.mfc.util

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger

/**
 * Extension function for the `Terminal` class to check if the terminal supports interactive mode.
 *
 * This function verifies whether the terminal is interactive by checking the `terminalInfo.interactive` property.
 * - If the terminal is not interactive, it displays a danger message indicating that interactive mode is not supported.
 * - Returns `true` if the terminal is non-interactive, otherwise `false`.
 *
 * @receiver The `Terminal` instance on which the check is performed.
 * @return `true` if the terminal is non-interactive, otherwise `false`.
 */
fun Terminal.checkNonInteractive(): Boolean {
    if (!terminalInfo.interactive) {
        danger("Interactive mode is not supported in non-interactive terminals.")
        return true
    }
    return false
}
