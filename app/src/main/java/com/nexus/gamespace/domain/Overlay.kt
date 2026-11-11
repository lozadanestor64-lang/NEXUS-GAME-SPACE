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
    TIMER("timer", "Temporizador"),
    NOTES("notes", "Notas rápidas"),
    LIBRARY("library", "Biblioteca de juegos"),
}

data class OverlayPrefs(
    val side: OverlaySide = OverlaySide.RIGHT,
    val alphaPercent: Int = 92,
    val scalePercent: Int = 100,
    /** Posición vertical de la pestaña en píxeles. -1 = posición por defecto. */
    val tabY: Int = -1,
    val hiddenTools: Set<String> = emptySet(),
) {
    fun isVisible(tool: OverlayTool): Boolean = tool.id !in hiddenTools

    companion object {
        const val ALPHA_MIN = 40
        const val ALPHA_MAX = 100
        val SCALES = listOf(85, 100, 115)
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
