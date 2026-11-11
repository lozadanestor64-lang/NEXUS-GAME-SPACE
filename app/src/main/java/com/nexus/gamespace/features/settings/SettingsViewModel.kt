package com.nexus.gamespace.features.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.gamespace.booster.BoosterController
import com.nexus.gamespace.data.NexusRepository
import com.nexus.gamespace.domain.BoosterRestoreState
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlayTool
import com.nexus.gamespace.overlay.OverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado real de cada permiso. Nunca se supone concedido: se vuelve a leer al regresar de Ajustes. */
data class PermissionsState(
    val overlay: Boolean = false,
    val notifications: Boolean = false,
    val dnd: Boolean = false,
    val writeSettings: Boolean = false,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NexusRepository(app)
    private val controller = BoosterController(app, viewModelScope)

    val prefs: StateFlow<OverlayPrefs> =
        repo.overlayPrefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverlayPrefs())
    val restoreState: StateFlow<BoosterRestoreState> = controller.restoreState
    val message: StateFlow<String?> = controller.message
    val running: StateFlow<Boolean> = OverlayService.running
    val permissions = MutableStateFlow(readPermissions())

    private fun readPermissions(): PermissionsState {
        val ctx = getApplication<Application>()
        controller.refreshAccess()
        val access = controller.access.value
        return PermissionsState(
            overlay = Settings.canDrawOverlays(ctx),
            notifications = NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            dnd = access.dnd,
            writeSettings = access.writeSettings,
        )
    }

    fun refresh() {
        permissions.value = readPermissions()
    }

    fun overlayPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${getApplication<Application>().packageName}"))

    fun notificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, getApplication<Application>().packageName)

    fun dndAccessIntent() = controller.dndAccessIntent()
    fun writeSettingsIntent() = controller.writeSettingsIntent()

    fun startOverlay(context: Context) {
        if (!Settings.canDrawOverlays(context)) return
        OverlayService.start(context)
    }

    fun stopOverlay(context: Context) = OverlayService.stop(context)

    fun restoreAll() = controller.restoreAll()

    fun setSide(side: com.nexus.gamespace.domain.OverlaySide) {
        viewModelScope.launch { repo.setOverlaySide(side) }
    }

    fun setAlpha(percent: Int) {
        viewModelScope.launch { repo.setOverlayAlpha(percent) }
    }

    fun setScale(percent: Int) {
        viewModelScope.launch { repo.setOverlayScale(percent) }
    }

    fun setToolVisible(tool: OverlayTool, visible: Boolean) {
        viewModelScope.launch { repo.setOverlayToolHidden(tool.id, hidden = !visible) }
    }
}
