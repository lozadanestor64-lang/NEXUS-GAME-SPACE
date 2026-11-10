package com.nexus.gamespace.features.booster

import android.app.Application
import android.app.NotificationManager
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.gamespace.core.BoosterActions
import com.nexus.gamespace.core.DeviceInfoProvider
import com.nexus.gamespace.data.NexusRepository
import com.nexus.gamespace.domain.BoosterMath
import com.nexus.gamespace.domain.BoosterRestoreState
import com.nexus.gamespace.domain.DeviceSnapshot
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.domain.Stopwatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccessState(val dnd: Boolean = false, val writeSettings: Boolean = false)

class BoosterViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NexusRepository(app)
    private val actions = BoosterActions(app)
    private val provider = DeviceInfoProvider(app)

    /** Se actualiza cada 2 s solo mientras la pantalla está visible. */
    val snapshot: StateFlow<DeviceSnapshot?> = flow {
        while (true) {
            emit(provider.snapshot())
            delay(2_000L)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profiles: StateFlow<List<GameProfile>> =
        repo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeProfileId: StateFlow<String> =
        repo.activeProfileId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "balanced")
    val restoreState: StateFlow<BoosterRestoreState> =
        repo.boosterRestore.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoosterRestoreState())

    val access = MutableStateFlow(readAccess())
    val currentBrightnessPercent = MutableStateFlow<Int?>(null)
    val message = MutableStateFlow<String?>(null)
    val stopwatch = MutableStateFlow(Stopwatch())
    val notes = MutableStateFlow("")

    init {
        refreshAccess()
        viewModelScope.launch { notes.value = repo.notes.first() }
    }

    private fun readAccess() = AccessState(actions.hasDndAccess(), actions.canWriteSettings())

    /** Hay que llamarlo al volver de Ajustes: nunca se supone que el permiso fue concedido. */
    fun refreshAccess() {
        access.value = readAccess()
        currentBrightnessPercent.value = actions.currentBrightness()?.let { BoosterMath.brightnessToPercent(it) }
    }

    fun setActiveProfile(id: String) = viewModelScope.launch { repo.setActiveProfile(id) }

    fun dndAccessIntent() = actions.dndAccessIntent()
    fun writeSettingsIntent() = actions.writeSettingsIntent()
    fun connectivityIntent() = actions.connectivityIntent()

    fun showVolumePanel() {
        actions.showVolumePanel()
    }

    // ---- No molestar ----
    fun applyDnd() {
        viewModelScope.launch {
            if (!actions.hasDndAccess()) {
                message.value = "Falta conceder el acceso a No molestar."
                return@launch
            }
            val had = repo.boosterRestore.first().dndPrevious != null
            if (!had) repo.saveDndPreviousIfAbsent(actions.currentDndFilter())
            val ok = actions.setDndFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            if (!ok && !had) repo.clearDndPrevious()
            message.value = if (ok) {
                "No molestar activado (solo interrupciones prioritarias). Se puede restaurar."
            } else {
                "Android rechazó el cambio de No molestar."
            }
        }
    }

    fun restoreDnd() {
        viewModelScope.launch { doRestoreDnd() }
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
        viewModelScope.launch {
            if (!actions.canWriteSettings()) {
                message.value = "Falta conceder el permiso para modificar ajustes del sistema."
                return@launch
            }
            val state = repo.boosterRestore.first()
            val had = state.brightnessPrevious != null
            val currentValue = actions.currentBrightness()
            if (currentValue == null) {
                message.value = "No se pudo leer el brillo actual, así que no se cambia nada."
                return@launch
            }
            if (!had) repo.saveBrightnessPreviousIfAbsent(currentValue, actions.currentBrightnessMode() ?: 0)
            val ok = actions.setBrightness(BoosterMath.percentToBrightness(percent), mode = 0)
            if (!ok && !had) repo.clearBrightnessPrevious()
            message.value = if (ok) "Brillo en $percent %. Se puede restaurar." else "Android rechazó el cambio de brillo."
            refreshAccess()
        }
    }

    fun restoreBrightness() {
        viewModelScope.launch { doRestoreBrightness() }
    }

    private suspend fun doRestoreBrightness() {
        val state = repo.boosterRestore.first()
        val value = state.brightnessPrevious ?: return
        val ok = actions.setBrightness(value, state.brightnessModePrevious ?: 0)
        if (ok) repo.clearBrightnessPrevious()
        message.value = if (ok) "Brillo restaurado." else "No se pudo restaurar el brillo (¿se revocó el permiso?)."
        refreshAccess()
    }

    fun restoreAll() {
        viewModelScope.launch {
            doRestoreDnd()
            doRestoreBrightness()
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
        viewModelScope.launch { repo.saveNotes(text) }
    }
}
