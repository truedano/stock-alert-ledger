package com.example.data.remote

import java.nio.charset.Charset
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaifexFuturesServiceTest {

    private val service = TaifexFuturesService()

    private fun buildClient(
        handler: (url: String, bodyText: String) -> Response,
    ): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val req = chain.request()
            val buf = Buffer()
            req.body?.writeTo(buf)
            handler(req.url.toString(), buf.readUtf8())
        }
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    private fun ms950CsvResponse(request: okhttp3.Request, csv: String): Response {
        val bytes = csv.toByteArray(Charset.forName("MS950"))
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(bytes.toResponseBody("text/html; charset=MS950".toMediaType()))
            .build()
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

    private fun htmlResponse(request: okhttp3.Request, html: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(html.toResponseBody("text/html; charset=UTF-8".toMediaType()))
            .build()
    }

    @Test
    fun `parses TAIFEX institutional net OI and computes MTX TMF retail ratios`() {
        val instJson = """
            [
              {"Date": "20261120", "ContractCode": "臺股期貨", "Item": "外資及陸資", "TradingVolume(Net)": "1310", "OpenInterest(Net)": "-97863"},
              {"Date": "20261120", "ContractCode": "臺股期貨", "Item": "投信", "TradingVolume(Net)": "-4192", "OpenInterest(Net)": "87599"},
              {"Date": "20261120", "ContractCode": "臺股期貨", "Item": "自營商", "TradingVolume(Net)": "1723", "OpenInterest(Net)": "282"},
              {"Date": "20261120", "ContractCode": "小型臺指期貨", "Item": "外資及陸資", "TradingVolume(Net)": "100", "OpenInterest(Net)": "600"},
              {"Date": "20261120", "ContractCode": "小型臺指期貨", "Item": "投信", "TradingVolume(Net)": "0", "OpenInterest(Net)": "0"},
              {"Date": "20261120", "ContractCode": "小型臺指期貨", "Item": "自營商", "TradingVolume(Net)": "-20", "OpenInterest(Net)": "-100"},
              {"Date": "20261120", "ContractCode": "微型臺指期貨", "Item": "外資及陸資", "TradingVolume(Net)": "-500", "OpenInterest(Net)": "-2000"}
            ]
        """.trimIndent()

        val dailyJson = """
            [
              {"Contract": "MTX", "ContractMonth(Week)": "202612", "TradingSession": "一般", "OpenInterest": "10000"},
              {"Contract": "MTX", "ContractMonth(Week)": "202612/202701", "TradingSession": "一般", "OpenInterest": "9999"},
              {"Contract": "MTX", "ContractMonth(Week)": "202612", "TradingSession": "盤後", "OpenInterest": "8888"},
              {"Contract": "TMF", "ContractMonth(Week)": "202612", "TradingSession": "一般", "OpenInterest": "8000"}
            ]
        """.trimIndent()

        val snap = service.parseFuturesChipData(instJson, dailyJson)
        assertNotNull(snap)
        snap!!
        assertEquals("20261120", snap.date)
        assertEquals("2026/11/20", snap.formattedDate)
        assertEquals("11/20", snap.shortDateLabel)
        assertNull(snap.foreignTxOiChange)
        assertEquals(-97863L, snap.foreignTxNetOi)
        assertEquals(1310L, snap.foreignTxTradingNet)
        assertEquals(87599L, snap.trustTxNetOi)
        assertEquals(282L, snap.dealerTxNetOi)

        // MTX: inst net = 600 + 0 - 100 = 500 -> retail net = -500 / 10000 = -5.0%
        assertEquals(-500L, snap.mtxRetailNetOi)
        assertEquals(10000L, snap.mtxTotalOi)
        assertEquals(-5.0, snap.mtxRetailRatio!!, 0.001)

        // TMF: inst net = -2000 -> retail net = +2000 / 8000 = +25.0%
        assertEquals(2000L, snap.tmfRetailNetOi)
        assertEquals(8000L, snap.tmfTotalOi)
        assertEquals(25.0, snap.tmfRetailRatio!!, 0.001)
    }

    @Test
    fun `parses TAIFEX official CSV across two trading days and computes exact OI daily change and retail ratios`() {
        val mtxCsv = """
            交易日期,契約,到期月份(週別),開盤價,最高價,最低價,收盤價,漲跌價,漲跌%,成交量,結算價,未沖銷契約數,最後最佳買價,最後最佳賣價,歷史最高價,歷史最低價,是否因訊息面暫停交易,交易時段,價差對單式委託成交量
            2026/11/20,MTX,202612  ,35000,35200,34800,35100,100,0.3%,1000,35100,30000,35099,35101,40000,30000,,一般,,
            2026/11/23,MTX,202612  ,34968,37118,34833,36581,781,2.18%,233922,36659,28293,36576,36580,50591,27401,,一般,,
            2026/11/23,MTX,202612  ,35918,36398,33716,33955,-1845,-5.15%,275374,-,-,33937,33959,50591,27401,,盤後,,
            2026/11/23,MTX,202701  ,34868,37200,34868,36632,699,1.95%,3477,36786,3561,36656,36666,47750,33801,,一般,,
            2026/11/23,MTX,202612/202701,10,10,10,10,0,0%,1,-,9999,10,11,10,10,,一般,,
        """.trimIndent()

        val tmfCsv = """
            交易日期,契約,到期月份(週別),開盤價,最高價,最低價,收盤價,漲跌價,漲跌%,成交量,結算價,未沖銷契約數,最後最佳買價,最後最佳賣價,歷史最高價,歷史最低價,是否因訊息面暫停交易,交易時段,價差對單式委託成交量
            2026/11/20,TMF,202612  ,35000,35200,34800,35100,100,0.3%,1000,35100,70000,35099,35101,40000,30000,,一般,,
            2026/11/23,TMF,202612  ,34962,37115,34800,36582,782,2.18%,438553,36659,68471,36574,36582,50592,27406,,一般,,
            2026/11/23,TMF,202701  ,34800,37195,34800,36656,723,2.01%,16265,36786,12745,36654,36669,47858,33799,,一般,,
        """.trimIndent()

        val instCsv = """
            日期,商品名稱,身份別,多方交易口數,多方交易契約金額(千元),空方交易口數,空方交易契約金額(千元),多空交易口數淨額,多空交易契約金額淨額(千元),多方未平倉口數,多方未平倉契約金額(千元),空方未平倉口數,空方未平倉契約金額(千元),多空未平倉口數淨額,多空未平倉契約金額淨額(千元)
            2026/11/20,臺股期貨,外資及陸資,118873,0,117563,0,1310,0,16303,0,114166,0,-97863,0
            2026/11/23,臺股期貨,自營商,9555,0,8487,0,1068,0,4472,0,3154,0,1318,0
            2026/11/23,臺股期貨,投信,5983,0,17,0,5966,0,97312,0,3747,0,93565,0
            2026/11/23,臺股期貨,外資及陸資,135560,0,141139,0,-5579,0,14599,0,118031,0,-103432,0
            2026/11/23,小型臺指期貨,自營商,0,0,0,0,-957,0,0,0,0,0,-1959,0
            2026/11/23,小型臺指期貨,投信,0,0,0,0,-8,0,0,0,0,0,-78,0
            2026/11/23,小型臺指期貨,外資及陸資,0,0,0,0,3775,0,0,0,0,0,5745,0
            2026/11/23,微型臺指期貨,自營商,0,0,0,0,2767,0,0,0,0,0,-35806,0
            2026/11/23,微型臺指期貨,投信,0,0,0,0,0,0,0,0,0,0,0,0
            2026/11/23,微型臺指期貨,外資及陸資,0,0,0,0,12368,0,0,0,0,0,2524,0
        """.trimIndent()

        val mtxByDate = service.parseDailyFutCsvTotalOiByDate(mtxCsv, "MTX")
        val tmfByDate = service.parseDailyFutCsvTotalOiByDate(tmfCsv, "TMF")
        assertEquals(30000L, mtxByDate["2026/11/20"])
        assertEquals(31854L, mtxByDate["2026/11/23"])
        assertEquals(81216L, tmfByDate["2026/11/23"])

        val snap = service.parseOfficialCsvData(
            instCsv = instCsv,
            mtxTotalOiByDate = mtxByDate,
            tmfTotalOiByDate = tmfByDate,
            targetDate = "2026/11/23",
            prevDate = "2026/11/20",
        )
        assertNotNull(snap)
        snap!!
        assertEquals("2026/11/23", snap.date)
        assertEquals("11/23", snap.shortDateLabel)
        assertEquals("2026/11/23", snap.formattedDate)
        assertEquals(-103432L, snap.foreignTxNetOi)
        assertEquals(-5569L, snap.foreignTxOiChange)
        assertEquals(-5579L, snap.foreignTxTradingNet)
        assertEquals(93565L, snap.trustTxNetOi)
        assertEquals(1318L, snap.dealerTxNetOi)

        // MTX inst net = -1959 - 78 + 5745 = 3708 -> retail net = -3708 / 31854 = -11.64%
        assertEquals(-3708L, snap.mtxRetailNetOi)
        assertEquals(31854L, snap.mtxTotalOi)
        assertEquals(-11.6406, snap.mtxRetailRatio!!, 0.001)

        // TMF inst net = -35806 + 0 + 2524 = -33282 -> retail net = 33282 / 81216 = +40.98%
        assertEquals(33282L, snap.tmfRetailNetOi)
        assertEquals(81216L, snap.tmfTotalOi)
        assertEquals(40.9796, snap.tmfRetailRatio!!, 0.001)
    }

    @Test
    fun `fetchFuturesChipSnapshot uses official MS950 CSV first and respects 30-minute cache unless forceRefresh`() = runTest {
        val mtxCsv = """
            交易日期,契約,到期月份(週別),開盤價,最高價,最低價,收盤價,漲跌價,漲跌%,成交量,結算價,未沖銷契約數,最後最佳買價,最後最佳賣價,歷史最高價,歷史最低價,是否因訊息面暫停交易,交易時段,價差對單式委託成交量
            2026/11/20,MTX,202612  ,0,0,0,0,0,0%,0,0,10000,0,0,0,0,,一般,,
            2026/11/23,MTX,202612  ,0,0,0,0,0,0%,0,0,20000,0,0,0,0,,一般,,
        """.trimIndent()
        val tmfCsv = """
            交易日期,契約,到期月份(週別),開盤價,最高價,最低價,收盤價,漲跌價,漲跌%,成交量,結算價,未沖銷契約數,最後最佳買價,最後最佳賣價,歷史最高價,歷史最低價,是否因訊息面暫停交易,交易時段,價差對單式委託成交量
            2026/11/20,TMF,202612  ,0,0,0,0,0,0%,0,0,40000,0,0,0,0,,一般,,
            2026/11/23,TMF,202612  ,0,0,0,0,0,0%,0,0,50000,0,0,0,0,,一般,,
        """.trimIndent()
        val instCsv = """
            日期,商品名稱,身份別,多方交易口數,多方交易契約金額(千元),空方交易口數,空方交易契約金額(千元),多空交易口數淨額,多空交易契約金額淨額(千元),多方未平倉口數,多方未平倉契約金額(千元),空方未平倉口數,空方未平倉契約金額(千元),多空未平倉口數淨額,多空未平倉契約金額淨額(千元)
            2026/11/20,臺股期貨,外資及陸資,0,0,0,0,0,0,0,0,0,0,-90000,0
            2026/11/23,臺股期貨,外資及陸資,0,0,0,0,-1200,0,0,0,0,0,-92000,0
            2026/11/23,小型臺指期貨,外資及陸資,0,0,0,0,0,0,0,0,0,0,2000,0
            2026/11/23,微型臺指期貨,外資及陸資,0,0,0,0,0,0,0,0,0,0,-5000,0
        """.trimIndent()

        var requestCount = 0
        val client = buildClient { url, bodyText ->
            requestCount += 1
            val req = okhttp3.Request.Builder().url(url).build()
            when {
                url.endsWith("futDataDown") && bodyText.contains("commodity_id=MTX") -> ms950CsvResponse(req, mtxCsv)
                url.endsWith("futDataDown") && bodyText.contains("commodity_id=TMF") -> ms950CsvResponse(req, tmfCsv)
                url.endsWith("futContractsDateDown") -> ms950CsvResponse(req, instCsv)
                else -> jsonResponse(req, 500, "{}")
            }
        }

        var nowMs = 1_795_400_000_000L
        val timedService = TaifexFuturesService(
            client = client,
            delayFn = {},
            clockMs = { nowMs },
        )

        val first = timedService.fetchFuturesChipSnapshot(forceRefresh = false)
        assertNotNull(first)
        assertEquals(3, requestCount)
        assertEquals(-92000L, first!!.foreignTxNetOi)
        assertEquals(-2000L, first.foreignTxOiChange)

        // 10 minutes later: should hit 30-minute memory cache
        nowMs += 10 * 60 * 1000L
        val cached = timedService.fetchFuturesChipSnapshot(forceRefresh = false)
        assertEquals(3, requestCount)
        assertEquals(-92000L, cached!!.foreignTxNetOi)

        // forceRefresh bypasses cache
        timedService.fetchFuturesChipSnapshot(forceRefresh = true)
        assertEquals(6, requestCount)

        // 31 minutes later: cache expires automatically
        nowMs += 31 * 60 * 1000L
        timedService.fetchFuturesChipSnapshot(forceRefresh = false)
        assertEquals(9, requestCount)
    }

    @Test
    fun `steps back to previous trading date when latest date institutional CSV is not yet published`() = runTest {
        val mtxCsv = """
            交易日期,契約,到期月份(週別),開盤價,最高價,最低價,收盤價,漲跌價,漲跌%,成交量,結算價,未沖銷契約數,最後最佳買價,最後最佳賣價,歷史最高價,歷史最低價,是否因訊息面暫停交易,交易時段,價差對單式委託成交量
            2026/11/19,MTX,202612  ,0,0,0,0,0,0%,0,0,10000,0,0,0,0,,一般,,
            2026/11/20,MTX,202612  ,0,0,0,0,0,0%,0,0,20000,0,0,0,0,,一般,,
            2026/11/23,MTX,202612  ,0,0,0,0,0,0%,0,0,30000,0,0,0,0,,一般,,
        """.trimIndent()
        val tmfCsv = """
            交易日期,契約,到期月份(週別),開盤價,最高價,最低價,收盤價,漲跌價,漲跌%,成交量,結算價,未沖銷契約數,最後最佳買價,最後最佳賣價,歷史最高價,歷史最低價,是否因訊息面暫停交易,交易時段,價差對單式委託成交量
            2026/11/19,TMF,202612  ,0,0,0,0,0,0%,0,0,40000,0,0,0,0,,一般,,
            2026/11/20,TMF,202612  ,0,0,0,0,0,0%,0,0,50000,0,0,0,0,,一般,,
            2026/11/23,TMF,202612  ,0,0,0,0,0,0%,0,0,60000,0,0,0,0,,一般,,
        """.trimIndent()
        val instCsvPrev = """
            日期,商品名稱,身份別,多方交易口數,多方交易契約金額(千元),空方交易口數,空方交易契約金額(千元),多空交易口數淨額,多空交易契約金額淨額(千元),多方未平倉口數,多方未平倉契約金額(千元),空方未平倉口數,空方未平倉契約金額(千元),多空未平倉口數淨額,多空未平倉契約金額淨額(千元)
            2026/11/19,臺股期貨,外資及陸資,0,0,0,0,0,0,0,0,0,0,-80000,0
            2026/11/20,臺股期貨,外資及陸資,0,0,0,0,500,0,0,0,0,0,-85000,0
            2026/11/20,小型臺指期貨,外資及陸資,0,0,0,0,0,0,0,0,0,0,1000,0
            2026/11/20,微型臺指期貨,外資及陸資,0,0,0,0,0,0,0,0,0,0,-2500,0
        """.trimIndent()

        val client = buildClient { url, bodyText ->
            val req = okhttp3.Request.Builder().url(url).build()
            when {
                url.endsWith("futDataDown") && bodyText.contains("commodity_id=MTX") -> ms950CsvResponse(req, mtxCsv)
                url.endsWith("futDataDown") && bodyText.contains("commodity_id=TMF") -> ms950CsvResponse(req, tmfCsv)
                url.endsWith("futContractsDateDown") && bodyText.contains("queryEndDate=2026%2F11%2F23") ->
                    htmlResponse(req, "<!DOCTYPE HTML><html><script>alert('DateTime error');</script></html>")
                url.endsWith("futContractsDateDown") && bodyText.contains("queryEndDate=2026%2F11%2F20") ->
                    ms950CsvResponse(req, instCsvPrev)
                else -> jsonResponse(req, 500, "{}")
            }
        }

        val testService = TaifexFuturesService(client = client, delayFn = {})
        val snap = testService.fetchFuturesChipSnapshot()
        assertNotNull(snap)
        snap!!
        assertEquals("2026/11/20", snap.date)
        assertEquals(-85000L, snap.foreignTxNetOi)
        assertEquals(-5000L, snap.foreignTxOiChange)
        // Denominator must align with 2026/11/20 (20000 / 50000), not 2026/11/23 (30000 / 60000)
        assertEquals(20000L, snap.mtxTotalOi)
        assertEquals(50000L, snap.tmfTotalOi)
        assertEquals(-5.0, snap.mtxRetailRatio!!, 0.001)
        assertEquals(5.0, snap.tmfRetailRatio!!, 0.001)
    }

    @Test
    fun `falls back to TAIFEX OpenAPI when official website CSV fails`() = runTest {
        val instJson = """
            [
              {"Date": "20261120", "ContractCode": "臺股期貨", "Item": "外資及陸資", "TradingVolume(Net)": "1310", "OpenInterest(Net)": "-97863"},
              {"Date": "20261120", "ContractCode": "小型臺指期貨", "Item": "外資及陸資", "TradingVolume(Net)": "0", "OpenInterest(Net)": "500"},
              {"Date": "20261120", "ContractCode": "微型臺指期貨", "Item": "外資及陸資", "TradingVolume(Net)": "0", "OpenInterest(Net)": "-2000"}
            ]
        """.trimIndent()
        val dailyJson = """
            [
              {"Contract": "MTX", "ContractMonth(Week)": "202612", "TradingSession": "一般", "OpenInterest": "10000"},
              {"Contract": "TMF", "ContractMonth(Week)": "202612", "TradingSession": "一般", "OpenInterest": "8000"}
            ]
        """.trimIndent()

        val client = buildClient { url, _ ->
            val req = okhttp3.Request.Builder().url(url).build()
            when {
                url.contains("futDataDown") -> htmlResponse(req, "<!DOCTYPE HTML><html>error</html>")
                url.contains("MarketDataOfMajorInstitutionalTradersDetailsOfFuturesContractsBytheDate") ->
                    jsonResponse(req, 200, instJson)
                url.contains("DailyMarketReportFut") ->
                    jsonResponse(req, 200, dailyJson)
                else -> jsonResponse(req, 404, "{}")
            }
        }

        val testService = TaifexFuturesService(client = client, delayFn = {})
        val snap = testService.fetchFuturesChipSnapshot()
        assertNotNull(snap)
        snap!!
        assertEquals("20261120", snap.date)
        assertEquals(-97863L, snap.foreignTxNetOi)
        assertNull(snap.foreignTxOiChange)
        assertEquals(-5.0, snap.mtxRetailRatio!!, 0.001)
        assertEquals(25.0, snap.tmfRetailRatio!!, 0.001)
    }
}
