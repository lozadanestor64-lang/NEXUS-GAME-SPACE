package com.nexus.gamespace.shizuku

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/** Punto de acceso único a Shizuku para el resto de la app (ViewModels). */
class ShizukuRepository(context: Context) {

    private val manager = ShizukuManager(context)

    val state: StateFlow<ShizukuState> = manager.state
    val log: StateFlow<List<ShizukuLogEntry>> = manager.log

    fun start() = manager.start()
    fun stop() = manager.stop()
    fun refresh() = manager.refresh()
    fun manualCheck() = manager.manualCheck()
    fun requestPermission() = manager.requestPermission()
    fun openShizukuIntent(): Intent? = manager.openShizukuIntent()
}
