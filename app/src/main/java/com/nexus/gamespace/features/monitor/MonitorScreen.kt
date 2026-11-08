package com.nexus.gamespace.features.monitor

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.core.NOT_AVAILABLE
import com.nexus.gamespace.core.formatGb
import com.nexus.gamespace.features.dashboard.DeviceViewModel
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn

@Composable
fun MonitorScreen(vm: DeviceViewModel = viewModel()) {
    val s by vm.snapshot.collectAsStateWithLifecycle()

    ScreenColumn(
        title = "Monitor",
        subtitle = "Solo datos reales de APIs públicas de Android. Lo que no se puede medir se marca como no disponible.",
    ) {
        val snap = s ?: run {
            NexusCard { Text("Leyendo…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@ScreenColumn
        }

        NexusCard {
            Text("CPU", style = MaterialTheme.typography.titleMedium)
            InfoRow("Procesador (SoC)", snap.socName ?: NOT_AVAILABLE)
            InfoRow("Hardware", Build.HARDWARE.ifBlank { NOT_AVAILABLE })
            InfoRow("Uso de CPU", NOT_AVAILABLE)
        }
        NexusCard {
            Text("GPU", style = MaterialTheme.typography.titleMedium)
            InfoRow("Modelo", NOT_AVAILABLE)
            InfoRow("Métricas", NOT_AVAILABLE)
        }
        NexusCard {
            Text("RAM física", style = MaterialTheme.typography.titleMedium)
            InfoRow("Total", formatGb(snap.totalRamBytes))
            InfoRow("Disponible", formatGb(snap.availRamBytes))
            InfoRow("Memoria baja", if (snap.lowMemory) "Sí" else "No")
            Text(
                "La RAM virtual (p. ej. RAM Boost) no se cuenta como RAM física.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NexusCard {
            Text("Temperatura", style = MaterialTheme.typography.titleMedium)
            InfoRow("Estado térmico del sistema", snap.thermal?.label ?: NOT_AVAILABLE)
            InfoRow("Temperatura de la batería", snap.batteryTempCelsius?.let { "%.1f °C".format(it) } ?: NOT_AVAILABLE)
            Text(
                "La temperatura de la batería no es la del procesador.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NexusCard {
            Text("Batería", style = MaterialTheme.typography.titleMedium)
            InfoRow("Nivel", snap.batteryPercent?.let { "$it%" } ?: NOT_AVAILABLE)
            InfoRow("Estado", when (snap.batteryCharging) { true -> "Cargando"; false -> "Sin cargar"; null -> NOT_AVAILABLE })
        }
        NexusCard {
            Text("Pantalla", style = MaterialTheme.typography.titleMedium)
            InfoRow(
                "Resolución",
                if (snap.widthPx != null && snap.heightPx != null) "${snap.widthPx} × ${snap.heightPx}" else NOT_AVAILABLE,
            )
            InfoRow("Frecuencia activa", snap.refreshRateHz?.let { "%.0f Hz".format(it) } ?: NOT_AVAILABLE)
            InfoRow("Densidad", snap.densityDpi?.let { "$it dpi" } ?: NOT_AVAILABLE)
        }
        NexusCard {
            Text("FPS de juegos", style = MaterialTheme.typography.titleMedium)
            InfoRow("Medición", NOT_AVAILABLE)
            Text(
                "Android no ofrece una fuente fiable para medir los FPS de juegos de terceros. " +
                    "La frecuencia de pantalla no son FPS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
