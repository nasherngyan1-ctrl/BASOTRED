package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.OkxMarketService
import com.example.data.SignalAction
import com.example.data.formatCryptoPrice
import com.example.data.formatUsdProfit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("OKX Sniper AI", appName)
    }

    @Test
    fun `verify DOODUSDT baseline signal and spot futures profit calculation`() {
        val service = OkxMarketService()
        val signal = service.createDoodUsdtBaselineSignal()

        assertEquals("DOODUSDT", signal.symbol)
        assertEquals(SignalAction.BUY, signal.action)
        assertEquals("0.213", formatCryptoPrice(signal.currentPrice, signal.priceDecimals))
        assertEquals("0.240", formatCryptoPrice(signal.targetPrice, signal.priceDecimals))

        val spotProfit10Usd = signal.calculateSpotProfitUsd(10.0)
        val futuresProfit10x = signal.calculateFuturesProfitUsd(10.0, 10)
        assertTrue(spotProfit10Usd > 1.20)
        assertEquals(formatUsdProfit(spotProfit10Usd * 10), formatUsdProfit(futuresProfit10x))
    }
}
