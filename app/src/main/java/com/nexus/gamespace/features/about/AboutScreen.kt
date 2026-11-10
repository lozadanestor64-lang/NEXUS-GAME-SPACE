package com.nexus.gamespace.features.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "No disponible"

    ScreenColumn(title = "Acerca de y privacidad") {
        NexusCard {
            InfoRow("Versión", version)
            InfoRow("Licencia", "Apache-2.0 (código abierto)")
        }
        NexusCard {
            Text("Privacidad", style = MaterialTheme.typography.titleMedium)
            listOf(
                "Dos accesos especiales opcionales (No molestar y modificar ajustes para el brillo): solo se piden al usar esa función y puedes revocarlos en Ajustes.",
                "No usa Internet ni envía datos a ningún sitio.",
                "Sin cuentas, sin publicidad y sin telemetría.",
                "No requiere root ni desbloquear el bootloader.",
                "Los perfiles y sesiones (cuando existan) se guardarán solo en tu teléfono.",
            ).forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
