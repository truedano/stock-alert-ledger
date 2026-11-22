package com.example.data.remote

import com.example.domain.model.TickerLookupResult
import java.io.IOException
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class ApiRetryResilienceTest {

    private fun buildClient(handler: (attempt: Int, request: okhttp3.Request) -> Response): Pair<OkHttpClient, () -> Int> {
        var attempts = 0
        val interceptor = Interceptor { chain ->
            attempts += 1
            handler(attempts, chain.request())
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        return client to { attempts }
    }

    private fun jsonResponse(request: okhttp3.Request, code: Int, body: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }

    @Test
    fun `yahoo finance retries transient 503 with exponential backoff and succeeds`() = runTest {
        val validChartJson = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "symbol": "2330.TW",
                      "currency": "TWD",
                      "shortName": "TSMC",
                      "regularMarketPrice": 1020.0,
                      "chartPreviousClose": 1005.0
                    },
                    "timestamp": [1700000000, 1700086400],
                    "indicators": {
                      "quote": [
                        {
                          "open": [1000.0, 1010.0],
                          "high": [1015.0, 1025.0],
                          "low": [995.0, 1008.0],
                          "close": [1005.0, 1020.0],
                          "volume": [10000, 12000]
                        }
                      ]
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val (client, getAttempts) = buildClient { attempt, request ->
            if (attempt < 3) {
                jsonResponse(request, 503, "{}")
            } else {
                jsonResponse(request, 200, validChartJson)
            }
        }
        val recordedDelays = mutableListOf<Long>()
        val service = YahooFinanceService(client = client, delayFn = { recordedDelays.add(it) })

        val result = service.fetchExactSymbol("2330.TW")

        assertTrue(result is TickerLookupResult.Success)
        assertEquals(3, getAttempts())
        assertEquals(listOf(400L, 800L), recordedDelays)
        assertEquals(1020.0, (result as TickerLookupResult.Success).quote.currentPrice, 0.001)
    }

    @Test
    fun `yahoo finance retries IOException and returns error when exhausted`() = runTest {
        val (client, getAttempts) = buildClient { _, _ ->
            throw IOException("Simulated network timeout")
        }
        val recordedDelays = mutableListOf<Long>()
        val service = YahooFinanceService(client = client, delayFn = { recordedDelays.add(it) })

        val result = service.fetchExactSymbol("AAPL")

        assertTrue(result is TickerLookupResult.Error)
        assertEquals(3, getAttempts())
        assertEquals(listOf(400L, 800L), recordedDelays)
    }

    @Test
    fun `yahoo finance does not retry on HTTP 429 or 404`() = runTest {
        val (client429, attempts429) = buildClient { _, request -> jsonResponse(request, 429, "{}") }
        val delays429 = mutableListOf<Long>()
        val res429 = YahooFinanceService(client429) { delays429.add(it) }.fetchExactSymbol("AAPL")
        assertTrue(res429 is TickerLookupResult.RateLimited)
        assertEquals(1, attempts429())
        assertTrue(delays429.isEmpty())

        val (client404, attempts404) = buildClient { _, request -> jsonResponse(request, 404, "{}") }
        val delays404 = mutableListOf<Long>()
        val res404 = YahooFinanceService(client404) { delays404.add(it) }.fetchExactSymbol("INVALID")
        assertTrue(res404 is TickerLookupResult.NotFound)
        assertEquals(1, attempts404())
        assertTrue(delays404.isEmpty())
    }

    @Test
    fun `twse margin service retries transient 502 and parses snapshot`() = runTest {
        val twseJson = """
            {
              "stat": "OK",
              "data": [
                {"date": "113/11/20", "keepRate": 162.5, "callAccNum": 100, "exeAccNum": 20},
                {"date": "113/11/21", "keepRate": 164.2, "callAccNum": 80, "exeAccNum": 15}
              ]
            }
        """.trimIndent()

        val (client, getAttempts) = buildClient { attempt, request ->
            if (attempt == 1) {
                jsonResponse(request, 502, "")
            } else {
                jsonResponse(request, 200, twseJson)
            }
        }
        val recordedDelays = mutableListOf<Long>()
        val service = TwseMarginService(client = client, delayFn = { recordedDelays.add(it) })

        val snapshot = service.fetchMarketMarginSnapshot()

        assertNotNull(snapshot)
        assertEquals(2, getAttempts())
        assertEquals(listOf(400L), recordedDelays)
        assertEquals(164.2, snapshot!!.keepRate, 0.001)
    }
}
