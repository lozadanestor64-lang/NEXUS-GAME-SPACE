package com.nexus.gamespace.booster

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import com.nexus.gamespace.core.BoosterActions
import com.nexus.gamespace.core.DeviceInfoProvider
import com.nexus.gamespace.core.InstalledAppsProvider
import com.nexus.gamespace.core.NetworkInfoProvider
import com.nexus.gamespace.core.NetworkKind
import com.nexus.gamespace.data.NexusRepository
import com.nexus.gamespace.domain.BoosterMath
import com.nexus.gamespace.domain.BoosterRestoreState
import com.nexus.gamespace.domain.CrosshairConfig
import com.nexus.gamespace.domain.CrosshairSettings
import com.nexus.gamespace.domain.DeviceSnapshot
import com.nexus.gamespace.domain.OverlayColors
import com.nexus.gamespace.domain.FavoritesLogic
import com.nexus.gamespace.domain.InstalledApp
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.RgbSettings
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.domain.Stopwatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveReadings(val snapshot: DeviceSnapshot, val network: NetworkKind)

data class AccessState(
    val dnd: Boolean = false,
    val writeSettings: Boolean = false,
    val autoRotate: Boolean? = null,
    val brightnessPercent: Int? = null,
)

/**
 * Lógica de las herramientas del panel flotante y de Configuración. No depende de ninguna pantalla:
 * lo usan el servicio de superposición y los ViewModels. Cada cambio temporal guarda antes el valor
 * original y se puede restaurar.
 */
class BoosterController(context: Context, private val scope: CoroutineScope) {

    private val ctx = context.applicationContext
    private val repo = NexusRepository(ctx)
    private val actions = BoosterActions(ctx)
    private val provider = DeviceInfoProvider(ctx)
    private val network = NetworkInfoProvider(ctx)
    private val appsProvider = InstalledAppsProvider(ctx)

