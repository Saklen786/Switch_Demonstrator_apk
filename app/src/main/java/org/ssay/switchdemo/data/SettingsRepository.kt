package org.ssay.switchdemo.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * FIXED #39: replaces the old direct SharedPreferences calls. DataStore is fully
 * asynchronous, type-safe and keeps the main thread off disk on cold start.
 */
private val Context.dataStore by preferencesDataStore("switch_demo_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEVICE_NAME           = stringPreferencesKey("ble_device_name")
        val BLINK_RATE_MS         = intPreferencesKey("blink_rate_ms")
        val IS_DARK_THEME         = booleanPreferencesKey("is_dark_theme")
        val SHOW_RAW_HEX          = booleanPreferencesKey("show_raw_hex")
        val SHOW_INDICATOR_LABELS = booleanPreferencesKey("show_indicator_labels")
        val USE_PLAIN_LABELS      = booleanPreferencesKey("use_plain_labels")
        val ONBOARDING_DONE       = booleanPreferencesKey("onboarding_done")
    }

    val deviceName: Flow<String>           = context.dataStore.data.map { it[Keys.DEVICE_NAME] ?: "RE_Switch_Dash" }
    val blinkRateMs: Flow<Int>             = context.dataStore.data.map { it[Keys.BLINK_RATE_MS] ?: 500 }
    val isDarkTheme: Flow<Boolean>         = context.dataStore.data.map { it[Keys.IS_DARK_THEME] ?: true }
    val showRawHex: Flow<Boolean>          = context.dataStore.data.map { it[Keys.SHOW_RAW_HEX] ?: false }
    val showIndicatorLabels: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_INDICATOR_LABELS] ?: true }
    val usePlainLabels: Flow<Boolean>      = context.dataStore.data.map { it[Keys.USE_PLAIN_LABELS] ?: true }
    val onboardingDone: Flow<Boolean>      = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setDeviceName(value: String)            { context.dataStore.edit { it[Keys.DEVICE_NAME] = value } }
    suspend fun setBlinkRateMs(value: Int)              { context.dataStore.edit { it[Keys.BLINK_RATE_MS] = value } }
    suspend fun setDarkTheme(value: Boolean)            { context.dataStore.edit { it[Keys.IS_DARK_THEME] = value } }
    suspend fun setShowRawHex(value: Boolean)           { context.dataStore.edit { it[Keys.SHOW_RAW_HEX] = value } }
    suspend fun setShowIndicatorLabels(value: Boolean)  { context.dataStore.edit { it[Keys.SHOW_INDICATOR_LABELS] = value } }
    suspend fun setUsePlainLabels(value: Boolean)       { context.dataStore.edit { it[Keys.USE_PLAIN_LABELS] = value } }
    suspend fun setOnboardingDone(value: Boolean)       { context.dataStore.edit { it[Keys.ONBOARDING_DONE] = value } }
}
