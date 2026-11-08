package com.nexus.gamespace.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Display
import com.nexus.gamespace.domain.DeviceSnapshot
import com.nexus.gamespace.domain.ThermalLevel

/** Lee el estado del dispositivo usando solo APIs públicas de Android. No requiere permisos. */
class DeviceInfoProvider(context: Context) {

    private val appContext = context.applicationContext

    fun snapshot(): DeviceSnapshot {
        val mem = ActivityManager.MemoryInfo().also {
            appContext.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
        }
        val stat = StatFs(Environment.getDataDirectory().path)
        val battery = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val display = appContext.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)

        return DeviceSnapshot(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            socName = readSocName(),
            totalRamBytes = mem.totalMem,
            availRamBytes = mem.availMem,
            lowMemory = mem.lowMemory,
            storageTotalBytes = stat.totalBytes,
            storageFreeBytes = stat.availableBytes,
            batteryPercent = readBatteryPercent(battery),
            batteryCharging = readCharging(battery),
            batteryTempCelsius = readBatteryTemp(battery),
            thermal = readThermal(),
            refreshRateHz = display?.refreshRate,
            widthPx = display?.let { realMetrics(it).widthPixels },
            heightPx = display?.let { realMetrics(it).heightPixels },
            densityDpi = display?.let { realMetrics(it).densityDpi },
        )
    }

    private fun readSocName(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val soc = Build.SOC_MODEL
        return soc.takeIf { it.isNotBlank() && !it.equals(Build.UNKNOWN, ignoreCase = true) }
    }

    private fun readBatteryPercent(intent: Intent?): Int? {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return null
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return level * 100 / scale
    }

    private fun readCharging(intent: Intent?): Boolean? {
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return null
        if (status < 0) return null
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    /** Temperatura de la BATERÍA (no del procesador). Android la entrega en décimas de °C. */
    private fun readBatteryTemp(intent: Intent?): Float? {
        val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: return null
        return if (raw == Int.MIN_VALUE) null else raw / 10f
    }

    /** Estado térmico del SISTEMA (API 29+). Es distinto de la temperatura de la batería. */
    private fun readThermal(): ThermalLevel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val pm = appContext.getSystemService(PowerManager::class.java)
        return ThermalLevel.fromAndroidStatus(pm.currentThermalStatus)
    }

    @Suppress("DEPRECATION")
    private fun realMetrics(display: Display): DisplayMetrics =
        DisplayMetrics().also { display.getRealMetrics(it) }
}
