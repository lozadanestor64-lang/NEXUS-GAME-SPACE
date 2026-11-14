package com.nexus.gamespace.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nexus.gamespace.MainActivity
import com.nexus.gamespace.R
import com.nexus.gamespace.booster.BoosterController
import com.nexus.gamespace.data.NexusRepository
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.CrosshairSettings
import com.nexus.gamespace.domain.OverlayColors
import com.nexus.gamespace.domain.RgbSettings
import com.nexus.gamespace.domain.OverlayGestures
import com.nexus.gamespace.domain.ColorRole
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
import com.nexus.gamespace.domain.TabGesture
import com.nexus.gamespace.shizuku.ShizukuManager
import com.nexus.gamespace.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Servicio en primer plano que dibuja el panel gaming por encima de otras apps con una ventana
 * TYPE_APPLICATION_OVERLAY: primero una pestaña lateral y, al abrirla, el panel completo.
 * No inyecta toques ni usa accesibilidad. Se inicia solo cuando el usuario lo activa y se detiene con
 * DESACTIVAR o con la acción «Detener» de su notificación.
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "com.nexus.gamespace.overlay.START"
        const val ACTION_STOP = "com.nexus.gamespace.overlay.STOP"
        const val ACTION_SHOW = "com.nexus.gamespace.overlay.SHOW"

        private const val CHANNEL_ID = "overlay_panel"
        private const val NOTIFICATION_ID = 4101
        private const val TAB_W_DP = 20f
        private const val TAB_H_DP = 88f
        private const val EXPAND_THRESHOLD_DP = 40f
        private const val CLOSE_ANIMATION_MS = 260L

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, OverlayService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private enum class Mode { NONE, TAB, PANEL, HIDDEN }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var wm: WindowManager
    private lateinit var repo: NexusRepository
    private lateinit var controller: BoosterController
    private lateinit var shizuku: ShizukuManager

    private var mode = Mode.NONE
    private var tabView: View? = null
    private var panelRoot: PanelRoot? = null
    private var crosshairView: CrosshairView? = null
    private var observing = false
    private var closing = false
    private var notesFocusable = false

    // Estado que lee la interfaz Compose del panel.
    private var prefs by mutableStateOf(OverlayPrefs())
    private var colors by mutableStateOf(OverlayColors())
    private var rgb by mutableStateOf(RgbSettings())
    private var panelVisible by mutableStateOf(false)
    private var landscape by mutableStateOf(false)
    private var maxHeightDp by mutableStateOf(480f)
    private var shizukuLabel by mutableStateOf("Sin comprobar")

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        wm = getSystemService(WindowManager::class.java)
        repo = NexusRepository(this)
        controller = BoosterController(this, scope)
        shizuku = ShizukuManager(this)
        createChannel()
        _running.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Obligatorio tras startForegroundService: se llama siempre primero.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), foregroundType())
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (observing) showTab() else startObserver()
            }
            else -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (mode == Mode.NONE) startObserver()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        landscape = isLandscape()
        updateMaxHeight()
        when (mode) {
            Mode.TAB -> showTab()
            Mode.PANEL -> panelRoot?.let { wm.updateViewLayout(it, panelParams()) }
            else -> Unit
        }
    }

    override fun onDestroy() {
        removeCrosshair()
        removeTab()
        removePanelNow()
        scope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        _running.value = false
        super.onDestroy()
    }

    // ------------------------------------------------------------------ preferencias

    private fun startObserver() {
        if (observing) return
        observing = true
        scope.launch {
            prefs = repo.overlayPrefs.first()
            landscape = isLandscape()
            updateMaxHeight()
            if (mode == Mode.NONE) showTab()
            repo.overlayPrefs.collect { newPrefs ->
                if (newPrefs == prefs) return@collect
                prefs = newPrefs
                updateMaxHeight()
                when (mode) {
                    Mode.TAB -> showTab()
                    Mode.PANEL -> panelRoot?.let { wm.updateViewLayout(it, panelParams()) }
                    else -> Unit
                }
            }
        }
        scope.launch { shizuku.state.collect { shizukuLabel = it.status.label } }
        scope.launch { repo.crosshair.collect { applyCrosshair(it) } }
        scope.launch { repo.overlayRgb.collect { rgb = it } }
        scope.launch {
            repo.overlayColors.collect { newColors ->
                val tabColorChanged = newColors.raw(ColorRole.ACTIVE_BUTTON) != colors.raw(ColorRole.ACTIVE_BUTTON)
                colors = newColors
                if (tabColorChanged && mode == Mode.TAB) showTab()
            }
        }
    }

    // ------------------------------------------------------------------ mira

    /**
     * La mira es una ventana pequeña y no táctil en el centro de la pantalla. Solo dibuja formas fijas.
     * Con alpha de ventana ≤ 0.8 Android 12+ deja pasar los toques al juego que hay debajo.
     */
    private fun applyCrosshair(settings: CrosshairSettings) {
        if (!settings.shouldShow(settings.activeGame)) {
            removeCrosshair()
            return
        }
        val config = settings.configFor(settings.activeGame)
        val d = resources.displayMetrics.density
        val existing = crosshairView
        if (existing == null) {
            val cfg = config
            val view = CrosshairView(this).apply { this.config = cfg }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.CENTER
                x = (config.offsetX * d).toInt()
                y = (config.offsetY * d).toInt()
                alpha = 0.8f
            }
            try {
                wm.addView(view, lp)
                crosshairView = view
            } catch (e: RuntimeException) {
                crosshairView = null
            }
        } else {
            existing.config = config
            (existing.layoutParams as? WindowManager.LayoutParams)?.let { lp ->
                lp.x = (config.offsetX * d).toInt()
                lp.y = (config.offsetY * d).toInt()
                try { wm.updateViewLayout(existing, lp) } catch (e: IllegalArgumentException) { crosshairView = null }
            }
        }
    }

    private fun removeCrosshair() {
        crosshairView?.let {
            try { wm.removeView(it) } catch (e: IllegalArgumentException) { /* ya quitada */ }
        }
        crosshairView = null
    }

    // ------------------------------------------------------------------ pestaña lateral

    private fun density(): Float = resources.displayMetrics.density * prefs.scalePercent / 100f

    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.currentWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }

    private fun isLandscape(): Boolean = screenSize().let { it.first > it.second }

    private fun updateMaxHeight() {
        val (_, sh) = screenSize()
        val available = sh / density() - 2f * prefs.marginDp - 8f
        maxHeightDp = (available * prefs.heightPercent / 100f).coerceAtLeast(200f)
    }

    private fun showTab() {
        removeTab()
        removePanelNow()
        mode = Mode.TAB
        val d = density()
        val (sw, sh) = screenSize()
        val w = (TAB_W_DP * d).toInt()
        val h = (TAB_H_DP * d).toInt()
        val lp = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (prefs.side == OverlaySide.LEFT) 0 else sw - w
            y = (if (prefs.tabY >= 0) prefs.tabY else sh / 3).coerceIn(0, (sh - h).coerceAtLeast(0))
        }
        val view = buildTabView(prefs.side, d)
        tabView = view
        try {
            wm.addView(view, lp)
        } catch (e: RuntimeException) {
            tabView = null
            mode = Mode.NONE
            stopSelf()
        }
    }

    private fun buildTabView(side: OverlaySide, d: Float): View {
        val r = 10f * d
        val radii = if (side == OverlaySide.LEFT) {
            floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
        } else {
            floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
        }
        val alpha = (prefs.alphaPercent / 100f * 255).toInt().coerceIn(100, 255)
        val tabRgb = (this.colors.raw(ColorRole.ACTIVE_BUTTON) and 0xFFFFFFL).toInt()
        val bg = GradientDrawable().apply {
            setColor((alpha shl 24) or tabRgb)
            cornerRadii = radii
            setStroke((1f * d).toInt().coerceAtLeast(1), 0x66FFFFFF)
        }
        return View(this).apply {
            background = bg
            contentDescription = "Abrir el panel de NEXUS GAME SPACE"
            setOnTouchListener(TabTouchListener())
        }
    }

    private inner class TabTouchListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false
        private val slop = ViewConfiguration.get(this@OverlayService).scaledTouchSlop

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val lp = v.layoutParams as? WindowManager.LayoutParams ?: return false
            val (sw, sh) = screenSize()
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = lp.x; startY = lp.y
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) moved = true
                    if (moved) {
                        lp.x = (startX + dx).toInt().coerceIn(0, (sw - lp.width).coerceAtLeast(0))
                        lp.y = (startY + dy).toInt().coerceIn(0, (sh - lp.height).coerceAtLeast(0))
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        showPanel()
                    } else {
                        val dx = e.rawX - downX
                        val center = lp.x + lp.width / 2f
                        val gesture = OverlayGestures.onRelease(
                            side = prefs.side, totalDx = dx, centerX = center, screenWidth = sw,
                            expandThresholdPx = EXPAND_THRESHOLD_DP * resources.displayMetrics.density,
                        )
                        if (gesture == TabGesture.EXPAND) {
                            showPanel()
                        } else {
                            val newSide = OverlayGestures.sideFor(center, sw)
                            lp.x = if (newSide == OverlaySide.LEFT) 0 else sw - lp.width
                            wm.updateViewLayout(v, lp)
                            val y = lp.y
                            scope.launch { repo.setOverlayPosition(newSide, y) }
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> Unit
            }
            return true
        }
    }

    private fun removeTab() {
        tabView?.let {
            try { wm.removeView(it) } catch (e: IllegalArgumentException) { /* ya quitada */ }
        }
        tabView = null
    }

    // ------------------------------------------------------------------ panel expandido

    private class PanelRoot(context: Context, private val onOutside: () -> Unit) : FrameLayout(context) {
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                onOutside()
                return true
            }
            return super.dispatchTouchEvent(ev)
        }
    }

    private fun panelParams(): WindowManager.LayoutParams {
        val (sw, sh) = screenSize()
        val d = density()
        val baseWidth = if (sw > sh) minOf(sw * 0.82f, 780f * d) else minOf(sw - 16f * d, 400f * d)
        val width = baseWidth * prefs.widthPercent / 100f
        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        // Solo se permite el foco (para escribir una nota) mientras la sección de notas está abierta.
        if (!notesFocusable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return WindowManager.LayoutParams(
            width.toInt(), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or if (prefs.side == OverlaySide.LEFT) Gravity.START else Gravity.END
            x = (prefs.marginDp * d).toInt()
            y = ((prefs.marginDp + 4) * d).toInt()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
    }

    private fun showPanel() {
        removeTab()
        removePanelNow()
        mode = Mode.PANEL
        closing = false
        notesFocusable = false
        controller.refreshAccess()
        shizuku.refresh()
        landscape = isLandscape()
        updateMaxHeight()

        val owner = this
        val compose = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                OverlayPanel(
                    controller = controller,
                    prefs = prefs,
                    colors = colors,
                    rgb = rgb,
                    shizukuLabel = shizukuLabel,
                    landscape = landscape,
                    maxHeightDp = maxHeightDp,
                    visible = panelVisible,
                    onClose = { closePanel() },
                    onHide = { hideOverlay() },
                    onSwitchSide = { switchSide() },
                    onOpenLibrary = { openLibrary() },
                    onLaunchGame = { launchGame(it) },
                    onOpenSettings = { openSettingsIntent(it) },
                    onNeedsKeyboard = { setNotesFocusable(it) },
                )
            }
        }
        val root = PanelRoot(this) { closePanel() }
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
        root.addView(compose)
        panelRoot = root
        panelVisible = false
        try {
            wm.addView(root, panelParams())
        } catch (e: RuntimeException) {
            panelRoot = null
            mode = Mode.NONE
            stopSelf()
            return
        }
        scope.launch {
            delay(40L)
            panelVisible = true
        }
    }

    private fun closePanel() {
        if (mode != Mode.PANEL || closing) return
        closing = true
        panelVisible = false
        scope.launch {
            delay(if (prefs.animation == AnimationMode.OFF) 0L else CLOSE_ANIMATION_MS)
            controller.message.value = null
            showTab()
        }
    }

    private fun removePanelNow() {
        panelRoot?.let {
            try { wm.removeView(it) } catch (e: IllegalArgumentException) { /* ya quitado */ }
        }
        panelRoot = null
        panelVisible = false
        closing = false
        notesFocusable = false
    }

    /** Oculta pestaña y panel hasta que el usuario pulse «Mostrar» en la notificación. */
    private fun hideOverlay() {
        removeTab()
        removePanelNow()
        mode = Mode.HIDDEN
    }

    private fun switchSide() {
        val next = prefs.side.other()
        scope.launch { repo.setOverlaySide(next) }
    }

    private fun setNotesFocusable(enabled: Boolean) {
        if (notesFocusable == enabled) return
        notesFocusable = enabled
        panelRoot?.let { wm.updateViewLayout(it, panelParams()) }
    }

    // ------------------------------------------------------------------ acciones que salen del panel

    private fun launchGame(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            controller.message.value = "No se puede abrir esa app."
            return
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            controller.markLaunched(packageName)
            closePanel()
        } catch (e: ActivityNotFoundException) {
            controller.message.value = "No se puede abrir esa app."
        }
    }

    private fun openSettingsIntent(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            closePanel()
        } catch (e: ActivityNotFoundException) {
            controller.message.value = "Este dispositivo no tiene esa pantalla de ajustes."
        }
    }

    private fun openLibrary() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, Screen.Library.path)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        try {
            startActivity(intent)
            closePanel()
        } catch (e: ActivityNotFoundException) {
            controller.message.value = "No se pudo abrir la biblioteca."
        }
    }

    // ------------------------------------------------------------------ notificación

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Panel flotante", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Indica que el panel gaming flotante está activo."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, OverlayService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nexus)
            .setContentTitle("NEXUS GAME SPACE")
            .setContentText("Panel flotante activo")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Mostrar", serviceIntent(ACTION_SHOW, 1))
            .addAction(0, "Detener", serviceIntent(ACTION_STOP, 2))
            .build()
    }
}
