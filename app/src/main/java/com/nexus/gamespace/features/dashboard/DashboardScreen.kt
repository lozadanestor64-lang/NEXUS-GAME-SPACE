package com.nexus.gamespace.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.core.NOT_AVAILABLE
import com.nexus.gamespace.core.formatGb
import com.nexus.gamespace.ui.components.CircularGauge
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn

@Composable
fun DashboardScreen(vm: DeviceViewModel = viewModel()) {
    val s by vm.snapshot.collectAsStateWithLifecycle()

    ScreenColumn(
        title = "NEXUS GAME SPACE",
        subtitle = s?.let { "${it.manufacturer} ${it.model} · Android ${it.androidVersion}" } ?: "Leyendo dispositivo…",
    ) {
        val snap = s
        if (snap == null) {
            NexusCard { Text("Leyendo el estado del dispositivo…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@ScreenColumn
        }

        NexusCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CircularGauge(
                    fraction = snap.batteryPercent?.let { it / 100f },
                    valueText = snap.batteryPercent?.let { "$it%" } ?: NOT_AVAILABLE,
                    label = "Batería",
                )
                CircularGauge(
                    fraction = snap.ramUsedFraction,
                    valueText = "${(snap.ramUsedFraction * 100).toInt()}%",
                    label = "RAM en uso",
                )
                CircularGauge(
                    fraction = snap.storageUsedFraction,
                    valueText = "${(snap.storageUsedFraction * 100).toInt()}%",
                    label = "Almacenamiento",
                )
            }
        }

        NexusCard {
            InfoRow("Estado térmico del sistema", snap.thermal?.label ?: NOT_AVAILABLE)
            InfoRow("Temperatura de la batería", snap.batteryTempCelsius?.let { "%.1f °C".format(it) } ?: NOT_AVAILABLE)
            InfoRow("Carga", when (snap.batteryCharging) { true -> "Cargando"; false -> "No cargando"; null -> NOT_AVAILABLE })
            InfoRow("RAM disponible", "${formatGb(snap.availRamBytes)} de ${formatGb(snap.totalRamBytes)}")
            InfoRow("Almacenamiento libre", "${formatGb(snap.storageFreeBytes)} de ${formatGb(snap.storageTotalBytes)}")
        }

        NexusCard {
            InfoRow("Shizuku", "Sin comprobar (Fase 6)")
            InfoRow("Modo gaming", "Inactivo")
            InfoRow("Perfil activo", "Ninguno (Fase 4)")
            InfoRow("Juego activo", "Sin detectar (Fase 7)")
        }

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ACTIVAR MODO GAMING", style = MaterialTheme.typography.labelLarge) }
        Text(
            "Se habilita en la Fase 4, cuando existan perfiles con acciones reales. " +
                "Hasta entonces la app no aplica ningún cambio al sistema.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
