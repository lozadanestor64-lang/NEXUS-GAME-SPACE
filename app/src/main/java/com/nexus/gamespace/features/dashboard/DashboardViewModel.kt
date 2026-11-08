package com.nexus.gamespace.features.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.gamespace.core.DeviceInfoProvider
import com.nexus.gamespace.domain.DeviceSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Actualiza el estado del dispositivo cada 5 s, y solo mientras la pantalla está visible
 * (WhileSubscribed), para no añadir carga innecesaria.
 */
class DeviceViewModel(app: Application) : AndroidViewModel(app) {

    private val provider = DeviceInfoProvider(app)

    val snapshot: StateFlow<DeviceSnapshot?> = flow {
        while (true) {
            emit(provider.snapshot())
            delay(REFRESH_MS)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private companion object {
        const val REFRESH_MS = 5_000L
    }
}
