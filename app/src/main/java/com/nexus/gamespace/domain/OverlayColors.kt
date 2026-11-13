package com.nexus.gamespace.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Cálculos de color sin dependencias de Android, para poder probarlos. Los colores son ARGB en un Long. */
object ColorMath {

    private fun channel(argb: Long, shift: Int): Int = ((argb shr shift) and 0xFF).toInt()

    private fun linear(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** Luminancia relativa WCAG (0 = negro, 1 = blanco). Se ignora el canal alfa. */
    fun luminance(argb: Long): Double =
        0.2126 * linear(channel(argb, 16)) + 0.7152 * linear(channel(argb, 8)) + 0.0722 * linear(channel(argb, 0))

    /** Relación de contraste WCAG entre dos colores (de 1 a 21). */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Acepta «#RRGGBB», «RRGGBB» y «#AARRGGBB». Devuelve null si no es un color válido. */
    fun parseHex(input: String): Long? {
        val hex = input.trim().removePrefix("#")
        if (hex.length != 6 && hex.length != 8) return null
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        val value = hex.toLong(16)
        return if (hex.length == 6) 0xFF000000L or value else value
    }

    fun toHex(argb: Long): String = "#%06X".format(argb and 0xFFFFFFL)

    fun withAlpha(argb: Long, alpha: Int): Long = (argb and 0xFFFFFFL) or (alpha.coerceIn(0, 255).toLong() shl 24)

    /** Devuelve [matiz 0..360, saturación 0..1, brillo 0..1]. */
    fun rgbToHsv(argb: Long): FloatArray {
        val r = channel(argb, 16) / 255f
        val g = channel(argb, 8) / 255f
        val b = channel(argb, 0) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val h = when {
            delta == 0f -> 0f
            maxC == r -> 60f * (((g - b) / delta) % 6f)
            maxC == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return floatArrayOf(if (h < 0f) h + 360f else h, if (maxC == 0f) 0f else delta / maxC, maxC)
    }

    fun hsvToArgb(h: Float, s: Float, v: Float): Long {
        val hh = ((h % 360f) + 360f) % 360f
        val ss = s.coerceIn(0f, 1f)
        val vv = v.coerceIn(0f, 1f)
        val c = vv * ss
        val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
        val m = vv - c
        val (r, g, b) = when {
            hh < 60f -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun to255(f: Float) = ((f + m) * 255f + 0.5f).toInt().coerceIn(0, 255).toLong()
        return 0xFF000000L or (to255(r) shl 16) or (to255(g) shl 8) or to255(b)
    }
}

enum class ColorGroup(val label: String) {
    TEXT("Texto"),
    BACKGROUND("Fondos"),
    OTHER("Otros elementos"),
}

/** Cada color personalizable del panel flotante, con su valor original. */
enum class ColorRole(val group: ColorGroup, val label: String, val default: Long) {
    TITLE(ColorGroup.TEXT, "Títulos", 0xFFFFFFFFL),
    TEXT_MAIN(ColorGroup.TEXT, "Texto principal", 0xFFE9E9EEL),
    TEXT_SECONDARY(ColorGroup.TEXT, "Texto secundario", 0xFFB4B5C0L),
    DESCRIPTION(ColorGroup.TEXT, "Descripciones", 0xFF9FA0ABL),
    BUTTON_TEXT(ColorGroup.TEXT, "Texto de botones principales", 0xFFFFFFFFL),
    CONTROL_TEXT(ColorGroup.TEXT, "Texto de controles", 0xFFF2F2F4L),
    INDICATOR_TEXT(ColorGroup.TEXT, "Texto de indicadores", 0xFFFFFFFFL),
    NUMBERS(ColorGroup.TEXT, "Números", 0xFFFFFFFFL),
    MONITOR_VALUES(ColorGroup.TEXT, "Valores de monitorización", 0xFFF2F2F4L),
    ACCENT_TEXT(ColorGroup.TEXT, "Texto de acento", 0xFFFF4D5AL),

    PANEL_BG(ColorGroup.BACKGROUND, "Fondo completo del panel", 0xFF07070AL),
    SETTINGS_BG(ColorGroup.BACKGROUND, "Fondo de configuración", 0xFF0B0B0FL),
    CARD_BG(ColorGroup.BACKGROUND, "Fondo de tarjetas", 0xFF121217L),
    BUTTON_BG(ColorGroup.BACKGROUND, "Fondo de botones", 0xFF1A1A20L),
    MENU_BG(ColorGroup.BACKGROUND, "Fondo de menús", 0xFF16161CL),
    DIALOG_BG(ColorGroup.BACKGROUND, "Fondo de ventanas emergentes", 0xFF0D0D12L),

    BORDER(ColorGroup.OTHER, "Bordes", 0xFF2A2A31L),
    ICONS(ColorGroup.OTHER, "Iconos", 0xFFFFFFFFL),
    BARS(ColorGroup.OTHER, "Barras y pistas", 0xFF2A2A31L),
    RINGS(ColorGroup.OTHER, "Anillos", 0xFFE5202EL),
    INDICATORS(ColorGroup.OTHER, "Indicadores y interruptores", 0xFFE5202EL),
    ACTIVE_BUTTON(ColorGroup.OTHER, "Botones activos", 0xFFE5202EL),
    GLOW(ColorGroup.OTHER, "Iluminación", 0xFFE5202EL),
    SELECTION(ColorGroup.OTHER, "Efectos de selección", 0xFFE5202EL),
}

data class ContrastWarning(val text: ColorRole, val background: ColorRole, val ratio: Double)

/**
 * Colores del panel. Solo se guardan los que el usuario cambió. Con el contraste automático activado, un
 * texto que sería casi ilegible sobre su fondo se sustituye por blanco o negro para que nunca desaparezca.
 */
data class OverlayColors(
    val values: Map<ColorRole, Long> = emptyMap(),
    val autoContrast: Boolean = true,
) {
    fun raw(role: ColorRole): Long = values[role] ?: role.default

    fun with(role: ColorRole, argb: Long): OverlayColors = copy(values = values + (role to (argb or 0xFF000000L)))

    fun reset(role: ColorRole): OverlayColors = copy(values = values - role)

    fun isCustom(role: ColorRole): Boolean = values[role]?.let { it != role.default } ?: false

    fun withAccent(argb: Long): OverlayColors = ACCENT_ROLES.fold(this) { acc, role -> acc.with(role, argb) }

    /** Color final de un rol. Los de texto pasan por el contraste automático. */
    fun resolved(role: ColorRole): Long {
        val backgrounds = TEXT_BACKGROUNDS[role] ?: return raw(role)
        if (!autoContrast) return raw(role)
        val color = raw(role)
        if (worstContrast(color, backgrounds) >= AUTO_THRESHOLD) return color
        val white = 0xFFFFFFFFL
        val black = 0xFF000000L
        return if (worstContrast(white, backgrounds) >= worstContrast(black, backgrounds)) white else black
    }

    private fun worstContrast(color: Long, backgrounds: List<ColorRole>): Double =
        backgrounds.minOf { ColorMath.contrast(color, raw(it)) }

    /** Textos cuya legibilidad es mala con los colores elegidos (avisos, aunque el contraste automático los corrija). */
    fun warnings(): List<ContrastWarning> = TEXT_BACKGROUNDS.mapNotNull { (text, backgrounds) ->
        val worst = backgrounds.minByOrNull { ColorMath.contrast(raw(text), raw(it)) } ?: return@mapNotNull null
        val ratio = ColorMath.contrast(raw(text), raw(worst))
        if (ratio < WARN_THRESHOLD) ContrastWarning(text, worst, ratio) else null
    }

    companion object {
        const val WARN_THRESHOLD = 4.5
        const val AUTO_THRESHOLD = 3.0

        val ACCENT_ROLES = listOf(
            ColorRole.ACCENT_TEXT, ColorRole.RINGS, ColorRole.INDICATORS,
            ColorRole.ACTIVE_BUTTON, ColorRole.GLOW, ColorRole.SELECTION,
        )

        /** Sobre qué fondos se dibuja cada texto. */
        val TEXT_BACKGROUNDS: Map<ColorRole, List<ColorRole>> = mapOf(
            ColorRole.TITLE to listOf(ColorRole.CARD_BG, ColorRole.PANEL_BG),
            ColorRole.TEXT_MAIN to listOf(ColorRole.CARD_BG, ColorRole.PANEL_BG),
            ColorRole.TEXT_SECONDARY to listOf(ColorRole.CARD_BG, ColorRole.PANEL_BG),
            ColorRole.DESCRIPTION to listOf(ColorRole.CARD_BG, ColorRole.PANEL_BG),
            ColorRole.BUTTON_TEXT to listOf(ColorRole.ACTIVE_BUTTON),
            ColorRole.CONTROL_TEXT to listOf(ColorRole.BUTTON_BG, ColorRole.MENU_BG, ColorRole.CARD_BG),
            ColorRole.INDICATOR_TEXT to listOf(ColorRole.CARD_BG),
            ColorRole.NUMBERS to listOf(ColorRole.CARD_BG),
            ColorRole.MONITOR_VALUES to listOf(ColorRole.CARD_BG),
            ColorRole.ACCENT_TEXT to listOf(ColorRole.CARD_BG, ColorRole.PANEL_BG),
        )
    }
}

/** Paletas predeterminadas. Todas conservan los mismos controles; solo cambian los colores. */
data class ColorPreset(val name: String, val colors: OverlayColors)

object ColorPresets {
    val all: List<ColorPreset> = listOf(
        ColorPreset("NEXUS rojo", OverlayColors()),
        // Con acentos claros el texto de los botones principales va en negro para mantener el contraste.
        ColorPreset("Azul hielo", OverlayColors().withAccent(0xFF4DA3FFL).with(ColorRole.BUTTON_TEXT, 0xFF000000L)),
        ColorPreset("Verde matrix", OverlayColors().withAccent(0xFF00E676L).with(ColorRole.BUTTON_TEXT, 0xFF000000L)),
        ColorPreset("Morado neón", OverlayColors().withAccent(0xFFB57BFFL).with(ColorRole.BUTTON_TEXT, 0xFF000000L)),
        ColorPreset("Naranja fuego", OverlayColors().withAccent(0xFFFF9100L).with(ColorRole.BUTTON_TEXT, 0xFF000000L)),
        ColorPreset(
            "Alto contraste",
            OverlayColors()
                .withAccent(0xFFFFEB3BL)
                .with(ColorRole.BUTTON_TEXT, 0xFF000000L)
                .with(ColorRole.PANEL_BG, 0xFF000000L)
                .with(ColorRole.SETTINGS_BG, 0xFF000000L)
                .with(ColorRole.DIALOG_BG, 0xFF000000L)
                .with(ColorRole.CARD_BG, 0xFF000000L)
                .with(ColorRole.MENU_BG, 0xFF000000L)
                .with(ColorRole.BUTTON_BG, 0xFF000000L)
                .with(ColorRole.BORDER, 0xFFFFFFFFL)
                .with(ColorRole.DESCRIPTION, 0xFFFFFFFFL)
                .with(ColorRole.TEXT_SECONDARY, 0xFFFFFFFFL),
        ),
    )
}
