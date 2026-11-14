package com.nexus.gamespace.domain

enum class OverlaySide {
    LEFT, RIGHT;

    fun other(): OverlaySide = if (this == LEFT) RIGHT else LEFT
}

/** Herramientas del panel. El usuario puede ocultar las que no quiera. */
enum class OverlayTool(val id: String, val label: String) {
    MONITOR("monitor", "Monitor del dispositivo"),
    PROFILES("profiles", "Perfiles gaming"),
    FAVORITES("favorites", "Apps favoritas"),
    DND("dnd", "No molestar"),
    BRIGHTNESS("brightness", "Brillo"),
    VOLUME("volume", "Volumen"),
    ROTATION("rotation", "Rotación de pantalla"),
    WIFI("wifi", "Panel Wi-Fi"),
    CROSSHAIR("crosshair", "Mira"),
    TIMER("timer", "Temporizador"),
    NOTES("notes", "Notas rápidas"),
    LIBRARY("library", "Biblioteca de juegos"),
}

/** Estilo visual del panel. Los dos tienen exactamente las mismas funciones; cambian distribución, formas y animaciones. */
enum class PanelStyle(val label: String, val description: String) {
    REDMAGIC(
        "REDMAGIC PANEL",
        "Diseño amplio y simétrico: indicadores circulares grandes al centro, controles a ambos lados y bordes angulares.",
    ),
    XIAOMI(
        "XIAOMI PANEL",
        "Diseño compacto: menú lateral con secciones, accesos rápidos, tarjetas redondeadas y transiciones deslizantes.",
    ),
}

enum class AnimationMode(val label: String, val durationMs: Int) {
    OFF("Desactivadas", 0),
    REDUCED("Reducidas", 250),
    SLOW("Lentas", 1400),
    NORMAL("Normales", 800),
    FAST("Rápidas", 400),
}

enum class SizePreset(val label: String) {
    COMPACT("Compacto"),
    NORMAL("Normal"),
    LARGE("Grande"),
    CUSTOM("Personalizado"),
}

data class OverlayPrefs(
    val side: OverlaySide = OverlaySide.RIGHT,
    val alphaPercent: Int = 92,
    val scalePercent: Int = 100,
    /** Posición vertical de la pestaña en píxeles. -1 = posición por defecto. */
    val tabY: Int = -1,
    val hiddenTools: Set<String> = emptySet(),
    val widthPercent: Int = 100,
    val heightPercent: Int = 100,
    val textScalePercent: Int = 100,
    val iconScalePercent: Int = 100,
    val spacingDp: Int = 8,
    val marginDp: Int = 8,
    val animation: AnimationMode = AnimationMode.NORMAL,
    val style: PanelStyle = PanelStyle.REDMAGIC,
) {
    fun isVisible(tool: OverlayTool): Boolean = tool.id !in hiddenTools

    /** Preajuste que coincide con los valores actuales, o CUSTOM si el usuario los ajustó a mano. */
    fun preset(): SizePreset =
        listOf(SizePreset.COMPACT, SizePreset.NORMAL, SizePreset.LARGE).firstOrNull { withPreset(it).sameSize(this) }
            ?: SizePreset.CUSTOM

    fun withPreset(preset: SizePreset): OverlayPrefs = when (preset) {
        SizePreset.COMPACT -> copy(scalePercent = 85, widthPercent = 90, heightPercent = 100, textScalePercent = 90,
            iconScalePercent = 90, spacingDp = 6, marginDp = 6)
        SizePreset.NORMAL -> copy(scalePercent = 100, widthPercent = 100, heightPercent = 100, textScalePercent = 100,
            iconScalePercent = 100, spacingDp = 8, marginDp = 8)
        SizePreset.LARGE -> copy(scalePercent = 115, widthPercent = 100, heightPercent = 100, textScalePercent = 110,
            iconScalePercent = 110, spacingDp = 10, marginDp = 10)
        SizePreset.CUSTOM -> this
    }

    private fun sameSize(o: OverlayPrefs) = scalePercent == o.scalePercent && widthPercent == o.widthPercent &&
        heightPercent == o.heightPercent && textScalePercent == o.textScalePercent &&
        iconScalePercent == o.iconScalePercent && spacingDp == o.spacingDp && marginDp == o.marginDp

    companion object {
        const val ALPHA_MIN = 40
        const val ALPHA_MAX = 100
        val SCALES = listOf(85, 100, 115)
        val SCALE_RANGE = 70..130
        val WIDTH_RANGE = 60..100
        val HEIGHT_RANGE = 50..100
        val TEXT_RANGE = 80..130
        val ICON_RANGE = 80..130
        val SPACING_RANGE = 4..16
        val MARGIN_RANGE = 0..24
    }
}

enum class TabGesture { EXPAND, MOVE }

/** Decide qué hacer al soltar la pestaña flotante tras arrastrarla. */
object OverlayGestures {

    /**
     * Un deslizamiento corto hacia el interior abre el panel. Un arrastre largo (más allá de la mitad
     * de la pantalla) o en otra dirección solo mueve la pestaña.
     */
    fun onRelease(
        side: OverlaySide,
        totalDx: Float,
        centerX: Float,
        screenWidth: Int,
        expandThresholdPx: Float,
    ): TabGesture {
        val inward = if (side == OverlaySide.LEFT) totalDx else -totalDx
        val crossedMiddle = if (side == OverlaySide.LEFT) centerX > screenWidth / 2f else centerX < screenWidth / 2f
        return if (inward >= expandThresholdPx && !crossedMiddle) TabGesture.EXPAND else TabGesture.MOVE
    }

    fun sideFor(centerX: Float, screenWidth: Int): OverlaySide =
        if (centerX < screenWidth / 2f) OverlaySide.LEFT else OverlaySide.RIGHT
}
