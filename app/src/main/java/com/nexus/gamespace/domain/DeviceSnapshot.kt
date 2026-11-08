package com.nexus.gamespace.domain

/** Foto del estado del dispositivo. Un valor null significa "No disponible". */
data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val socName: String?,
    val totalRamBytes: Long,
    val availRamBytes: Long,
    val lowMemory: Boolean,
    val storageTotalBytes: Long,
    val storageFreeBytes: Long,
    val batteryPercent: Int?,
    val batteryCharging: Boolean?,
    val batteryTempCelsius: Float?,
    val thermal: ThermalLevel?,
    val refreshRateHz: Float?,
    val widthPx: Int?,
    val heightPx: Int?,
    val densityDpi: Int?,
) {
    val ramUsedFraction: Float
        get() = if (totalRamBytes > 0) 1f - availRamBytes.toFloat() / totalRamBytes else 0f

    val storageUsedFraction: Float
        get() = if (storageTotalBytes > 0) 1f - storageFreeBytes.toFloat() / storageTotalBytes else 0f
}
