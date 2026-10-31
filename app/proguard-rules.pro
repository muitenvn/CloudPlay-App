# ProGuard / R8 rules for CloudPlay

# 1. Preserve WebView JavaScript Interfaces (keep class names and methods)
-keepattributes JavascriptInterface
-keep @interface android.webkit.JavascriptInterface
-keepclasseswithmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.example.cloudplay.XboxBridge {
    *;
}

# 2. Preserve Enums stored in AppSettings / SharedPreferences / VPN state
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep enum com.example.** {
    *;
}

# 3. Preserve Moshi, Retrofit, org.json & Kotlin reflection/serialization data models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonClass <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }
-keep class com.example.data.** { *; }
-keep class com.example.vpn.** { *; }
-keep class com.example.cloudplay.** { *; }
-dontwarn com.squareup.moshi.**
-dontwarn okio.**
-dontwarn retrofit2.**

# 4. Preserve Room entities, DAOs and Database implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 5. Preserve Line numbers for stack traces and crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
