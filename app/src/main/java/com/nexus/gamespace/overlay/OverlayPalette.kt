package com.nexus.gamespace.overlay

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nexus.gamespace.domain.ColorRole
import com.nexus.gamespace.domain.OverlayColors

/**
 * Colores ya resueltos del panel (con el contraste automático aplicado). Todos los componentes del panel
 * los leen de aquí, de modo que un cambio en el Color Studio llega a todas las pantallas y ventanas.
 */
class OvPalette(val colors: OverlayColors) {
    private fun c(role: ColorRole) = Color(colors.resolved(role))

    val title get() = c(ColorRole.TITLE)
    val text get() = c(ColorRole.TEXT_MAIN)
    val textSecondary get() = c(ColorRole.TEXT_SECONDARY)
    val description get() = c(ColorRole.DESCRIPTION)
    val buttonText get() = c(ColorRole.BUTTON_TEXT)
    val controlText get() = c(ColorRole.CONTROL_TEXT)
    val indicatorText get() = c(ColorRole.INDICATOR_TEXT)
    val numbers get() = c(ColorRole.NUMBERS)
    val monitorValues get() = c(ColorRole.MONITOR_VALUES)
    val accentText get() = c(ColorRole.ACCENT_TEXT)

    val panelBg get() = c(ColorRole.PANEL_BG)
    val settingsBg get() = c(ColorRole.SETTINGS_BG)
    val cardBg get() = c(ColorRole.CARD_BG)
    val buttonBg get() = c(ColorRole.BUTTON_BG)
    val menuBg get() = c(ColorRole.MENU_BG)
    val dialogBg get() = c(ColorRole.DIALOG_BG)

    val border get() = c(ColorRole.BORDER)
    val icons get() = c(ColorRole.ICONS)
    val bars get() = c(ColorRole.BARS)
    val rings get() = c(ColorRole.RINGS)
    val indicators get() = c(ColorRole.INDICATORS)
    val activeButton get() = c(ColorRole.ACTIVE_BUTTON)
    val glow get() = c(ColorRole.GLOW)
    val selection get() = c(ColorRole.SELECTION)
}

val LocalOvPalette = compositionLocalOf { OvPalette(OverlayColors()) }

/** Esquema de Material 3 construido con la paleta, para que los componentes de Material también usen los colores elegidos. */
fun overlayColorScheme(p: OvPalette): ColorScheme = darkColorScheme(
    primary = p.activeButton,
    onPrimary = p.buttonText,
    primaryContainer = p.selection.copy(alpha = 0.30f),
    onPrimaryContainer = p.controlText,
    secondary = p.selection,
    onSecondary = p.buttonText,
    secondaryContainer = p.selection.copy(alpha = 0.30f),
    onSecondaryContainer = p.controlText,
    background = p.panelBg,
    onBackground = p.text,
    surface = p.cardBg,
    onSurface = p.text,
    surfaceVariant = p.menuBg,
    onSurfaceVariant = p.textSecondary,
    outline = p.border,
    outlineVariant = p.border,
)
