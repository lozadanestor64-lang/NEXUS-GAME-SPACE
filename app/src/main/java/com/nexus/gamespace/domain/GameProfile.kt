package com.nexus.gamespace.domain

enum class ProfileType(val id: String) {
    PERFORMANCE("performance"),
    BALANCED("balanced"),
    BATTERY_SAVER("battery_saver"),
    CUSTOM("custom");

    companion object {
        fun fromId(id: String): ProfileType? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Ajustes de un perfil. En la v0.3.0 se guardan y se editan; entran en vigor cuando se
 * completen las sesiones y el optimizador.
 */
data class ProfileSettings(
    val thermalAlerts: Boolean,
    val recordSessionStats: Boolean,
    val openGameOnStart: Boolean,
    val monitorIntervalSec: Int,
    val doNotDisturb: Boolean,
)

data class GameProfile(
    val id: String,
    val type: ProfileType,
    val name: String,
    val description: String,
    val colorArgb: Long,
    val iconKey: String,
    val settings: ProfileSettings,
    val updatedAt: Long,
)

object DefaultProfiles {
    val palette: List<Long> = listOf(0xFFE5202EL, 0xFFFFB020L, 0xFF3DDC84L, 0xFF4DA3FFL, 0xFFB57BFFL)

    fun all(now: Long): List<GameProfile> = ProfileType.entries.map { default(it, now) }

    fun default(type: ProfileType, now: Long): GameProfile = when (type) {
        ProfileType.PERFORMANCE -> GameProfile(
            id = type.id, type = type, name = "Performance",
            description = "Sesiones gaming con todas las ayudas compatibles activadas.",
            colorArgb = 0xFFE5202EL, iconKey = "star",
            settings = ProfileSettings(
                thermalAlerts = true, recordSessionStats = true, openGameOnStart = true,
                monitorIntervalSec = 1, doNotDisturb = true,
            ),
            updatedAt = now,
        )
        ProfileType.BALANCED -> GameProfile(
            id = type.id, type = type, name = "Balanced",
            description = "Equilibrio entre consumo y experiencia.",
            colorArgb = 0xFFFFB020L, iconKey = "refresh",
            settings = ProfileSettings(
                thermalAlerts = true, recordSessionStats = true, openGameOnStart = true,
                monitorIntervalSec = 2, doNotDisturb = false,
            ),
            updatedAt = now,
        )
        ProfileType.BATTERY_SAVER -> GameProfile(
            id = type.id, type = type, name = "Battery Saver",
            description = "Prioriza la batería: menos actividad propia de la app durante la sesión.",
            colorArgb = 0xFF3DDC84L, iconKey = "favorite",
            settings = ProfileSettings(
                thermalAlerts = true, recordSessionStats = true, openGameOnStart = true,
                monitorIntervalSec = 5, doNotDisturb = false,
            ),
            updatedAt = now,
        )
        ProfileType.CUSTOM -> GameProfile(
            id = type.id, type = type, name = "Custom",
            description = "Tu propia configuración.",
            colorArgb = 0xFF4DA3FFL, iconKey = "build",
            settings = ProfileSettings(
                thermalAlerts = true, recordSessionStats = true, openGameOnStart = true,
                monitorIntervalSec = 2, doNotDisturb = false,
            ),
            updatedAt = now,
        )
    }

    fun default(id: String, now: Long): GameProfile? = ProfileType.fromId(id)?.let { default(it, now) }
}
