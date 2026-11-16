package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.WatchlistRuleEntity
import com.example.domain.model.ConditionEvaluationResult
import com.example.domain.model.Formatters
import com.example.domain.model.MarketCurrency
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

data class QuickBuyPrefill(
    val ruleId: Long?,
    val symbol: String,
    val displaySymbol: String,
    val name: String,
    val currency: MarketCurrency,
    val unitPrice: Double,
    val plannedAmount: Double,
    val triggerTag: String,
    val buyDate: String = Formatters.formatDateYmd(),
) {
    val estimatedShares: Double
        get() = if (unitPrice > 0.0 && plannedAmount > 0.0) plannedAmount / unitPrice else 0.0
}

object StockNotificationHelper {
    const val CHANNEL_ID = "stock_ma_buy_alerts"
    const val FOREGROUND_CHANNEL_ID = "stock_ma_foreground_service"
    const val FOREGROUND_NOTIFICATION_ID = 2001
    const val ACTION_QUICK_BUY_FROM_NOTIFICATION = "com.example.action.QUICK_BUY_FROM_NOTIFICATION"

    private const val PREFS_NOTIFICATION_GUARD = "notification_guard_prefs"
    private const val KEY_INTERNAL_TOKEN = "internal_intent_token"

    const val EXTRA_OPEN_QUICK_BUY = "extra_open_quick_buy"
    const val EXTRA_INTERNAL_TOKEN = "extra_internal_token"
    const val EXTRA_RULE_ID = "extra_rule_id"
    const val EXTRA_SYMBOL = "extra_symbol"
    const val EXTRA_DISPLAY_SYMBOL = "extra_display_symbol"
    const val EXTRA_NAME = "extra_name"
    const val EXTRA_CURRENCY = "extra_currency"
    const val EXTRA_PRICE = "extra_price"
    const val EXTRA_PLANNED_AMOUNT = "extra_planned_amount"
    const val EXTRA_TRIGGER_TAG = "extra_trigger_tag"

    private val SAFE_SYMBOL_REGEX = Regex("^[A-Z0-9.\\-^=]{1,20}$")

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

