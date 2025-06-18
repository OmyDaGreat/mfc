package xyz.malefic.mfc.util

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.installMordant
import com.github.ajalt.clikt.core.theme

/**
 * Abstract base class for creating commands using the Clikt library.
 *
 * This class extends `CoreCliktCommand` and provides additional functionality such as:
 * - Automatic installation of Mordant for enhanced terminal output.
 * - Customizable help text formatting using themes.
 *
 * @property name The name of the command, used in help output. If not provided, it is inferred from the class name.
 * @property help The help text displayed for the command.
 */
abstract class CliktCommand(
    /**
     * The name of the command. If not provided, it will be inferred from the class name.
     */
    name: String? = null,
    /**
     * The help text for the command, displayed in the help output.
     */
    val help: String = "",
) : CoreCliktCommand(name) {
    init {
        installMordant()
    }

    /**
     * Overrides the default help text formatting.
     *
     * @param context The Clikt context containing theme information.
     * @return The formatted help text.
     */
    override fun help(context: Context) = context.theme.info(help)
}
