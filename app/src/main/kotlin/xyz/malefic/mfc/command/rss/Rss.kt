package xyz.malefic.mfc.command.rss

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import org.w3c.dom.Element
import xyz.malefic.mfc.util.SuspendingCliktCommand
import xyz.malefic.mfc.util.clearConsole
import xyz.malefic.mfc.util.rssURLs
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.text.format

class RssCommand : SuspendingCliktCommand("rss", "Manage RSS feeds") {
    override suspend fun run() = Unit
}

class FetchCommand : SuspendingCliktCommand("fetch", "Fetch all RSS feeds") {
    private val terminal = Terminal()

    override suspend fun run() {
        if (rssURLs.isEmpty()) {
            terminal.println(red("No RSS URLs found."))
            return
        }

        val allItems = mutableListOf<Pair<String, Date>>()
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)

        rssURLs.forEach { url ->
            try {
                val connection = URI(url).toURL().openConnection()
                val inputStream = connection.getInputStream()
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)

                val items = document.getElementsByTagName("item")
                for (i in 0 until items.length) {
                    val item = items.item(i) as Element
                    val itemTitle = item.getElementsByTagName("title").item(0).textContent
                    val pubDate = item.getElementsByTagName("pubDate").item(0).textContent
                    val parsedDate = dateFormat.parse(pubDate)
                    allItems.add("$itemTitle (from $url)" to parsedDate)
                }
            } catch (e: Exception) {
                terminal.println(red("Failed to fetch RSS feed from $url: ${e.message}"))
            }
        }

        allItems.sortByDescending { it.second }

        val pageSize = 2
        var currentPage = 0

        fun displayPage() {
            clearConsole()

            val startIndex = currentPage * pageSize
            val endIndex = minOf(startIndex + pageSize, allItems.size)

            terminal.println(cyan("Displaying items ${startIndex + 1} to $endIndex:"))
            allItems.subList(startIndex, endIndex).forEach { (title, date) ->
                terminal.println(yellow("${dateFormat.format(date)} - $title"))
            }

            if (endIndex == allItems.size) {
                terminal.println(magenta("End of items."))
            }

            terminal.println(green("Use 'n' for next page, 'p' for previous page, or 'q' to quit."))
        }

        displayPage()

        var shouldExit = false

        while (!shouldExit) {
            val input = readLine()?.trim()?.lowercase()
            when (input) {
                "n" -> { // Next page
                    if ((currentPage + 1) * pageSize < allItems.size) currentPage++
                }
                "p" -> { // Previous page
                    if (currentPage > 0) currentPage--
                }
                "q" -> { // Quit
                    shouldExit = true
                }
            }

            if (!shouldExit) {
                displayPage()
            }
        }
    }
}

class AddCommand : SuspendingCliktCommand("add", "Add an RSS feed URL") {
    private val url: String by argument(help = "URL of the RSS feed")

    override suspend fun run() {
        rssURLs.add(url)
        echo("Added RSS feed URL: $url")
    }
}

class DeleteCommand : SuspendingCliktCommand("delete", "Delete an RSS feed URL") {
    override suspend fun run() {
        if (rssURLs.isEmpty()) {
            echo("No RSS URLs found.")
            return
        }

        echo("Select an RSS feed URL to delete:")
        rssURLs.forEachIndexed { index, url ->
            echo("$index: $url")
        }

        echo("Enter the index of the URL to delete:")
        val input = readLine()?.toIntOrNull()

        if (input == null || input !in rssURLs.indices) {
            echo("Invalid index.")
            return
        }

        val removedUrl = rssURLs.elementAt(input)
        rssURLs.remove(removedUrl)
        echo("Deleted RSS feed URL: $removedUrl")
    }
}
