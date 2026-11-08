package com.nexus.gamespace.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn
import java.text.DateFormat
import java.util.Date

fun profileIcon(key: String): ImageVector = when (key) {
    "star" -> Icons.Filled.Star
    "refresh" -> Icons.Filled.Refresh
    "favorite" -> Icons.Filled.Favorite
    else -> Icons.Filled.Build
}

fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

@Composable
fun ProfilesScreen(onEdit: (String) -> Unit, vm: ProfilesViewModel = viewModel()) {
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val activeId by vm.activeId.collectAsStateWithLifecycle()
    val counts by vm.gamesPerProfile.collectAsStateWithLifecycle()

    ScreenColumn(
        title = "Perfiles",
        subtitle = "El perfil activo se usa en los juegos que no tengan uno propio.",
    ) {
        profiles.forEach { p ->
            val color = Color(p.colorArgb)
            NexusCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(profileIcon(p.iconKey), contentDescription = null, tint = color) }
                    Text(
                        p.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(p.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                InfoRow("Estado", if (p.id == activeId) "Activo" else "Inactivo")
                InfoRow("Juegos asignados", (counts[p.id] ?: 0).toString())
                InfoRow("Modificado", formatDateTime(p.updatedAt))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (p.id != activeId) {
                        Button(onClick = { vm.setActive(p.id) }) { Text("ACTIVAR", style = MaterialTheme.typography.labelLarge) }
                    }
                    OutlinedButton(onClick = { onEdit(p.id) }) { Text("EDITAR", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }
}
