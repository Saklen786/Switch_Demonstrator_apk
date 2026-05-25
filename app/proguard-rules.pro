# FIXED #59: ProGuard / R8 keep rules for release builds.
# These are required because the BLE GATT callbacks are called via reflection-style
# inheritance from the Android framework, and the JSON parser uses field-name reflection.

# --- Keep all Android Bluetooth GATT callbacks ---
-keepclassmembers class * extends android.bluetooth.BluetoothGattCallback {
    public *;
}
-keepclassmembers class * extends android.bluetooth.le.ScanCallback {
    public *;
}

# --- Keep app data classes used by JSONObject reflection / kotlinx.serialization ---
-keep class org.ssay.switchdemo.data.** { *; }
-keep class org.ssay.switchdemo.viewmodel.** { *; }

# --- Kotlinx Serialization (declared in build but currently unused) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Compose: keep Composable function annotations ---
-keep class androidx.compose.runtime.Composable
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- AndroidX DataStore proto serializer (kept defensively) ---
-keep class androidx.datastore.*.** { *; }

# --- General Kotlin / coroutines ---
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlinx.coroutines.flow.**
