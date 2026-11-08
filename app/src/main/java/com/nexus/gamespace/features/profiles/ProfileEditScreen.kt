package com.nexus.gamespace.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.domain.DefaultProfiles
import com.nexus.gamespace.domain.ProfileType
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn

@Composable
fun ProfileEditScreen(profileId: String, onDone: () -> Unit, vm: ProfilesViewModel = viewModel()) {
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val original = profiles.firstOrNull { it.id == profileId }

    if (original == null) {
        ScreenColumn(title = "Perfil") {
            Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var draft by remember(original.id) { mutableStateOf(original) }
    val isCustom = draft.type == ProfileType.CUSTOM
    val s = draft.settings

    ScreenColumn(
        title = "Editar perfil",
        subtitle = "Los ajustes se guardan en tu teléfono. Entran en vigor cuando se completen las sesiones y el optimizador.",
    ) {
        NexusCard {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it.take(24)) },
                label = { Text("Nombre") },
                enabled = isCustom,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { draft = draft.copy(description = it.take(120)) },
                label = { Text("Descripción") },
                enabled = isCustom,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!isCustom) {
                Text(
                    "El nombre y la descripción de los perfiles predefinidos no se pueden cambiar.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Color", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DefaultProfiles.palette.forEach { c ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                            .border(if (draft.colorArgb == c) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable { draft = draft.copy(colorArgb = c) },
                    )
                }
            }
        }

        NexusCard {
            SettingSwitch(
                "Alertas térmicas", "Avisar en la sesión si el estado térmico del sistema sube.",
                s.thermalAlerts,
            ) { draft = draft.copy(settings = s.copy(thermalAlerts = it)) }
            SettingSwitch(
                "Registrar estadísticas", "Guardar batería inicial y final, estado térmico y duración.",
                s.recordSessionStats,
            ) { draft = draft.copy(settings = s.copy(recordSessionStats = it)) }
            SettingSwitch(
                "Abrir el juego al iniciar", "Lanzar el juego al empezar la sesión desde la app.",
                s.openGameOnStart,
            ) { draft = draft.copy(settings = s.copy(openGameOnStart = it)) }
            SettingSwitch(
                "No molestar durante la sesión", "Requiere conceder acceso a No molestar (centro de permisos).",
                s.doNotDisturb,
            ) { draft = draft.copy(settings = s.copy(doNotDisturb = it)) }

            Text("Frecuencia del monitor en sesión", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 5).forEach { sec ->
                    FilterChip(
                        selected = s.monitorIntervalSec == sec,
                        onClick = { draft = draft.copy(settings = s.copy(monitorIntervalSec = sec)) },
                        label = { Text("$sec s") },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    vm.save(draft.copy(name = draft.name.trim(), updatedAt = System.currentTimeMillis()))
                    onDone()
                },
                enabled = draft.name.isNotBlank(),
            ) { Text("GUARDAR", style = MaterialTheme.typography.labelLarge) }
            OutlinedButton(onClick = onDone) { Text("CANCELAR", style = MaterialTheme.typography.labelLarge) }
        }
        TextButton(onClick = { vm.defaultFor(profileId)?.let { draft = it } }) {
            Text("RESTAURAR PREDETERMINADOS")
        }
        Text(
            "Restaurar solo rellena los valores originales. Pulsa GUARDAR para conservarlos.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
