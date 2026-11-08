package com.nexus.gamespace.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

data class ShizukuLogEntry(val timestamp: Long, val message: String)

/**
 * Estado real de Shizuku. Lee el servicio solo cuando se le pide (refresh) o cuando Shizuku avisa
 * (Binder recibido / perdido / resultado de permiso). No sondea en bucle.
 */
class ShizukuManager(context: Context) {

    private val appContext = context.applicationContext
    private val permissions = ShizukuPermissionManager()

    private val _state = MutableStateFlow(ShizukuState.initial())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<ShizukuLogEntry>>(emptyList())
    val log: StateFlow<List<ShizukuLogEntry>> = _log.asStateFlow()

    @Volatile private var binderWasLost = false

    private val connection = ShizukuConnectionListener(
        onBinderReceived = {
            binderWasLost = false
            addLog("Binder de Shizuku recibido")
            refresh()
        },
        onBinderDead = {
            binderWasLost = true
            addLog("Binder de Shizuku perdido")
            refresh()
        },
        onPermissionResult = { code, result ->
            val granted = result == PackageManager.PERMISSION_GRANTED
            addLog("Resultado de la solicitud ($code): ${if (granted) "concedido" else "denegado"}")
            refresh()
        },
    )

    /** Registra los listeners y hace una primera comprobación. Llamar a stop() al terminar. */
    fun start() {
        connection.register()
        refresh()
    }

    fun stop() {
        connection.unregister()
    }

    fun refresh() {
        val installed = isShizukuInstalled()
        val binderAlive = try { Shizuku.pingBinder() } catch (t: Throwable) { false }

        var permission: PermissionCheck? = null
        var version: Int? = null
        var uid: Int? = null
        var error: String? = null

        if (binderAlive) {
            val result = permissions.check()
            permission = result.check
            error = result.error
            version = try { Shizuku.getVersion() } catch (t: Throwable) { null }
            if (permission == PermissionCheck.GRANTED) {
                uid = try { Shizuku.getUid() } catch (t: Throwable) { null }
            }
        }

        val status = ShizukuStatusResolver.resolve(
            ShizukuInputs(installed, binderAlive, binderWasLost, permission, version)
        )
        _state.value = ShizukuState(
            status = status,
            installed = installed,
            binderAlive = binderAlive,
            permission = permission,
            serverVersion = version,
            mode = uid?.let { ShizukuMode.fromUid(it) },
            uid = uid,
            lastCheckedAt = System.currentTimeMillis(),
            error = error,
        )
    }

    /** Comprobación manual pedida por el usuario: deja constancia en el registro. */
    fun manualCheck() {
        refresh()
        addLog("Comprobación manual: ${_state.value.status.label}")
    }

    fun requestPermission() {
        val current = _state.value
        if (!current.canRequestPermission) {
            addLog("Solicitud no enviada: ${current.status.label}")
            return
        }
        val error = permissions.request(REQUEST_CODE)
        addLog(if (error == null) "Solicitud de autorización enviada" else "Error al solicitar autorización: $error")
    }

    fun openShizukuIntent(): Intent? =
        appContext.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)

    private fun isShizukuInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun addLog(message: String) {
        _log.value = (listOf(ShizukuLogEntry(System.currentTimeMillis(), message)) + _log.value).take(MAX_LOG)
    }

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE = 1001
        const val MAX_LOG = 30
    }
}
