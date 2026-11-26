package com.example.data.remote

import com.example.domain.model.ValuationCalculator
import com.example.domain.model.ValuationMarket
import com.example.domain.model.ValuationMethod
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValuationRemoteServiceTest {

    private fun jsonResponse(request: okhttp3.Request, code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }

    private fun textResponse(request: okhttp3.Request, code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .body(body.toResponseBody("text/plain; charset=utf-8".toMediaType()))
            .build()
    }

    @Test
    fun `fetches screener, enriches TW with BWIBBU_ALL, computes 5Y bands and caches results`() = runTest {
        val twScreenerJson = """
            {
              "finance": {
                "result": [{
                  "quotes": [
                    {
                      "symbol": "2330.TW",
                      "shortName": "TAIWAN SEMICONDUCTOR",
                      "marketCap": 57829176115200,
                      "regularMarketPrice": 2230.0,
                      "regularMarketChangePercent": 1.8,
                      "trailingPE": 22.88,
                      "forwardPE": 14.91,
                      "epsTrailingTwelveMonths": 97.47,
                      "epsForward": 149.60,
                      "priceToBook": 8.26,
                      "bookValue": 269.85
                    },
                    {
                      "symbol": "2881.TW",
                      "shortName": "FUBON FINANCIAL",
                      "marketCap": 1961031106560,
                      "regularMarketPrice": 143.5,
                      "regularMarketChangePercent": -0.5,
                      "forwardPE": 13.85,
                      "epsForward": 10.36
                    }
                  ]
                }]
              }
            }
        """.trimIndent()

        val usScreenerJson = """
            {
              "finance": {
                "result": [{
                  "quotes": [
                    {
                      "symbol": "AAPL",
                      "shortName": "Apple Inc.",
                      "marketCap": 5072893771776,
                      "regularMarketPrice": 349.08,
                      "trailingPE": 38.87,
                      "forwardPE": 33.01,
                      "epsTrailingTwelveMonths": 8.98,
                      "epsForward": 10.57,
                      "priceToBook": 45.25,
                      "bookValue": 7.71
                    },
                    {
                      "symbol": "BRK-B",
                      "shortName": "Berkshire Hathaway Inc. New",
                      "marketCap": 1215989809152,
                      "regularMarketPrice": 572.36,
                      "trailingPE": 16.02,
                      "forwardPE": 25.09,
                      "epsTrailingTwelveMonths": 35.73,
                      "epsForward": 22.82,
                      "priceToBook": 0.001067,
                      "bookValue": 536413.0
                    }
                  ]
                }]
              }
            }
        """.trimIndent()

        val bwibbuJson = """
            [
              {"Date":"1151123","Code":"2330","Name":"台積電","PEratio":"23.10","DividendYield":"1.25","PBratio":"8.30"},
              {"Date":"1151123","Code":"2881","Name":"富邦金","PEratio":"17.90","DividendYield":"3.45","PBratio":"1.60"}
            ]
        """.trimIndent()

        val timeseriesJson = """
            {
              "timeseries": {
                "result": [
                  {
                    "meta": {"type": ["trailingPeRatio"]},
                    "trailingPeRatio": [
                      {"reportedValue": {"raw": 16.0}},
                      {"reportedValue": {"raw": 18.0}},
                      {"reportedValue": {"raw": 21.0}},
                      {"reportedValue": {"raw": 24.0}},
                      {"reportedValue": {"raw": 28.0}}
                    ]
                  },
                  {
                    "meta": {"type": ["trailingPbRatio"]},
                    "trailingPbRatio": [
                      {"reportedValue": {"raw": 1.20}},
                      {"reportedValue": {"raw": 1.35}},
                      {"reportedValue": {"raw": 1.50}},
                      {"reportedValue": {"raw": 1.70}},
                      {"reportedValue": {"raw": 1.95}}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val screenerCalls = java.util.concurrent.atomic.AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when {
                    url.contains("fc.yahoo.com") -> textResponse(req, 200, "ok")
                    url.contains("getcrumb") -> textResponse(req, 200, "testCrumb")
                    url.contains("BWIBBU_ALL") -> jsonResponse(req, 200, bwibbuJson)
                    url.contains("screener") -> {
                        screenerCalls.incrementAndGet()
                        val buf = okio.Buffer()
                        req.body?.writeTo(buf)
                        val bodyStr = buf.readUtf8()
                        if (bodyStr.contains("\"tw\"")) {
                            jsonResponse(req, 200, twScreenerJson)
                        } else {
                            jsonResponse(req, 200, usScreenerJson)
                        }
                    }
                    url.contains("fundamentals-timeseries") -> jsonResponse(req, 200, timeseriesJson)
                    else -> jsonResponse(req, 404, "{}")
                }
            })
            .build()

        var nowMs = 1_795_400_000_000L
        val service = ValuationRemoteService(
            context = null,
            client = client,
            delayFn = {},
            clockMs = { nowMs },
        )

        val snap = service.fetchValuationRadar(forceRefresh = false)
        assertNotNull(snap)
        snap!!
        assertEquals(2, snap.twTop10.size)
        assertEquals(2, snap.usTop10.size)
        assertEquals(2, screenerCalls.get())

        val fubon = snap.twTop10.first { it.symbol == "2881.TW" }
        assertEquals("富邦金", fubon.name)
        assertEquals(17.90, fubon.trailingPe!!, 0.01)
        assertEquals(1.60, fubon.priceToBook!!, 0.01)
        assertEquals(3.45, fubon.dividendYieldPercent!!, 0.01)
        assertEquals(ValuationMethod.PB_PRIORITY, fubon.primaryMethod)
        assertTrue(fubon.pbBand.isAutoCalibrated)

        val brk = snap.usTop10.first { it.symbol == "BRK-B" }
        assertEquals(ValuationMethod.PB_PRIORITY, brk.primaryMethod)
        assertEquals(1.60, brk.priceToBook!!, 0.02)

        // Memory cache hit within 30 minutes
        nowMs += 5 * 60 * 1000L
        val cached = service.fetchValuationRadar(forceRefresh = false)
        assertNotNull(cached)
        assertTrue(cached!!.isFromCache)
        assertEquals(2, screenerCalls.get())
    }

    @Test
    fun `falls back gracefully to v7 quote batch when screener fails`() = runTest {
        val fallbackQuoteJson = """
            {
              "quoteResponse": {
                "result": [
                  {
                    "symbol": "2330.TW",
                    "shortName": "TSMC",
                    "marketCap": 57829176115200,
                    "regularMarketPrice": 2230.0,
                    "trailingPE": 22.88,
                    "forwardPE": 14.91,
                    "epsTrailingTwelveMonths": 97.47,
                    "epsForward": 149.60
                  },
                  {
                    "symbol": "AAPL",
                    "shortName": "Apple Inc.",
                    "marketCap": 5072893771776,
                    "regularMarketPrice": 349.08,
                    "trailingPE": 38.87,
                    "forwardPE": 33.01,
                    "epsTrailingTwelveMonths": 8.98,
                    "epsForward": 10.57
                  }
                ]
              }
            }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when {
                    url.contains("fc.yahoo.com") -> textResponse(req, 200, "ok")
                    url.contains("getcrumb") -> textResponse(req, 200, "testCrumb")
                    url.contains("screener") -> jsonResponse(req, 500, "{}")
                    url.contains("v7/finance/quote") -> jsonResponse(req, 200, fallbackQuoteJson)
                    url.contains("BWIBBU_ALL") -> jsonResponse(req, 200, "[]")
                    else -> jsonResponse(req, 404, "{}")
                }
            })
            .build()

        val service = ValuationRemoteService(
            context = null,
            client = client,
            delayFn = {},
        )

        val snap = service.fetchValuationRadar(forceRefresh = true)
        assertNotNull(snap)
        snap!!
        assertEquals(1, snap.twTop10.size)
        assertEquals("2330.TW", snap.twTop10.first().symbol)
        assertEquals(1, snap.usTop10.size)
        assertEquals("AAPL", snap.usTop10.first().symbol)
    }

    @Test
    fun `fetchCustomValuations filters indices, enriches TW stocks, and reuses cached historical bands`() = runTest {
        var timeseriesCalls = 0
        val batchQuoteJson = """
            {
              "quoteResponse": {
                "result": [
                  {
                    "symbol": "2303.TW",
                    "shortName": "UMC",
                    "marketCap": 600000000000,
                    "regularMarketPrice": 48.5
                  },
                  {
                    "symbol": "AMD",
                    "shortName": "Advanced Micro Devices",
                    "marketCap": 250000000000,
                    "regularMarketPrice": 150.0,
                    "forwardPE": 25.0,
                    "epsForward": 6.0
                  }
                ]
              }
            }
        """.trimIndent()
        val bwibbuJson = """
            [
              {"Date":"1151123","Code":"2303","Name":"聯電","PEratio":"12.50","DividendYield":"5.80","PBratio":"1.65"}
            ]
        """.trimIndent()
        val timeseriesJson = """
            {
              "timeseries": {
                "result": [
                  {
                    "trailingPeRatio": [
                      {"reportedValue": {"raw": 10.0}},
                      {"reportedValue": {"raw": 12.0}},
                      {"reportedValue": {"raw": 14.0}},
                      {"reportedValue": {"raw": 16.0}}
                    ],
                    "trailingPbRatio": [
                      {"reportedValue": {"raw": 1.2}},
                      {"reportedValue": {"raw": 1.5}},
                      {"reportedValue": {"raw": 1.8}},
                      {"reportedValue": {"raw": 2.1}}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when {
                    url.contains("fc.yahoo.com") -> textResponse(req, 200, "ok")
                    url.contains("getcrumb") -> textResponse(req, 200, "testCrumb")
                    url.contains("v7/finance/quote") -> jsonResponse(req, 200, batchQuoteJson)
                    url.contains("BWIBBU_ALL") -> jsonResponse(req, 200, bwibbuJson)
                    url.contains("fundamentals-timeseries") -> {
                        timeseriesCalls++
                        jsonResponse(req, 200, timeseriesJson)
                    }
                    else -> jsonResponse(req, 404, "{}")
                }
            })
            .build()

        val service = ValuationRemoteService(
            context = null,
            client = client,
            delayFn = {},
        )

        val first = service.fetchCustomValuations(listOf("2303.TW", "AMD", "^TWII"))
        assertEquals(2, first.size)
        assertEquals("聯電", first[0].name)
        assertEquals(ValuationMarket.CUSTOM, first[0].market)
        assertEquals(com.example.domain.model.MarketCurrency.TWD, first[0].currency)
        assertEquals(12.5, first[0].trailingPe!!, 0.01)
        assertEquals(com.example.domain.model.MarketCurrency.USD, first[1].currency)
        assertEquals(2, timeseriesCalls)

        // Second call without forceRefresh should reuse cached 5Y bands
        val second = service.fetchCustomValuations(listOf("2303.TW", "AMD"), forceRefresh = false)
        assertEquals(2, second.size)
        assertEquals(2, timeseriesCalls)
    }
}
