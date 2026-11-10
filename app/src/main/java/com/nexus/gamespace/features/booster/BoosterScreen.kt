package com.nexus.gamespace.features.booster

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.core.NOT_AVAILABLE
import com.nexus.gamespace.core.formatGb
import com.nexus.gamespace.domain.formatElapsed
import com.nexus.gamespace.ui.components.CircularGauge
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn
import android.os.SystemClock

@Composable
fun BoosterScreen(
    onOpenLibrary: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenShizuku: () -> Unit,
    vm: BoosterViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val activeId by vm.activeProfileId.collectAsStateWithLifecycle()
    val restore by vm.restoreState.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    val brightnessPct by vm.currentBrightnessPercent.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val stopwatch by vm.stopwatch.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()

    // Al volver de Ajustes se vuelve a comprobar el permiso real.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshAccess() }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Este dispositivo no tiene esa pantalla de ajustes", Toast.LENGTH_SHORT).show()
        }
    }

    // El cronómetro se redibuja cada segundo solo mientras corre y la pantalla está visible.
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(stopwatch.running) {
        while (stopwatch.running) {
            tick = SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(1_000L)
        }
        tick = SystemClock.elapsedRealtime()
    }

    ScreenColumn(
        title = "NEXUS BOOSTER",
        subtitle = "Accesos reales del sistema. Cada cambio temporal guarda el valor original y se puede restaurar.",
    ) {
        message?.let {
            NexusCard { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }

        NexusCard {
            Text("Sesión", style = MaterialTheme.typography.titleMedium)
            InfoRow("Estado", "Sin sesión activa")
            Text(
                "El motor de sesiones llega en la siguiente entrega. Mientras tanto puedes usar el temporizador de abajo.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        snap?.let { s ->
            NexusCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CircularGauge(
                        fraction = s.batteryPercent?.let { it / 100f },
                        valueText = s.batteryPercent?.let { "$it%" } ?: NOT_AVAILABLE,
                        label = "Batería",
                    )
                    CircularGauge(
                        fraction = 1f - s.ramUsedFraction,
                        valueText = formatGb(s.availRamBytes),
                        label = "RAM libre",
                    )
                }
                InfoRow("Estado térmico del sistema", s.thermal?.label ?: NOT_AVAILABLE)
                InfoRow("Temperatura de la batería", s.batteryTempCelsius?.let { "%.1f °C".format(it) } ?: NOT_AVAILABLE)
                Text(
                    "El estado térmico es informativo: esta app no enfría ni cambia los límites térmicos.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NexusCard {
            Text("Perfil gaming", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.filter { it.id != "custom" }.forEach { p ->
                    FilterChip(
                        selected = p.id == activeId,
                        onClick = { vm.setActiveProfile(p.id) },
                        label = { Text(p.name) },
                    )
                }
            }
            InfoRow("Perfil activo", profiles.firstOrNull { it.id == activeId }?.name ?: "—")
            Text(
                "El perfil se guarda y se usará en las sesiones. Todavía no cambia ajustes del sistema por sí solo.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenProfiles) { Text("EDITAR PERFILES") }
        }

        NexusCard {
            Text("No molestar", style = MaterialTheme.typography.titleMedium)
            InfoRow("Acceso", if (access.dnd) "Concedido" else "No concedido")
            if (!access.dnd) {
                Text(
                    "Android pide que lo concedas tú en Ajustes. Puedes revocarlo ahí mismo cuando quieras.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { open(vm.dndAccessIntent()) }) { Text("CONCEDER ACCESO") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.applyDnd() }) { Text("ACTIVAR") }
                    OutlinedButton(onClick = { vm.restoreDnd() }, enabled = restore.dndPrevious != null) {
                        Text("RESTAURAR")
                    }
                }
                Text(
                    "Activa «solo prioritarias»: sigue permitiendo alarmas y llamadas de favoritos.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NexusCard {
            Text("Brillo", style = MaterialTheme.typography.titleMedium)
            InfoRow("Permiso para modificar ajustes", if (access.writeSettings) "Concedido" else "No concedido")
            if (!access.writeSettings) {
                Text(
                    "Android lo trata como acceso especial. Lo concedes en Ajustes y lo puedes quitar allí.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { open(vm.writeSettingsIntent()) }) { Text("CONCEDER ACCESO") }
            } else {
                var pct by remember(brightnessPct) { mutableFloatStateOf((brightnessPct ?: 50).toFloat().coerceIn(1f, 100f)) }
                InfoRow("Brillo actual", brightnessPct?.let { "$it %" } ?: NOT_AVAILABLE)
                Slider(value = pct, onValueChange = { pct = it }, valueRange = 1f..100f)
                Text("${pct.toInt()} %", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.applyBrightness(pct.toInt()) }) { Text("APLICAR") }
                    OutlinedButton(onClick = { vm.restoreBrightness() }, enabled = restore.brightnessPrevious != null) {
                        Text("RESTAURAR")
                    }
                }
            }
        }

        NexusCard {
            Text("Volumen y conectividad", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { vm.showVolumePanel() }, modifier = Modifier.fillMaxWidth()) {
                Text("ABRIR PANEL DE VOLUMEN")
            }
            OutlinedButton(onClick = { open(vm.connectivityIntent()) }, modifier = Modifier.fillMaxWidth()) {
                Text("ABRIR PANEL DE CONECTIVIDAD")
            }
            Text(
                "Android no permite que una app active o desactive el Wi-Fi; se abre el panel del sistema para que lo cambies tú.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NexusCard {
            Text("Temporizador", style = MaterialTheme.typography.titleMedium)
            Text(
                formatElapsed(stopwatch.elapsed(if (stopwatch.running) tick else SystemClock.elapsedRealtime())),
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.toggleStopwatch() }) { Text(if (stopwatch.running) "PAUSAR" else "INICIAR") }
                OutlinedButton(onClick = { vm.resetStopwatch() }) { Text("REINICIAR") }
            }
        }

        NexusCard {
            Text("Notas", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = notes,
                onValueChange = { vm.onNotesChanged(it.take(2_000)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe aquí (se guarda en tu teléfono)") },
                minLines = 3,
            )
        }

        NexusCard {
            Text("Cambios temporales", style = MaterialTheme.typography.titleMedium)
            if (!restore.hasChanges) {
                Text("No hay cambios pendientes de restaurar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                if (restore.dndPrevious != null) InfoRow("No molestar", "Modificado")
                if (restore.brightnessPrevious != null) {
                    InfoRow("Brillo", "Original: ${com.nexus.gamespace.domain.BoosterMath.brightnessToPercent(restore.brightnessPrevious!!)} %")
                }
            }
            Button(onClick = { vm.restoreAll() }, enabled = restore.hasChanges, modifier = Modifier.fillMaxWidth()) {
                Text("RESTAURAR TODO")
            }
        }

        NexusCard {
            Text("Accesos", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("BIBLIOTECA DE JUEGOS") }
            OutlinedButton(onClick = onOpenShizuku, modifier = Modifier.fillMaxWidth()) { Text("SHIZUKU CENTER") }
        }

        NexusCard {
            Text("Todavía no disponible", style = MaterialTheme.typography.titleMedium)
            InfoRow("Panel flotante", "Siguiente fase")
            InfoRow("Grabación de pantalla", "Fase posterior")
            Text(
                "El panel flotante necesitará el permiso de superposición, que solo se pedirá al activarlo. " +
                    "La grabación usará MediaProjection y pedirá tu consentimiento cada vez.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
