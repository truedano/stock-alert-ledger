package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.MarketCurrency
import com.example.notification.QuickBuyPrefill
import com.example.notification.StockNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("股價監控與記帳", appName)
    }

    @Test
    fun `notification quick buy intent round-trips prefill details`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefill = QuickBuyPrefill(
            ruleId = 42L,
            symbol = "2330.TW",
            displaySymbol = "2330.TW",
            name = "台積電",
            currency = MarketCurrency.TWD,
            unitPrice = 1015.0,
            plannedAmount = 30000.0,
            triggerTag = "觸及 20MA",
        )
        val intent = StockNotificationHelper.buildQuickBuyIntent(context, prefill)
        val parsed = StockNotificationHelper.parseQuickBuyPrefill(context, intent)
        assertNotNull(parsed)
        assertEquals(42L, parsed!!.ruleId)
        assertEquals("2330.TW", parsed.symbol)
        assertEquals(1015.0, parsed.unitPrice, 0.001)
        assertEquals(30000.0, parsed.plannedAmount, 0.001)
        assertEquals("觸及 20MA", parsed.triggerTag)
    }

    @Test
    fun `forged or invalid external quick buy intents are rejected`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val forgedIntent = Intent(context, MainActivity::class.java).apply {
            action = StockNotificationHelper.ACTION_QUICK_BUY_FROM_NOTIFICATION
            putExtra(StockNotificationHelper.EXTRA_OPEN_QUICK_BUY, true)
            putExtra(StockNotificationHelper.EXTRA_INTERNAL_TOKEN, "forged-token")
            putExtra(StockNotificationHelper.EXTRA_SYMBOL, "2330.TW")
            putExtra(StockNotificationHelper.EXTRA_PRICE, 1000.0)
            putExtra(StockNotificationHelper.EXTRA_PLANNED_AMOUNT, 10000.0)
        }
        assertNull(StockNotificationHelper.parseQuickBuyPrefill(context, forgedIntent))

        val validIntentWithBadPrice = StockNotificationHelper.buildQuickBuyIntent(
            context,
            QuickBuyPrefill(
                ruleId = 1L,
                symbol = "AAPL",
                displaySymbol = "AAPL",
                name = "Apple",
                currency = MarketCurrency.USD,
                unitPrice = 200.0,
                plannedAmount = 1000.0,
                triggerTag = "月線 20MA",
            ),
        ).apply {
            putExtra(StockNotificationHelper.EXTRA_PRICE, Double.NaN)
        }
        assertNull(StockNotificationHelper.parseQuickBuyPrefill(context, validIntentWithBadPrice))
    }
}
