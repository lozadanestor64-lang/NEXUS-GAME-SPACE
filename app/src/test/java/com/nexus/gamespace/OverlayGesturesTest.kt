package com.nexus.gamespace

import com.nexus.gamespace.domain.OverlayGestures
import com.nexus.gamespace.domain.OverlayPrefs
import com.nexus.gamespace.domain.OverlaySide
import com.nexus.gamespace.domain.OverlayTool
import com.nexus.gamespace.domain.TabGesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayGesturesTest {
    private val width = 1080

    @Test fun shortInwardSwipeOnLeftExpands() =
        assertEquals(TabGesture.EXPAND, OverlayGestures.onRelease(OverlaySide.LEFT, 120f, 140f, width, 100f))

    @Test fun shortInwardSwipeOnRightExpands() =
        assertEquals(TabGesture.EXPAND, OverlayGestures.onRelease(OverlaySide.RIGHT, -120f, 940f, width, 100f))

    @Test fun tooShortSwipeOnlyMoves() =
        assertEquals(TabGesture.MOVE, OverlayGestures.onRelease(OverlaySide.LEFT, 30f, 50f, width, 100f))

    @Test fun outwardDragDoesNotExpand() =
        assertEquals(TabGesture.MOVE, OverlayGestures.onRelease(OverlaySide.LEFT, -150f, 10f, width, 100f))

    @Test fun verticalDragDoesNotExpand() =
        assertEquals(TabGesture.MOVE, OverlayGestures.onRelease(OverlaySide.RIGHT, 5f, 1070f, width, 100f))

    @Test fun longDragPastMiddleMovesAcrossInsteadOfExpanding() =
        assertEquals(TabGesture.MOVE, OverlayGestures.onRelease(OverlaySide.LEFT, 800f, 810f, width, 100f))

    @Test fun snapsToNearestSide() {
        assertEquals(OverlaySide.LEFT, OverlayGestures.sideFor(200f, width))
        assertEquals(OverlaySide.RIGHT, OverlayGestures.sideFor(900f, width))
    }

    @Test fun otherSide() {
        assertEquals(OverlaySide.RIGHT, OverlaySide.LEFT.other())
        assertEquals(OverlaySide.LEFT, OverlaySide.RIGHT.other())
    }

    @Test fun toolVisibilityFollowsHiddenSet() {
        val prefs = OverlayPrefs(hiddenTools = setOf(OverlayTool.NOTES.id))
        assertFalse(prefs.isVisible(OverlayTool.NOTES))
        assertTrue(prefs.isVisible(OverlayTool.DND))
    }

    @Test fun toolIdsAreUnique() =
        assertEquals(OverlayTool.entries.size, OverlayTool.entries.map { it.id }.toSet().size)
}
