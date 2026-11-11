package com.nexus.gamespace

import com.nexus.gamespace.domain.BoosterMath
import com.nexus.gamespace.domain.BoosterRestoreState
import com.nexus.gamespace.domain.Stopwatch
import com.nexus.gamespace.domain.formatElapsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoosterLogicTest {

    @Test fun brightnessNeverZero() {
        assertEquals(2, BoosterMath.percentToBrightness(1))
        assertTrue(BoosterMath.percentToBrightness(0) >= 1)
        assertTrue(BoosterMath.percentToBrightness(-50) >= 1)
    }

    @Test fun brightnessClampsToMax() {
        assertEquals(255, BoosterMath.percentToBrightness(100))
        assertEquals(255, BoosterMath.percentToBrightness(500))
    }

    @Test fun brightnessPercentRoundTrip() {
        assertEquals(100, BoosterMath.brightnessToPercent(255))
        assertEquals(0, BoosterMath.brightnessToPercent(0))
        assertEquals(50, BoosterMath.brightnessToPercent(128))
    }

    @Test fun stopwatchStartPauseElapsed() {
        val running = Stopwatch().start(1_000L)
        assertTrue(running.running)
        assertEquals(4_000L, running.elapsed(5_000L))
        val paused = running.pause(5_000L)
        assertFalse(paused.running)
        assertEquals(4_000L, paused.elapsed(99_000L))
    }

    @Test fun stopwatchResumeAccumulates() {
        val sw = Stopwatch().start(0L).pause(2_000L).start(10_000L)
        assertEquals(5_000L, sw.elapsed(13_000L))
    }

    @Test fun stopwatchStartTwiceDoesNotReset() {
        val sw = Stopwatch().start(1_000L)
        assertEquals(sw, sw.start(9_000L))
    }

    @Test fun stopwatchNeverNegative() {
        assertEquals(0L, Stopwatch().start(5_000L).elapsed(1_000L))
    }

    @Test fun stopwatchReset() = assertEquals(Stopwatch(), Stopwatch().start(0L).reset())

    @Test fun formatElapsed() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("01:05", formatElapsed(65_000))
        assertEquals("1:02:05", formatElapsed(3_725_000))
        assertEquals("00:00", formatElapsed(-5))
    }

    @Test fun restoreStateHasChanges() {
        assertFalse(BoosterRestoreState().hasChanges)
        assertTrue(BoosterRestoreState(dndPrevious = 1).hasChanges)
        assertTrue(BoosterRestoreState(brightnessPrevious = 100, brightnessModePrevious = 0).hasChanges)
        assertTrue(BoosterRestoreState(rotationPrevious = 1).hasChanges)
    }
}
