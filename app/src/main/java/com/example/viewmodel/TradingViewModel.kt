package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.ExtractedChatSignal
import com.example.data.NotificationHelper
import com.example.data.OkxMarketService
import com.example.data.SignalAction
import com.example.data.TradingSignal
import com.example.data.countDecimalsFromString
import com.example.data.formatCryptoPrice
import com.example.data.formatPercent
import com.example.data.formatUsdProfit
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val TAG = "TradingViewModel"
private const val PREFS_NAME = "okx_sniper_prefs"
private const val KEY_CAPITAL = "capital_usd"
private const val KEY_LEVERAGE = "futures_leverage"
private const val KEY_CUSTOM_API = "custom_gemini_api_key"

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val okxService = OkxMarketService()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Structured JSON Schema for Gemini Chart & Trading Analysis
    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "symbol" to Schema.string(description = "Exact coin pair symbol on OKX, e.g., SOLUSDT or BTCUSDT"),
            "action" to Schema.enumeration(
                values = listOf("BUY", "SELL"),
                description = "Whether the user should BUY or SELL right now based on technical analysis"
            ),
            "entryPrice" to Schema.double(description = "Exact numeric current/entry price read from chart or OKX data"),
            "targetPrice" to Schema.double(description = "Exact numeric target take-profit price"),
            "stopLossPrice" to Schema.double(description = "Exact numeric stop-loss price"),
            "kurdishReason" to Schema.string(
                description = "Clear Sorani Kurdish explanation of the technical setup (candlesticks, support/resistance, RSI/EMA)."
            )
        )
    )

    private val structuredAiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = analysisSchema
            }
        )
    }

    private val _capitalUsd = MutableStateFlow(prefs.getFloat(KEY_CAPITAL, 10.0f).toDouble())
    val capitalUsd: StateFlow<Double> = _capitalUsd.asStateFlow()

    private val _leverage = MutableStateFlow(prefs.getInt(KEY_LEVERAGE, 10))
    val leverage: StateFlow<Int> = _leverage.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString(KEY_CUSTOM_API, "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _activeSignal = MutableStateFlow(okxService.createDoodUsdtBaselineSignal())
    val activeSignal: StateFlow<TradingSignal> = _activeSignal.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(300)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _isScanningMarket = MutableStateFlow(false)
    val isScanningMarket: StateFlow<Boolean> = _isScanningMarket.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "سڵاو! لە بەشی سەرەوە ئەپەکە خۆکارانە پەیوەستە بە زانیاری ڕاستەقینەی بازاڕی OKX و هەموو 5 خولەک جارێک باشترین دراو دەدۆزێتەوە.\nلێرەش لە خوارەوە وێنەی هەر چارتێک بنێریت یان ناوی دراوێک بنووسیت، بە وردی نرخی کڕین و فرۆشتن و قازانجی تەواوی Spot و Futuresت پێ دەڵێم."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    init {
        // Immediately fetch real live OKX data on startup so prices are 100% real from second 1
        scanMarketAndSendNotification(sendSystemNotification = false)
        startFiveMinuteScannerAndLiveTicker()
    }

    private fun startFiveMinuteScannerAndLiveTicker() {
        // 1. 1-second countdown timer for the 5-minute notification cycle
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val next = _countdownSeconds.value - 1
                if (next <= 0) {
                    _countdownSeconds.value = 300
                    scanMarketAndSendNotification(sendSystemNotification = true)
                } else {
                    _countdownSeconds.value = next
                }
            }
        }

        // 2. Continuous live price refresh every 3 seconds directly from OKX API
        viewModelScope.launch {
            while (true) {
                delay(3_000L)
                if (!_isScanningMarket.value) {
                    val updated = okxService.refreshLivePrice(_activeSignal.value)
                    _activeSignal.value = updated
                }
            }
        }
    }

    fun updateCapitalAndLeverage(newCapitalUsd: Double, newLeverage: Int) {
        val safeCapital = newCapitalUsd.coerceAtLeast(0.1)
        val safeLeverage = newLeverage.coerceIn(1, 125)
        _capitalUsd.value = safeCapital
        _leverage.value = safeLeverage
        prefs.edit()
            .putFloat(KEY_CAPITAL, safeCapital.toFloat())
            .putInt(KEY_LEVERAGE, safeLeverage)
            .apply()
    }

    fun saveCustomApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        _customApiKey.value = trimmed
        prefs.edit().putString(KEY_CUSTOM_API, trimmed).apply()
    }

    fun triggerImmediateScan() {
        _countdownSeconds.value = 300
        scanMarketAndSendNotification(sendSystemNotification = true)
    }

    private fun scanMarketAndSendNotification(sendSystemNotification: Boolean) {
        viewModelScope.launch {
            _isScanningMarket.value = true
            try {
                val bestSignal = okxService.scanBestMarketOpportunity(
                    excludeSymbol = if (_activeSignal.value.isLiveFromOkx) _activeSignal.value.symbol else null
                )
                _activeSignal.value = bestSignal
                if (sendSystemNotification) {
                    NotificationHelper.sendSignalNotification(
                        context = getApplication(),
                        signal = bestSignal,
                        capitalUsd = _capitalUsd.value,
                        leverage = _leverage.value
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Market scan failed", e)
            } finally {
                _isScanningMarket.value = false
            }
        }
    }

    /**
     * Analyzes user chat message and/or attached OKX chart screenshot with exact mathematical profit calculation.
     */
    fun sendChatMessage(userText: String, chartBitmap: Bitmap?) {
        if (userText.isBlank() && chartBitmap == null) return

        val displayPrompt = when {
            userText.isNotBlank() -> userText.trim()
            else -> "ئەم وێنەیەی چارتەکەم بۆ شی بکەرەوە؛ ئێستا بیکڕم یان بیفرۆشم؟ و لە Spot و Futures چەندم قازانجە؟"
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = displayPrompt,
            attachedBitmap = chartBitmap
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true
        _chatError.value = null

        viewModelScope.launch {
            try {
                val cap = _capitalUsd.value
                val lev = _leverage.value

                // Check if user mentioned a specific coin in text so we fetch its real-time OKX price
                val mentionedSymbol = extractMentionedSymbol(displayPrompt)
                val liveRequestedSignal = if (mentionedSymbol != null) {
                    okxService.analyzeCoin(mentionedSymbol)
                } else {
                    null
                }
                if (liveRequestedSignal != null) {
                    _activeSignal.value = liveRequestedSignal
                }

                val referenceSignal = liveRequestedSignal ?: _activeSignal.value
                val refEntryStr = formatCryptoPrice(referenceSignal.currentPrice, referenceSignal.priceDecimals)
                val refTargetStr = formatCryptoPrice(referenceSignal.targetPrice, referenceSignal.priceDecimals)
                val refStopStr = formatCryptoPrice(referenceSignal.stopLoss, referenceSignal.priceDecimals)

                val systemPrompt = if (chartBitmap != null) {
                    """
                    تۆ شارەزای شیکاری تەکنیکی چارتەکانی بۆرسەی OKXیت.
                    تکایە بە وردی سەیری ئەم وێنەیەی چارتەکە بکە:
                    1. ناوی دراوەکە لەسەر وێنەکە بخوێنەرەوە (ئەگەر دیار نەبوو و بەکارهێنەر ناوی دراوی نەنووسیبوو، بنووسە CHART).
                    2. نرخی ئێستای سەر وێنەکە (لای ڕاستی چارتەکە) بە وردی و بە هەموو ژمارەکانی دوای فاریزە بخوێنەرەوە و بیکە بە entryPrice.
                    3. بەپێی مۆمە سەوز و سوورەکان و پاڵپشتی و بەرگری سەر وێنەکە بڕیار بدە ئایا ئێستا کاتی کڕینە (BUY) یان فرۆشتن (SELL).
                    4. نرخی ئامانج (targetPrice) و نرخی ستۆپ لۆس (stopLossPrice) بەپێی ژمارەکانی سەر چارتەکە دیاری بکە (بۆ کڕین دەبێت targetPrice بەرزتر بێت لە entryPrice، و بۆ فرۆشتن دەبێت targetPrice نزمتر بێت لە entryPrice).
                    5. لە kurdishReason بە زمانی کوردی هۆکاری تەکنیکی لەسەر وێنەکە ڕوون بکەرەوە.
                    پرسیاری بەکارهێنەر: $displayPrompt
                    """.trimIndent()
                } else {
                    """
                    تۆ شارەزای شیکاری تەکنیکی بۆرسەی OKXیت.
                    ئەمە داتای زیندوو و ڕاستەقینەی ئێستای بۆرسەی OKXـە بۆ دراوی ${referenceSignal.symbol}:
                    - نرخی ڕاستەقینەی ئێستا لە OKX (entryPrice): $refEntryStr
                    - نرخی ئامانج بەپێی ATR و شیکاری تەکنیکی (targetPrice): $refTargetStr
                    - نرخی ستۆپ لۆس (stopLossPrice): $refStopStr
                    - بڕیاری تەکنیکی: ${referenceSignal.action.name} (RSI=${referenceSignal.rsi.toInt()}, EMA9=${formatCryptoPrice(referenceSignal.ema9, referenceSignal.priceDecimals)}, EMA21=${formatCryptoPrice(referenceSignal.ema21, referenceSignal.priceDecimals)})
                    تکایە دەقاودەق ئەم نرخە ڕاستەقینانەی OKX بەکاربهێنە و هیچ نرخێکی خەیاڵی مەنووسە.
                    پرسیاری بەکارهێنەر: $displayPrompt
                    """.trimIndent()
                }

                val jsonResponseString = withTimeout(60_000L) {
                    val userKey = _customApiKey.value
                    if (userKey.isNotBlank()) {
                        callCustomGeminiKeyApi(userKey, systemPrompt, chartBitmap)
                    } else {
                        val response = if (chartBitmap != null) {
                            val scaledBitmap = scaleBitmapUnderLimit(chartBitmap)
                            structuredAiModel.generateContent(
                                content {
                                    image(scaledBitmap)
                                    text(systemPrompt)
                                }
                            )
                        } else {
                            structuredAiModel.generateContent(systemPrompt)
                        }
                        response.text ?: throw IllegalStateException("وەڵامێکی بەتاڵ لە مۆدێلەوە گەڕایەوە.")
                    }
                }

                val parsedJson = JSONObject(jsonResponseString)
                val sym = parsedJson.optString("symbol", referenceSignal.symbol).uppercase()
                val actStr = parsedJson.optString("action", referenceSignal.action.name)
                var act = if (actStr.equals("SELL", ignoreCase = true)) SignalAction.SELL else SignalAction.BUY

                // If user asked via text (no image) about an OKX coin, enforce 100% live OKX price accuracy
                val rawEntry = if (chartBitmap == null) {
                    referenceSignal.currentPrice
                } else {
                    parsedJson.optDouble("entryPrice", referenceSignal.currentPrice).takeIf { it > 0.0 }
                        ?: referenceSignal.currentPrice
                }

                var rawTarget = if (chartBitmap == null) {
                    referenceSignal.targetPrice
                } else {
                    parsedJson.optDouble("targetPrice", referenceSignal.targetPrice).takeIf { it > 0.0 }
                        ?: referenceSignal.targetPrice
                }

                // Ensure mathematical consistency between BUY/SELL direction and target vs entry
                if (act == SignalAction.BUY && rawTarget <= rawEntry) {
                    rawTarget = rawEntry * 1.018
                } else if (act == SignalAction.SELL && rawTarget >= rawEntry) {
                    rawTarget = rawEntry * 0.982
                }

                val rawStop = if (chartBitmap == null) {
                    referenceSignal.stopLoss
                } else {
                    val parsedStop = parsedJson.optDouble("stopLossPrice", 0.0)
                    if (act == SignalAction.BUY && parsedStop > 0.0 && parsedStop < rawEntry) {
                        parsedStop
                    } else if (act == SignalAction.SELL && parsedStop > rawEntry) {
                        parsedStop
                    } else if (act == SignalAction.BUY) {
                        rawEntry * 0.991
                    } else {
                        rawEntry * 1.009
                    }
                }

                val decimals = if (chartBitmap == null) {
                    referenceSignal.priceDecimals
                } else {
                    countDecimalsFromString(parsedJson.optString("entryPrice", rawEntry.toString()))
                }

                val entryFormatted = formatCryptoPrice(rawEntry, decimals)
                val targetFormatted = formatCryptoPrice(rawTarget, decimals)
                val stopFormatted = formatCryptoPrice(rawStop, decimals)

                // Exact mathematical Spot & Futures profit calculation in Kotlin (never relying on LLM arithmetic)
                val moveRatio = if (rawEntry > 0.0) abs(rawTarget - rawEntry) / rawEntry else 0.0
                val movePctStr = formatPercent(moveRatio * 100.0)
                val spotProfitUsdVal = cap * moveRatio
                val futuresProfitUsdVal = spotProfitUsdVal * lev
                val spotProfitFormatted = formatUsdProfit(spotProfitUsdVal)
                val futuresProfitFormatted = formatUsdProfit(futuresProfitUsdVal)

                val kurdishReason = parsedJson.optString("kurdishReason", "").trim()
                val actionWord = if (act == SignalAction.BUY) "بکڕە" else "بیفرۆشە"
                val directionWord = if (act == SignalAction.BUY) "بەرزدەبێتەوە بۆ" else "دادەبەزێت بۆ"
                val exitWord = if (act == SignalAction.BUY) "بیفرۆشە" else "قازانج وەربگرە"

                val formattedReply = buildString {
                    appendLine("ئێستا دراوی ${sym.lowercase()} $actionWord")
                    appendLine("نرخی ئێستا $entryFormatted")
                    appendLine("$directionWord $targetFormatted ($movePctStr)")
                    appendLine("$exitWord — ستۆپ لۆس: $stopFormatted")
                    appendLine("$spotProfitFormatted دۆلارت قازانجە")
                    appendLine("لە بەشی Spot قازانج: +$$spotProfitFormatted دۆلار (بە سەرمایەی $$cap)")
                    appendLine("لە بەشی Futures (${lev}x) قازانج: +$$futuresProfitFormatted دۆلار")
                    if (kurdishReason.isNotBlank()) {
                        appendLine("شیکاری: $kurdishReason")
                    }
                }.trim()

                val aiMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = formattedReply,
                    extractedSignal = ExtractedChatSignal(
                        symbol = sym,
                        action = act,
                        entryPrice = entryFormatted,
                        targetPrice = targetFormatted,
                        stopLossPrice = stopFormatted,
                        spotProfitUsd = "+$$spotProfitFormatted",
                        futuresProfitUsd = "+$$futuresProfitFormatted"
                    )
                )

                _chatMessages.value = _chatMessages.value + aiMessage
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI analysis timed out", e)
                _chatError.value = "شیکارییەکە کاتێکی زۆری خایاند (60 چرکە). تکایە دووبارە هەوڵبدەرەوە."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI analysis failed", e)
                _chatError.value = e.localizedMessage ?: "کێشەیەک لە پەیوەندیکردن بە زیرەکی دەستکرد ڕوویدا."
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChatError() {
        _chatError.value = null
    }

    private fun extractMentionedSymbol(text: String): String? {
        val regex = Regex("\\b([A-Za-z0-9]{2,10})[-/]?USDT\\b", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            return match.groupValues[1].uppercase() + "USDT"
        }
        val commonCoins = listOf(
            "DOOD", "BTC", "ETH", "SOL", "PEPE", "XRP", "DOGE", "SUI",
            "TON", "BNB", "ADA", "TRX", "AVAX", "LINK", "SHIB", "DOT", "NEAR", "ARB", "OP", "APT"
        )
        val upperText = text.uppercase()
        for (coin in commonCoins) {
            if (Regex("\\b$coin\\b").containsMatchIn(upperText)) {
                return "${coin}USDT"
            }
        }
        return null
    }

    private fun scaleBitmapUnderLimit(bitmap: Bitmap): Bitmap {
        val maxDim = 1024
        if (bitmap.width <= maxDim && bitmap.height <= maxDim) return bitmap
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (ratio > 1f) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private suspend fun callCustomGeminiKeyApi(
        apiKey: String,
        prompt: String,
        bitmap: Bitmap?
    ): String = withContext(Dispatchers.IO) {
        val partsArray = JSONArray()
        if (bitmap != null) {
            val scaled = scaleBitmapUnderLimit(bitmap)
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val inlineData = JSONObject().apply {
                put("mime_type", "image/jpeg")
                put("data", base64Image)
            }
            partsArray.put(JSONObject().put("inline_data", inlineData))
        }
        partsArray.put(
            JSONObject().put(
                "text",
                "$prompt\nRespond strictly in valid JSON with keys: symbol (string), action (BUY or SELL), entryPrice (number), targetPrice (number), stopLossPrice (number), kurdishReason (string)."
            )
        )

        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
            put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(requestPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("هەڵەی کلیلی API (${response.code}): تکایە دڵنیابەرەوە لە دروستی کلیلەکەت.")
            }
            val root = JSONObject(bodyStr)
            val candidates = root.optJSONArray("candidates")
            val firstContent = candidates?.optJSONObject(0)?.optJSONObject("content")
            val firstPart = firstContent?.optJSONArray("parts")?.optJSONObject(0)
            firstPart?.optString("text") ?: throw IllegalStateException("وەڵامی بەتاڵ لە API گەڕایەوە.")
        }
    }
}