            val fgChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                context.getString(R.string.foreground_service_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.foreground_service_channel_description)
                setShowBadge(false)
            }
            manager?.createNotificationChannel(fgChannel)
        }
    }

    fun buildForegroundNotification(context: Context, statusText: String? = null): Notification {
        createNotificationChannel(context)
        val title = context.getString(R.string.foreground_service_running_title)
        val text = statusText ?: context.getString(R.string.foreground_service_running_desc)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_trend)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showBuyAlertNotification(
        context: Context,
        rule: WatchlistRuleEntity,
        evaluation: ConditionEvaluationResult,
        currentPrice: Double,
    ) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val refLabel = evaluation.targetReferenceLabel
        val refPriceStr = evaluation.targetReferencePrice?.let {
            Formatters.formatPrice(it, rule.currency)
        } ?: "--"
        val currentPriceStr = Formatters.formatPrice(currentPrice, rule.currency)
        val amountStr = Formatters.formatAmount(rule.plannedBuyAmount, rule.currency)
        val estShares = if (currentPrice > 0.0) rule.plannedBuyAmount / currentPrice else 0.0
        val sharesStr = Formatters.formatShares(estShares)

        val title = "📉 [${rule.displaySymbol} ${rule.name}] 已觸發 $refLabel！"
        val body = "現價 $currentPriceStr（$refLabel: $refPriceStr）｜預定買入 $amountStr（約 $sharesStr 股）"

        val intent = buildQuickBuyIntent(
            context = context,
            prefill = QuickBuyPrefill(
                ruleId = rule.id,
                symbol = rule.symbol,
                displaySymbol = rule.displaySymbol,
                name = rule.name,
                currency = rule.currency,
                unitPrice = currentPrice,
                plannedAmount = rule.plannedBuyAmount,
                triggerTag = evaluation.triggerTag,
            ),
        )

        val requestCode = (rule.id.toInt() and 0x7FFFFFFF).coerceAtLeast(100)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_trend)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        postNotificationSafely(context, requestCode, notification)
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val title = "📉 [2330.TW 台積電 (測試)] 已觸及 20MA！"
        val body = "現價 NT$ 1,015（20MA: NT$ 1,028）｜預定買入 NT$ 30,000（約 29.56 股）"

        val intent = buildQuickBuyIntent(
            context = context,
            prefill = QuickBuyPrefill(
                ruleId = null,
                symbol = "2330.TW",
                displaySymbol = "2330.TW",
                name = "台積電 (測試)",
                currency = MarketCurrency.TWD,
                unitPrice = 1015.0,
                plannedAmount = 30000.0,
                triggerTag = "觸及 20MA",
            ),
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            9991,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_trend)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        postNotificationSafely(context, 9991, notification)
    }

    private fun postNotificationSafely(
        context: Context,
        notificationId: Int,
        notification: Notification,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked concurrently by the system
        }
    }

    fun buildQuickBuyIntent(context: Context, prefill: QuickBuyPrefill): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_BUY_FROM_NOTIFICATION
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_QUICK_BUY, true)
            putExtra(EXTRA_INTERNAL_TOKEN, getOrCreateInternalToken(context))
            prefill.ruleId?.let { putExtra(EXTRA_RULE_ID, it) }
            putExtra(EXTRA_SYMBOL, prefill.symbol)
            putExtra(EXTRA_DISPLAY_SYMBOL, prefill.displaySymbol)
            putExtra(EXTRA_NAME, prefill.name)
            putExtra(EXTRA_CURRENCY, prefill.currency.code)
            putExtra(EXTRA_PRICE, prefill.unitPrice)
            putExtra(EXTRA_PLANNED_AMOUNT, prefill.plannedAmount)
            putExtra(EXTRA_TRIGGER_TAG, prefill.triggerTag)
        }
    }

    fun parseQuickBuyPrefill(context: Context, intent: Intent?): QuickBuyPrefill? {
        if (intent == null) return null
        if (intent.action != ACTION_QUICK_BUY_FROM_NOTIFICATION) return null
        if (!intent.getBooleanExtra(EXTRA_OPEN_QUICK_BUY, false)) return null

        val providedToken = intent.getStringExtra(EXTRA_INTERNAL_TOKEN) ?: return null
        val expectedToken = getOrCreateInternalToken(context)
        val isTokenValid = MessageDigest.isEqual(
            providedToken.toByteArray(StandardCharsets.UTF_8),
            expectedToken.toByteArray(StandardCharsets.UTF_8),
        )
        if (!isTokenValid) return null

        val rawSymbol = intent.getStringExtra(EXTRA_SYMBOL)?.trim()?.uppercase(Locale.US) ?: return null
        if (!SAFE_SYMBOL_REGEX.matches(rawSymbol)) return null

        val displaySymbol = intent.getStringExtra(EXTRA_DISPLAY_SYMBOL)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= 24 }
            ?: rawSymbol
        val name = intent.getStringExtra(EXTRA_NAME)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(80)
            ?: rawSymbol
        val currency = MarketCurrency.fromCurrencyCode(intent.getStringExtra(EXTRA_CURRENCY))

        val price = intent.getDoubleExtra(EXTRA_PRICE, Double.NaN)
        if (!price.isFinite() || price <= 0.0 || price > 1_000_000_000.0) return null

        val plannedAmount = intent.getDoubleExtra(EXTRA_PLANNED_AMOUNT, 0.0)
        if (!plannedAmount.isFinite() || plannedAmount < 0.0 || plannedAmount > 1_000_000_000_000.0) {
            return null
        }

        val triggerTag = intent.getStringExtra(EXTRA_TRIGGER_TAG)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(40)
            ?: "觸發條件"
        val ruleIdRaw = intent.getLongExtra(EXTRA_RULE_ID, -1L)

        return QuickBuyPrefill(
            ruleId = if (ruleIdRaw > 0L) ruleIdRaw else null,
            symbol = rawSymbol,
            displaySymbol = displaySymbol,
            name = name,
            currency = currency,
            unitPrice = price,
            plannedAmount = plannedAmount,
            triggerTag = triggerTag,
        )
    }

    private fun getOrCreateInternalToken(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(
            PREFS_NOTIFICATION_GUARD,
            Context.MODE_PRIVATE,
        )
        val existing = prefs.getString(KEY_INTERNAL_TOKEN, null)
        if (!existing.isNullOrBlank()) return existing

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val generated = Base64.encodeToString(randomBytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_INTERNAL_TOKEN, generated).apply()
        return generated
    }
}
