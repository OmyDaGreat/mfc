package xyz.malefic.mfc.util

import xyz.malefic.compose.prefs.collection.PersistentHashSet
import java.util.prefs.Preferences

val prefs: Preferences = Preferences.userRoot().node("xyz.malefic.mfc")

val rssURLs = PersistentHashSet<String>("rss_urls", prefs)
