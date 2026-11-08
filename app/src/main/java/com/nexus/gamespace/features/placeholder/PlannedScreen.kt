package com.nexus.gamespace.features.placeholder

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn

/** Pantalla de una función aún no implementada. Dice claramente que no hace nada todavía. */
@Composable
fun PlannedScreen(title: String, description: String, phase: Int, plan: List<String>) {
    ScreenColumn(title = title, subtitle = description) {
        NexusCard {
            InfoRow("Estado", "No implementado todavía")
            InfoRow("Se construye en", "Fase $phase")
        }
        NexusCard {
            Text("Qué incluirá", style = MaterialTheme.typography.titleMedium)
            plan.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
