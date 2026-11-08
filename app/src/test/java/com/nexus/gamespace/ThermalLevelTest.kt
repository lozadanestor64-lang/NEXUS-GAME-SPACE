package com.nexus.gamespace

import com.nexus.gamespace.domain.ThermalLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ThermalLevelTest {
    @Test fun noneIsNormal() = assertEquals(ThermalLevel.NORMAL, ThermalLevel.fromAndroidStatus(0))
    @Test fun lightAndModerateAreAttention() {
        assertEquals(ThermalLevel.ATTENTION, ThermalLevel.fromAndroidStatus(1))
        assertEquals(ThermalLevel.ATTENTION, ThermalLevel.fromAndroidStatus(2))
    }
    @Test fun severeIsElevated() = assertEquals(ThermalLevel.ELEVATED, ThermalLevel.fromAndroidStatus(3))
    @Test fun criticalAndAboveAreCritical() {
        listOf(4, 5, 6).forEach { assertEquals(ThermalLevel.CRITICAL, ThermalLevel.fromAndroidStatus(it)) }
    }
}
