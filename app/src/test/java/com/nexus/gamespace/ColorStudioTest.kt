package com.nexus.gamespace

import com.nexus.gamespace.data.JsonCodec
import com.nexus.gamespace.domain.ColorMath
import com.nexus.gamespace.domain.ColorPresets
import com.nexus.gamespace.domain.ColorRole
import com.nexus.gamespace.domain.OverlayColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorStudioTest {

    private val white = 0xFFFFFFFFL
    private val black = 0xFF000000L

    // ---- contraste ----
    @Test fun blackOnWhiteIsMaxContrast() = assertEquals(21.0, ColorMath.contrast(black, white), 0.01)
    @Test fun sameColorIsMinContrast() = assertEquals(1.0, ColorMath.contrast(0xFF123456L, 0xFF123456L), 0.001)
    @Test fun contrastIsSymmetric() =
        assertEquals(ColorMath.contrast(0xFFE5202EL, white), ColorMath.contrast(white, 0xFFE5202EL), 0.0001)

    // ---- HEX ----
    @Test fun parsesSixDigitHexWithOrWithoutHash() {
        assertEquals(0xFFE5202EL, ColorMath.parseHex("#E5202E"))
        assertEquals(0xFFE5202EL, ColorMath.parseHex("e5202e"))
        assertEquals(0xFFE5202EL, ColorMath.parseHex("  #e5202E "))
    }

    @Test fun parsesEightDigitHexWithAlpha() = assertEquals(0x80E5202EL, ColorMath.parseHex("#80E5202E"))

    @Test fun rejectsInvalidHex() {
        assertNull(ColorMath.parseHex(""))
        assertNull(ColorMath.parseHex("#12345"))
        assertNull(ColorMath.parseHex("#GGGGGG"))
        assertNull(ColorMath.parseHex("#1234567"))
    }

    @Test fun toHexRoundTrips() = assertEquals(0xFF00FF66L, ColorMath.parseHex(ColorMath.toHex(0xFF00FF66L)))

    // ---- HSV ----
    @Test fun hsvRoundTripKeepsColor() {
        listOf(0xFFE5202EL, 0xFF00E676L, 0xFF4DA3FFL, 0xFFFFFFFFL, 0xFF000000L, 0xFF808080L).forEach { c ->
            val hsv = ColorMath.rgbToHsv(c)
            val back = ColorMath.hsvToArgb(hsv[0], hsv[1], hsv[2])
            assertEquals(ColorMath.toHex(c), ColorMath.toHex(back))
        }
    }

    @Test fun pureRedHasHueZero() = assertEquals(0f, ColorMath.rgbToHsv(0xFFFF0000L)[0], 0.01f)
    @Test fun pureGreenHasHue120() = assertEquals(120f, ColorMath.rgbToHsv(0xFF00FF00L)[0], 0.01f)

    // ---- colores del panel ----
    @Test fun defaultsAreReadable() {
        assertTrue("Los colores originales no deben dar avisos", OverlayColors().warnings().isEmpty())
    }

    @Test fun everyRoleHasADefault() {
        val colors = OverlayColors()
        ColorRole.entries.forEach { assertEquals(it.default, colors.raw(it)) }
    }

    @Test fun darkTextOnDarkCardIsWarned() {
        val colors = OverlayColors().with(ColorRole.TEXT_MAIN, 0xFF111111L)
        assertTrue(colors.warnings().any { it.text == ColorRole.TEXT_MAIN })
    }

    @Test fun autoContrastNeverLeavesInvisibleText() {
        // El caso del error original: texto negro sobre fondo negro.
        val colors = OverlayColors(autoContrast = true)
            .with(ColorRole.TEXT_MAIN, black).with(ColorRole.CARD_BG, black).with(ColorRole.PANEL_BG, black)
        val shown = colors.resolved(ColorRole.TEXT_MAIN)
        assertTrue(ColorMath.contrast(shown, black) >= OverlayColors.AUTO_THRESHOLD)
        assertEquals(white, shown)
    }

    @Test fun autoContrastCanBeTurnedOff() {
        val colors = OverlayColors(autoContrast = false).with(ColorRole.TEXT_MAIN, black).with(ColorRole.CARD_BG, black)
        assertEquals(black, colors.resolved(ColorRole.TEXT_MAIN))
    }

    @Test fun autoContrastKeepsReadableColors() {
        val colors = OverlayColors().with(ColorRole.TEXT_MAIN, 0xFFFF9100L)
        assertEquals(0xFFFF9100L, colors.resolved(ColorRole.TEXT_MAIN))
    }

    @Test fun autoContrastPicksBlackOnLightBackground() {
        val colors = OverlayColors()
            .with(ColorRole.CARD_BG, white).with(ColorRole.PANEL_BG, white).with(ColorRole.TEXT_MAIN, white)
        assertEquals(black, colors.resolved(ColorRole.TEXT_MAIN))
    }

    @Test fun buttonTextIsCheckedAgainstActiveButton() {
        val colors = OverlayColors().with(ColorRole.ACTIVE_BUTTON, white).with(ColorRole.BUTTON_TEXT, white)
        assertTrue(colors.warnings().any { it.text == ColorRole.BUTTON_TEXT })
        assertEquals(black, colors.resolved(ColorRole.BUTTON_TEXT))
    }

    @Test fun nonTextRolesAreNeverAltered() {
        val colors = OverlayColors().with(ColorRole.BORDER, black).with(ColorRole.CARD_BG, black)
        assertEquals(black, colors.resolved(ColorRole.BORDER))
    }

    @Test fun resetRestoresOriginalValue() {
        val colors = OverlayColors().with(ColorRole.RINGS, 0xFF00E676L)
        assertTrue(colors.isCustom(ColorRole.RINGS))
        val back = colors.reset(ColorRole.RINGS)
        assertEquals(ColorRole.RINGS.default, back.raw(ColorRole.RINGS))
        assertFalse(back.isCustom(ColorRole.RINGS))
    }

    @Test fun accentChangesOnlyAccentRoles() {
        val colors = OverlayColors().withAccent(0xFF4DA3FFL)
        OverlayColors.ACCENT_ROLES.forEach { assertEquals(0xFF4DA3FFL, colors.raw(it)) }
        assertEquals(ColorRole.CARD_BG.default, colors.raw(ColorRole.CARD_BG))
        assertEquals(ColorRole.TEXT_MAIN.default, colors.raw(ColorRole.TEXT_MAIN))
    }

    @Test fun colorsAreAlwaysOpaque() {
        assertEquals(0xFF, (OverlayColors().with(ColorRole.RINGS, 0x00112233L).raw(ColorRole.RINGS) shr 24).toInt() and 0xFF)
    }

    @Test fun presetsAreDistinctAndNamed() {
        assertEquals(ColorPresets.all.size, ColorPresets.all.map { it.name }.toSet().size)
        assertEquals(ColorPresets.all.size, ColorPresets.all.map { it.colors.values }.toSet().size)
    }

    @Test fun everyPresetKeepsGoodContrast() {
        ColorPresets.all.forEach { preset ->
            preset.colors.warnings().forEach {
                // Ningún texto debe quedar por debajo del umbral de corrección automática.
                assertTrue("${preset.name}: ${it.text} ${it.ratio}", it.ratio >= OverlayColors.AUTO_THRESHOLD)
            }
        }
    }

    // ---- persistencia ----
    @Test fun colorsSurviveSaveAndLoad() {
        val colors = OverlayColors(autoContrast = false)
            .with(ColorRole.TEXT_MAIN, 0xFFFF9100L).with(ColorRole.CARD_BG, 0xFF101820L)
        assertEquals(colors, JsonCodec.decodeColors(JsonCodec.encodeColors(colors)))
    }

    @Test fun missingOrCorruptColorsGiveDefaults() {
        assertEquals(OverlayColors(), JsonCodec.decodeColors(null))
        assertEquals(OverlayColors(), JsonCodec.decodeColors(""))
        assertEquals(OverlayColors(), JsonCodec.decodeColors("{no es json"))
    }

    @Test fun invalidSavedValuesAndUnknownRolesAreIgnored() {
        val raw = """{"auto":true,"colors":{"TEXT_MAIN":"not-a-color","INVENTED_ROLE":"#FFFFFF","RINGS":"#00E676"}}"""
        val loaded = JsonCodec.decodeColors(raw)
        assertEquals(ColorRole.TEXT_MAIN.default, loaded.raw(ColorRole.TEXT_MAIN))
        assertEquals(0xFF00E676L, loaded.raw(ColorRole.RINGS))
        assertNotEquals(0, loaded.values.size)
    }
}
