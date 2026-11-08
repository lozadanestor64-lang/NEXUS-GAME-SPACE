package com.nexus.gamespace.shizuku

/** Estados visibles de la integración con Shizuku. */
enum class ShizukuStatus(val label: String) {
    NOT_INSTALLED("Shizuku no instalado"),
    NOT_RUNNING("Shizuku no iniciado"),
    WAITING_AUTHORIZATION("Esperando autorización"),
    DENIED("Permiso denegado"),
    CONNECTED("Conectado"),
    CONNECTED_LIMITED("Conectado sin los privilegios requeridos"),
    CONNECTION_LOST("Conexión perdida"),
    ERROR("Error al comprobar"),
}

enum class PermissionCheck { GRANTED, NOT_GRANTED, DENIED, UNSUPPORTED, ERROR }

/** Entorno con el que se inició Shizuku, según el uid del servicio. ADB no es root. */
enum class ShizukuMode(val label: String) {
    ADB("ADB (shell)"),
    ROOT("Root"),
    OTHER("Otro");

    companion object {
        fun fromUid(uid: Int): ShizukuMode = when (uid) {
            0 -> ROOT
            2000 -> ADB
            else -> OTHER
        }
    }
}

data class ShizukuState(
    val status: ShizukuStatus,
    val installed: Boolean,
    val binderAlive: Boolean,
    val permission: PermissionCheck?,
    val serverVersion: Int?,
    val mode: ShizukuMode?,
    val uid: Int?,
    val lastCheckedAt: Long?,
    val error: String?,
) {
    val canRequestPermission: Boolean
        get() = binderAlive && permission == PermissionCheck.NOT_GRANTED

    val isReady: Boolean
        get() = status == ShizukuStatus.CONNECTED

    companion object {
        fun initial() = ShizukuState(
            status = ShizukuStatus.NOT_RUNNING,
            installed = false, binderAlive = false, permission = null,
            serverVersion = null, mode = null, uid = null, lastCheckedAt = null, error = null,
        )
    }
}

/** Datos reales leídos de Android y de la API de Shizuku, antes de decidir el estado. */
data class ShizukuInputs(
    val installed: Boolean,
    val binderAlive: Boolean,
    val binderWasLost: Boolean,
    val permission: PermissionCheck?,
    val serverVersion: Int?,
)

/** Decide el estado a partir de datos comprobados. Nunca marca «Conectado» sin Binder y sin permiso. */
object ShizukuStatusResolver {
    const val MIN_SERVER_VERSION = 11

    fun resolve(i: ShizukuInputs): ShizukuStatus = when {
        !i.binderAlive && i.binderWasLost -> ShizukuStatus.CONNECTION_LOST
        !i.binderAlive && !i.installed -> ShizukuStatus.NOT_INSTALLED
        !i.binderAlive -> ShizukuStatus.NOT_RUNNING
        i.permission == null || i.permission == PermissionCheck.ERROR -> ShizukuStatus.ERROR
        i.permission == PermissionCheck.UNSUPPORTED -> ShizukuStatus.CONNECTED_LIMITED
        i.permission == PermissionCheck.DENIED -> ShizukuStatus.DENIED
        i.permission == PermissionCheck.NOT_GRANTED -> ShizukuStatus.WAITING_AUTHORIZATION
        i.serverVersion != null && i.serverVersion < MIN_SERVER_VERSION -> ShizukuStatus.CONNECTED_LIMITED
        else -> ShizukuStatus.CONNECTED
    }
}
