# Custom Proguard rules for R8 optimization and shrink control

# Keep Compose/Kotlin serialization classes intact
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.multiprocess.**

# Kotlin Serialization specifics
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    @org.jetbrains.kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    @org.jetbrains.kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.json.** { *; }

# Keep GMS Font Provider classes
-dontwarn androidx.compose.ui.text.googlefonts.**
-keep class androidx.compose.ui.text.googlefonts.** { *; }

# Keep SQLCipher classes
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Bypass Tink annotation missing class warnings
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn javax.annotation.concurrent.GuardedBy

