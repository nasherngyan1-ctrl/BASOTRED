package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SignalAction
import com.example.data.TradingSignal
import com.example.data.formatCryptoPrice
import com.example.data.formatPercent
import com.example.data.formatUsdProfit
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedLight
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenLight
import com.example.ui.theme.EyeCareBackground
import com.example.ui.theme.EyeCareBorder
import com.example.ui.theme.EyeCareSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TopSignalChartSection(
    signal: TradingSignal,
    capitalUsd: Double,
    leverage: Int,
    countdownSeconds: Int,
    isScanning: Boolean,
    onScanNowClick: () -> Unit,
    onOpenCapitalDialog: () -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBuy = signal.action == SignalAction.BUY
    val mainSignalColor = if (isBuy) BullishGreen else BearishRed

    val priceTickUp = signal.currentPrice >= signal.previousPrice
    val animatedPriceColor by animateColorAsState(
        targetValue = if (priceTickUp) BullishGreenLight else BearishRedLight,
        animationSpec = tween(durationMillis = 350),
        label = "livePriceColor"
    )

    val currentPriceStr = formatCryptoPrice(signal.currentPrice, signal.priceDecimals)
    val targetPriceStr = formatCryptoPrice(signal.targetPrice, signal.priceDecimals)
    val movePctStr = formatPercent(signal.expectedMovePercent)
    val spotProfitStr = formatUsdProfit(signal.calculateSpotProfitUsd(capitalUsd))
    val futuresProfitStr = formatUsdProfit(signal.calculateFuturesProfitUsd(capitalUsd, leverage))

    val minutes = countdownSeconds / 60
    val seconds = countdownSeconds % 60
    val timerFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("top_signal_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EyeCareSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EyeCareBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minimal Top Header: Auto 5m Notification Status + Small Settings Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onScanNowClick)
                            .padding(vertical = 2.dp)
                            .testTag("scan_now_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = BullishGreen
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "ئاگادارکردنەوەی خۆکاری 5 خولەکی",
                                tint = BullishGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "نۆتیفیکەیشنی OKX ($timerFormatted)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EyeCareBackground)
                                .clickable(onClick = onOpenCapitalDialog)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("capital_leverage_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "پارە و لیڤرێج",
                                tint = AccentGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$${formatUsdProfit(capitalUsd)} • ${leverage}x",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EyeCareBackground)
                                .clickable(onClick = onOpenApiKeyDialog)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("ai_key_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "کلیلی AI",
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AI",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = EyeCareBorder.copy(alpha = 0.6f))

                // Clean, Simple Text-Only Notification Block with Exact Live OKX Decimals
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EyeCareBackground)
                        .border(1.dp, mainSignalColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Line 1: ئێستا دراوی ... بکڕە / بیفرۆشە
                    Text(
                        text = buildAnnotatedString {
                            append("ئێستا دراوی ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            ) {
                                append(signal.symbol.lowercase())
                            }
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    color = if (isBuy) BullishGreenLight else BearishRedLight,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(if (isBuy) "بکڕە" else "بیفرۆشە")
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp
                    )

                    // Line 2: نرخی ئێستا ...
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextSecondary)) {
                                append("نرخی ئێستا ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedPriceColor
                                )
                            ) {
                                append(currentPriceStr)
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp
                    )

                    // Line 3: بەرزدەبێتەوە بۆ ... (+1.45%)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextSecondary)) {
                                append(if (isBuy) "بەرزدەبێتەوە بۆ " else "دادەبەزێت بۆ ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBuy) BullishGreenLight else BearishRedLight
                                )
                            ) {
                                append("$targetPriceStr ($movePctStr)")
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp
                    )

                    // Line 4: بیفرۆشە
                    Text(
                        text = if (isBuy) "بیفرۆشە" else "قازانج وەربگرە",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = BearishRedLight,
                        fontSize = 15.sp
                    )

                    // Line 5: ... دۆلارت قازانجە
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = BullishGreenLight
                                )
                            ) {
                                append(spotProfitStr)
                            }
                            withStyle(
                                SpanStyle(
                                    color = BullishGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(" دۆلارت قازانجە")
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = EyeCareBorder.copy(alpha = 0.5f)
                    )

                    // Line 6 & 7: Spot & Futures Profit in Simple Text (with Capital & Leverage shown clearly)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextSecondary)) {
                                append("لە بەشی Spot قازانج (بە $${formatUsdProfit(capitalUsd)}): ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = BullishGreenLight
                                )
                            ) {
                                append("+$spotProfitStr دۆلار")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("spot_profit_box")
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextSecondary)) {
                                append("لە بەشی Futures (${leverage}x) قازانج: ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = BullishGreenLight
                                )
                            ) {
                                append("+$futuresProfitStr دۆلار")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("futures_profit_box")
                    )
                }
            }
        }
    }
}
