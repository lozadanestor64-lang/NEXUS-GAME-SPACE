package com.nexus.gamespace.overlay

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nexus.gamespace.domain.ColorMath
import com.nexus.gamespace.domain.ColorRole
import com.nexus.gamespace.domain.OverlayColors
import com.nexus.gamespace.domain.RgbEngine
import com.nexus.gamespace.domain.RgbSettings
import com.nexus.gamespace.domain.RgbTarget

/**
 * Colores ya resueltos del panel (contraste automático y RGB aplicados). Todos los componentes los leen de aquí.
 * [phase] devuelve la fase actual del efecto RGB (0..1); se lee al dibujar o componer, así solo se redibuja
 * lo que usa colores con RGB. El texto y las alertas térmicas nunca pasan por el RGB.
 */
class OvPalette(
    val colors: OverlayColors,
    val rgb: RgbSettings = RgbSettings(),
    private val phase: () -> Float = { 0.25f },
) {
    private fun c(role: ColorRole) = Color(colors.resolved(role))

    private fun fx(role: ColorRole, target: RgbTarget, offset: Float): Color {
        val base = colors.resolved(role)
        if (!RgbEngine.isActive(rgb, target)) return Color(base)
        return Color(RgbEngine.colorAt(rgb.effect, phase(), offset, base, rgb.intensityPercent, rgb.reverse))
    }

    /** Fondos con RGB: tinte oscuro (brillo máximo limitado) para no perjudicar la lectura del texto. */
    private fun bg(role: ColorRole, level: Float): Color {
        val base = colors.resolved(role)
        if (!RgbEngine.isActive(rgb, RgbTarget.BACKGROUND)) return Color(base)
        val accent = colors.resolved(ColorRole.RINGS)
        val effect = RgbEngine.colorAt(rgb.effect, phase(), 0.9f, accent, 100, rgb.reverse)
        val tinted = RgbEngine.darkTint(effect, level)
        return Color(RgbEngine.mix(base, tinted, rgb.intensityPercent / 100f))
    }

    /** Color de muestra del efecto en una posición (0..1), para la vista previa del RGB. */
    fun sample(offset: Float): Color =
        Color(RgbEngine.colorAt(rgb.effect, phase(), offset, colors.resolved(ColorRole.RINGS), rgb.intensityPercent, rgb.reverse))

    val title get() = c(ColorRole.TITLE)
    val text get() = c(ColorRole.TEXT_MAIN)
    val textSecondary get() = c(ColorRole.TEXT_SECONDARY)
    val description get() = c(ColorRole.DESCRIPTION)
    val controlText get() = c(ColorRole.CONTROL_TEXT)
    val indicatorText get() = c(ColorRole.INDICATOR_TEXT)
    val numbers get() = c(ColorRole.NUMBERS)
    val monitorValues get() = c(ColorRole.MONITOR_VALUES)
    val accentText get() = c(ColorRole.ACCENT_TEXT)

    /** Con RGB en los botones se elige blanco o negro según el color que tengan en ese instante. */
    val buttonText: Color
        get() {
            if (!RgbEngine.isActive(rgb, RgbTarget.BUTTONS)) return c(ColorRole.BUTTON_TEXT)
            val bgColor = activeButton
            val argb = 0xFF000000L or (((bgColor.red * 255f + 0.5f).toLong()) shl 16) or
                (((bgColor.green * 255f + 0.5f).toLong()) shl 8) or ((bgColor.blue * 255f + 0.5f).toLong())
            val white = 0xFFFFFFFFL
            val black = 0xFF000000L
            return if (ColorMath.contrast(white, argb) >= ColorMath.contrast(black, argb)) Color.White else Color.Black
        }

    val panelBg get() = bg(ColorRole.PANEL_BG, 0.14f)
    val settingsBg get() = bg(ColorRole.SETTINGS_BG, 0.14f)
    val dialogBg get() = bg(ColorRole.DIALOG_BG, 0.14f)
    val cardBg get() = bg(ColorRole.CARD_BG, 0.20f)
    val buttonBg get() = c(ColorRole.BUTTON_BG)
    val menuBg get() = c(ColorRole.MENU_BG)

    val border get() = fx(ColorRole.BORDER, RgbTarget.BORDERS, 0.0f)
    val icons get() = fx(ColorRole.ICONS, RgbTarget.ICONS, 0.6f)
    val bars get() = c(ColorRole.BARS)
    val rings get() = fx(ColorRole.RINGS, RgbTarget.RINGS, 0.3f)
    val indicators get() = fx(ColorRole.INDICATORS, RgbTarget.BUTTONS, 0.5f)
    val activeButton get() = fx(ColorRole.ACTIVE_BUTTON, RgbTarget.BUTTONS, 0.45f)
    val glow get() = fx(ColorRole.GLOW, RgbTarget.GLOW, 0.15f)
    val selection get() = fx(ColorRole.SELECTION, RgbTarget.BUTTONS, 0.55f)
}

val LocalOvPalette = compositionLocalOf { OvPalette(OverlayColors()) }

/** Esquema de Material 3 construido con la paleta estática (sin RGB), para que no se recomponga con cada fotograma. */
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
