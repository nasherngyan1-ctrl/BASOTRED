package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "okx_sniper_5m_signals"
    private const val CHANNEL_NAME = "ئاگادارکردنەوەی 5 خولەکی OKX"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ناردنی سیگناڵی کڕین و فرۆشتنی دراوەکانی OKX هەموو 5 خولەک جارێک"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun sendSignalNotification(
        context: Context,
        signal: TradingSignal,
        capitalUsd: Double,
        leverage: Int
    ) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val currentFormatted = formatCryptoPrice(signal.currentPrice, signal.priceDecimals)
        val targetFormatted = formatCryptoPrice(signal.targetPrice, signal.priceDecimals)
        val spotProfit = formatUsdProfit(signal.calculateSpotProfitUsd(capitalUsd))
        val futuresProfit = formatUsdProfit(signal.calculateFuturesProfitUsd(capitalUsd, leverage))

        val actionWord = if (signal.action == SignalAction.BUY) "بکڕە" else "بیفرۆشە"
        val moveDirectionWord = if (signal.action == SignalAction.BUY) "بەرزدەبێتەوە بۆ" else "دادەبەزێت بۆ"
        val exitWord = if (signal.action == SignalAction.BUY) "بیفرۆشە" else "قازانج وەربگرە"

        val title = "ئێستا دراوی ${signal.symbol.lowercase()} $actionWord"
        val fullMessage = buildString {
            appendLine("ئێستا دراوی ${signal.symbol.lowercase()} $actionWord")
            appendLine("نرخی ئێستا $currentFormatted")
            appendLine("$moveDirectionWord $targetFormatted")
            appendLine(exitWord)
            appendLine("$spotProfit دۆلارت قازانجە")
            appendLine("لە بەشی Spot قازانج: +$$spotProfit | لە بەشی Futures (${leverage}x) قازانج: +$$futuresProfit")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("نرخی ئێستا $currentFormatted ⬅️ $moveDirectionWord $targetFormatted | Spot: +$$spotProfit | Futures: +$$futuresProfit")
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (_: SecurityException) {
        }
    }
}
