package com.nexus.gamespace.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class NetworkKind(val label: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Datos móviles"),
    ETHERNET("Ethernet"),
    OTHER("Otra red"),
    NONE("Sin conexión"),
}

/** Tipo de conexión activa (permiso normal ACCESS_NETWORK_STATE). No lee SSID ni ubicación. */
class NetworkInfoProvider(context: Context) {

    private val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    fun current(): NetworkKind {
        val network = cm.activeNetwork ?: return NetworkKind.NONE
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkKind.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkKind.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.ETHERNET
            else -> NetworkKind.OTHER
        }
    }
}
