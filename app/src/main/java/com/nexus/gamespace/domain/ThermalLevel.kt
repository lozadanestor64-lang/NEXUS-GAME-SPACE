package com.nexus.gamespace.domain

/**
 * Estado térmico simplificado, derivado de los valores documentados de
 * PowerManager.THERMAL_STATUS_* (0 = NONE ... 6 = SHUTDOWN). No usa temperaturas inventadas.
 */
enum class ThermalLevel(val label: String) {
    NORMAL("Normal"),
    ATTENTION("Atención"),
    ELEVATED("Elevado"),
    CRITICAL("Crítico");

    companion object {
        fun fromAndroidStatus(status: Int): ThermalLevel = when {
            status <= 0 -> NORMAL        // THERMAL_STATUS_NONE
            status <= 2 -> ATTENTION     // LIGHT, MODERATE
            status == 3 -> ELEVATED      // SEVERE
            else -> CRITICAL             // CRITICAL, EMERGENCY, SHUTDOWN
        }
    }
}
