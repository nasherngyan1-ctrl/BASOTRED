package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.ExtractedChatSignal
import com.example.data.SignalAction
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedBg
import com.example.ui.theme.BearishRedLight
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenBg
import com.example.ui.theme.BullishGreenLight
import com.example.ui.theme.EyeCareBackground
import com.example.ui.theme.EyeCareBorder
import com.example.ui.theme.EyeCareSurface
import com.example.ui.theme.EyeCareSurfaceVariant
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BottomChatSection(
    messages: List<ChatMessage>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    selectedBitmap: Bitmap?,
    onPickImageClick: () -> Unit,
    onClearSelectedImage: () -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(EyeCareSurface)
                .border(
                    width = 1.dp,
                    color = EyeCareBorder,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("bottom_chat_section")
        ) {
            // Subtle Section Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "شیکاری وێنەی چارت و چاتی زیرەکی دەستکرد (بە نرخی ڕەنگدار)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = BullishGreen
                        )
                        Text(
                            text = "لە شیکاریدایە...",
                            style = MaterialTheme.typography.labelMedium,
                            color = BullishGreenLight
                        )
                    }
                }
            }

            // Error Banner if any
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    color = BearishRedBg.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BearishRedLight,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "داخستن",
                                tint = BearishRedLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }
            }

            // Attached Image Thumbnail Preview
            if (selectedBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EyeCareBackground)
                        .border(1.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            bitmap = selectedBitmap.asImageBitmap(),
                            contentDescription = "وێنەی چارتی هەڵبژێردراو",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "وێنەی چارت ئامادەیە بۆ شیکاری تەکنیکی",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BullishGreenLight
                        )
                    }
                    IconButton(onClick = onClearSelectedImage) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "لابردنی وێنە",
                            tint = BearishRedLight
                        )
                    }
                }
            }

            // Bottom Input Row: Image Picker + Text Input + Send Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Attach Chart Image Button
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onPickImageClick)
                        .testTag("attach_image_button"),
                    color = EyeCareBackground,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "ناردنی وێنەی چارت",
                            tint = AccentCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Message Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = {
                        Text(
                            text = "وێنەی چارت بنێرە یان ناوی دراوێک بنووسە...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EyeCareBackground,
                        unfocusedContainerColor = EyeCareBackground,
                        focusedBorderColor = BullishGreen,
                        unfocusedBorderColor = EyeCareBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = BullishGreen
                    )
                )

                // Send Button
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isLoading) { onSendClick() }
                        .testTag("send_chat_button"),
                    color = BullishGreen,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ناردن",
                            tint = EyeCareBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.96f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isUser) EyeCareSurfaceVariant else EyeCareBackground)
                .border(
                    width = 1.dp,
                    color = if (isUser) EyeCareBorder else BullishGreen.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // If user attached a chart screenshot
            if (message.attachedBitmap != null) {
                Image(
                    bitmap = message.attachedBitmap.asImageBitmap(),
                    contentDescription = "وێنەی چارتی نێردراو",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Rich Red & Green Colored Text for Prices and Trading Actions
            Text(
                text = buildColoredTradingText(message.text),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 21.sp
            )

            // If AI returned structured signal prices, render Red & Green Colored Price Badges
            if (message.extractedSignal != null) {
                ColoredChatSignalSummaryCard(signal = message.extractedSignal)
            }
        }
    }
}

@Composable
private fun ColoredChatSignalSummaryCard(signal: ExtractedChatSignal) {
    val isBuy = signal.action == SignalAction.BUY
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EyeCareSurface)
            .border(
                width = 1.dp,
                color = if (isBuy) BullishGreen.copy(alpha = 0.55f) else BearishRed.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action Pill (Green for Buy, Red for Sell)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isBuy) BullishGreenBg else BearishRedBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isBuy) "🟢 ئێستا ${signal.symbol} بکڕە" else "🔴 ئێستا ${signal.symbol} بیفرۆشە",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBuy) BullishGreenLight else BearishRedLight
                )
            }

            // Entry & Exit Colored Prices
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ئێستا: ${signal.entryPrice}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BullishGreenLight
                )
                Text(
                    text = "ئامانج: ${signal.targetPrice}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBuy) BullishGreen else BearishRedLight
                )
                Text(
                    text = "ستۆپ: ${signal.stopLossPrice}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BearishRedLight
                )
            }
        }

        // Colored Profit Pills for Spot & Futures
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BullishGreenBg.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Spot قازانج: ${signal.spotProfitUsd}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BullishGreenLight
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BullishGreenBg.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Futures قازانج: ${signal.futuresProfitUsd}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
            }
        }
    }
}

/**
 * Highlights prices, dollar amounts, percentages, and buy/sell keywords in Green and Red
 * inside chat messages as requested by the user.
 */
fun buildColoredTradingText(rawText: String): AnnotatedString {
    return buildAnnotatedString {
        append(rawText)

        // 1. Highlight Green Bullish Words (کڕین، بکڕە، بەرزدەبێتەوە، قازانج)
        val greenWords = listOf("بکڕە", "بیکڕە", "بیکڕیت", "کڕین", "بەرزدەبێتەوە", "قازانجە", "قازانج", "ئامانج", "BUY", "LONG")
        for (word in greenWords) {
            var startIndex = rawText.indexOf(word, ignoreCase = true)
            while (startIndex >= 0) {
                addStyle(
                    style = SpanStyle(
                        color = BullishGreenLight,
                        fontWeight = FontWeight.Bold
                    ),
                    start = startIndex,
                    end = startIndex + word.length
                )
                startIndex = rawText.indexOf(word, startIndex + word.length, ignoreCase = true)
            }
        }

        // 2. Highlight Red Bearish / Exit Words (بیفرۆشە، فرۆشتن، دادەبەزێت، ستۆپ لۆس)
        val redWords = listOf("بیفرۆشە", "بیفرۆشیت", "فرۆشتن", "دادەبەزێت", "ستۆپ لۆس", "ستۆپ", "زەرەر", "SELL", "SHORT")
        for (word in redWords) {
            var startIndex = rawText.indexOf(word, ignoreCase = true)
            while (startIndex >= 0) {
                addStyle(
                    style = SpanStyle(
                        color = BearishRedLight,
                        fontWeight = FontWeight.Bold
                    ),
                    start = startIndex,
                    end = startIndex + word.length
                )
                startIndex = rawText.indexOf(word, startIndex + word.length, ignoreCase = true)
            }
        }

        // 3. Highlight Prices & Numbers (e.g., 0.213, 0.240, +$0.10, $10) in Green or Red based on context
        val priceRegex = Regex("([+-]?\\$?\\d+(?:\\.\\d+)?%?)")
        priceRegex.findAll(rawText).forEach { match ->
            val token = match.value
            val start = match.range.first
            val end = match.range.last + 1

            // Look slightly before the number in text to see if it's a stop-loss/sell price or buy/target/profit price
            val contextStart = (start - 18).coerceAtLeast(0)
            val precedingContext = rawText.substring(contextStart, start)

            val isRedPrice = token.startsWith("-") ||
                precedingContext.contains("ستۆپ") ||
                precedingContext.contains("فرۆش") ||
                precedingContext.contains("دابەز")

            addStyle(
                style = SpanStyle(
                    color = if (isRedPrice) BearishRedLight else BullishGreenLight,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                start = start,
                end = end
            )
        }
    }
}
