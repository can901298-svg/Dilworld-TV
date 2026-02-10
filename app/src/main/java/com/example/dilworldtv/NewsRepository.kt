package com.example.dilworldtv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.Instant

/**
 * AA RSS (TeyitHattı) "Tüm Haberler" feed'i.
 * Kaynak: https://www.aa.com.tr/tr/teyithatti/rss/news?cat=0
 */
class NewsRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val url = "https://www.aa.com.tr/tr/teyithatti/rss/news?cat=0"

    suspend fun fetchTitles(limit: Int = 10): NewsUi = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("AA RSS HTTP ${resp.code}")
            val xml = resp.body?.string() ?: error("Empty body")
            val titles = parseTitles(xml, limit)
            NewsUi(titles = titles, fetchedAtIso = Instant.now().toString())
        }
    }

    private fun parseTitles(xml: String, limit: Int): List<String> {
        val titles = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var inItem = false
        var inTitle = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT && titles.size < limit) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "item" -> inItem = true
                        "title" -> if (inItem) inTitle = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem && inTitle) {
                        val t = parser.text?.trim().orEmpty()
                        if (t.isNotEmpty()) titles.add(t)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "item" -> inItem = false
                        "title" -> inTitle = false
                    }
                }
            }
            parser.next()
        }
        return titles
    }
}
