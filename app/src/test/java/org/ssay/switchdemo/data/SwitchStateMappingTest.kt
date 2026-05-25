package org.ssay.switchdemo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FIXED #43: locks down the 17-position switch decoder. A wrong mapping would
 * be invisible until a demo goes wrong; this guards every documented state.
 */
class SwitchStateMappingTest {

    @Test fun state6_isPlainDipper() {
        val s = mapStateIntToSwitchState(6, 3.28f)
        assertEquals(HeadlightMode.DIPPER, s.headlight)
        assertFalse(s.pass)
        assertEquals(IndicatorMode.NONE, s.indicator)
        assertFalse(s.horn)
        assertFalse(s.warn)
        assertEquals(3.28f, s.adcVolts, 0.001f)
    }

    @Test fun state0_isPlainUpper() {
        val s = mapStateIntToSwitchState(0)
        assertEquals(HeadlightMode.UPPER, s.headlight)
        assertEquals(IndicatorMode.NONE, s.indicator)
        assertFalse(s.horn)
    }

    @Test fun state11_isDipperPassLeft() {
        val s = mapStateIntToSwitchState(11)
        assertEquals(HeadlightMode.DIPPER, s.headlight)
        assertTrue(s.pass)
        assertEquals(IndicatorMode.LEFT, s.indicator)
        assertFalse(s.horn)
    }

    @Test fun state15_isDipperLeftHorn() {
        val s = mapStateIntToSwitchState(15)
        assertEquals(HeadlightMode.DIPPER, s.headlight)
        assertEquals(IndicatorMode.LEFT, s.indicator)
        assertTrue(s.horn)
        assertFalse(s.pass)
    }

    @Test fun state16_isWarn() {
        val s = mapStateIntToSwitchState(16)
        assertTrue(s.warn)
    }

    @Test fun outOfRangeIsWarn() {
        val s = mapStateIntToSwitchState(99)
        assertTrue(s.warn)
    }

    @Test fun every_state_0_to_15_isNotWarn() {
        for (i in 0..15) {
            assertFalse("state $i should not be a warning", mapStateIntToSwitchState(i).warn)
        }
    }

    @Test fun plainLanguageSummary_describesCommonCases() {
        assertEquals("Low beam",
            mapStateIntToSwitchState(6).plainLanguageSummary())
        assertEquals("High beam, Right indicator",
            mapStateIntToSwitchState(1).plainLanguageSummary())
        assertEquals("Low beam, Flash to pass, Left indicator",
            mapStateIntToSwitchState(11).plainLanguageSummary())
        assertEquals("Hardware warning",
            mapStateIntToSwitchState(16).plainLanguageSummary())
    }
}
