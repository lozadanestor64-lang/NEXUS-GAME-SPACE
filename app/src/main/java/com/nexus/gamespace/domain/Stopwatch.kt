package com.nexus.gamespace.domain

/** Cronómetro sin hilos propios: guarda el tiempo acumulado y desde cuándo corre. */
data class Stopwatch(val accumulatedMs: Long = 0L, val runningSinceMs: Long? = null) {
    val running: Boolean get() = runningSinceMs != null

    fun elapsed(nowMs: Long): Long =
        accumulatedMs + (runningSinceMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    fun start(nowMs: Long): Stopwatch = if (running) this else copy(runningSinceMs = nowMs)

    fun pause(nowMs: Long): Stopwatch = if (!running) this else Stopwatch(elapsed(nowMs), null)

    fun reset(): Stopwatch = Stopwatch()
}

/** 65_000 -> "01:05"; 3_725_000 -> "1:02:05". */
fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

object BoosterMath {
    /** Porcentaje 1..100 -> valor de brillo 1..255. Nunca devuelve 0 para no dejar la pantalla negra. */
    fun percentToBrightness(percent: Int): Int = (percent.coerceIn(1, 100) * 255 / 100).coerceAtLeast(1)

    fun brightnessToPercent(value: Int): Int = (value.coerceIn(0, 255) * 100 / 255)
}

/** Valores originales guardados antes de un cambio temporal. null = no hay cambio pendiente de restaurar. */
data class BoosterRestoreState(
    val dndPrevious: Int? = null,
    val brightnessPrevious: Int? = null,
    val brightnessModePrevious: Int? = null,
    val rotationPrevious: Int? = null,
) {
    val hasChanges: Boolean
        get() = dndPrevious != null || brightnessPrevious != null || rotationPrevious != null
}
