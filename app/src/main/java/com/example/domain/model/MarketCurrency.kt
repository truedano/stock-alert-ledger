package com.example.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.serialization.Serializable

@Serializable
enum class MarketCurrency(
    val code: String,
    val prefix: String,
    val marketTag: String,
) {
    TWD(code = "TWD", prefix = "NT$", marketTag = "台股"),
    USD(code = "USD", prefix = "US$", marketTag = "美股");

    companion object {
        fun fromCurrencyCode(raw: String?): MarketCurrency {
            return when (raw?.trim()?.uppercase(Locale.US)) {
                "TWD" -> TWD
                else -> USD
            }
        }
    }
}

object Formatters {
    private val symbols = DecimalFormatSymbols(Locale.US)

    private val twdAmountFormat = DecimalFormat("#,##0", symbols)
    private val twdPriceFormat = DecimalFormat("#,##0.##", symbols)
    private val usdFormat = DecimalFormat("#,##0.00", symbols)
    private val sharesIntFormat = DecimalFormat("#,##0", symbols)
    private val sharesDecimalFormat = DecimalFormat("#,##0.##", symbols)
    private val sharesFineDecimalFormat = DecimalFormat("#,##0.####", symbols)
    private val percentFormat = DecimalFormat("0.00", symbols)

    fun formatAmount(amount: Double, currency: MarketCurrency): String {
        val safe = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        return when (currency) {
            MarketCurrency.TWD -> "NT$ ${twdAmountFormat.format(safe.roundToLong())}"
            MarketCurrency.USD -> "US$ ${usdFormat.format(safe)}"
        }
    }

    fun formatPrice(price: Double, currency: MarketCurrency): String {
        val safe = if (price.isNaN() || price.isInfinite()) 0.0 else price
        return when (currency) {
            MarketCurrency.TWD -> "NT$ ${twdPriceFormat.format(safe)}"
            MarketCurrency.USD -> "US$ ${usdFormat.format(safe)}"
        }
    }

    fun formatPrice(price: Double): String {
        val safe = if (price.isNaN() || price.isInfinite()) 0.0 else price
        return if (safe % 1.0 == 0.0) safe.toLong().toString()
        else String.format(Locale.US, "%.2f", safe)
    }

    fun formatRawPriceForInput(price: Double, currency: MarketCurrency): String {
        val safe = if (price.isNaN() || price.isInfinite()) 0.0 else price
        return when (currency) {
            MarketCurrency.TWD -> {
                if (abs(safe - safe.roundToLong()) < 1e-4) {
                    safe.roundToLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", safe)
                }
            }
            MarketCurrency.USD -> String.format(Locale.US, "%.2f", safe)
        }
    }

    fun formatShares(shares: Double): String {
        val safe = if (shares.isNaN() || shares.isInfinite() || shares < 0.0) 0.0 else shares
        val rounded = safe.roundToLong()
        return when {
            abs(safe - rounded) < 1e-4 -> sharesIntFormat.format(rounded)
            safe < 1.0 -> sharesFineDecimalFormat.format(safe)
            else -> sharesDecimalFormat.format(safe)
        }
    }

    fun formatSignedPercent(percent: Double): String {
        val safe = if (percent.isNaN() || percent.isInfinite()) 0.0 else percent
        val sign = if (safe > 0.0001) "+" else if (safe < -0.0001) "-" else ""
        return "$sign${percentFormat.format(abs(safe))}%"
    }

    fun formatDateYmd(timestampMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
        return sdf.format(Date(timestampMillis))
    }

    fun formatDateKey(timestampMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timestampMillis))
    }

    fun formatDateTime(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "尚未更新"
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.TAIWAN)
        return sdf.format(Date(timestampMillis))
    }
}
