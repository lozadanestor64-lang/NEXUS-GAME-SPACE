package com.nexus.gamespace

import com.nexus.gamespace.domain.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceSnapshotTest {
    private fun snap(total: Long, avail: Long) = DeviceSnapshot(
        "Motorola", "moto g15", "15", 35, null, total, avail, false, 100L, 25L,
        null, null, null, null, null, null, null, null,
    )

    @Test fun ramUsedFraction() = assertEquals(0.75f, snap(100, 25).ramUsedFraction, 0.001f)
    @Test fun storageUsedFraction() = assertEquals(0.75f, snap(100, 25).storageUsedFraction, 0.001f)
    @Test fun zeroTotalDoesNotDivideByZero() = assertEquals(0f, snap(0, 0).ramUsedFraction, 0.001f)
}
