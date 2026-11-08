package com.nexus.gamespace.features.shizuku

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.nexus.gamespace.shizuku.ShizukuLogEntry
import com.nexus.gamespace.shizuku.ShizukuRepository
import com.nexus.gamespace.shizuku.ShizukuState
import kotlinx.coroutines.flow.StateFlow

/** Registra los listeners de Shizuku al crearse y los libera en onCleared. */
class ShizukuViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ShizukuRepository(app)

    val state: StateFlow<ShizukuState> = repo.state
    val log: StateFlow<List<ShizukuLogEntry>> = repo.log

    init {
        repo.start()
    }

    fun refresh() = repo.refresh()
    fun manualCheck() = repo.manualCheck()
    fun requestPermission() = repo.requestPermission()
    fun openShizukuIntent(): Intent? = repo.openShizukuIntent()

    override fun onCleared() {
        repo.stop()
        super.onCleared()
    }
}
