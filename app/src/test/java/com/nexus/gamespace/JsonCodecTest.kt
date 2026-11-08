package com.nexus.gamespace

import com.nexus.gamespace.data.JsonCodec
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonCodecTest {

    @Test fun gamesRoundTrip() {
        val games = listOf(
            GameEntry("com.a", "A", favorite = true, profileId = "performance", addedAt = 10L, lastLaunchedAt = 99L),
            GameEntry("com.b", "B", addedAt = 20L),
        )
        assertEquals(games, JsonCodec.decodeGames(JsonCodec.encodeGames(games)))
    }

    @Test fun nullProfileAndLaunchSurviveRoundTrip() {
        val decoded = JsonCodec.decodeGames(JsonCodec.encodeGames(listOf(GameEntry("com.b", "B"))))
        assertNull(decoded.single().profileId)
        assertNull(decoded.single().lastLaunchedAt)
    }

    @Test fun emptyOrCorruptGamesGiveEmptyList() {
        assertTrue(JsonCodec.decodeGames(null).isEmpty())
        assertTrue(JsonCodec.decodeGames("").isEmpty())
        assertTrue(JsonCodec.decodeGames("{no es json").isEmpty())
    }

    @Test fun noDataGivesFourDefaultProfiles() {
        val profiles = JsonCodec.decodeProfiles(null, now = 5L)
        assertEquals(ProfileType.entries.map { it.id }, profiles.map { it.id })
    }

    @Test fun corruptProfilesFallBackToDefaults() {
        assertEquals(4, JsonCodec.decodeProfiles("[[[", now = 5L).size)
    }

    @Test fun customProfileEditsPersist() {
        val base = JsonCodec.decodeProfiles(null, now = 1L)
        val custom = base.first { it.type == ProfileType.CUSTOM }
        val edited = custom.copy(
            name = "Mi perfil", colorArgb = 0xFFB57BFFL, updatedAt = 42L,
            settings = custom.settings.copy(doNotDisturb = true, monitorIntervalSec = 5),
        )
        val saved = JsonCodec.encodeProfiles(base.map { if (it.id == edited.id) edited else it })
        val loaded = JsonCodec.decodeProfiles(saved, now = 2L).first { it.type == ProfileType.CUSTOM }
        assertEquals("Mi perfil", loaded.name)
        assertEquals(0xFFB57BFFL, loaded.colorArgb)
        assertEquals(42L, loaded.updatedAt)
        assertTrue(loaded.settings.doNotDisturb)
        assertEquals(5, loaded.settings.monitorIntervalSec)
    }

    @Test fun presetNameCannotBeChangedByStoredData() {
        val base = JsonCodec.decodeProfiles(null, now = 1L)
        val tampered = base.map { if (it.type == ProfileType.PERFORMANCE) it.copy(name = "Otro") else it }
        val loaded = JsonCodec.decodeProfiles(JsonCodec.encodeProfiles(tampered), now = 2L)
        assertEquals("Performance", loaded.first { it.type == ProfileType.PERFORMANCE }.name)
    }

    @Test fun presetSettingChangesPersist() {
        val base = JsonCodec.decodeProfiles(null, now = 1L)
        val perf = base.first { it.type == ProfileType.PERFORMANCE }
        val changed = perf.copy(settings = perf.settings.copy(thermalAlerts = false))
        val loaded = JsonCodec.decodeProfiles(
            JsonCodec.encodeProfiles(base.map { if (it.id == perf.id) changed else it }), now = 2L,
        ).first { it.id == perf.id }
        assertEquals(false, loaded.settings.thermalAlerts)
    }
}
