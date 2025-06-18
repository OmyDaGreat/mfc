package xyz.malefic.mfc.command.rss

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Document
import org.w3c.dom.Element
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.clearConsole
import xyz.malefic.mfc.util.rssURLs
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class RssCommand : CliktCommand("rss", "Manage RSS feeds") {
    override fun run() = Unit
}

class FetchCommand : CliktCommand("fetch", "Fetch all RSS feeds") {
    private val terminal = Terminal()
    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
    private val pageSize = 5

    override fun run() {
        if (rssURLs.isEmpty()) {
            terminal.println(gray("No RSS URLs found."))
            return
        }

        val allItems = runBlocking { fetchAllItems() }
        if (allItems.isEmpty()) {
            terminal.println(gray("No items found in the RSS feeds."))
            return
        }

        paginateItems(allItems)
    }

    private suspend fun fetchAllItems(): List<Pair<String, Date>> =
        coroutineScope {
            val allItems = mutableListOf<Pair<String, Date>>()
            val fetchJobs =
                rssURLs.map { url ->
                    async {
                        try {
                            val document = rss(url)
                            parseItems(document, url)
                        } catch (e: Exception) {
                            terminal.println(gray("Failed to fetch RSS feed from $url: ${e.message}"))
                            emptyList<Pair<String, Date>>()
                        }
                    }
                }

            fetchJobs.forEach { job ->
                allItems.addAll(job.await())
            }

            allItems.sortByDescending { it.second }
            allItems
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
                "w", "d" -> if ((currentPage + 1) * pageSize < allItems.size) currentPage++
                "s", "a" -> if (currentPage > 0) currentPage--
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

        terminal.println(blue("Use 'w' or 'd' for next page, 's' or 'a' for previous page, or 'q' to quit."))
    }
}

class AddCommand : CliktCommand("add", "Add an RSS feed URL") {
    private val terminal = Terminal()
    private val url: String by argument(help = "URL of the RSS feed")

    override fun run() {
        try {
            rss(url)
            rssURLs.add(url)
            terminal.println(blue("Added RSS feed URL: $url"))
        } catch (e: Exception) {
            terminal.println(gray("Invalid RSS feed URL: ${e.message}"))
        }
    }
}

class DeleteCommand : CliktCommand("delete", "Delete an RSS feed URL") {
    private val terminal = Terminal()

    override fun run() {
        if (rssURLs.isEmpty()) {
            terminal.println(gray("No RSS URLs found."))
            return
        }

        if (rssURLs.size == 1) {
            terminal.println(gray("Only one RSS URL available. Deleting it."))
            rssURLs.clear()
            return
        }

        val response =
            terminal.prompt(
                "Select an RSS feed URL to delete:",
                choices = rssURLs,
            )

        if (response == null) {
            terminal.println(gray("No valid selection made."))
            return
        }

        rssURLs.remove(response)
        terminal.println(blue("Deleted RSS feed URL: $response"))
    }
}

private fun rss(url: String): Document {
    val connection = URI(url).toURL().openConnection()
    val inputStream = connection.getInputStream()
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
}
