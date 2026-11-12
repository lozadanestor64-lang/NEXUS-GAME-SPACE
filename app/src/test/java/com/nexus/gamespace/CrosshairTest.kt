package com.nexus.gamespace

import com.nexus.gamespace.data.JsonCodec
import com.nexus.gamespace.domain.CrosshairConfig
import com.nexus.gamespace.domain.CrosshairParts
import com.nexus.gamespace.domain.CrosshairSettings
import com.nexus.gamespace.domain.CrosshairStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrosshairTest {

    private fun parts(style: CrosshairStyle, custom: CrosshairConfig = CrosshairConfig()) =
        custom.copy(style = style).parts()

    @Test fun styleToParts() {
        assertEquals(CrosshairParts(dot = true, cross = false, circle = false), parts(CrosshairStyle.DOT))
        assertEquals(CrosshairParts(dot = false, cross = true, circle = false), parts(CrosshairStyle.CROSS))
        assertEquals(CrosshairParts(dot = false, cross = false, circle = true), parts(CrosshairStyle.CIRCLE))
        assertEquals(CrosshairParts(dot = true, cross = true, circle = false), parts(CrosshairStyle.CROSS_DOT))
        assertEquals(CrosshairParts(dot = true, cross = false, circle = true), parts(CrosshairStyle.CIRCLE_DOT))
    }

    @Test fun customUsesItsOwnToggles() {
        val c = CrosshairConfig(customDot = false, customCross = false, customCircle = true)
        assertEquals(CrosshairParts(dot = false, cross = false, circle = true), parts(CrosshairStyle.CUSTOM, c))
    }

    @Test fun clampKeepsValuesInRange() {
        val c = CrosshairConfig(sizeDp = 999, thicknessDp = 0, gapDp = -5, alphaPercent = 500, offsetX = 9999, offsetY = -9999).clamped()
        assertEquals(CrosshairConfig.SIZE_RANGE.last, c.sizeDp)
        assertEquals(CrosshairConfig.THICKNESS_RANGE.first, c.thicknessDp)
        assertEquals(CrosshairConfig.GAP_RANGE.first, c.gapDp)
        assertEquals(CrosshairConfig.ALPHA_RANGE.last, c.alphaPercent)
        assertEquals(CrosshairConfig.OFFSET_RANGE.last, c.offsetX)
        assertEquals(CrosshairConfig.OFFSET_RANGE.first, c.offsetY)
    }

    @Test fun perGameConfigOverridesGlobalOnlyForThatGame() {
        val mine = CrosshairConfig(style = CrosshairStyle.DOT, sizeDp = 12)
        val s = CrosshairSettings().withConfig("game.a", mine)
        assertEquals(CrosshairStyle.DOT, s.configFor("game.a").style)
        assertEquals(s.global, s.configFor("game.b"))
        assertEquals(s.global, s.configFor(null))
    }

    @Test fun editingGlobalDoesNotTouchGameOverrides() {
        val s = CrosshairSettings().withConfig("game.a", CrosshairConfig(sizeDp = 12))
            .withConfig(null, CrosshairConfig(sizeDp = 50))
        assertEquals(12, s.configFor("game.a").sizeDp)
        assertEquals(50, s.configFor("game.b").sizeDp)
    }

    @Test fun clearingOverrideFallsBackToGlobal() {
        val s = CrosshairSettings().withConfig("game.a", CrosshairConfig(sizeDp = 12)).withoutOverride("game.a")
        assertEquals(s.global, s.configFor("game.a"))
    }

    @Test fun excludedGamesNeverShowTheCrosshair() {
        val s = CrosshairSettings(enabled = true).withExcluded("game.a", true)
        assertFalse(s.shouldShow("game.a"))
        assertTrue(s.shouldShow("game.b"))
        assertTrue(s.shouldShow(null))
        assertTrue(s.withExcluded("game.a", false).shouldShow("game.a"))
    }

    @Test fun disabledNeverShows() = assertFalse(CrosshairSettings(enabled = false).shouldShow(null))

    @Test fun jsonRoundTrip() {
        val s = CrosshairSettings(enabled = true, activeGame = "game.a")
            .withConfig(null, CrosshairConfig(style = CrosshairStyle.CIRCLE_DOT, colorArgb = 0xFFFF4081L, sizeDp = 40, offsetX = -12, offsetY = 7))
            .withConfig("game.a", CrosshairConfig(style = CrosshairStyle.CUSTOM, customDot = false, customCircle = true))
            .withExcluded("game.z", true)
        assertEquals(s, JsonCodec.decodeCrosshair(JsonCodec.encodeCrosshair(s)))
    }

    @Test fun nullActiveGameSurvivesRoundTrip() {
        val s = CrosshairSettings(enabled = true, activeGame = null)
        assertEquals(null, JsonCodec.decodeCrosshair(JsonCodec.encodeCrosshair(s)).activeGame)
    }

    @Test fun missingOrCorruptJsonGivesDefaults() {
        assertEquals(CrosshairSettings(), JsonCodec.decodeCrosshair(null))
        assertEquals(CrosshairSettings(), JsonCodec.decodeCrosshair(""))
        assertEquals(CrosshairSettings(), JsonCodec.decodeCrosshair("{no es json"))
    }

    @Test fun savedOutOfRangeValuesAreClampedOnLoad() {
        val raw = """{"enabled":true,"global":{"style":"DOT","size":5000,"thickness":99,"alpha":1}}"""
        val loaded = JsonCodec.decodeCrosshair(raw).global
        assertEquals(CrosshairConfig.SIZE_RANGE.last, loaded.sizeDp)
        assertEquals(CrosshairConfig.THICKNESS_RANGE.last, loaded.thicknessDp)
        assertEquals(CrosshairConfig.ALPHA_RANGE.first, loaded.alphaPercent)
    }

    @Test fun unknownStyleFallsBackToDefault() {
        val loaded = JsonCodec.decodeCrosshair("""{"global":{"style":"XYZ"}}""").global
        assertEquals(CrosshairConfig().style, loaded.style)
    }
}
