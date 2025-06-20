package xyz.malefic.mfc.util

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger

fun Terminal.checkInteractive(): Boolean {
    if (!terminalInfo.interactive) {
        danger("Interactive mode is not supported in non-interactive terminals.")
        return true
    }
    return false
}
