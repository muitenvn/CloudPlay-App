# ProGuard / R8 rules for CloudPlay

# Preserve WebView JavaScript Interfaces
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve Moshi models and adapters
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonClass <fields>;
}
-dontwarn com.squareup.moshi.**

# Preserve Room entities and DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}

# Preserve Line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
