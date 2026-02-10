package com.example.dilworldtv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import kotlin.math.roundToInt

/**
 * TCMB today.xml ile USD/EUR kurlarını alır.
 * Gram altın için:
 * - today.xml içinde XAU varsa, mevcut değerleri kullanarak yaklaşık gram TL hesaplamaya çalışır.
 */
class FxRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetch(): FxUi = withContext(Dispatchers.IO) {
        val url = "https://www.tcmb.gov.tr/kurlar/today.xml"
        val req = Request.Builder().url(url).build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("TCMB HTTP ${resp.code}")
            val xml = resp.body?.string() ?: error("Empty body")
            parse(xml)
        }
    }

    private fun parse(xml: String): FxUi {
        var usdSell: Double? = null
        var eurSell: Double? = null
        var xauSell: Double? = null
        var xauUnit: Double? = 1.0
        var asOf: String? = null

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var currentKod: String? = null
        var currentTag: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name == "Tarih_Date") {
                        asOf = parser.getAttributeValue(null, "Tarih")
                    }
                    if (name == "Currency") {
                        currentKod = parser.getAttributeValue(null, "Kod") // USD, EUR, XAU olabilir
                    } else if (name == "ForexSelling" || name == "Unit") {
                        currentTag = name
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrEmpty()) {
                        when {
                            currentKod == "USD" && currentTag == "ForexSelling" -> usdSell = text.replace(",", ".").toDoubleOrNull()
                            currentKod == "EUR" && currentTag == "ForexSelling" -> eurSell = text.replace(",", ".").toDoubleOrNull()
                            currentKod == "XAU" && currentTag == "ForexSelling" -> xauSell = text.replace(",", ".").toDoubleOrNull()
                            currentKod == "XAU" && currentTag == "Unit" -> xauUnit = text.replace(",", ".").toDoubleOrNull()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "Currency") currentKod = null
                    if (parser.name == "ForexSelling" || parser.name == "Unit") currentTag = null
                }
            }
            parser.next()
        }

        // Gram altın TL yaklaşık:
        // Heuristik: XAU ForexSelling değeri büyükse TL/ONS gibi davranıp 31.1035'e böleriz.
        val gramGoldTry = run {
            val xau = xauSell
            if (xau == null) null
            else {
                val perUnit = xau / (xauUnit ?: 1.0)
                val tlPerOunce = if (perUnit > 500) perUnit else null
                val tlPerGram = tlPerOunce?.div(31.1035)
                tlPerGram?.let { ((it * 100.0).roundToInt() / 100.0).toString().replace(".", ",") }
            }
        }

        return FxUi(
            usd = usdSell?.let { fmt2(it) },
            eur = eurSell?.let { fmt2(it) },
            gramGoldTry = gramGoldTry,
            asOfDate = asOf
        )
    }

    private fun fmt2(v: Double): String {
        val r = ((v * 100.0).roundToInt() / 100.0).toString()
        return r.replace(".", ",")
    }
}
