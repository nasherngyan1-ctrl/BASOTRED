package com.example

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiKeySettingsDialog
import com.example.ui.components.BottomChatSection
import com.example.ui.components.CapitalLeverageDialog
import com.example.ui.components.TopSignalChartSection
import com.example.ui.theme.EyeCareBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TradingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                OkxSniperScreen()
            }
        }
    }
}

@Composable
fun OkxSniperScreen(
    tradingViewModel: TradingViewModel = viewModel()
) {
    val context = LocalContext.current

    val activeSignal by tradingViewModel.activeSignal.collectAsStateWithLifecycle()
    val capitalUsd by tradingViewModel.capitalUsd.collectAsStateWithLifecycle()
    val leverage by tradingViewModel.leverage.collectAsStateWithLifecycle()
    val customApiKey by tradingViewModel.customApiKey.collectAsStateWithLifecycle()
    val countdownSeconds by tradingViewModel.countdownSeconds.collectAsStateWithLifecycle()
    val isScanningMarket by tradingViewModel.isScanningMarket.collectAsStateWithLifecycle()
    val chatMessages by tradingViewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by tradingViewModel.isChatLoading.collectAsStateWithLifecycle()
    val chatError by tradingViewModel.chatError.collectAsStateWithLifecycle()

    var chatInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showCapitalDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Notification Permission Launcher for Android 13+ (5-minute notifications)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Zero-Permission Photo Picker for Chart Screenshot Analysis
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bmp
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EyeCareBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP SECTION: Simple, Clean Text Notification (Auto-tracks OKX every 5 minutes)
        TopSignalChartSection(
            signal = activeSignal,
            capitalUsd = capitalUsd,
            leverage = leverage,
            countdownSeconds = countdownSeconds,
            isScanning = isScanningMarket,
            onScanNowClick = { tradingViewModel.triggerImmediateScan() },
            onOpenCapitalDialog = { showCapitalDialog = true },
            onOpenApiKeyDialog = { showApiKeyDialog = true },
            modifier = Modifier.fillMaxWidth()
        )

        // BOTTOM SECTION: Chart Screenshot Upload & AI Chat (Colored Prices in Answers)
        BottomChatSection(
            messages = chatMessages,
            inputText = chatInput,
            onInputTextChange = { chatInput = it },
            selectedBitmap = selectedBitmap,
            onPickImageClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearSelectedImage = { selectedBitmap = null },
            onSendClick = {
                val textToSend = chatInput
                val bmpToSend = selectedBitmap
                if (textToSend.isNotBlank() || bmpToSend != null) {
                    chatInput = ""
                    selectedBitmap = null
                    tradingViewModel.sendChatMessage(textToSend, bmpToSend)
                }
            },
            isLoading = isChatLoading,
            errorMessage = chatError,
            onDismissError = { tradingViewModel.clearChatError() },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }

    if (showCapitalDialog) {
        CapitalLeverageDialog(
            initialCapital = capitalUsd,
            initialLeverage = leverage,
            onDismiss = { showCapitalDialog = false },
            onConfirm = { newCap, newLev ->
                tradingViewModel.updateCapitalAndLeverage(newCap, newLev)
                showCapitalDialog = false
            }
        )
    }

    if (showApiKeyDialog) {
        AiKeySettingsDialog(
            currentKey = customApiKey,
            onDismiss = { showApiKeyDialog = false },
            onSaveKey = { newKey ->
                tradingViewModel.saveCustomApiKey(newKey)
                showApiKeyDialog = false
            }
        )
    }
}
