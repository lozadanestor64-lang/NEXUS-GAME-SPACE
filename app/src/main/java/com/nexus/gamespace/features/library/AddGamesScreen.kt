package com.nexus.gamespace.features.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.gamespace.ui.components.AppIcon
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusTextSecondary

@Composable
fun AddGamesScreen(onBack: () -> Unit, vm: LibraryViewModel = viewModel()) {
    val apps by vm.pickerList.collectAsStateWithLifecycle()
    val saved by vm.savedPackages.collectAsStateWithLifecycle()
    val query by vm.pickerQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadInstalled() }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text("Añadir juegos", style = MaterialTheme.typography.headlineMedium)
        }
        Text(
            "Elige tus juegos entre las apps instaladas. Android no distingue juegos de apps de forma fiable, " +
                "así que decides tú. «Sugerido» significa que la propia app se declara como juego.",
            style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { vm.pickerQuery.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar apps") },
            singleLine = true,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.packageName, size = 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (app.suggestedGame) {
                            Text("Sugerido como juego", style = MaterialTheme.typography.bodyMedium, color = NexusRed)
                        }
                    }
                    if (app.packageName in saved) {
                        TextButton(onClick = { vm.removeGame(app.packageName) }) { Text("QUITAR") }
                    } else {
                        Button(onClick = { vm.addGame(app) }) { Text("AÑADIR") }
                    }
                }
            }
        }
    }
}
