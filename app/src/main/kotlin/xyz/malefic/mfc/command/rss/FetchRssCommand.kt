package xyz.malefic.mfc.command.rss

import com.github.ajalt.clikt.parameters.arguments.argument
import xyz.malefic.mfc.util.CliktCommand
import xyz.malefic.mfc.util.rssURLs
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

class RssCommand : CliktCommand(help = "Manage RSS feeds", name = "rss") {
    override fun run() = Unit
}

class FetchCommand : CliktCommand(help = "Fetch all RSS feeds", name = "fetch") {
    override fun run() {
        if (rssURLs.isEmpty()) {
            echo("No RSS URLs found.")
            return
        }

        rssURLs.forEach { url ->
            try {
                val connection = URI(url).toURL().openConnection()
                val inputStream = connection.getInputStream()
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)

                val channel = document.getElementsByTagName("channel").item(0)
                val title = channel.childNodes.item(1).textContent
                println("Feed Title: $title")

                val items = document.getElementsByTagName("item")
                for (i in 0 until items.length) {
                    val item = items.item(i)
                    val itemTitle = item.childNodes.item(1).textContent
                    val itemLink = item.childNodes.item(3).textContent
                    println("Item: $itemTitle\nLink: $itemLink\n")
                }
            } catch (e: Exception) {
                echo("Failed to fetch RSS feed from $url: ${e.message}")
            }
        }
    }
}

class AddCommand : CliktCommand(help = "Add an RSS feed URL", name = "add") {
    private val url: String by argument(help = "URL of the RSS feed")

    override fun run() {
        rssURLs.add(url)
        echo("Added RSS feed URL: $url")
    }
}

class DeleteCommand : CliktCommand(help = "Delete an RSS feed URL", name = "delete") {
    private val url: String by argument(help = "URL of the RSS feed")

    override fun run() {
        if (rssURLs.remove(url)) {
            echo("Deleted RSS feed URL: $url")
        } else {
            echo("RSS feed URL not found: $url")
        }
    }
}
