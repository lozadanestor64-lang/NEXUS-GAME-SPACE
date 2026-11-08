package com.nexus.gamespace.shizuku

import rikka.shizuku.Shizuku

/**
 * Registra y libera los listeners oficiales de Shizuku: Binder recibido, Binder perdido
 * y resultado de la solicitud de permiso.
 */
class ShizukuConnectionListener(
    private val onBinderReceived: () -> Unit,
    private val onBinderDead: () -> Unit,
    private val onPermissionResult: (requestCode: Int, grantResult: Int) -> Unit,
) {
    private val receivedListener = Shizuku.OnBinderReceivedListener { onBinderReceived() }
    private val deadListener = Shizuku.OnBinderDeadListener { onBinderDead() }
    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            onPermissionResult(requestCode, grantResult)
        }

    private var registered = false

    fun register() {
        if (registered) return
        Shizuku.addBinderReceivedListener(receivedListener)
        Shizuku.addBinderDeadListener(deadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        Shizuku.removeBinderReceivedListener(receivedListener)
        Shizuku.removeBinderDeadListener(deadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        registered = false
    }
}
