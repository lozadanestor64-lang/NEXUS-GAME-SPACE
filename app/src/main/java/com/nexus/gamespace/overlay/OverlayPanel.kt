package com.nexus.gamespace.overlay

import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.booster.BoosterController
import com.nexus.gamespace.core.NOT_AVAILABLE
import com.nexus.gamespace.core.formatGb
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
import com.nexus.gamespace.domain.OverlayTool
import com.nexus.gamespace.domain.formatElapsed
import com.nexus.gamespace.ui.components.AppIcon
import com.nexus.gamespace.ui.components.CircularGauge
import com.nexus.gamespace.ui.components.InfoRow
import com.nexus.gamespace.ui.components.NexusCard
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusTheme

/** Panel gaming que se dibuja sobre el juego. Todos sus controles llaman a acciones reales. */
@Composable
fun OverlayPanel(
    controller: BoosterController,
    prefs: OverlayPrefs,
    shizukuLabel: String,
    landscape: Boolean,
    maxHeightDp: Float,
    visible: Boolean,
    onClose: () -> Unit,
    onHide: () -> Unit,
    onSwitchSide: () -> Unit,
    onOpenLibrary: () -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenSettings: (Intent) -> Unit,
    onNotesOpen: (Boolean) -> Unit,
) {
    val base = LocalDensity.current
    val scaled = Density(base.density * prefs.scalePercent / 100f, base.fontScale)

    CompositionLocalProvider(LocalDensity provides scaled) {
        NexusTheme {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally { w -> if (prefs.side == OverlaySide.LEFT) -w else w } + fadeIn(),
                exit = slideOutHorizontally { w -> if (prefs.side == OverlaySide.LEFT) -w else w } + fadeOut(),
            ) {
                PanelSurface(
                    controller, prefs, shizukuLabel, landscape, maxHeightDp,
                    onClose, onHide, onSwitchSide, onOpenLibrary, onLaunchGame, onOpenSettings, onNotesOpen,
                )
            }
        }
    }
}

