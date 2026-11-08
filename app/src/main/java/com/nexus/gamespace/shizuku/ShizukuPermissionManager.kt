package com.nexus.gamespace.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

data class PermissionResult(val check: PermissionCheck, val error: String? = null)

/** Comprueba y solicita la autorización de Shizuku usando solo la API oficial. */
class ShizukuPermissionManager {

    fun check(): PermissionResult = try {
        when {
            Shizuku.isPreV11() -> PermissionResult(PermissionCheck.UNSUPPORTED, "Versión de Shizuku anterior a la 11")
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> PermissionResult(PermissionCheck.GRANTED)
            Shizuku.shouldShowRequestPermissionRationale() -> PermissionResult(PermissionCheck.DENIED)
            else -> PermissionResult(PermissionCheck.NOT_GRANTED)
        }
    } catch (t: Throwable) {
        PermissionResult(PermissionCheck.ERROR, t.message ?: t.javaClass.simpleName)
    }

    /** Muestra el diálogo de Shizuku. El resultado llega al listener de permisos. */
    fun request(requestCode: Int): String? = try {
        Shizuku.requestPermission(requestCode)
        null
    } catch (t: Throwable) {
        t.message ?: t.javaClass.simpleName
    }
}
