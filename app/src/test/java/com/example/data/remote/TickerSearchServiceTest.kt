package com.example.data.remote

import com.example.domain.model.MarketCurrency
import com.example.domain.model.TickerSearchCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TickerSearchServiceTest {

    private val service = TickerSearchService()

    @Test
    fun `parses twse and tpex stock lists accurately`() {
        val twseJson = """
            [
              {"Code": "2330", "Name": "台積電"},
              {"Code": "00631L", "Name": "元大台灣50正2"}
            ]
        """.trimIndent()
        val tpexJson = """
            [
              {"SecuritiesCompanyCode": "6488", "CompanyName": "環球晶"}
            ]
        """.trimIndent()

        val twse = service.parseTwseStockListJson(twseJson)
        val tpex = service.parseTpexStockListJson(tpexJson)

        assertEquals(2, twse.size)
        assertEquals("2330.TW", twse[0].symbol)
        assertEquals("台積電", twse[0].name)
        assertEquals("00631L.TW", twse[1].symbol)
        assertEquals(1, tpex.size)
        assertEquals("6488.TWO", tpex[0].symbol)
        assertEquals("環球晶", tpex[0].name)
    }

    @Test
    fun `filters and ranks chinese and numeric ticker queries`() {
        val pool = listOf(
            TickerSearchCandidate("0050.TW", "元大台灣50", MarketCurrency.TWD, "台股上市"),
            TickerSearchCandidate("00631L.TW", "元大台灣50正2", MarketCurrency.TWD, "台股上市"),
            TickerSearchCandidate("2330.TW", "台積電", MarketCurrency.TWD, "台股上市"),
            TickerSearchCandidate("6770.TW", "力積電", MarketCurrency.TWD, "台股上市"),
        )

        val byChinese = service.filterAndRankLocalCandidates("正2", pool)
        assertEquals(1, byChinese.size)
        assertEquals("00631L.TW", byChinese[0].symbol)

        val byPrefix = service.filterAndRankLocalCandidates("台積", pool)
        assertEquals(1, byPrefix.size)
        assertEquals("2330.TW", byPrefix[0].symbol)

        val byCode = service.filterAndRankLocalCandidates("0050", pool)
        assertEquals("0050.TW", byCode[0].symbol)
    }

    @Test
    fun `parses yahoo search json and excludes unsupported quote types`() {
        val yahooJson = """
            {
              "quotes": [
                {"symbol": "NVDA", "shortname": "NVIDIA Corporation", "quoteType": "EQUITY", "exchDisp": "NASDAQ"},
                {"symbol": "0050.TW", "shortname": "Yuanta Taiwan Top 50", "quoteType": "ETF", "exchDisp": "Taiwan"},
                {"symbol": "BTC-USD", "shortname": "Bitcoin USD", "quoteType": "CRYPTOCURRENCY", "exchDisp": "CCC"}
              ]
            }
        """.trimIndent()

        val parsed = service.parseYahooSearchJson(yahooJson)
        assertEquals(2, parsed.size)
        assertEquals("NVDA", parsed[0].symbol)
        assertEquals(MarketCurrency.USD, parsed[0].currency)
        assertEquals("0050.TW", parsed[1].symbol)
        assertEquals(MarketCurrency.TWD, parsed[1].currency)
        assertTrue(parsed.none { it.symbol == "BTC-USD" })
    }
}
