package com.nexus.gamespace.domain

enum class CrosshairStyle(val label: String) {
    DOT("Punto"),
    CROSS("Cruz"),
    CIRCLE("Círculo"),
    CROSS_DOT("Cruz con punto"),
    CIRCLE_DOT("Círculo con punto"),
    CUSTOM("Personalizada"),
}

/** Qué piezas se dibujan. */
data class CrosshairParts(val dot: Boolean, val cross: Boolean, val circle: Boolean)

/**
 * Mira cosmética y estática: solo dibuja formas fijas en el centro de la pantalla. No lee la imagen del juego,
 * no detecta objetivos y no automatiza nada. Las medidas están en dp.
 */
data class CrosshairConfig(
    val style: CrosshairStyle = CrosshairStyle.CROSS_DOT,
    val colorArgb: Long = 0xFF00FF66L,
    val sizeDp: Int = 28,
    val thicknessDp: Int = 2,
    val gapDp: Int = 4,
    val alphaPercent: Int = 90,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    /** Solo se usan con el estilo CUSTOM. */
    val customDot: Boolean = true,
    val customCross: Boolean = true,
    val customCircle: Boolean = false,
) {
    fun parts(): CrosshairParts = when (style) {
        CrosshairStyle.DOT -> CrosshairParts(dot = true, cross = false, circle = false)
        CrosshairStyle.CROSS -> CrosshairParts(dot = false, cross = true, circle = false)
        CrosshairStyle.CIRCLE -> CrosshairParts(dot = false, cross = false, circle = true)
        CrosshairStyle.CROSS_DOT -> CrosshairParts(dot = true, cross = true, circle = false)
        CrosshairStyle.CIRCLE_DOT -> CrosshairParts(dot = true, cross = false, circle = true)
        CrosshairStyle.CUSTOM -> CrosshairParts(customDot, customCross, customCircle)
    }

    fun clamped(): CrosshairConfig = copy(
        sizeDp = sizeDp.coerceIn(SIZE_RANGE),
        thicknessDp = thicknessDp.coerceIn(THICKNESS_RANGE),
        gapDp = gapDp.coerceIn(GAP_RANGE),
        alphaPercent = alphaPercent.coerceIn(ALPHA_RANGE),
        offsetX = offsetX.coerceIn(OFFSET_RANGE),
        offsetY = offsetY.coerceIn(OFFSET_RANGE),
    )

    companion object {
        val SIZE_RANGE = 6..80
        val THICKNESS_RANGE = 1..8
        val GAP_RANGE = 0..30
        val ALPHA_RANGE = 20..100
        val OFFSET_RANGE = -400..400
        val PALETTE: List<Long> = listOf(
            0xFF00FF66L, 0xFFE5202EL, 0xFFFFFFFFL, 0xFFFFEB3BL,
            0xFF00E5FFL, 0xFFFF4081L, 0xFF4DA3FFL, 0xFFB57BFFL,
        )
    }
}

/** Configuración global, mira propia de cada juego y juegos donde la mira no debe aparecer. */
data class CrosshairSettings(
    val enabled: Boolean = false,
    val global: CrosshairConfig = CrosshairConfig(),
    val perGame: Map<String, CrosshairConfig> = emptyMap(),
    val excluded: Set<String> = emptySet(),
    /** Último juego abierto desde NEXUS GAME SPACE. Sin permiso de uso no se puede detectar el juego en primer plano. */
    val activeGame: String? = null,
) {
    fun configFor(game: String?): CrosshairConfig =
        if (game != null) perGame[game] ?: global else global

    fun withConfig(game: String?, config: CrosshairConfig): CrosshairSettings =
        if (game == null) copy(global = config.clamped()) else copy(perGame = perGame + (game to config.clamped()))

    fun withoutOverride(game: String): CrosshairSettings = copy(perGame = perGame - game)

    fun withExcluded(game: String, excluded: Boolean): CrosshairSettings =
        copy(excluded = if (excluded) this.excluded + game else this.excluded - game)

    fun shouldShow(game: String?): Boolean = enabled && (game == null || game !in excluded)
}
