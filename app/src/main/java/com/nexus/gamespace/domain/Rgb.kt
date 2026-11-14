package com.nexus.gamespace.domain

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** Partes del panel a las que se puede aplicar RGB. El texto y las alertas térmicas nunca llevan RGB. */
enum class RgbTarget(val label: String) {
    BORDERS("Bordes"),
    ICONS("Iconos"),
    BUTTONS("Botones"),
    RINGS("Anillos"),
    BACKGROUND("Fondo"),
    GLOW("Iluminación"),
}

enum class RgbEffect(val label: String) {
    RAINBOW("Rainbow"),
    CHROMA("Chroma"),
    SPECTRUM("Spectrum"),
    GRADIENT("Gradient"),
    BREATHING("Breathing"),
    COLOR_WAVE("Color Wave"),
}

enum class RgbSpeed(val label: String, val periodMs: Int) {
    SLOW("Lenta", 10_000),
    NORMAL("Normal", 5_000),
    FAST("Rápida", 2_500),
    CUSTOM("Personalizada", 4_000),
}

/**
 * NEXUS RGB MODE. enabled = false lo desactiva por completo (sin animación ni coste de batería).
 * global = true aplica el efecto a todos los componentes del panel; si no, solo a los de [targets].
 */
data class RgbSettings(
    val enabled: Boolean = false,
    val global: Boolean = true,
    val targets: Set<RgbTarget> = emptySet(),
    val effect: RgbEffect = RgbEffect.RAINBOW,
    val speed: RgbSpeed = RgbSpeed.NORMAL,
    val customPeriodMs: Int = 4_000,
    /** false = el efecto avanza hacia delante; true = en sentido inverso. */
    val reverse: Boolean = false,
    val intensityPercent: Int = 100,
    /** Con la batería baja y sin cargar, el RGB se queda quieto para no gastar batería. */
    val reduceOnLowBattery: Boolean = true,
) {
    companion object {
        val PERIOD_RANGE = 1_000..20_000
        val INTENSITY_RANGE = 10..100
        const val LOW_BATTERY_PERCENT = 20
    }
}

object RgbEngine {

    fun periodMs(s: RgbSettings): Int = when (s.speed) {
        RgbSpeed.CUSTOM -> s.customPeriodMs.coerceIn(RgbSettings.PERIOD_RANGE)
        else -> s.speed.periodMs
    }

    fun isActive(s: RgbSettings, target: RgbTarget): Boolean = s.enabled && (s.global || target in s.targets)

    /** El RGB se anima solo si está activado, las animaciones lo permiten y la batería no está baja. */
    fun shouldAnimate(s: RgbSettings, mode: AnimationMode, lowBattery: Boolean): Boolean =
        s.enabled &&
            (mode == AnimationMode.SLOW || mode == AnimationMode.NORMAL || mode == AnimationMode.FAST) &&
            !(s.reduceOnLowBattery && lowBattery)

    fun isLowBattery(percent: Int?, charging: Boolean?): Boolean =
        percent != null && percent <= RgbSettings.LOW_BATTERY_PERCENT && charging != true

    /**
     * Color de un elemento en un instante. phase va de 0 a 1 y se repite; offset (0..1) desplaza el efecto
     * según la posición del elemento, para que Chroma y Color Wave formen una onda.
     * La intensidad mezcla el color base con el del efecto (0 = solo base).
     */
    fun colorAt(effect: RgbEffect, phase: Float, offset: Float, base: Long, intensityPercent: Int, reverse: Boolean): Long {
        val raw = if (reverse) 1f - phase else phase
        val ph = raw - floor(raw)
        val k = intensityPercent.coerceIn(0, 100) / 100f
        val hsv = ColorMath.rgbToHsv(base)
        val twoPi = (2.0 * PI).toFloat()

        val fx: Long = when (effect) {
            RgbEffect.RAINBOW -> ColorMath.hsvToArgb(ph * 360f, 1f, 1f)
            RgbEffect.CHROMA -> ColorMath.hsvToArgb(((ph + offset) % 1f) * 360f, 0.9f, 1f)
            RgbEffect.SPECTRUM -> {
                val seg = ph * 6f
                val i = floor(seg).toInt()
                val f = seg - i
                val blend = ((f - 0.7f) / 0.3f).coerceIn(0f, 1f)
                ColorMath.hsvToArgb(i * 60f + 60f * blend, 1f, 1f)
            }
            RgbEffect.GRADIENT -> {
                val h = hsv[0] + 70f * sin(twoPi * (ph + offset * 0.25f))
                ColorMath.hsvToArgb(h, max(hsv[1], 0.6f), max(hsv[2], 0.8f))
            }
            RgbEffect.BREATHING -> {
                val level = 0.35f + 0.65f * (0.5f + 0.5f * sin(twoPi * ph - twoPi / 4f))
                ColorMath.hsvToArgb(hsv[0], hsv[1], hsv[2] * level)
            }
            RgbEffect.COLOR_WAVE -> {
                val h = hsv[0] + 120f * sin(twoPi * (ph + offset))
                ColorMath.hsvToArgb(h, max(hsv[1], 0.7f), max(hsv[2], 0.9f))
            }
        }
        return mix(base, fx, k)
    }

    /** Oscurece un color a un brillo máximo (para fondos con RGB sin perjudicar la lectura del texto). */
    fun darkTint(color: Long, maxValue: Float): Long {
        val hsv = ColorMath.rgbToHsv(color)
        return ColorMath.hsvToArgb(hsv[0], hsv[1], hsv[2].coerceAtMost(maxValue))
    }

    fun mix(a: Long, b: Long, t: Float): Long {
        val k = t.coerceIn(0f, 1f)
        fun ch(shift: Int): Long {
            val x = ((a shr shift) and 0xFF).toFloat()
            val y = ((b shr shift) and 0xFF).toFloat()
            return (x + (y - x) * k + 0.5f).toInt().coerceIn(0, 255).toLong()
        }
        return 0xFF000000L or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
