package com.nexus.gamespace

import com.nexus.gamespace.data.JsonCodec
import com.nexus.gamespace.domain.AnimationMode
import com.nexus.gamespace.domain.FavoritesLogic
import com.nexus.gamespace.domain.GameEntry
import com.nexus.gamespace.domain.InstalledApp
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.SizePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesAndPrefsTest {

    private fun app(pkg: String, label: String = pkg) = InstalledApp(pkg, label, suggestedGame = false)
    private fun order(list: List<GameEntry>) = FavoritesLogic.ordered(list).map { it.packageName }

    @Test fun addingKeepsSelectionOrder() {
        val list = FavoritesLogic.addFavorites(emptyList(), listOf(app("b"), app("a"), app("c")), now = 1L)
        assertEquals(listOf("b", "a", "c"), order(list))
        assertTrue(list.all { it.favorite })
    }

    @Test fun addingExistingLibraryGameOnlyMarksItFavorite() {
        val library = listOf(GameEntry("a", "A", favorite = false, profileId = "performance", addedAt = 5L))
        val list = FavoritesLogic.addFavorites(library, listOf(app("a")), now = 9L)
        assertEquals(1, list.size)
        assertTrue(list.single().favorite)
        assertEquals("performance", list.single().profileId)
        assertEquals(5L, list.single().addedAt)
    }

    @Test fun addingAlreadyFavoriteDoesNotDuplicateOrReorder() {
        val first = FavoritesLogic.addFavorites(emptyList(), listOf(app("a"), app("b")), now = 1L)
        val again = FavoritesLogic.addFavorites(first, listOf(app("a"), app("c")), now = 2L)
        assertEquals(listOf("a", "b", "c"), order(again))
        assertEquals(3, again.size)
    }

    @Test fun removeFavoriteKeepsGameInLibrary() {
        val list = FavoritesLogic.addFavorites(emptyList(), listOf(app("a"), app("b")), now = 1L)
        val result = FavoritesLogic.removeFavorite(list, "a")
        assertEquals(listOf("b"), order(result))
        assertEquals(2, result.size)
        assertFalse(result.first { it.packageName == "a" }.favorite)
    }

    @Test fun moveSwapsNeighbours() {
        val list = FavoritesLogic.addFavorites(emptyList(), listOf(app("a"), app("b"), app("c")), now = 1L)
        assertEquals(listOf("a", "c", "b"), order(FavoritesLogic.move(list, "c", -1)))
        assertEquals(listOf("b", "a", "c"), order(FavoritesLogic.move(list, "a", 1)))
    }

    @Test fun moveOutOfRangeChangesNothing() {
        val list = FavoritesLogic.addFavorites(emptyList(), listOf(app("a"), app("b")), now = 1L)
        assertEquals(list, FavoritesLogic.move(list, "a", -1))
        assertEquals(list, FavoritesLogic.move(list, "b", 1))
        assertEquals(list, FavoritesLogic.move(list, "zzz", 1))
    }

    @Test fun favoriteOrderSurvivesSaveAndLoad() {
        val list = FavoritesLogic.move(
            FavoritesLogic.addFavorites(emptyList(), listOf(app("a"), app("b"), app("c")), now = 1L), "c", -2,
        )
        val loaded = JsonCodec.decodeGames(JsonCodec.encodeGames(list))
        assertEquals(order(list), order(loaded))
        assertEquals(listOf("c", "a", "b"), order(loaded))
    }

    @Test fun oldSavedGamesWithoutOrderStillLoad() {
        val loaded = JsonCodec.decodeGames("""[{"pkg":"x","label":"X","fav":true}]""")
        assertEquals(0, loaded.single().favoriteOrder)
        assertTrue(loaded.single().favorite)
    }

    @Test fun presetsAreRecognised() {
        val base = OverlayPrefs()
        assertEquals(SizePreset.NORMAL, base.preset())
        assertEquals(SizePreset.COMPACT, base.withPreset(SizePreset.COMPACT).preset())
        assertEquals(SizePreset.LARGE, base.withPreset(SizePreset.LARGE).preset())
    }

    @Test fun manualChangeBecomesCustom() {
        val custom = OverlayPrefs().withPreset(SizePreset.COMPACT).copy(widthPercent = 75)
        assertEquals(SizePreset.CUSTOM, custom.preset())
    }

    @Test fun presetKeepsSideAnimationAndOpacity() {
        val prefs = OverlayPrefs(alphaPercent = 60, animation = AnimationMode.FAST)
        val compact = prefs.withPreset(SizePreset.COMPACT)
        assertEquals(60, compact.alphaPercent)
        assertEquals(AnimationMode.FAST, compact.animation)
        assertEquals(prefs.side, compact.side)
    }

    @Test fun animationDurationsAreOrdered() {
        assertEquals(0, AnimationMode.OFF.durationMs)
        assertTrue(AnimationMode.FAST.durationMs < AnimationMode.NORMAL.durationMs)
        assertTrue(AnimationMode.NORMAL.durationMs < AnimationMode.SLOW.durationMs)
        assertTrue(AnimationMode.REDUCED.durationMs < AnimationMode.FAST.durationMs)
    }
}
