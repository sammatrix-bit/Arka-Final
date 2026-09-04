package com.arka.vpn.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arka.vpn.data.ConfigEntity
import com.arka.vpn.data.PrivateAccessStore
import com.arka.vpn.model.ConnectionMode
import com.arka.vpn.model.ConnectionState
import com.arka.vpn.repo.ConfigImporter
import com.arka.vpn.repo.ConfigTester
import com.arka.vpn.vpn.ArkaVpnService
import com.arka.vpn.vpncore.ArkaCoreManager
import com.arka.vpn.vpncore.ConfigConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * منطق کامل صفحه اصلی.
 *
 * جریان واقعیِ اتصال:
 * 1) کانفیگ‌های واقعی دیتابیس رو با TCP handshake واقعی تست می‌کنه (مثل قبل).
 * 2) کانفیگ برنده رو با ConfigConverter به یک JSON واقعیِ Xray-core تبدیل می‌کنه.
 * 3) اجازه‌ی VPN اندروید رو می‌گیره (VpnService.prepare).
 * 4) ArkaVpnService واقعی رو استارت می‌کنه که TUN واقعی می‌سازه و به هسته‌ی Xray-core واقعی وصلش می‌کنه.
 * 5) وضعیت «متصل»، پینگ، و آمار ترافیک همه از هسته‌ی واقعی (ArkaCoreManager) خونده می‌شن — دیگه تایمر شبیه‌سازی‌شده نیست.
 */
class ArkaViewModel(application: Application) : AndroidViewModel(application) {

    private val importer = ConfigImporter(application)
    private val privateStore = PrivateAccessStore(application)

    private val _uiState = MutableStateFlow(ArkaUiState())
    val uiState: StateFlow<ArkaUiState> = _uiState.asStateFlow()

    /** وقتی اجازه‌ی VPN لازم باشه، این Intent برای UI فرستاده می‌شه تا با startActivityForResult باز بشه. */
    private val _vpnPermissionRequest = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val vpnPermissionRequest: SharedFlow<Intent> = _vpnPermissionRequest.asSharedFlow()

    private var connectJob: Job? = null
    private var statsJob: Job? = null
    private var pendingConfigJson: String? = null
    private var pendingMode: ConnectionMode? = null
    private var realSpeedMbpsForGraph: Double = 0.0

    private data class Found(val entity: ConfigEntity, val result: ConfigTester.TestResult)

    init {
        val unlocked = privateStore.isActivated()
        _uiState.update { it.copy(privateUnlocked = unlocked) }
        autoImportOnFirstLaunch()
        startSparkline()
        watchRealCoreState()
    }

