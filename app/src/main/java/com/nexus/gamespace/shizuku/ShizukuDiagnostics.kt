package com.nexus.gamespace.shizuku

/** Textos de ayuda y diagnóstico derivados del estado real. No contienen datos personales. */
object ShizukuDiagnostics {

    fun messages(state: ShizukuState): List<String> = buildList {
        when (state.status) {
            ShizukuStatus.NOT_INSTALLED ->
                add("La app Shizuku no está instalada. NEXUS GAME SPACE funciona igual sin ella; solo se desactivan las funciones avanzadas.")
            ShizukuStatus.NOT_RUNNING ->
                add("Shizuku está instalada pero su servicio no está iniciado. Ábrela e inicia el servicio.")
            ShizukuStatus.WAITING_AUTHORIZATION ->
                add("El servicio está activo. Falta autorizar a NEXUS GAME SPACE con SOLICITAR AUTORIZACIÓN.")
            ShizukuStatus.DENIED ->
                add("Denegaste el permiso. Para concederlo abre Shizuku y autoriza a NEXUS GAME SPACE en la lista de aplicaciones.")
            ShizukuStatus.CONNECTED ->
                add("Conexión y autorización verificadas. Todavía no se ejecuta ninguna operación privilegiada: se añadirán solo operaciones permitidas, documentadas y reversibles.")
            ShizukuStatus.CONNECTED_LIMITED ->
                add("Shizuku responde, pero su versión o su respuesta no cumple lo necesario. Actualiza Shizuku.")
            ShizukuStatus.CONNECTION_LOST ->
                add("El servicio de Shizuku se detuvo mientras la app estaba abierta. Sin root, hay que iniciarlo de nuevo (por ejemplo, tras reiniciar el teléfono).")
            ShizukuStatus.ERROR ->
                add("No se pudo comprobar el permiso de Shizuku.")
        }
        if (state.mode == ShizukuMode.ADB) {
            add("Entorno ADB: tiene menos privilegios que root. Ambos no son equivalentes.")
        }
        if (state.mode == ShizukuMode.ROOT) {
            add("Entorno root detectado. La app no lo exige ni lo usa para cambios no permitidos.")
        }
        state.error?.let { add("Detalle técnico: $it") }
    }
}
