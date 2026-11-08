package com.nexus.gamespace.features.library

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.domain.GameProfile
import com.nexus.gamespace.ui.components.AppIcon
import com.nexus.gamespace.ui.theme.NexusBorder
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusTextSecondary

@Composable
fun LibraryScreen(onAddGames: () -> Unit, vm: LibraryViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var settingsFor by remember { mutableStateOf<LibraryItem?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Juegos", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onAddGames) { Text("AÑADIR", style = MaterialTheme.typography.labelLarge) }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar en tu biblioteca") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryFilter.entries.forEach { f ->
                FilterChip(selected = filter == f, onClick = { vm.filter.value = f }, label = { Text(f.label) })
            }
        }

        if (items.isEmpty()) {
            EmptyState(hasGames = query.isBlank() && filter == LibraryFilter.ALL, onAddGames = onAddGames)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.entry.packageName }) { item ->
                    GameCard(
                        item = item,
                        profile = profiles.firstOrNull { it.id == item.entry.profileId },
                        onLaunch = {
                            val intent = vm.launchIntent(item.entry.packageName)
                            if (intent == null) {
                                Toast.makeText(context, "No se puede abrir esta app", Toast.LENGTH_SHORT).show()
                            } else {
                                try {
                                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    vm.markLaunched(item.entry.packageName)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "No se puede abrir esta app", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onFavorite = { vm.toggleFavorite(item.entry.packageName) },
                        onSettings = { settingsFor = item },
                    )
                }
            }
        }
    }

    settingsFor?.let { item ->
        GameSettingsDialog(
            item = item,
            profiles = profiles,
            onSave = { profileId -> vm.setProfile(item.entry.packageName, profileId); settingsFor = null },
            onRemove = { vm.removeGame(item.entry.packageName); settingsFor = null },
            onDismiss = { settingsFor = null },
        )
    }
}

@Composable
private fun EmptyState(hasGames: Boolean, onAddGames: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NexusBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasGames) {
                Text("Tu biblioteca está vacía", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Elige qué apps son tus juegos. Quitarlas de aquí nunca las desinstala.",
                    style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary,
                )
                Button(onClick = onAddGames) { Text("AÑADIR JUEGOS", style = MaterialTheme.typography.labelLarge) }
            } else {
                Text("Ningún juego coincide", style = MaterialTheme.typography.titleMedium)
                Text("Prueba con otra búsqueda o filtro.", style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary)
            }
        }
    }
}

@Composable
private fun GameCard(
    item: LibraryItem,
    profile: GameProfile?,
    onLaunch: () -> Unit,
    onFavorite: () -> Unit,
    onSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NexusBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(item.entry.packageName, size = 56.dp)
                Box(Modifier.weight(1f))
                IconButton(onClick = onFavorite) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (item.entry.favorite) "Quitar de favoritos" else "Marcar como favorito",
                        tint = if (item.entry.favorite) NexusRed else NexusBorder,
                    )
                }
            }
            Text(item.entry.label, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape)
                        .background(profile?.let { Color(it.colorArgb) } ?: NexusBorder),
                )
                Text(
                    profile?.name ?: "Perfil activo",
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary,
                )
            }
            if (!item.installed) {
                Text("Ya no está instalada", style = MaterialTheme.typography.bodyMedium, color = NexusRed)
            }
            Button(onClick = onLaunch, enabled = item.installed, modifier = Modifier.fillMaxWidth()) {
                Text("INICIAR", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("AJUSTES DEL JUEGO") }
        }
    }
}

@Composable
private fun GameSettingsDialog(
    item: LibraryItem,
    profiles: List<GameProfile>,
    onSave: (String?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(item.entry.profileId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.entry.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Perfil de este juego", style = MaterialTheme.typography.titleMedium)
                ProfileOption("Usar el perfil activo", selected == null) { selected = null }
                profiles.forEach { p ->
                    ProfileOption(p.name, selected == p.id) { selected = p.id }
                }
                TextButton(onClick = onRemove) { Text("QUITAR DE LA BIBLIOTECA", color = NexusRed) }
                Text(
                    "Quitar un juego de la biblioteca no lo desinstala.",
                    style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary,
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(selected) }) { Text("GUARDAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun ProfileOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
