package xyz.malefic.mfc.command.rss

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.input.InputReceiver.Status.Companion.Finished
import com.github.ajalt.mordant.input.InputReceiver.Status.Continue
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.muted
import com.github.ajalt.mordant.terminal.success
import com.github.ajalt.mordant.terminal.warning
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Document
import org.w3c.dom.Element
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.betterPrompt
import xyz.malefic.mfc.util.clearConsole
import xyz.malefic.mfc.util.rssURLs
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class RssCommand : CliktCommand("rss", "Manage RSS feeds") {
    init {
        subcommands(FetchCommand(), AddCommand(), DeleteCommand())
    }

    override fun run() = Unit
}

class FetchCommand : CliktCommand("fetch", "Fetch all RSS feeds") {
    private val terminal = Terminal()
    private val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH)
    private val pageSize = 5

    override fun run() =
        with(terminal) {
            if (rssURLs.isEmpty()) {
                muted("No RSS URLs found.")
                return
            }

            val allItems = runBlocking { fetchAllItems() }
            if (allItems.isEmpty()) {
                muted("No items found in the RSS feeds.")
                return
            }

            if (!terminalInfo.interactive) {
                clearConsole()
                info("Displaying all items:")
                allItems.forEach { (title, date) ->
                    info("${dateFormat.format(date)} - $title")
                }
                return
            }

            paginateItems(allItems)
        }

    private suspend fun fetchAllItems() =
        coroutineScope {
            val allItems = mutableListOf<Pair<String, Date>>()
            val fetchJobs =
                rssURLs.map { url ->
                    async {
                        try {
                            parseItems(rss(url), url)
                        } catch (e: Exception) {
                            terminal.warning("Failed to fetch RSS feed from $url: ${e.message}")
                            emptyList()
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

        fun showPage() = displayPage(allItems, currentPage)

        showPage()

        terminal.receiveKeyEvents { event ->
            terminal.muted("Key event: ${event.key}")
            showPage()

            when {
                event.isCtrlC -> Finished
                event.key == "q" -> Finished
                else -> {
                    when (event.key) {
                        "RightArrow", "d" -> {
                            if ((currentPage + 1) * pageSize < allItems.size) currentPage++
                        }
                        "LeftArrow", "a" -> {
                            if (currentPage > 0) currentPage--
                        }
                    }
                    Continue
                }
            }.also {
                showPage()
            }
        }
    }

    private fun displayPage(
        allItems: List<Pair<String, Date>>,
        currentPage: Int,
    ) = with(terminal) {
        clearConsole()
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, allItems.size)

        info("Displaying items ${startIndex + 1} to $endIndex:")
        allItems.subList(startIndex, endIndex).forEach { (title, date) ->
            info("${dateFormat.format(date)} - $title")
        }

        if (endIndex == allItems.size) {
            muted("End of items.")
        }

        info("Use the right arrow key or 'd' for next page, left arrow key or 'a' for previous page, or 'q' to quit.")
    }
}

class AddCommand : CliktCommand("add", "Add an RSS feed URL") {
    private val terminal = Terminal()
    private val url: String by argument(help = "URL of the RSS feed")

    override fun run() {
        try {
            terminal.info("Checking validity.")
            rss(url)
            rssURLs.add(url)
            terminal.success("Added RSS feed URL: $url")
        } catch (e: Exception) {
            terminal.danger("Invalid RSS feed URL: ${e.message}")
        }
    }
}

class DeleteCommand : CliktCommand("delete", "Delete an RSS feed URL") {
    private val terminal = Terminal()

    override fun run() {
        if (rssURLs.isEmpty()) {
            terminal.muted("No RSS URLs found.")
            return
        }

        if (rssURLs.size == 1) {
            terminal.muted("Only one RSS URL available. Deleting it.")
            rssURLs.clear()
            return
        }

        val response =
            terminal.betterPrompt(
                "Select an RSS feed URL to delete:",
                rssURLs.toList(),
            )

        if (response == null) {
            terminal.muted("No valid selection made.")
            return
        }

        if (rssURLs.remove(response)) {
            terminal.success("Deleted RSS feed URL: $response")
        } else {
            terminal.danger("RSS feed URL not found: $response")
        }
    }
}

/**
 * Fetches and parses the RSS feed from the given URL.
 *
 * @param url The URL of the RSS feed to fetch.
 * @return The parsed XML Document representing the RSS feed.
 * @throws Exception if the URL is invalid or the feed cannot be fetched or parsed.
 */
private fun rss(url: String): Document {
    val connection = URI(url).toURL().openConnection()
    val inputStream = connection.getInputStream()
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
}