    private fun autoImportOnFirstLaunch() {
        viewModelScope.launch {
            val empty = withContext(Dispatchers.IO) { importer.isDatabaseEmpty() }
            if (empty) {
                val result = withContext(Dispatchers.IO) { importer.importFromAssets() }
                refreshCounts()
                val total = result.values.sum()
                if (total > 0) emitToast("$total کانفیگ به‌صورت خودکار ایمپورت شد")
            } else {
                refreshCounts()
            }
        }
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val counts = withContext(Dispatchers.IO) { importer.countsByMode() }
            _uiState.update { it.copy(configCounts = counts) }
        }
    }

    fun selectMode(mode: ConnectionMode) {
        if (_uiState.value.connectionState == ConnectionState.CONNECTING) return
        _uiState.update { it.copy(mode = mode) }
    }

    fun selectSource(source: String, onNeedActivation: () -> Unit) {
        if (source == "private" && !_uiState.value.privateUnlocked) {
            onNeedActivation()
            return
        }
        _uiState.update { it.copy(source = source) }
    }

    fun activatePrivate(codeOrLink: String): Boolean {
        if (codeOrLink.trim().length < 6) return false
        privateStore.activate(codeOrLink.trim())
        _uiState.update { it.copy(privateUnlocked = true, source = "private") }
        emitToast("منبع شخصی فعال شد")
        return true
    }

    fun onPowerTap() {
        when (_uiState.value.connectionState) {
            ConnectionState.IDLE -> connect()
            ConnectionState.CONNECTING -> cancelConnecting()
            ConnectionState.CONNECTED -> disconnect()
        }
    }

    // ── مرحله ۱ و ۲: پیدا کردن کانفیگ سالم (تست واقعی) + تبدیل به کانفیگ واقعی Xray-core ──

    private fun connect() {
        val mode = _uiState.value.mode
        _uiState.update {
            it.copy(connectionState = ConnectionState.CONNECTING, progress = 0f, activeConfig = null, pingMs = null)
        }

        connectJob = viewModelScope.launch {
            val allConfigs = withContext(Dispatchers.IO) { importer.linksFor(mode) }

            if (allConfigs.isEmpty()) {
                emitToast("هیچ کانفیگی در بخش «${mode.label}» نیست — اول از ⚙ ایمپورت کن")
                _uiState.update { it.copy(connectionState = ConnectionState.IDLE, progress = 0f) }
                return@launch
            }

            val found = withContext(Dispatchers.IO) {
                when (mode) {
                    ConnectionMode.FAST -> searchFast(allConfigs)
                    ConnectionMode.HARD -> searchHard(allConfigs)
                    else -> searchFirstHealthy(allConfigs)
                }
            }

            if (!isActive) return@launch

            if (found == null) {
                emitToast("هیچ کانفیگ سالمی در بخش «${mode.label}» جواب نداد")
                _uiState.update { it.copy(connectionState = ConnectionState.IDLE, progress = 0f) }
                return@launch
            }

            val configJson = withContext(Dispatchers.Default) {
                ConfigConverter.buildFullConfig(found.entity.link)
            }
            if (configJson == null) {
                emitToast("این کانفیگ به فرمت هسته قابل تبدیل نیست")
                _uiState.update { it.copy(connectionState = ConnectionState.IDLE, progress = 0f) }
                return@launch
            }

            _uiState.update { it.copy(activeConfig = found.entity, progress = 1f) }
            pendingConfigJson = configJson
            pendingMode = mode

            // ── مرحله ۳: اجازه‌ی VPN ──
            val app = getApplication<Application>()
            val prepareIntent = VpnService.prepare(app)
            if (prepareIntent != null) {
                _vpnPermissionRequest.emit(prepareIntent)
            } else {
                startRealVpnService(configJson)
            }
        }
    }

    /** بعد از این‌که کاربر توی دیالوگ سیستمی اندروید اجازه داد. */
    fun onVpnPermissionGranted() {
        val json = pendingConfigJson ?: return
        startRealVpnService(json)
    }

    fun onVpnPermissionDenied() {
        pendingConfigJson = null
        emitToast("بدون اجازه‌ی VPN امکان اتصال نیست")
        _uiState.update { it.copy(connectionState = ConnectionState.IDLE, progress = 0f, activeConfig = null) }
    }

    // ── مرحله ۴: استارت واقعی سرویس VPN ──

    private fun startRealVpnService(configJson: String) {
        val app = getApplication<Application>()
        val intent = Intent(app, ArkaVpnService::class.java).apply {
            action = ArkaVpnService.ACTION_CONNECT
            putExtra(ArkaVpnService.EXTRA_CONFIG_JSON, configJson)
        }
        ContextCompat.startForegroundService(app, intent)
        // وضعیت CONNECTED واقعاً وقتی ست می‌شه که ArkaCoreManager.isRunning از طریق watchRealCoreState() بالا بره
    }

    /** مرحله ۵: گوش دادن دائمی به وضعیت واقعی هسته — نه شبیه‌سازی. */
    private fun watchRealCoreState() {
        viewModelScope.launch {
            ArkaCoreManager.isRunning.collect { running ->
                if (running) {
                    _uiState.update { it.copy(connectionState = ConnectionState.CONNECTED, progress = 1f) }
                    pendingMode?.let { emitToast(it.connectedMessage) }
                    pendingConfigJson = null
                    startRealStatsLoop()
                } else {
                    statsJob?.cancel()
                    if (_uiState.value.connectionState != ConnectionState.IDLE) {
                        _uiState.update {
                            it.copy(
                                connectionState = ConnectionState.IDLE,
                                progress = 0f,
                                activeConfig = null,
                                pingMs = null,
                                elapsedSeconds = 0,
                                dataUsageMb = 0.0,
                                currentSpeedMbps = 0.0
                            )
                        }
                        realSpeedMbpsForGraph = 0.0
                    }
                }
            }
        }
    }

    /** پینگ و ترافیک واقعی — مستقیم از هسته‌ی وصل‌شده، نه رندوم. */
    private fun startRealStatsLoop() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            var seconds = 0
            var lastTotalBytes = 0L
            launch {
                while (isActive) {
                    delay(1000)
                    seconds++
                    val stats = withContext(Dispatchers.IO) { ArkaCoreManager.queryTrafficStats() }
                    val totalBytes = stats.sumOf { it.third }
                    val deltaBytes = (totalBytes - lastTotalBytes).coerceAtLeast(0)
                    lastTotalBytes = totalBytes
                    val speedMbps = (deltaBytes * 8) / 1_000_000.0
                    realSpeedMbpsForGraph = speedMbps
                    _uiState.update {
                        it.copy(
                            elapsedSeconds = seconds,
                            dataUsageMb = totalBytes / 1024.0 / 1024.0,
                            currentSpeedMbps = speedMbps
                        )
                    }
                }
            }
            launch {
                while (isActive) {
                    val ping = withContext(Dispatchers.IO) { ArkaCoreManager.measureDelay() }
                    if (ping >= 0) _uiState.update { it.copy(pingMs = ping.toInt()) }
                    delay(4000)
                }
            }
        }
    }

    /** «سریع»: تا ۲۰ کانفیگ رو موازی تست می‌کنه، کم‌تاخیرترینِ سالم رو انتخاب می‌کنه. */
    private suspend fun searchFast(configs: List<ConfigEntity>): Found? {
        val pool = configs.take(20)
        if (pool.isEmpty()) return null
        val results = testBatch(pool, concurrency = 10, totalForProgress = pool.size)
        val best = results.filter { it.second.reachable }.minByOrNull { it.second.latencyMs }
        return best?.let { Found(it.first, it.second) }
    }

    /** «عادی» / «ثابت» / «آمریکا»: به ترتیب تست می‌کنه و روی اولین کانفیگ سالم می‌ایسته. */
    private suspend fun searchFirstHealthy(configs: List<ConfigEntity>): Found? {
        val pool = configs.take(15)
        if (pool.isEmpty()) return null
        val total = pool.size
        for ((index, cfg) in pool.withIndex()) {
            val result = ConfigTester.testReachability(cfg.link)
            _uiState.update { it.copy(progress = (index + 1) / total.toFloat()) }
            if (result.reachable) return Found(cfg, result)
        }
        return null
    }

    /** «سخت»: لیست بزرگ رو موازی تست می‌کنه، اگه پاس اول هیچی پیدا نکرد یک بار دیگه امتحان می‌کنه. */
    private suspend fun searchHard(configs: List<ConfigEntity>): Found? {
        val pool = configs.take(300)
        if (pool.isEmpty()) return null
        val totalPlanned = pool.size * 2

        val firstPass = testBatch(pool, concurrency = 15, totalForProgress = totalPlanned)
        val firstHit = firstPass.filter { it.second.reachable }.minByOrNull { it.second.latencyMs }
        if (firstHit != null) {
            _uiState.update { it.copy(progress = 1f) }
            return Found(firstHit.first, firstHit.second)
        }

        val secondPass = testBatch(pool, concurrency = 15, totalForProgress = totalPlanned, progressOffset = pool.size)
        _uiState.update { it.copy(progress = 1f) }
        val secondHit = secondPass.filter { it.second.reachable }.minByOrNull { it.second.latencyMs }
        return secondHit?.let { Found(it.first, it.second) }
    }

    private suspend fun testBatch(
        configs: List<ConfigEntity>,
        concurrency: Int,
        totalForProgress: Int,
        progressOffset: Int = 0
    ): List<Pair<ConfigEntity, ConfigTester.TestResult>> = coroutineScope {
        val completed = java.util.concurrent.atomic.AtomicInteger(progressOffset)
        configs.chunked(concurrency).flatMap { chunk ->
            chunk.map { cfg ->
                async(Dispatchers.IO) {
                    val result = ConfigTester.testReachability(cfg.link)
                    val done = completed.incrementAndGet()
                    _uiState.update { it.copy(progress = done / totalForProgress.toFloat()) }
                    cfg to result
                }
            }.awaitAll()
        }
    }

    private fun cancelConnecting() {
        connectJob?.cancel()
        pendingConfigJson = null
        pendingMode = null
        val app = getApplication<Application>()
        app.startService(Intent(app, ArkaVpnService::class.java).apply { action = ArkaVpnService.ACTION_DISCONNECT })
        _uiState.update { it.copy(connectionState = ConnectionState.IDLE, progress = 0f, activeConfig = null) }
        emitToast("اتصال لغو شد")
    }

    private fun disconnect() {
        val app = getApplication<Application>()
        app.startService(Intent(app, ArkaVpnService::class.java).apply { action = ArkaVpnService.ACTION_DISCONNECT })
        emitToast("اتصال قطع شد")
        // بقیه‌ی ریست state (پینگ/تایمر/داده) خودکار توسط watchRealCoreState() وقتی isRunning=false بشه انجام می‌شه
    }

    private fun startSparkline() {
        viewModelScope.launch {
            while (isActive) {
                delay(160)
                val state = _uiState.value
                val target = if (state.connectionState == ConnectionState.CONNECTED) {
                    (realSpeedMbpsForGraph.coerceIn(0.0, 50.0) / 50.0) * 100.0
                } else {
                    kotlin.random.Random.nextDouble(3.0, 8.0)
                }
                val last = state.sparkline.lastOrNull() ?: 5f
                val next = last + (target.toFloat() - last) * 0.42f
                val newList = state.sparkline.drop(1) + next
                _uiState.update { it.copy(sparkline = newList) }
            }
        }
    }

    // ── دیالوگ تنظیمات (⚙) ──

    fun importFromAssets() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { importer.importFromAssets() }
            refreshCounts()
            val added = result.filter { it.value > 0 }
            val msg = if (added.isEmpty()) {
                "کانفیگ جدیدی در فایل‌ها پیدا نشد"
            } else {
                added.entries.joinToString("، ") { "${it.value} کانفیگ به «${it.key.label}»" } + " اضافه شد"
            }
            emitToast(msg)
        }
    }

    fun importFromClipboard(mode: ConnectionMode, text: String) {
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) { importer.importFromClipboard(mode, text) }
            refreshCounts()
            val msg = if (added > 0) {
                "$added کانفیگ به بخش «${mode.label}» اضافه شد"
            } else {
                "کانفیگ جدیدی برای بخش «${mode.label}» پیدا نشد (تکراری یا پروتکل نامعتبر بود)"
            }
            emitToast(msg)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { importer.clearAll() }
            refreshCounts()
            emitToast("دیتابیس پاک شد")
        }
    }

    fun emitToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
