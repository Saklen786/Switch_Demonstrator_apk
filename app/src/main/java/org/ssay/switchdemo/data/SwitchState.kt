package org.ssay.switchdemo.data

enum class HeadlightMode { DIPPER, UPPER }
enum class IndicatorMode { NONE, LEFT, RIGHT }
enum class BleConnectionState { DISCONNECTED, SCANNING, CONNECTED }

data class SwitchState(
    val headlight: HeadlightMode = HeadlightMode.DIPPER,
    val pass: Boolean = false,
    val indicator: IndicatorMode = IndicatorMode.NONE,
    val horn: Boolean = false,
    val warn: Boolean = false,
    val adcVolts: Float = 0f
)

fun mapStateIntToSwitchState(stateInt: Int, adcVolts: Float = 0f): SwitchState {
    return when (stateInt) {
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
}

// Demo mode: cycles through all switch states to simulate hardware
val DEMO_STATES: List<Pair<Int, Float>> = listOf(
    Pair(6,  3.28f),
    Pair(0,  3.28f),
    Pair(8,  3.27f),
    Pair(1,  3.27f),
    Pair(10, 3.26f),
    Pair(2,  3.26f),
    Pair(12, 3.25f),
    Pair(3,  3.25f),
    Pair(7,  3.28f),
    Pair(9,  3.27f),
    Pair(11, 3.26f),
    Pair(13, 3.25f),
    Pair(16, 3.20f),
    Pair(6,  3.28f)
)