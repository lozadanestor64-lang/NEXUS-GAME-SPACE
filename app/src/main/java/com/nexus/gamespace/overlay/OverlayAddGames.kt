package com.nexus.gamespace.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.booster.BoosterController
import com.nexus.gamespace.domain.InstalledApp
import com.nexus.gamespace.ui.components.AppIcon
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusTextSecondary

/**
 * Ventana AGREGAR JUEGOS: apps con actividad lanzable (visibilidad limitada de paquetes), con su icono
 * original, buscador y selección múltiple. Al agregar vuelve al panel y los iconos ya aparecen en Favoritos.
 */
@Composable
fun AddGamesContent(controller: BoosterController, onDone: () -> Unit) {
    val apps by controller.installedApps.collectAsState()
    val favorites by controller.favorites.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val spacing = LocalOverlaySpacing.current

    LaunchedEffect(Unit) { controller.loadInstalledApps() }

    val favoriteSet = remember(favorites) { favorites.map { it.packageName }.toSet() }
    val visible = remember(apps, query) {
        apps.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<InstalledApp> { it.suggestedGame }.thenBy { it.label.lowercase() })
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Buscar aplicaciones") },
        )
        Text(
            if (apps.isEmpty()) "Buscando aplicaciones…" else "${selected.size} seleccionadas · ${visible.size} aplicaciones",
            style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
        )
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(visible, key = { it.packageName }) { app ->
                val already = app.packageName in favoriteSet
                val checked = already || app.packageName in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !already) {
                            selected = if (app.packageName in selected) selected - app.packageName else selected + app.packageName
                        }
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.packageName, size = 36.dp, shape = CircleShape)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (already) {
                            Text("Ya está en favoritos", style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary)
                        } else if (app.suggestedGame) {
                            Text("Sugerido como juego", style = MaterialTheme.typography.labelSmall, color = NexusRed)
                        }
                    }
                    Checkbox(
                        checked = checked,
                        enabled = !already,
                        onCheckedChange = { isOn ->
                            selected = if (isOn) selected + app.packageName else selected - app.packageName
                        },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("CANCELAR", onDone, Modifier.weight(1f), filled = false)
            OvButton(
                "AGREGAR (${selected.size})",
                {
                    controller.addFavorites(apps.filter { it.packageName in selected })
                    onDone()
                },
                Modifier.weight(1f),
                enabled = selected.isNotEmpty(),
            )
        }
    }
}
