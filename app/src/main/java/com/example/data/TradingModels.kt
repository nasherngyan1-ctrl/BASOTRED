package com.example.data

import android.graphics.Bitmap
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

enum class SignalAction {
    BUY,  // کڕین (Long)
    SELL  // فرۆشتن (Short)
}

data class OkxCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
}

data class TradingSignal(
    val symbol: String,               // e.g. "SOLUSDT"
    val instId: String,               // e.g. "SOL-USDT" or "DOOD-USDT-SWAP"
    val action: SignalAction,         // BUY or SELL
    val currentPrice: Double,         // Live OKX last price
    val previousPrice: Double = currentPrice,
    val targetPrice: Double,          // Realistic ATR / Swing target
    val stopLoss: Double,             // Realistic ATR stop loss
    val priceDecimals: Int,           // Exact decimal places from OKX tick size
    val rsi: Double,                  // Wilder's RSI(14)
    val ema9: Double,
    val ema21: Double,
    val confidence: Int,
    val change24hPercent: Double,
    val coinDescriptionKurdish: String,
    val candles: List<OkxCandle>,
    val isLiveFromOkx: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val expectedMoveRatio: Double
        get() = if (currentPrice > 0.0) {
            abs(targetPrice - currentPrice) / currentPrice
        } else {
            0.0
        }

    val expectedMovePercent: Double
        get() = expectedMoveRatio * 100.0

    fun calculateSpotProfitUsd(capitalUsd: Double): Double {
        return capitalUsd * expectedMoveRatio
    }

    fun calculateFuturesProfitUsd(capitalUsd: Double, leverage: Int): Double {
        return capitalUsd * expectedMoveRatio * leverage.coerceAtLeast(1)
    }
}

data class ExtractedChatSignal(
    val symbol: String,
    val action: SignalAction,
    val entryPrice: String,
    val targetPrice: String,
    val stopLossPrice: String,
    val spotProfitUsd: String,
    val futuresProfitUsd: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val attachedBitmap: Bitmap? = null,
    val extractedSignal: ExtractedChatSignal? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Formats a crypto price preserving exact OKX decimal precision.
 */
fun formatCryptoPrice(price: Double, explicitDecimals: Int? = null): String {
    if (price <= 0.0 || price.isNaN() || price.isInfinite()) return "0.00"
    if (explicitDecimals != null) {
        return BigDecimal.valueOf(price)
            .setScale(explicitDecimals, RoundingMode.HALF_UP)
            .toPlainString()
    }
    val maxDecimals = inferPriceDecimals(price)
    val bd = BigDecimal.valueOf(price)
        .setScale(maxDecimals, RoundingMode.HALF_UP)
        .stripTrailingZeros()
    val finalScale = bd.scale().coerceAtLeast(if (price >= 1.0) 2 else 3)
    return bd.setScale(finalScale, RoundingMode.HALF_UP).toPlainString()
}

fun inferPriceDecimals(price: Double): Int {
    val p = abs(price)
    return when {
        p >= 1000.0 -> 2
        p >= 10.0 -> 3
        p >= 1.0 -> 4
        p >= 0.1 -> 4
        p >= 0.01 -> 5
        p >= 0.001 -> 6
        p >= 0.0001 -> 7
        else -> 8
    }
}

fun countDecimalsFromString(rawPriceStr: String): Int {
    val clean = rawPriceStr.trim()
    val dotIndex = clean.indexOf('.')
    if (dotIndex < 0) return 2
    return (clean.length - dotIndex - 1).coerceIn(2, 8)
}

fun formatUsdProfit(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite()) return "0.00"
    return when {
        abs(amount) > 0.0 && abs(amount) < 0.01 -> String.format(Locale.US, "%.4f", amount)
        abs(amount) < 1.0 -> String.format(Locale.US, "%.2f", amount)
        else -> String.format(Locale.US, "%.2f", amount)
    }
}

fun formatPercent(pct: Double): String {
    return String.format(Locale.US, "%.2f%%", pct)
}
