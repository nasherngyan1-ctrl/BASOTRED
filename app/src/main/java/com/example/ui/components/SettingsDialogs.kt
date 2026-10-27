package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenLight
import com.example.ui.theme.EyeCareBackground
import com.example.ui.theme.EyeCareBorder
import com.example.ui.theme.EyeCareSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CapitalLeverageDialog(
    initialCapital: Double,
    initialLeverage: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double, Int) -> Unit
) {
    var capitalInput by remember { mutableStateOf(initialCapital.toString()) }
    var leverageInput by remember { mutableStateOf(initialLeverage.toString()) }

    val presetCapitals = listOf(10.0, 25.0, 50.0, 100.0)
    val presetLeverages = listOf(5, 10, 20, 50)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = EyeCareSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text(
                    text = "دیاریکردنی بڕی پارە و لیڤرێجی Futures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "بڕی سەرمایەکەت بە دۆلار (USDT) بۆ Spot و Futures:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetCapitals.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EyeCareBackground)
                                    .border(1.dp, BullishGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { capitalInput = preset.toInt().toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$${preset.toInt()}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = BullishGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = capitalInput,
                        onValueChange = { capitalInput = it },
                        label = { Text("بڕی پارە بە دۆلار ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("capital_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = EyeCareBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Text(
                        text = "لیڤرێجی بەشی Futures (x):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetLeverages.forEach { lev ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EyeCareBackground)
                                    .border(1.dp, AccentGold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .clickable { leverageInput = lev.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${lev}x",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = leverageInput,
                        onValueChange = { leverageInput = it },
                        label = { Text("لیڤرێج (بۆ نموونە 10)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leverage_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = EyeCareBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = capitalInput.toDoubleOrNull() ?: initialCapital
                        val lev = leverageInput.toIntOrNull() ?: initialLeverage
                        onConfirm(cap, lev)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BullishGreen,
                        contentColor = EyeCareBackground
                    )
                ) {
                    Text("پاشەکەوتکردن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("پاشگەزبوونەوە", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun AiKeySettingsDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSaveKey: (String) -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentKey) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = EyeCareSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text(
                    text = "باشترین زیرەکی دەستکرد بۆ ترەیدینگ (Gemini AI)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✅ مۆدێلی Google Gemini باشترین و وردترین زیرەکی دەستکردە بۆ خوێندنەوەی وێنەی مۆمەکانی چارت و شیکاری تەکنیکی OKX.\n" +
                            "ئەپەکە ئێستا بە شێوەی خۆکار چالاکە؛ هەروەها ئەگەر دەتەوێت کلیلی تایبەتی خۆت (Gemini API Key) بەکاربهێنیت، لێرە زیادی بکە:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Gemini API Key (ئارەزوومەندانە)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = EyeCareBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSaveKey(apiKeyText) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BullishGreen,
                        contentColor = EyeCareBackground
                    )
                ) {
                    Text("پاشەکەوتکردن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("داخستن", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CustomCoinSearchDialog(
    onDismiss: () -> Unit,
    onSelectCoin: (String) -> Unit
) {
    var coinText by remember { mutableStateOf("") }
    val popularCoins = listOf("DOODUSDT", "SUIUSDT", "TONUSDT", "DOGEUSDT", "ADAUSDT", "LINKUSDT")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = EyeCareSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text(
                    text = "هەڵبژاردنی هەر دراوێکی ناو OKX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ناوی دراوەکە بنووسە (بۆ نموونە DOODUSDT یان SOLUSDT):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = coinText,
                        onValueChange = { coinText = it.uppercase() },
                        label = { Text("ناوی دراو لە OKX") },
                        placeholder = { Text("DOODUSDT") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_coin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BullishGreen,
                            unfocusedBorderColor = EyeCareBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularCoins.take(3).forEach { sym ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EyeCareBackground)
                                    .border(1.dp, EyeCareBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectCoin(sym)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sym,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = BullishGreenLight
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (coinText.isNotBlank()) {
                            onSelectCoin(coinText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BullishGreen,
                        contentColor = EyeCareBackground
                    )
                ) {
                    Text("شیکاری دراوەکە", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("داخستن", color = TextSecondary)
                }
            }
        )
    }
}
