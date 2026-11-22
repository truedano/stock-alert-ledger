package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TaifexFuturesServiceTest {

    private val service = TaifexFuturesService()

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
}
