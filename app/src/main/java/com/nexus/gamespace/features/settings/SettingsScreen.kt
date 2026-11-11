package com.nexus.gamespace.features.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
import com.nexus.gamespace.domain.OverlayTool
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val restore by vm.restoreState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    // Al volver de Ajustes se vuelve a leer el estado real de cada permiso.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Este dispositivo no tiene esa pantalla de ajustes", Toast.LENGTH_SHORT).show()
        }
    }

    // Android 13+: el aviso de «panel activo» necesita el permiso de notificaciones. El panel funciona aunque lo rechaces.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refresh()
        vm.startOverlay(context)
    }

    ScreenColumn(
        title = "Configuración general",
        subtitle = "Panel gaming flotante, permisos especiales y cambios temporales.",
    ) {
        NexusCard {
            Text("Panel gaming flotante", style = MaterialTheme.typography.titleMedium)
            InfoRow("Estado", if (running) "Activo" else "Inactivo")
            InfoRow("Permiso de superposición", if (perms.overlay) "Concedido" else "No concedido")
            Text(
                "Muestra una pestaña lateral sobre tus juegos. El permiso de superposición solo se pide aquí, " +
                    "al activar el panel, y puedes quitarlo en Ajustes cuando quieras.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!perms.overlay) {
                Button(onClick = { open(vm.overlayPermissionIntent()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("CONCEDER PERMISO DE SUPERPOSICIÓN", style = MaterialTheme.typography.labelLarge)
                }
            } else if (!running) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !perms.notifications) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.startOverlay(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ACTIVAR PANEL FLOTANTE", style = MaterialTheme.typography.labelLarge) }
            } else {
                OutlinedButton(onClick = { vm.stopOverlay(context) }, modifier = Modifier.fillMaxWidth()) {
                    Text("DESACTIVAR PANEL FLOTANTE", style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                "Para abrirlo: toca la pestaña o deslízala hacia el interior de la pantalla. Arrástrala para moverla; " +
                    "si la sueltas pasada la mitad de la pantalla cambia de lado.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NexusCard {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            Text("Lado de la pestaña", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(prefs.side == OverlaySide.LEFT, { vm.setSide(OverlaySide.LEFT) }, { Text("Izquierda") })
                FilterChip(prefs.side == OverlaySide.RIGHT, { vm.setSide(OverlaySide.RIGHT) }, { Text("Derecha") })
            }
            var alpha by remember(prefs.alphaPercent) { mutableFloatStateOf(prefs.alphaPercent.toFloat()) }
            Text("Opacidad: ${alpha.toInt()} %", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                onValueChangeFinished = { vm.setAlpha(alpha.toInt()) },
                valueRange = OverlayPrefs.ALPHA_MIN.toFloat()..OverlayPrefs.ALPHA_MAX.toFloat(),
            )
            Text("Tamaño", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayPrefs.SCALES.forEach { s ->
                    FilterChip(
                        selected = prefs.scalePercent == s,
                        onClick = { vm.setScale(s) },
                        label = { Text(when (s) { 85 -> "Pequeño"; 115 -> "Grande"; else -> "Normal" }) },
                    )
                }
            }
        }

        NexusCard {
            Text("Herramientas del panel", style = MaterialTheme.typography.titleMedium)
            OverlayTool.entries.forEach { tool ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = prefs.isVisible(tool), onCheckedChange = { vm.setToolVisible(tool, it) })
                }
            }
        }

        NexusCard {
            Text("Permisos especiales", style = MaterialTheme.typography.titleMedium)
            PermissionRow("Superposición", perms.overlay, "Panel flotante") { open(vm.overlayPermissionIntent()) }
            PermissionRow("No molestar", perms.dnd, "Activar No molestar desde el panel") { open(vm.dndAccessIntent()) }
            PermissionRow("Modificar ajustes del sistema", perms.writeSettings, "Brillo y rotación") { open(vm.writeSettingsIntent()) }
            PermissionRow("Notificaciones", perms.notifications, "Aviso del panel activo") { open(vm.notificationSettingsIntent()) }
            Text(
                "Ninguno se concede al instalar. Cada botón abre la pantalla de Android donde puedes concederlo o revocarlo.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NexusCard {
            Text("Cambios temporales", style = MaterialTheme.typography.titleMedium)
            Text(
                if (restore.hasChanges) "Hay cambios hechos desde el panel pendientes de restaurar."
                else "No hay cambios pendientes de restaurar.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            OutlinedButton(
                onClick = { vm.restoreAll() },
                enabled = restore.hasChanges,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("RESTAURAR TODO") }
        }
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean, purpose: String, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(
                "$purpose · ${if (granted) "Concedido" else "No concedido"}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpen) { Text(if (granted) "REVISAR" else "CONCEDER") }
    }
}
