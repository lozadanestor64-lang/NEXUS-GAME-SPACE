package com.nexus.gamespace.features.more

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.components.ScreenColumn
import com.nexus.gamespace.ui.navigation.Screen
import androidx.compose.material3.MaterialTheme

private val entries = listOf(
    Screen.GameConfig, Screen.Lab, Screen.Shizuku, Screen.Network,
    Screen.Toolbox, Screen.Settings, Screen.About,
)

@Composable
fun MoreScreen(onOpen: (Screen) -> Unit) {
    ScreenColumn(title = "Más") {
        entries.forEach { screen ->
            NexusCard(modifier = Modifier.clickable { onOpen(screen) }) {
                Text(screen.title, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
