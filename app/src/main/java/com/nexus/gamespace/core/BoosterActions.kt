package com.nexus.gamespace.core

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Acciones del Booster que usan solo APIs públicas de Android. Cada una devuelve false si Android
 * la rechaza; nunca se ejecutan comandos ni se usan APIs ocultas.
 */
class BoosterActions(context: Context) {

    private val ctx = context.applicationContext
    private val notificationManager = ctx.getSystemService(NotificationManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)

    // ---- No molestar (acceso especial: ACCESS_NOTIFICATION_POLICY) ----
    fun hasDndAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun currentDndFilter(): Int = notificationManager.currentInterruptionFilter

    fun setDndFilter(filter: Int): Boolean {
        if (!hasDndAccess()) return false
        return try {
            notificationManager.setInterruptionFilter(filter)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun dndAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    // ---- Brillo (acceso especial: WRITE_SETTINGS) ----
    fun canWriteSettings(): Boolean = Settings.System.canWrite(ctx)

    fun currentBrightness(): Int? = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (e: Settings.SettingNotFoundException) {
        null
    }

    fun currentBrightnessMode(): Int? = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
    } catch (e: Settings.SettingNotFoundException) {
        null
    }

    /** Pone brillo manual. mode = SCREEN_BRIGHTNESS_MODE_MANUAL (0) o AUTOMATIC (1). */
    fun setBrightness(value: Int, mode: Int): Boolean {
        if (!canWriteSettings()) return false
        return try {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value.coerceIn(1, 255))
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun writeSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    // ---- Rotación automática (acceso especial: WRITE_SETTINGS) ----
    fun autoRotateEnabled(): Boolean? = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 1
    } catch (e: Settings.SettingNotFoundException) {
        null
    }

    fun setAutoRotate(enabled: Boolean): Boolean {
        if (!canWriteSettings()) return false
        return try {
            Settings.System.putInt(ctx.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enabled) 1 else 0)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    // ---- Volumen: abre el panel de volumen del sistema ----
    fun adjustVolume(raise: Boolean) {
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    fun showVolumePanel() {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    // ---- Conectividad: Android no deja a las apps cambiar el Wi-Fi; se abre el panel del sistema ----
    fun connectivityIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
}
