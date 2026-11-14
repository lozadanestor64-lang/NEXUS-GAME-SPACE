package com.nexus.gamespace

import com.nexus.gamespace.data.JsonCodec
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.ColorMath
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.PanelStyle
import com.nexus.gamespace.domain.RgbEffect
import com.nexus.gamespace.domain.RgbEngine
import com.nexus.gamespace.domain.RgbSettings
import com.nexus.gamespace.domain.RgbSpeed
import com.nexus.gamespace.domain.RgbTarget
import com.nexus.gamespace.domain.SizePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RgbAndStyleTest {

    private val red = 0xFFE5202EL

    // ---- velocidad Chroma ----
    @Test fun speedPresetsAreOrdered() {
        val slow = RgbEngine.periodMs(RgbSettings(speed = RgbSpeed.SLOW))
        val normal = RgbEngine.periodMs(RgbSettings(speed = RgbSpeed.NORMAL))
        val fast = RgbEngine.periodMs(RgbSettings(speed = RgbSpeed.FAST))
        assertTrue(slow > normal && normal > fast)
    }

    @Test fun customSpeedUsesItsOwnPeriodAndIsClamped() {
        assertEquals(7_000, RgbEngine.periodMs(RgbSettings(speed = RgbSpeed.CUSTOM, customPeriodMs = 7_000)))
        assertEquals(RgbSettings.PERIOD_RANGE.last, RgbEngine.periodMs(RgbSettings(speed = RgbSpeed.CUSTOM, customPeriodMs = 999_999)))
        assertEquals(RgbSettings.PERIOD_RANGE.first, RgbEngine.periodMs(RgbSettings(speed = RgbSpeed.CUSTOM, customPeriodMs = 1)))
    }

    // ---- activación ----
    @Test fun disabledIsNeverActive() {
        RgbTarget.entries.forEach { assertFalse(RgbEngine.isActive(RgbSettings(enabled = false), it)) }
    }

    @Test fun globalAppliesToEveryTarget() {
        RgbTarget.entries.forEach { assertTrue(RgbEngine.isActive(RgbSettings(enabled = true, global = true), it)) }
    }

    @Test fun selectiveAppliesOnlyToChosenTargets() {
        val s = RgbSettings(enabled = true, global = false, targets = setOf(RgbTarget.BORDERS, RgbTarget.RINGS))
        assertTrue(RgbEngine.isActive(s, RgbTarget.BORDERS))
        assertTrue(RgbEngine.isActive(s, RgbTarget.RINGS))
        assertFalse(RgbEngine.isActive(s, RgbTarget.ICONS))
        assertFalse(RgbEngine.isActive(s, RgbTarget.BACKGROUND))
    }

    @Test fun selectiveWithoutTargetsAppliesNothing() {
        val s = RgbSettings(enabled = true, global = false, targets = emptySet())
        RgbTarget.entries.forEach { assertFalse(RgbEngine.isActive(s, it)) }
    }

    // ---- ahorro de batería ----
    @Test fun animatesOnlyWhenEnabledAndAnimationsAllow() {
        val on = RgbSettings(enabled = true)
        assertTrue(RgbEngine.shouldAnimate(on, AnimationMode.NORMAL, lowBattery = false))
        assertTrue(RgbEngine.shouldAnimate(on, AnimationMode.SLOW, lowBattery = false))
        assertTrue(RgbEngine.shouldAnimate(on, AnimationMode.FAST, lowBattery = false))
        assertFalse(RgbEngine.shouldAnimate(on, AnimationMode.OFF, lowBattery = false))
        assertFalse(RgbEngine.shouldAnimate(on, AnimationMode.REDUCED, lowBattery = false))
        assertFalse(RgbEngine.shouldAnimate(RgbSettings(enabled = false), AnimationMode.NORMAL, lowBattery = false))
    }

    @Test fun lowBatteryFreezesRgbUnlessUserOptedOut() {
        assertFalse(RgbEngine.shouldAnimate(RgbSettings(enabled = true), AnimationMode.NORMAL, lowBattery = true))
        assertTrue(
            RgbEngine.shouldAnimate(RgbSettings(enabled = true, reduceOnLowBattery = false), AnimationMode.NORMAL, lowBattery = true),
        )
    }

    @Test fun lowBatteryDetection() {
        assertTrue(RgbEngine.isLowBattery(15, charging = false))
        assertTrue(RgbEngine.isLowBattery(20, charging = null))
        assertFalse(RgbEngine.isLowBattery(15, charging = true))
        assertFalse(RgbEngine.isLowBattery(50, charging = false))
        assertFalse(RgbEngine.isLowBattery(null, charging = false))
    }

    // ---- efectos ----
    @Test fun zeroIntensityKeepsBaseColor() {
        RgbEffect.entries.forEach { e ->
            assertEquals(ColorMath.toHex(red), ColorMath.toHex(RgbEngine.colorAt(e, 0.37f, 0.2f, red, 0, false)))
        }
    }

    @Test fun resultsAreAlwaysOpaque() {
        RgbEffect.entries.forEach { e ->
            for (i in 0..10) {
                val c = RgbEngine.colorAt(e, i / 10f, 0.3f, red, 100, false)
                assertEquals(0xFF, ((c shr 24) and 0xFF).toInt())
            }
        }
    }

    @Test fun rainbowChangesHueOverTime() {
        val a = RgbEngine.colorAt(RgbEffect.RAINBOW, 0.0f, 0f, red, 100, false)
        val b = RgbEngine.colorAt(RgbEffect.RAINBOW, 0.33f, 0f, red, 100, false)
        assertNotEquals(a, b)
        assertEquals(0f, ColorMath.rgbToHsv(a)[0], 1f)
    }

    @Test fun rainbowRepeatsEveryPeriod() {
        assertEquals(
            ColorMath.toHex(RgbEngine.colorAt(RgbEffect.RAINBOW, 0.25f, 0f, red, 100, false)),
            ColorMath.toHex(RgbEngine.colorAt(RgbEffect.RAINBOW, 1.25f, 0f, red, 100, false)),
        )
    }

    @Test fun chromaDiffersByElementPosition() {
        val a = RgbEngine.colorAt(RgbEffect.CHROMA, 0.1f, 0.0f, red, 100, false)
        val b = RgbEngine.colorAt(RgbEffect.CHROMA, 0.1f, 0.5f, red, 100, false)
        assertNotEquals(a, b)
    }

    @Test fun rainbowIsTheSameForEveryPosition() {
        assertEquals(
            RgbEngine.colorAt(RgbEffect.RAINBOW, 0.4f, 0.0f, red, 100, false),
            RgbEngine.colorAt(RgbEffect.RAINBOW, 0.4f, 0.9f, red, 100, false),
        )
    }

    @Test fun breathingKeepsTheChosenHue() {
        val base = ColorMath.rgbToHsv(red)[0]
        for (i in 1..9) {
            val c = RgbEngine.colorAt(RgbEffect.BREATHING, i / 10f, 0f, red, 100, false)
            assertEquals(base, ColorMath.rgbToHsv(c)[0], 3f)
        }
    }

    @Test fun breathingChangesBrightness() {
        val dim = ColorMath.rgbToHsv(RgbEngine.colorAt(RgbEffect.BREATHING, 0.0f, 0f, red, 100, false))[2]
        val bright = ColorMath.rgbToHsv(RgbEngine.colorAt(RgbEffect.BREATHING, 0.5f, 0f, red, 100, false))[2]
        assertTrue(bright > dim)
    }

    @Test fun reverseRunsTheEffectBackwards() {
        val forward = RgbEngine.colorAt(RgbEffect.RAINBOW, 0.2f, 0f, red, 100, false)
        val backward = RgbEngine.colorAt(RgbEffect.RAINBOW, 0.8f, 0f, red, 100, true)
        assertEquals(ColorMath.toHex(forward), ColorMath.toHex(backward))
    }

    @Test fun spectrumUsesPrimaryHuesAtTheStartOfEachStep() {
        val hue = ColorMath.rgbToHsv(RgbEngine.colorAt(RgbEffect.SPECTRUM, 2f / 6f + 0.01f, 0f, red, 100, false))[0]
        assertEquals(120f, hue, 2f)
    }

    @Test fun halfIntensityIsBetweenBaseAndEffect() {
        val full = RgbEngine.colorAt(RgbEffect.RAINBOW, 0.33f, 0f, red, 100, false)
        val half = RgbEngine.colorAt(RgbEffect.RAINBOW, 0.33f, 0f, red, 50, false)
        val redChannel = { c: Long -> ((c shr 16) and 0xFF).toInt() }
        assertTrue(redChannel(half) in minOf(redChannel(red), redChannel(full))..maxOf(redChannel(red), redChannel(full)))
    }

    @Test fun darkTintNeverGetsBright() {
        RgbEffect.entries.forEach { e ->
            for (i in 0..10) {
                val tinted = RgbEngine.darkTint(RgbEngine.colorAt(e, i / 10f, 0.4f, red, 100, false), 0.2f)
                assertTrue(ColorMath.rgbToHsv(tinted)[2] <= 0.21f)
            }
        }
    }

    @Test fun tintedBackgroundKeepsWhiteTextReadable() {
        for (i in 0..10) {
            val tinted = RgbEngine.darkTint(RgbEngine.colorAt(RgbEffect.RAINBOW, i / 10f, 0f, red, 100, false), 0.2f)
            assertTrue(ColorMath.contrast(0xFFFFFFFFL, tinted) >= 7.0)
        }
    }

    // ---- persistencia RGB ----
    @Test fun rgbSurvivesSaveAndLoad() {
        val s = RgbSettings(
            enabled = true, global = false, targets = setOf(RgbTarget.ICONS, RgbTarget.GLOW),
            effect = RgbEffect.COLOR_WAVE, speed = RgbSpeed.CUSTOM, customPeriodMs = 6_000,
            reverse = true, intensityPercent = 60, reduceOnLowBattery = false,
        )
        assertEquals(s, JsonCodec.decodeRgb(JsonCodec.encodeRgb(s)))
    }

    @Test fun missingOrCorruptRgbGivesDefaults() {
        assertEquals(RgbSettings(), JsonCodec.decodeRgb(null))
        assertEquals(RgbSettings(), JsonCodec.decodeRgb("{no es json"))
        assertFalse(RgbSettings().enabled)
    }

    @Test fun unknownRgbValuesFallBackAndAreClamped() {
        val loaded = JsonCodec.decodeRgb("""{"enabled":true,"effect":"XYZ","speed":"WARP","customMs":99999999,"intensity":0,"targets":["ICONS","NOPE"]}""")
        assertEquals(RgbSettings().effect, loaded.effect)
        assertEquals(RgbSettings().speed, loaded.speed)
        assertEquals(RgbSettings.PERIOD_RANGE.last, loaded.customPeriodMs)
        assertEquals(RgbSettings.INTENSITY_RANGE.first, loaded.intensityPercent)
        assertEquals(setOf(RgbTarget.ICONS), loaded.targets)
    }

    // ---- estilos ----
    @Test fun thereAreExactlyTwoStyles() {
        assertEquals(setOf("REDMAGIC PANEL", "XIAOMI PANEL"), PanelStyle.entries.map { it.label }.toSet())
    }

    @Test fun defaultStyleIsRedMagic() = assertEquals(PanelStyle.REDMAGIC, OverlayPrefs().style)

    @Test fun switchingStyleKeepsEverythingElse() {
        val prefs = OverlayPrefs(alphaPercent = 70, animation = AnimationMode.FAST, scalePercent = 110, hiddenTools = setOf("notes"))
        val xiaomi = prefs.copy(style = PanelStyle.XIAOMI)
        assertEquals(prefs.alphaPercent, xiaomi.alphaPercent)
        assertEquals(prefs.animation, xiaomi.animation)
        assertEquals(prefs.scalePercent, xiaomi.scalePercent)
        assertEquals(prefs.hiddenTools, xiaomi.hiddenTools)
        assertEquals(PanelStyle.REDMAGIC, xiaomi.copy(style = PanelStyle.REDMAGIC).style)
    }

    @Test fun sizePresetsKeepTheChosenStyle() {
        val xiaomi = OverlayPrefs(style = PanelStyle.XIAOMI)
        assertEquals(PanelStyle.XIAOMI, xiaomi.withPreset(SizePreset.COMPACT).style)
        assertEquals(SizePreset.COMPACT, xiaomi.withPreset(SizePreset.COMPACT).preset())
    }

    @Test fun styleDescriptionsExistForBoth() {
        PanelStyle.entries.forEach { assertTrue(it.description.isNotBlank()) }
    }
}
