package com.nexus.gamespace.overlay

import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nexus.gamespace.booster.BoosterController
import com.nexus.gamespace.core.NOT_AVAILABLE
import com.nexus.gamespace.core.formatGb
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
import com.nexus.gamespace.domain.OverlayTool
import com.nexus.gamespace.domain.ThermalLevel
import com.nexus.gamespace.domain.formatElapsed
import com.nexus.gamespace.ui.components.AppIcon
import com.nexus.gamespace.ui.theme.NexusOk
import com.nexus.gamespace.ui.theme.NexusRed
import com.nexus.gamespace.ui.theme.NexusTextSecondary
import com.nexus.gamespace.ui.theme.NexusTheme
import com.nexus.gamespace.ui.theme.NexusWarn
import kotlin.math.roundToInt

private enum class PanelScreen { MAIN, ADD_GAMES, SETTINGS, CROSSHAIR }

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
    onNeedsKeyboard: (Boolean) -> Unit,
) {
    val base = LocalDensity.current
    val scaled = Density(
        base.density * prefs.scalePercent / 100f,
        base.fontScale * prefs.textScalePercent / 100f,
    )
    val towardsLeft = prefs.side == OverlaySide.LEFT
    val enter: EnterTransition = when (prefs.animation) {
        AnimationMode.OFF -> EnterTransition.None
        AnimationMode.REDUCED -> fadeIn(tween(150))
        else -> slideInHorizontally(tween(280)) { w -> if (towardsLeft) -w else w } + fadeIn(tween(280))
    }
    val exit: ExitTransition = when (prefs.animation) {
        AnimationMode.OFF -> ExitTransition.None
        AnimationMode.REDUCED -> fadeOut(tween(120))
        else -> slideOutHorizontally(tween(240)) { w -> if (towardsLeft) -w else w } + fadeOut(tween(240))
    }

    CompositionLocalProvider(
        LocalDensity provides scaled,
        LocalOverlaySpacing provides prefs.spacingDp.dp,
        LocalIconScale provides prefs.iconScalePercent / 100f,
        LocalAnimation provides prefs.animation,
    ) {
        NexusTheme(typography = OverlayTypography) {
            AnimatedVisibility(visible = visible, enter = enter, exit = exit) {
                PanelSurface(
                    controller, prefs, shizukuLabel, landscape, maxHeightDp,
                    onClose, onHide, onSwitchSide, onOpenLibrary, onLaunchGame, onOpenSettings, onNeedsKeyboard,
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
    onNeedsKeyboard: (Boolean) -> Unit,
) {
    val message by controller.message.collectAsState()
    val restore by controller.restoreState.collectAsState()
    val spacing = LocalOverlaySpacing.current
    val shape = RoundedCornerShape(18.dp)

    var screen by remember { mutableStateOf(PanelScreen.MAIN) }
    var notesOpen by remember { mutableStateOf(false) }
    val needsKeyboard = notesOpen || screen == PanelScreen.ADD_GAMES
    // El teclado necesita foco de ventana; solo se concede mientras hace falta.
    LaunchedEffect(needsKeyboard) { onNeedsKeyboard(needsKeyboard) }
    DisposableEffect(Unit) { onDispose { onNeedsKeyboard(false) } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeightDp.dp)
            .clip(shape)
            .background(Color(0xFF07070A).copy(alpha = prefs.alphaPercent / 100f))
            .border(1.dp, NexusRed.copy(alpha = 0.6f), shape)
            .padding(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        // ---- Encabezado: siempre visible ----
        if (screen == PanelScreen.MAIN) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "NEXUS GAME SPACE", style = MaterialTheme.typography.titleLarge, color = NexusRed,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                OvRoundButton("Agregar juegos", { screen = PanelScreen.ADD_GAMES }, filled = true) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                }
                OvRoundButton("Configuración del panel", { screen = PanelScreen.SETTINGS }) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
                }
                OvRoundButton("Cambiar de lado", onSwitchSide) {
                    Text("⇄", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                OvButton("OCULTAR", onHide, filled = false)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OvRoundButton("Volver al panel", { screen = PanelScreen.MAIN }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(
                    when (screen) {
                        PanelScreen.ADD_GAMES -> "AGREGAR JUEGOS"
                        PanelScreen.CROSSHAIR -> "MIRA"
                        else -> "CONFIGURACIÓN DEL PANEL"
                    },
                    style = MaterialTheme.typography.titleLarge, color = NexusRed,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ---- Contenido: lo único que se desplaza ----
        Box(Modifier.weight(1f, fill = false)) {
            when (screen) {
                PanelScreen.MAIN -> MainContent(
                    controller, prefs, shizukuLabel, landscape,
                    onOpenLibrary, onLaunchGame, onOpenSettings, notesOpen, { notesOpen = it },
                    onOpenCrosshair = { screen = PanelScreen.CROSSHAIR },
                )
                PanelScreen.CROSSHAIR -> CrosshairContent(controller)
                PanelScreen.ADD_GAMES -> AddGamesContent(controller) { screen = PanelScreen.MAIN }
                PanelScreen.SETTINGS -> PanelSettingsContent(
                    prefs = prefs,
                    onSave = { controller.saveAppearance(it) },
                    onSide = { controller.setSide(it) },
                )
            }
        }

        // ---- Pie: siempre visible ----
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (restore.hasChanges) {
            OvButton("RESTAURAR CAMBIOS TEMPORALES", { controller.restoreAll() }, Modifier.fillMaxWidth(), filled = false)
        }
        OvButton("VOLVER AL JUEGO", onClose, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MainContent(
    controller: BoosterController,
    prefs: OverlayPrefs,
    shizukuLabel: String,
    landscape: Boolean,
    onOpenLibrary: () -> Unit,
    onLaunchGame: (String) -> Unit,
    onOpenSettings: (Intent) -> Unit,
    notesOpen: Boolean,
    onNotesOpenChange: (Boolean) -> Unit,
    onOpenCrosshair: () -> Unit,
) {
    val spacing = LocalOverlaySpacing.current
    val t = Tools(controller, prefs, shizukuLabel, onOpenLibrary, onLaunchGame, onOpenSettings, notesOpen, onNotesOpenChange, onOpenCrosshair)

    if (landscape) {
        // Tres zonas: herramientas a la izquierda, monitor al centro y controles a la derecha.
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {
                t.Favorites(); t.Profiles(); t.Timer(); t.Notes(); t.Library()
            }
            Column(Modifier.weight(1.15f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {
                t.Monitor()
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {
                t.Dnd(); t.Rotation(); t.Brightness(); t.Volume(); t.Wifi(); t.Crosshair(); t.Capture()
            }
        }
    } else {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing)) {
            t.Monitor(); t.Favorites(); t.Profiles()
            t.Dnd(); t.Rotation(); t.Brightness(); t.Volume(); t.Wifi(); t.Crosshair()
            t.Timer(); t.Notes(); t.Library(); t.Capture()
        }
    }
}

/** Cada herramienta respeta si el usuario la ocultó en Configuración. */
private class Tools(
    val controller: BoosterController,
    val prefs: OverlayPrefs,
    val shizukuLabel: String,
    val onOpenLibrary: () -> Unit,
    val onLaunchGame: (String) -> Unit,
    val onOpenSettings: (Intent) -> Unit,
    val notesOpen: Boolean,
    val onNotesOpenChange: (Boolean) -> Unit,
    val onOpenCrosshair: () -> Unit,
) {
    @Composable fun Crosshair() { if (prefs.isVisible(OverlayTool.CROSSHAIR)) CrosshairTool(controller, onOpenCrosshair) }
    @Composable fun Monitor() { if (prefs.isVisible(OverlayTool.MONITOR)) MonitorTool(controller, shizukuLabel) }
    @Composable fun Favorites() { if (prefs.isVisible(OverlayTool.FAVORITES)) FavoritesTool(controller, onLaunchGame) }
    @Composable fun Profiles() { if (prefs.isVisible(OverlayTool.PROFILES)) ProfilesTool(controller) }
    @Composable fun Dnd() { if (prefs.isVisible(OverlayTool.DND)) DndTool(controller, onOpenSettings) }
    @Composable fun Rotation() { if (prefs.isVisible(OverlayTool.ROTATION)) RotationTool(controller, onOpenSettings) }
    @Composable fun Brightness() { if (prefs.isVisible(OverlayTool.BRIGHTNESS)) BrightnessTool(controller, onOpenSettings) }
    @Composable fun Volume() { if (prefs.isVisible(OverlayTool.VOLUME)) VolumeTool(controller) }
    @Composable fun Wifi() {
        if (prefs.isVisible(OverlayTool.WIFI)) {
            OvButton("PANEL WI-FI", { onOpenSettings(controller.connectivityIntent()) }, Modifier.fillMaxWidth(), filled = false)
        }
    }
    @Composable fun Timer() { if (prefs.isVisible(OverlayTool.TIMER)) TimerTool(controller) }
    @Composable fun Notes() { if (prefs.isVisible(OverlayTool.NOTES)) NotesTool(controller, notesOpen, onNotesOpenChange) }
    @Composable fun Library() {
        if (prefs.isVisible(OverlayTool.LIBRARY)) {
            OvButton("BIBLIOTECA DE JUEGOS", onOpenLibrary, Modifier.fillMaxWidth(), filled = false)
        }
    }
    @Composable fun Capture() { CaptureNote() }
}

// ----------------------------------------------------------------------------- herramientas

@Composable
private fun MonitorTool(controller: BoosterController, shizukuLabel: String) {
    val live by controller.live.collectAsState()
    val profiles by controller.profiles.collectAsState()
    val activeId by controller.activeProfileId.collectAsState()
    val iconScale = LocalIconScale.current

    OvCard(title = "Monitor") {
        val l = live
        if (l == null) {
            Text("Leyendo…", style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary)
        } else {
            val s = l.snapshot
            val ring = (60 * iconScale).dp
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OvRing(
                    fraction = s.batteryPercent?.let { it / 100f }, label = "Batería",
                    center = { f -> "${(f * 100).roundToInt()}%" }, color = NexusRed, ringSize = ring,
                )
                OvRing(
                    fraction = 1f - s.ramUsedFraction, label = "RAM libre",
                    center = { f -> "${(f * 100).roundToInt()}%" }, color = NexusRed, ringSize = ring,
                )
                OvRing(
                    fraction = s.thermal?.let { thermalFraction(it) }, label = "Térmico",
                    center = { _ -> s.thermal?.label ?: NOT_AVAILABLE },
                    color = s.thermal?.let { thermalColor(it) } ?: NexusRed, ringSize = ring,
                )
            }
            OvStatPair("RAM libre", "${formatGb(s.availRamBytes)} de ${formatGb(s.totalRamBytes)}",
                "Temp. batería", s.batteryTempCelsius?.let { "%.1f °C".format(it) } ?: NOT_AVAILABLE)
            OvStatPair("Conexión", l.network.label,
                "Carga", when (s.batteryCharging) { true -> "Cargando"; false -> "No cargando"; null -> NOT_AVAILABLE })
            OvStatPair("CPU", s.socName ?: NOT_AVAILABLE, "Uso de CPU / GPU", NOT_AVAILABLE)
            OvStatPair("Pantalla (no son FPS)", s.refreshRateHz?.let { "%.0f Hz".format(it) } ?: NOT_AVAILABLE,
                "FPS del juego", NOT_AVAILABLE)
            OvStatPair("Perfil activo", profiles.firstOrNull { it.id == activeId }?.name ?: "—",
                "Juego activo", "Sin detectar")
            OvStatPair("Shizuku", shizukuLabel, "Sesión", "Sin sesión activa")
            Text(
                "El anillo térmico muestra el estado del sistema por niveles. Es informativo: la app no enfría el teléfono.",
                style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
            )
        }
    }
}

private fun thermalFraction(level: ThermalLevel): Float = when (level) {
    ThermalLevel.NORMAL -> 0.25f
    ThermalLevel.ATTENTION -> 0.5f
    ThermalLevel.ELEVATED -> 0.75f
    ThermalLevel.CRITICAL -> 1f
}

private fun thermalColor(level: ThermalLevel): Color = when (level) {
    ThermalLevel.NORMAL -> NexusOk
    ThermalLevel.ATTENTION -> NexusWarn
    ThermalLevel.ELEVATED, ThermalLevel.CRITICAL -> NexusRed
}

@Composable
private fun FavoritesTool(controller: BoosterController, onLaunchGame: (String) -> Unit) {
    val favorites by controller.favorites.collectAsState()
    var editing by remember { mutableStateOf(false) }
    val iconScale = LocalIconScale.current

    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Favoritos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (favorites.isNotEmpty()) {
                OvButton(if (editing) "LISTO" else "EDITAR", { editing = !editing }, filled = false)
            }
        }
        if (favorites.isEmpty()) {
            Text(
                "Pulsa el botón + de arriba para agregar tus juegos.",
                style = MaterialTheme.typography.bodyMedium, color = NexusTextSecondary,
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                favorites.forEach { g ->
                    FavoriteItem(
                        game = g, editing = editing, iconSize = (46 * iconScale).dp,
                        onLaunch = { onLaunchGame(g.packageName) },
                        onLeft = { controller.moveFavorite(g.packageName, -1) },
                        onRight = { controller.moveFavorite(g.packageName, 1) },
                        onRemove = { controller.removeFavorite(g.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    game: GameEntry,
    editing: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    onLaunch: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.width(iconSize + 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(
            game.packageName, size = iconSize, shape = CircleShape,
            modifier = Modifier.clickable(enabled = !editing, onClick = onLaunch),
        )
        Text(
            game.label, style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        if (editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                OvRoundButton("Mover a la izquierda", onLeft, size = 24.dp) { Text("◀", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                OvRoundButton("Quitar de favoritos", onRemove, size = 24.dp, filled = true) { Text("✕", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                OvRoundButton("Mover a la derecha", onRight, size = 24.dp) { Text("▶", color = Color.White, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun ProfilesTool(controller: BoosterController) {
    val profiles by controller.profiles.collectAsState()
    val activeId by controller.activeProfileId.collectAsState()
    OvCard(title = "Perfil gaming") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            profiles.filter { it.id != "custom" }.forEach { p ->
                FilterChip(
                    selected = p.id == activeId,
                    onClick = { controller.setActiveProfile(p.id) },
                    label = { Text(p.name, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                )
            }
        }
        Text(
            "Se guarda como perfil activo. Todavía no cambia ajustes del sistema por sí solo.",
            style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
        )
    }
}

@Composable
private fun TimerTool(controller: BoosterController) {
    val sw by controller.stopwatch.collectAsState()
    var tick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    // Solo se redibuja mientras el cronómetro corre y el panel está abierto.
    LaunchedEffect(sw.running) {
        while (sw.running) {
            tick = SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(1_000L)
        }
        tick = SystemClock.elapsedRealtime()
    }
    OvCard(title = "Temporizador") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatElapsed(sw.elapsed(tick)), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), maxLines = 1)
            OvButton(if (sw.running) "PAUSAR" else "INICIAR", { controller.toggleStopwatch() })
            OvButton("REINICIAR", { controller.resetStopwatch() }, filled = false)
        }
    }
}

@Composable
private fun NotesTool(controller: BoosterController, open: Boolean, onOpenChange: (Boolean) -> Unit) {
    val notes by controller.notes.collectAsState()
    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Notas rápidas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            OvButton(if (open) "CERRAR" else "ABRIR", { onOpenChange(!open) }, filled = false)
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
                style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
            )
        }
    }
}

@Composable
private fun DndTool(controller: BoosterController, onOpenSettings: (Intent) -> Unit) {
    val access by controller.access.collectAsState()
    val restore by controller.restoreState.collectAsState()
    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("No molestar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!access.dnd) {
                OvButton("CONCEDER", { onOpenSettings(controller.dndAccessIntent()) }, filled = false)
            } else {
                Switch(
                    checked = restore.dndPrevious != null,
                    onCheckedChange = { if (it) controller.applyDnd() else controller.restoreDnd() },
                )
            }
        }
        if (!access.dnd) {
            Text("Android pide que concedas el acceso en Ajustes.", style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary)
        }
    }
}

@Composable
private fun RotationTool(controller: BoosterController, onOpenSettings: (Intent) -> Unit) {
    val access by controller.access.collectAsState()
    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Rotación automática", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!access.writeSettings) {
                OvButton("CONCEDER", { onOpenSettings(controller.writeSettingsIntent()) }, filled = false)
            } else {
                Switch(checked = access.autoRotate == true, onCheckedChange = { controller.setAutoRotate(it) })
            }
        }
        if (!access.writeSettings) {
            Text("Requiere el permiso de modificar ajustes del sistema.", style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary)
        }
    }
}

@Composable
private fun BrightnessTool(controller: BoosterController, onOpenSettings: (Intent) -> Unit) {
    val access by controller.access.collectAsState()
    val restore by controller.restoreState.collectAsState()
    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Brillo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (!access.writeSettings) {
                OvButton("CONCEDER", { onOpenSettings(controller.writeSettingsIntent()) }, filled = false)
            } else {
                OvButton("RESTAURAR", { controller.restoreBrightness() }, filled = false, enabled = restore.brightnessPrevious != null)
            }
        }
        if (access.writeSettings) {
            OvSlider("Nivel", access.brightnessPercent ?: 50, 1..100, "%") { controller.applyBrightness(it) }
        } else {
            Text("Requiere el permiso de modificar ajustes del sistema.", style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary)
        }
    }
}

@Composable
private fun VolumeTool(controller: BoosterController) {
    OvCard(title = "Volumen") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OvButton("−", { controller.adjustVolume(false) }, Modifier.weight(1f), filled = false)
            OvButton("+", { controller.adjustVolume(true) }, Modifier.weight(1f), filled = false)
            OvButton("PANEL", { controller.showVolumePanel() }, Modifier.weight(1.4f), filled = false)
        }
    }
}

@Composable
private fun CrosshairTool(controller: BoosterController, onOpen: () -> Unit) {
    val settings by controller.crosshair.collectAsState()
    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Mira", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = settings.enabled, onCheckedChange = { controller.setCrosshairEnabled(it) })
        }
        OvButton("AJUSTAR MIRA", onOpen, Modifier.fillMaxWidth(), filled = false)
        Text(
            "Retícula cosmética y fija. Algunos juegos competitivos prohíben las superposiciones.",
            style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
        )
    }
}

@Composable
private fun CaptureNote() {
    OvCard(title = "Captura y grabación") {
        Text(
            "No disponible todavía. Android solo permite capturar otras apps con MediaProjection y tu consentimiento. " +
                "Mientras tanto usa la captura del sistema.",
            style = MaterialTheme.typography.labelSmall, color = NexusTextSecondary,
        )
    }
}
