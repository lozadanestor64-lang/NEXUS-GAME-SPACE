package com.nexus.gamespace.features.shizuku

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.core.NOT_AVAILABLE
import com.nexus.gamespace.features.profiles.formatDateTime
import com.nexus.gamespace.shizuku.PermissionCheck
import com.nexus.gamespace.shizuku.ShizukuDiagnostics
import com.nexus.gamespace.shizuku.ShizukuStatus
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn
import com.nexus.gamespace.ui.theme.NexusBorder
import com.nexus.gamespace.ui.theme.NexusOk
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusWarn

private fun statusColor(status: ShizukuStatus): Color = when (status) {
    ShizukuStatus.CONNECTED -> NexusOk
    ShizukuStatus.WAITING_AUTHORIZATION, ShizukuStatus.CONNECTED_LIMITED, ShizukuStatus.NOT_RUNNING -> NexusWarn
    ShizukuStatus.NOT_INSTALLED -> NexusBorder
    else -> NexusRed
}

@Composable
fun ShizukuScreen(vm: ShizukuViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val log by vm.log.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }

    // Al volver desde la app Shizuku se vuelve a comprobar el estado real.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    ScreenColumn(
        title = "SHIZUKU CENTER",
        subtitle = "Modo avanzado opcional. La app funciona igual sin Shizuku.",
    ) {
        NexusCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(statusColor(state.status)))
                Text(
                    state.status.label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            ShizukuDiagnostics.messages(state).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        NexusCard {
            InfoRow("App Shizuku instalada", if (state.installed) "Sí" else "No")
            InfoRow("Servicio iniciado (Binder)", if (state.binderAlive) "Sí" else "No")
            InfoRow(
                "Autorización",
                when (state.permission) {
                    PermissionCheck.GRANTED -> "Concedida"
                    PermissionCheck.NOT_GRANTED -> "Pendiente"
                    PermissionCheck.DENIED -> "Denegada"
                    PermissionCheck.UNSUPPORTED -> "Versión no compatible"
                    PermissionCheck.ERROR -> "Error al comprobar"
                    null -> NOT_AVAILABLE
                },
            )
            InfoRow("Versión de la API del servicio", state.serverVersion?.toString() ?: NOT_AVAILABLE)
            InfoRow("Entorno", state.mode?.label ?: NOT_AVAILABLE)
            InfoRow("Última comprobación", state.lastCheckedAt?.let { formatDateTime(it) } ?: NOT_AVAILABLE)
        }

        Button(onClick = { vm.manualCheck() }, modifier = Modifier.fillMaxWidth()) {
            Text("COMPROBAR SHIZUKU", style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = { vm.requestPermission() },
            enabled = state.canRequestPermission,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("SOLICITAR AUTORIZACIÓN", style = MaterialTheme.typography.labelLarge) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val intent = vm.openShizukuIntent()
                    if (intent == null) {
                        Toast.makeText(context, "Shizuku no está instalada", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No se puede abrir Shizuku", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = state.installed,
            ) { Text("ABRIR SHIZUKU") }
            OutlinedButton(onClick = { vm.refresh() }) { Text("ACTUALIZAR ESTADO") }
        }
        OutlinedButton(onClick = { showHelp = !showHelp }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showHelp) "OCULTAR INSTRUCCIONES" else "VER INSTRUCCIONES")
        }

        if (showHelp) {
            NexusCard {
                Text("Cómo activar Shizuku sin root", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "1. Instala la app Shizuku desde su sitio oficial o la tienda de tu dispositivo.",
                    "2. Ajustes → Acerca del teléfono → toca varias veces «Número de compilación» para activar las opciones de desarrollador.",
                    "3. Ajustes → Opciones de desarrollador → activa «Depuración inalámbrica» (necesitas estar conectado a una red Wi-Fi).",
                    "4. Abre Shizuku y elige «Iniciar mediante depuración inalámbrica». Empareja con el código que muestra Android.",
                    "5. Vuelve a esta pantalla, pulsa COMPROBAR SHIZUKU y después SOLICITAR AUTORIZACIÓN.",
                    "Sin root, Shizuku se detiene al reiniciar el teléfono y hay que iniciarlo de nuevo.",
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        NexusCard {
            Text("Registro de eventos", style = MaterialTheme.typography.titleMedium)
            if (log.isEmpty()) {
                Text("Todavía no hay eventos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            log.forEach {
                InfoRow(formatDateTime(it.timestamp), it.message)
            }
        }
    }
}
