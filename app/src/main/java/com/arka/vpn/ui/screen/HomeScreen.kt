package com.arka.vpn.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arka.vpn.model.ConnectionMode
import com.arka.vpn.model.ConnectionState
import com.arka.vpn.ui.components.ArkaTopBar
import com.arka.vpn.ui.components.ClipboardCategoryDialog
import com.arka.vpn.ui.components.ManageConfigsDialog
import com.arka.vpn.ui.components.ModeSelector
import com.arka.vpn.ui.components.PowerRing
import com.arka.vpn.ui.components.PrivateActivationDialog
import com.arka.vpn.ui.components.SoonRow
import com.arka.vpn.ui.components.SourcesSection
import com.arka.vpn.ui.components.SpeedGraphCard
import com.arka.vpn.ui.components.StatsRow
import com.arka.vpn.ui.theme.AccentAmber
import com.arka.vpn.ui.theme.AccentGreen
import com.arka.vpn.ui.theme.TextMuted
import com.arka.vpn.ui.theme.TextPrimary
import com.arka.vpn.viewmodel.ArkaViewModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(viewModel: ArkaViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    var showSettings by remember { mutableStateOf(false) }
    var showClipboardPicker by remember { mutableStateOf(false) }
    var showPrivateActivation by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        } else {
            viewModel.onVpnPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.vpnPermissionRequest.collect { intent ->
            vpnPermissionLauncher.launch(intent)
        }
    }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ArkaTopBar(onSettingsClick = { showSettings = true })

            Spacer(modifier = Modifier.height(6.dp))
            ModeSelector(selected = state.mode, onSelect = viewModel::selectMode)

            Spacer(modifier = Modifier.height(14.dp))
            val stateColor = when (state.connectionState) {
                ConnectionState.CONNECTED -> AccentGreen
                ConnectionState.CONNECTING -> AccentAmber
                ConnectionState.IDLE -> TextMuted
            }
            val stateText = when (state.connectionState) {
                ConnectionState.IDLE -> "اتصال خاموش است"
                ConnectionState.CONNECTING -> "در حال برقراری اتصال…"
                ConnectionState.CONNECTED -> "متصل هستید — ترافیک امن شد"
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(stateText, color = stateColor, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PowerRing(
                    state = state.connectionState,
                    progress = state.progress,
                    onTap = viewModel::onPowerTap
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val routeText = when {
                state.mode == ConnectionMode.USA -> "مسیر آمریکا • آی‌پی آمریکا"
                state.source == "private" -> "مسیر شخصی • فعال"
                else -> "مسیریابی هوشمند • خودکار"
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(routeText, color = TextMuted, fontSize = 10.5.sp)
            }
            if (state.connectionState == ConnectionState.CONNECTED) {
                state.activeConfig?.let { cfg ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        val serverLabel = cfg.remark?.takeIf { it.isNotBlank() } ?: cfg.protocol.uppercase()
                        Text(
                            "کانفیگ فعال: $serverLabel — پینگ واقعی ${state.pingMs ?: "—"} ms",
                            color = AccentGreen,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                val speedLabel = if (state.connectionState == ConnectionState.CONNECTED)
                    String.format(Locale.US, "%.1f MB/s", state.currentSpeedMbps) else "—"
                SpeedGraphCard(speedLabel = speedLabel, values = state.sparkline, lineColor = stateColor)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                StatsRow(
                    pingLabel = state.pingMs?.let { "$it ms" } ?: "—",
                    timeLabel = formatTime(state.elapsedSeconds),
                    dataLabel = formatData(state.dataUsageMb),
                    pingColor = if (state.connectionState == ConnectionState.CONNECTED) AccentGreen else TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                SourcesSection(
                    selectedSource = state.source,
                    privateUnlocked = state.privateUnlocked,
                    onSelectSource = { src ->
                        viewModel.selectSource(src) { showPrivateActivation = true }
                    }
                )
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                SoonRow()
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    "نسخه ۱٫۰ • تمام ترافیک شما رمزگذاری می‌شود",
                    color = TextMuted.copy(alpha = 0.55f),
                    fontSize = 9.5.sp
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showSettings) {
        ManageConfigsDialog(
            counts = state.configCounts,
            onImportAssets = { viewModel.importFromAssets() },
            onImportClipboard = {
                showSettings = false
                showClipboardPicker = true
            },
            onClearDatabase = { viewModel.clearDatabase() },
            onDismiss = { showSettings = false }
        )
    }

    if (showClipboardPicker) {
        val clipText = clipboardManager.getText()?.text.orEmpty()
        if (clipText.isBlank()) {
            LaunchedEffect(Unit) {
                viewModel.emitToast("کلیپ‌بورد خالی است")
                showClipboardPicker = false
            }
        } else {
            ClipboardCategoryDialog(
                onPick = { mode ->
                    viewModel.importFromClipboard(mode, clipText)
                    showClipboardPicker = false
                },
                onDismiss = { showClipboardPicker = false }
            )
        }
    }

    if (showPrivateActivation) {
        PrivateActivationDialog(
            onActivate = { code -> viewModel.activatePrivate(code) },
            onDismiss = { showPrivateActivation = false }
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private fun formatData(dataMb: Double): String =
    if (dataMb < 1024) "${dataMb.roundToInt()} MB"
    else String.format(Locale.US, "%.2f GB", dataMb / 1024)
