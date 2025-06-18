package xyz.malefic.mfc.command.rss

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.terminal.Terminal
import org.w3c.dom.Document
import org.w3c.dom.Element
import xyz.malefic.mfc.util.SuspendingCliktCommand
import xyz.malefic.mfc.util.clearConsole
import xyz.malefic.mfc.util.rssURLs
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class RssCommand : SuspendingCliktCommand("rss", "Manage RSS feeds") {
    override suspend fun run() = Unit
}

class FetchCommand : SuspendingCliktCommand("fetch", "Fetch all RSS feeds") {
    private val terminal = Terminal()
    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
    private val pageSize = 2

    override suspend fun run() {
        if (rssURLs.isEmpty()) {
            terminal.println(gray("No RSS URLs found."))
            return
        }

        val allItems = fetchAllItems()
        if (allItems.isEmpty()) {
            terminal.println(gray("No items found in the RSS feeds."))
            return
        }

        paginateItems(allItems)
    }

    private fun fetchAllItems(): List<Pair<String, Date>> {
        val allItems = mutableListOf<Pair<String, Date>>()
        rssURLs.forEach { url ->
            try {
                val document = rss(url)
                allItems.addAll(parseItems(document, url))
            } catch (e: Exception) {
                terminal.println(gray("Failed to fetch RSS feed from $url: ${e.message}"))
            }
        }
        allItems.sortByDescending { it.second }
        return allItems
    }

    private fun parseItems(
        document: Document,
        url: String,
    ): List<Pair<String, Date>> {
        val items = document.getElementsByTagName("item")
        val parsedItems = mutableListOf<Pair<String, Date>>()
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            val itemTitle = item.getElementsByTagName("title").item(0).textContent
            val pubDate = item.getElementsByTagName("pubDate").item(0).textContent
            val parsedDate = dateFormat.parse(pubDate)
            parsedItems.add("$itemTitle (from $url)" to parsedDate)
        }
        return parsedItems
    }

    private fun paginateItems(allItems: List<Pair<String, Date>>) {
        var currentPage = 0
        var shouldExit = false

        while (!shouldExit) {
            displayPage(allItems, currentPage)
            val input = readLine()?.trim()?.lowercase()
            when (input) {
                "n" -> if ((currentPage + 1) * pageSize < allItems.size) currentPage++
                "p" -> if (currentPage > 0) currentPage--
                "q" -> shouldExit = true
            }
        }
    }

    private fun displayPage(
        allItems: List<Pair<String, Date>>,
        currentPage: Int,
    ) {
        clearConsole()
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, allItems.size)

        terminal.println(blue("Displaying items ${startIndex + 1} to $endIndex:"))
        allItems.subList(startIndex, endIndex).forEach { (title, date) ->
            terminal.println(white("${dateFormat.format(date)} - $title"))
        }

        if (endIndex == allItems.size) {
            terminal.println(gray("End of items."))
        }

        terminal.println(blue("Use 'n' for next page, 'p' for previous page, or 'q' to quit."))
    }
}

class AddCommand : SuspendingCliktCommand("add", "Add an RSS feed URL") {
    private val terminal = Terminal()
    private val url: String by argument(help = "URL of the RSS feed")

    override suspend fun run() {
        try {
            rss(url)
            rssURLs.add(url)
            terminal.println(blue("Added RSS feed URL: $url"))
        } catch (e: Exception) {
            terminal.println(gray("Invalid RSS feed URL: ${e.message}"))
        }
    }
}

class DeleteCommand : SuspendingCliktCommand("delete", "Delete an RSS feed URL") {
    private val terminal = Terminal()

    override suspend fun run() {
        if (rssURLs.isEmpty()) {
            terminal.println(gray("No RSS URLs found."))
            return
        }

        terminal.println(blue("Select an RSS feed URL to delete:"))
        rssURLs.forEachIndexed { index, url ->
            terminal.println(white("$index: $url"))
        }

        terminal.println(blue("Enter the index of the URL to delete:"))
        val input = readLine()?.toIntOrNull()

        if (input == null || input !in rssURLs.indices) {
            terminal.println(gray("Invalid index."))
            return
        }

        val removedUrl = rssURLs.elementAt(input)
        rssURLs.remove(removedUrl)
        terminal.println(blue("Deleted RSS feed URL: $removedUrl"))
    }
}

private fun rss(url: String): Document {
    val connection = URI(url).toURL().openConnection()
    val inputStream = connection.getInputStream()
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
}