@Composable
private fun PanelSurface(
    controller: BoosterController,
    prefs: OverlayPrefs,
    shizukuLabel: String,
    landscape: Boolean,
    maxHeightDp: Float,
    onClose: () -> Unit,
    onHide: () -> Unit,
    onSwitchSide: () -> Unit,
    onOpenLibrary: () -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenSettings: (Intent) -> Unit,
    onNotesOpen: (Boolean) -> Unit,
) {
    val message by controller.message.collectAsState()
    val restore by controller.restoreState.collectAsState()
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeightDp.dp)
            .clip(shape)
            .background(Color(0xFF08080A).copy(alpha = prefs.alphaPercent / 100f))
            .border(1.dp, NexusRed.copy(alpha = 0.65f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "NEXUS GAME SPACE",
                style = MaterialTheme.typography.titleMedium,
                color = NexusRed,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSwitchSide) { Text("⇄ LADO") }
            TextButton(onClick = onHide) { Text("OCULTAR") }
        }

        val toolBlocks = ToolBlocks(controller, prefs, shizukuLabel, onOpenLibrary, onLaunchGame, onOpenSettings, onNotesOpen)

        if (landscape) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { toolBlocks.Left() }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { toolBlocks.Center() }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { toolBlocks.Right() }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                toolBlocks.Center()
                toolBlocks.Left()
                toolBlocks.Right()
            }
        }

        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (restore.hasChanges) {
            OutlinedButton(onClick = { controller.restoreAll() }, modifier = Modifier.fillMaxWidth()) {
                Text("RESTAURAR CAMBIOS TEMPORALES")
            }
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("VOLVER AL JUEGO", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Agrupa las herramientas en tres columnas (izquierda, centro, derecha) como en un panel de juego. */
private class ToolBlocks(
    val controller: BoosterController,
    val prefs: OverlayPrefs,
    val shizukuLabel: String,
    val onOpenLibrary: () -> Unit,
    val onLaunchGame: (String) -> Unit,
    val onOpenSettings: (Intent) -> Unit,
    val onNotesOpen: (Boolean) -> Unit,
) {
    @Composable
    fun Left() {
        if (prefs.isVisible(OverlayTool.PROFILES)) ProfilesTool(controller)
        if (prefs.isVisible(OverlayTool.FAVORITES)) FavoritesTool(controller, onLaunchGame)
        if (prefs.isVisible(OverlayTool.TIMER)) TimerTool(controller)
        if (prefs.isVisible(OverlayTool.NOTES)) NotesTool(controller, onNotesOpen)
        if (prefs.isVisible(OverlayTool.LIBRARY)) {
            OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("BIBLIOTECA DE JUEGOS") }
        }
    }

    @Composable
    fun Center() {
        if (prefs.isVisible(OverlayTool.MONITOR)) MonitorTool(controller, shizukuLabel)
    }

    @Composable
    fun Right() {
        if (prefs.isVisible(OverlayTool.DND)) DndTool(controller, onOpenSettings)
        if (prefs.isVisible(OverlayTool.BRIGHTNESS)) BrightnessTool(controller, onOpenSettings)
        if (prefs.isVisible(OverlayTool.VOLUME)) VolumeTool(controller)
        if (prefs.isVisible(OverlayTool.ROTATION)) RotationTool(controller, onOpenSettings)
        if (prefs.isVisible(OverlayTool.WIFI)) {
            OutlinedButton(onClick = { onOpenSettings(controller.connectivityIntent()) }, modifier = Modifier.fillMaxWidth()) {
                Text("PANEL WI-FI")
            }
        }
        CaptureNote()
    }
}

@Composable
private fun MonitorTool(controller: BoosterController, shizukuLabel: String) {
    val live by controller.live.collectAsState()
    NexusCard {
        Text("Dispositivo", style = MaterialTheme.typography.titleMedium)
        val l = live
        if (l == null) {
            Text("Leyendo…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val s = l.snapshot
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CircularGauge(
                    fraction = s.batteryPercent?.let { it / 100f },
                    valueText = s.batteryPercent?.let { "$it%" } ?: NOT_AVAILABLE,
                    label = "Batería", size = 72.dp,
                )
                CircularGauge(
                    fraction = 1f - s.ramUsedFraction,
                    valueText = formatGb(s.availRamBytes),
                    label = "RAM libre", size = 72.dp,
                )
            }
            InfoRow("Estado térmico", s.thermal?.label ?: NOT_AVAILABLE)
            InfoRow("Temp. batería", s.batteryTempCelsius?.let { "%.1f °C".format(it) } ?: NOT_AVAILABLE)
            InfoRow("Conexión", l.network.label)
            InfoRow("CPU", s.socName ?: NOT_AVAILABLE)
            InfoRow("Uso de CPU / GPU", NOT_AVAILABLE)
            InfoRow("Pantalla", s.refreshRateHz?.let { "%.0f Hz (no son FPS)".format(it) } ?: NOT_AVAILABLE)
            InfoRow("FPS del juego", NOT_AVAILABLE)
            InfoRow("Sesión", "Sin sesión activa")
            InfoRow("Shizuku", shizukuLabel)
            Text(
                "El estado térmico es informativo: esta app no enfría el teléfono ni cambia sus límites.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfilesTool(controller: BoosterController) {
    val profiles by controller.profiles.collectAsState()
    val activeId by controller.activeProfileId.collectAsState()
    NexusCard {
        Text("Perfil gaming", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            profiles.filter { it.id != "custom" }.forEach { p ->
                FilterChip(
                    selected = p.id == activeId,
                    onClick = { controller.setActiveProfile(p.id) },
                    label = { Text(p.name) },
                )
            }
        }
        Text(
            "Se guarda como perfil activo para las sesiones. Todavía no cambia ajustes del sistema por sí solo.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoritesTool(controller: BoosterController, onLaunchGame: (String) -> Unit) {
    val favorites by controller.favorites.collectAsState()
    NexusCard {
        Text("Apps favoritas", style = MaterialTheme.typography.titleMedium)
        if (favorites.isEmpty()) {
            Text(
                "Marca juegos con la estrella en la biblioteca para tenerlos aquí.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                favorites.forEach { g ->
                    AppIcon(g.packageName, size = 46.dp, modifier = Modifier.clickable { onLaunchGame(g.packageName) })
                }
            }
        }
    }
}

@Composable
private fun TimerTool(controller: BoosterController) {
    val sw by controller.stopwatch.collectAsState()
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sw.running) {
        while (sw.running) {
            tick = SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(1_000L)
        }
        tick = SystemClock.elapsedRealtime()
    }
    NexusCard {
        Text("Temporizador", style = MaterialTheme.typography.titleMedium)
        Text(formatElapsed(sw.elapsed(tick)), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { controller.toggleStopwatch() }) { Text(if (sw.running) "PAUSAR" else "INICIAR") }
            OutlinedButton(onClick = { controller.resetStopwatch() }) { Text("REINICIAR") }
        }
    }
}

@Composable
private fun NotesTool(controller: BoosterController, onNotesOpen: (Boolean) -> Unit) {
    val notes by controller.notes.collectAsState()
    var open by remember { mutableStateOf(false) }
    // Escribir necesita foco de teclado; solo se concede mientras esta sección está abierta.
    LaunchedEffect(open) { onNotesOpen(open) }
    DisposableEffect(Unit) { onDispose { onNotesOpen(false) } }

    NexusCard {
        Text("Notas rápidas", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
            Text(if (open) "CERRAR NOTAS" else "ABRIR NOTAS")
        }
        if (open) {
            OutlinedTextField(
                value = notes,
                onValueChange = { controller.onNotesChanged(it.take(2_000)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("Escribe aquí") },
            )
            Text(
                "Al escribir, Android puede quitarle el foco al juego y algunos juegos se pausan.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DndTool(controller: BoosterController, onOpenSettings: (Intent) -> Unit) {
    val access by controller.access.collectAsState()
    val restore by controller.restoreState.collectAsState()
    NexusCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("No molestar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!access.dnd) {
                TextButton(onClick = { onOpenSettings(controller.dndAccessIntent()) }) { Text("CONCEDER ACCESO") }
            } else {
                Switch(
                    checked = restore.dndPrevious != null,
                    onCheckedChange = { if (it) controller.applyDnd() else controller.restoreDnd() },
                )
            }
        }
        if (!access.dnd) {
            Text(
                "Android pide que lo concedas tú en Ajustes.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BrightnessTool(controller: BoosterController, onOpenSettings: (Intent) -> Unit) {
    val access by controller.access.collectAsState()
    val restore by controller.restoreState.collectAsState()
    NexusCard {
        Text("Brillo", style = MaterialTheme.typography.titleMedium)
        if (!access.writeSettings) {
            TextButton(onClick = { onOpenSettings(controller.writeSettingsIntent()) }) { Text("CONCEDER ACCESO") }
        } else {
            var pct by remember(access.brightnessPercent) {
                mutableFloatStateOf((access.brightnessPercent ?: 50).toFloat().coerceIn(1f, 100f))
            }
            Slider(
                value = pct,
                onValueChange = { pct = it },
                onValueChangeFinished = { controller.applyBrightness(pct.toInt()) },
                valueRange = 1f..100f,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${pct.toInt()} %", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { controller.restoreBrightness() }, enabled = restore.brightnessPrevious != null) {
                    Text("RESTAURAR")
                }
            }
        }
    }
}

@Composable
private fun VolumeTool(controller: BoosterController) {
    NexusCard {
        Text("Volumen", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { controller.adjustVolume(false) }) { Text("−") }
            OutlinedButton(onClick = { controller.adjustVolume(true) }) { Text("+") }
            OutlinedButton(onClick = { controller.showVolumePanel() }) { Text("PANEL") }
        }
    }
}

@Composable
private fun RotationTool(controller: BoosterController, onOpenSettings: (Intent) -> Unit) {
    val access by controller.access.collectAsState()
    NexusCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Rotación automática", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!access.writeSettings) {
                TextButton(onClick = { onOpenSettings(controller.writeSettingsIntent()) }) { Text("CONCEDER ACCESO") }
            } else {
                Switch(
                    checked = access.autoRotate == true,
                    onCheckedChange = { controller.setAutoRotate(it) },
                )
            }
        }
    }
}

@Composable
private fun CaptureNote() {
    NexusCard {
        Text("Captura y grabación", style = MaterialTheme.typography.titleMedium)
        InfoRow("Estado", "No disponible todavía")
        Text(
            "Android solo permite capturar otras apps con MediaProjection y tu consentimiento. " +
                "Llegará en una fase posterior; mientras, usa la captura del sistema.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
    }
}