    /** Lectura cada 2 s, y solo mientras alguien la observa (panel abierto o pantalla visible). */
    val live: StateFlow<LiveReadings?> = flow {
        while (true) {
            emit(LiveReadings(provider.snapshot(), network.current()))
            delay(2_000L)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(1_000), null)

    val profiles: StateFlow<List<GameProfile>> =
        repo.profiles.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeProfileId: StateFlow<String> =
        repo.activeProfileId.stateIn(scope, SharingStarted.WhileSubscribed(5_000), "balanced")
    val restoreState: StateFlow<BoosterRestoreState> =
        repo.boosterRestore.stateIn(scope, SharingStarted.WhileSubscribed(5_000), BoosterRestoreState())
    val favorites: StateFlow<List<GameEntry>> =
        repo.games.map { list -> FavoritesLogic.ordered(list) }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val rgb: StateFlow<RgbSettings> =
        repo.overlayRgb.stateIn(scope, SharingStarted.WhileSubscribed(5_000), RgbSettings())
    val colors: StateFlow<OverlayColors> =
        repo.overlayColors.stateIn(scope, SharingStarted.WhileSubscribed(5_000), OverlayColors())
    val crosshair: StateFlow<CrosshairSettings> =
        repo.crosshair.stateIn(scope, SharingStarted.WhileSubscribed(5_000), CrosshairSettings())

    val access = MutableStateFlow(readAccess())
    val message = MutableStateFlow<String?>(null)
    val stopwatch = MutableStateFlow(Stopwatch())
    val notes = MutableStateFlow("")

    init {
        scope.launch { notes.value = repo.notes.first() }
    }

    private fun readAccess() = AccessState(
        dnd = actions.hasDndAccess(),
        writeSettings = actions.canWriteSettings(),
        autoRotate = actions.autoRotateEnabled(),
        brightnessPercent = actions.currentBrightness()?.let { BoosterMath.brightnessToPercent(it) },
    )

    /** Hay que llamarlo al abrir el panel y al volver de Ajustes: nunca se supone un permiso concedido. */
    fun refreshAccess() {
        access.value = readAccess()
    }

    fun dndAccessIntent() = actions.dndAccessIntent()
    fun writeSettingsIntent() = actions.writeSettingsIntent()
    fun connectivityIntent() = actions.connectivityIntent()

    fun setActiveProfile(id: String) {
        scope.launch { repo.setActiveProfile(id) }
    }

    fun markLaunched(packageName: String) {
        scope.launch {
            repo.updateCrosshair { it.copy(activeGame = packageName) }
            repo.updateGames { list ->
                list.map { if (it.packageName == packageName) it.copy(lastLaunchedAt = System.currentTimeMillis()) else it }
            }
        }
    }

    // ---- Favoritos del panel (misma biblioteca que la app) ----
    /** Lee las apps lanzables (visibilidad limitada de paquetes). Se llama al abrir AGREGAR JUEGOS. */
    fun loadInstalledApps() {
        scope.launch(Dispatchers.Default) { installedApps.value = appsProvider.launchableApps() }
    }

    fun addFavorites(apps: List<InstalledApp>) {
        scope.launch { repo.updateGames { list -> FavoritesLogic.addFavorites(list, apps, System.currentTimeMillis()) } }
    }

    fun removeFavorite(packageName: String) {
        scope.launch { repo.updateGames { list -> FavoritesLogic.removeFavorite(list, packageName) } }
    }

    fun moveFavorite(packageName: String, delta: Int) {
        scope.launch { repo.updateGames { list -> FavoritesLogic.move(list, packageName, delta) } }
    }

    fun saveAppearance(prefs: OverlayPrefs) {
        scope.launch { repo.saveOverlayAppearance(prefs) }
    }

    fun setSide(side: com.nexus.gamespace.domain.OverlaySide) {
        scope.launch { repo.setOverlaySide(side) }
    }

    // ---- RGB ----
    fun saveRgb(rgb: RgbSettings) {
        scope.launch { repo.saveOverlayRgb(rgb) }
    }

    // ---- Colores ----
    fun saveColors(colors: OverlayColors) {
        scope.launch { repo.saveOverlayColors(colors) }
    }

    // ---- Mira ----
    fun setCrosshairEnabled(enabled: Boolean) {
        scope.launch { repo.updateCrosshair { it.copy(enabled = enabled) } }
    }

    /** game == null guarda la mira global; con un paquete, guarda una mira propia de ese juego. */
    fun saveCrosshair(game: String?, config: CrosshairConfig) {
        scope.launch { repo.updateCrosshair { it.withConfig(game, config) } }
    }

    fun clearCrosshairOverride(game: String) {
        scope.launch { repo.updateCrosshair { it.withoutOverride(game) } }
    }

    fun setCrosshairExcluded(game: String, excluded: Boolean) {
        scope.launch { repo.updateCrosshair { it.withExcluded(game, excluded) } }
    }

    fun adjustVolume(raise: Boolean) = actions.adjustVolume(raise)
    fun showVolumePanel() = actions.showVolumePanel()

    // ---- No molestar ----
    fun applyDnd() {
        scope.launch {
            if (!actions.hasDndAccess()) {
                message.value = "Falta conceder el acceso a No molestar."
                return@launch
            }
            val had = repo.boosterRestore.first().dndPrevious != null
            if (!had) repo.saveDndPreviousIfAbsent(actions.currentDndFilter())
            val ok = actions.setDndFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            if (!ok && !had) repo.clearDndPrevious()
            message.value = if (ok) "No molestar activado (solo prioritarias)." else "Android rechazó el cambio de No molestar."
        }
    }

    fun restoreDnd() {
        scope.launch { doRestoreDnd() }
    }

    private suspend fun doRestoreDnd() {
        val previous = repo.boosterRestore.first().dndPrevious ?: return
        val target = if (previous <= 0) NotificationManager.INTERRUPTION_FILTER_ALL else previous
        val ok = actions.setDndFilter(target)
        if (ok) repo.clearDndPrevious()
        message.value = if (ok) "No molestar restaurado." else "No se pudo restaurar No molestar (¿se revocó el acceso?)."
    }

    // ---- Brillo ----
    fun applyBrightness(percent: Int) {
        scope.launch {
            if (!actions.canWriteSettings()) {
                message.value = "Falta conceder el permiso para modificar ajustes del sistema."
                return@launch
            }
            val currentValue = actions.currentBrightness()
            if (currentValue == null) {
                message.value = "No se pudo leer el brillo actual, así que no se cambia nada."
                return@launch
            }
            val had = repo.boosterRestore.first().brightnessPrevious != null
            if (!had) repo.saveBrightnessPreviousIfAbsent(currentValue, actions.currentBrightnessMode() ?: 0)
            val ok = actions.setBrightness(BoosterMath.percentToBrightness(percent), mode = 0)
            if (!ok && !had) repo.clearBrightnessPrevious()
            message.value = if (ok) "Brillo en $percent %." else "Android rechazó el cambio de brillo."
            refreshAccess()
        }
    }

    fun restoreBrightness() {
        scope.launch { doRestoreBrightness() }
    }

    private suspend fun doRestoreBrightness() {
        val state = repo.boosterRestore.first()
        val value = state.brightnessPrevious ?: return
        val ok = actions.setBrightness(value, state.brightnessModePrevious ?: 0)
        if (ok) repo.clearBrightnessPrevious()
        message.value = if (ok) "Brillo restaurado." else "No se pudo restaurar el brillo (¿se revocó el permiso?)."
        refreshAccess()
    }

    // ---- Rotación automática ----
    fun setAutoRotate(enabled: Boolean) {
        scope.launch {
            if (!actions.canWriteSettings()) {
                message.value = "Falta conceder el permiso para modificar ajustes del sistema."
                return@launch
            }
            val current = actions.autoRotateEnabled()
            if (current == null) {
                message.value = "No se pudo leer la rotación actual, así que no se cambia nada."
                return@launch
            }
            val had = repo.boosterRestore.first().rotationPrevious != null
            if (!had) repo.saveRotationPreviousIfAbsent(if (current) 1 else 0)
            val ok = actions.setAutoRotate(enabled)
            if (!ok && !had) repo.clearRotationPrevious()
            message.value = if (ok) {
                if (enabled) "Rotación automática activada." else "Rotación bloqueada."
            } else {
                "Android rechazó el cambio de rotación."
            }
            refreshAccess()
        }
    }

    private suspend fun doRestoreRotation() {
        val previous = repo.boosterRestore.first().rotationPrevious ?: return
        val ok = actions.setAutoRotate(previous == 1)
        if (ok) repo.clearRotationPrevious()
        message.value = if (ok) "Rotación restaurada." else "No se pudo restaurar la rotación."
        refreshAccess()
    }

    fun restoreAll() {
        scope.launch {
            doRestoreDnd()
            doRestoreBrightness()
            doRestoreRotation()
        }
    }

    // ---- Temporizador ----
    fun toggleStopwatch() {
        val now = SystemClock.elapsedRealtime()
        val current = stopwatch.value
        stopwatch.value = if (current.running) current.pause(now) else current.start(now)
    }

    fun resetStopwatch() {
        stopwatch.value = stopwatch.value.reset()
    }

    // ---- Notas ----
    fun onNotesChanged(text: String) {
        notes.value = text
        scope.launch { repo.saveNotes(text) }
    }
}
