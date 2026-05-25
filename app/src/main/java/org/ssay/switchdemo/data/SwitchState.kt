package org.ssay.switchdemo.data

enum class HeadlightMode { DIPPER, UPPER }
enum class IndicatorMode { NONE, LEFT, RIGHT }

/**
 * FIXED #8: added RECONNECTING so the UI can distinguish a plain disconnect
 * from an in-flight automatic retry.
 */
enum class BleConnectionState { DISCONNECTED, SCANNING, CONNECTED, RECONNECTING }

data class SwitchState(
    val headlight: HeadlightMode = HeadlightMode.DIPPER,
    val pass: Boolean = false,
    val indicator: IndicatorMode = IndicatorMode.NONE,
    val horn: Boolean = false,
    val warn: Boolean = false,
    val adcVolts: Float = 0f
) {
    /**
     * FIXED #55: human-readable summary used for accessibility / "plain language" UI.
     * Engineers still see the raw enums; non-technical viewers get prose.
     */
    fun plainLanguageSummary(): String {
        if (warn) return "Hardware warning"
        val parts = mutableListOf<String>()
        parts += if (headlight == HeadlightMode.UPPER) "High beam" else "Low beam"
        if (pass) parts += "Flash to pass"
        when (indicator) {
            IndicatorMode.LEFT  -> parts += "Left indicator"
            IndicatorMode.RIGHT -> parts += "Right indicator"
            IndicatorMode.NONE  -> Unit
        }
        if (horn) parts += "Horn"
        return parts.joinToString(", ")
    }

    /** Engineering shorthand (kept for the technical audience). */
    fun engineeringSummary(): String {
        if (warn) return "WARN"
        val parts = mutableListOf<String>()
        parts += if (headlight == HeadlightMode.UPPER) "UPPER" else "DIPPER"
        if (pass) parts += "PASS"
        when (indicator) {
            IndicatorMode.LEFT  -> parts += "LEFT"
            IndicatorMode.RIGHT -> parts += "RIGHT"
            IndicatorMode.NONE  -> Unit
        }
        if (horn) parts += "HORN"
        return parts.joinToString(" + ")
    }
}

fun mapStateIntToSwitchState(stateInt: Int, adcVolts: Float = 0f): SwitchState = when (stateInt) {
    0  -> SwitchState(headlight = HeadlightMode.UPPER, adcVolts = adcVolts)
    1  -> SwitchState(headlight = HeadlightMode.UPPER, indicator = IndicatorMode.RIGHT, adcVolts = adcVolts)
    2  -> SwitchState(headlight = HeadlightMode.UPPER, indicator = IndicatorMode.LEFT, adcVolts = adcVolts)
    3  -> SwitchState(headlight = HeadlightMode.UPPER, horn = true, adcVolts = adcVolts)
    4  -> SwitchState(headlight = HeadlightMode.UPPER, indicator = IndicatorMode.RIGHT, horn = true, adcVolts = adcVolts)
    5  -> SwitchState(headlight = HeadlightMode.UPPER, indicator = IndicatorMode.LEFT, horn = true, adcVolts = adcVolts)
    6  -> SwitchState(headlight = HeadlightMode.DIPPER, adcVolts = adcVolts)
    7  -> SwitchState(headlight = HeadlightMode.DIPPER, pass = true, adcVolts = adcVolts)
    8  -> SwitchState(headlight = HeadlightMode.DIPPER, indicator = IndicatorMode.RIGHT, adcVolts = adcVolts)
    9  -> SwitchState(headlight = HeadlightMode.DIPPER, pass = true, indicator = IndicatorMode.RIGHT, adcVolts = adcVolts)
    10 -> SwitchState(headlight = HeadlightMode.DIPPER, indicator = IndicatorMode.LEFT, adcVolts = adcVolts)
    11 -> SwitchState(headlight = HeadlightMode.DIPPER, pass = true, indicator = IndicatorMode.LEFT, adcVolts = adcVolts)
    12 -> SwitchState(headlight = HeadlightMode.DIPPER, horn = true, adcVolts = adcVolts)
    13 -> SwitchState(headlight = HeadlightMode.DIPPER, pass = true, horn = true, adcVolts = adcVolts)
    14 -> SwitchState(headlight = HeadlightMode.DIPPER, indicator = IndicatorMode.RIGHT, horn = true, adcVolts = adcVolts)
    15 -> SwitchState(headlight = HeadlightMode.DIPPER, indicator = IndicatorMode.LEFT, horn = true, adcVolts = adcVolts)
    16 -> SwitchState(warn = true, adcVolts = adcVolts)
    else -> SwitchState(warn = true, adcVolts = adcVolts)
}

/** Demo mode: cycles through all switch states to simulate hardware. */
val DEMO_STATES: List<Pair<Int, Float>> = listOf(
    6 to 3.28f, 0 to 3.28f, 8  to 3.27f, 1  to 3.27f,
    10 to 3.26f, 2 to 3.26f, 12 to 3.25f, 3  to 3.25f,
    7 to 3.28f, 9 to 3.27f, 11 to 3.26f, 13 to 3.25f,
    16 to 3.20f, 6 to 3.28f
)
