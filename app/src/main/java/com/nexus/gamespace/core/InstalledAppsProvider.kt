package com.nexus.gamespace.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.nexus.gamespace.domain.InstalledApp

/**
 * Lista apps con actividad lanzable usando visibilidad limitada de paquetes (<queries> en el manifiesto).
 * No usa QUERY_ALL_PACKAGES y no decide por el usuario qué es un juego.
 */
class InstalledAppsProvider(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager

    fun launchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return results
            .map { info ->
                val app = info.activityInfo.applicationInfo
                InstalledApp(
                    packageName = app.packageName,
                    label = info.loadLabel(pm).toString(),
                    suggestedGame = app.category == ApplicationInfo.CATEGORY_GAME,
                )
            }
            .filter { it.packageName != appContext.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Nombre actual de la app, o null si ya no está instalada. */
    fun labelOf(packageName: String): String? = try {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)
}
