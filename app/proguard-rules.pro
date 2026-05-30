# Add project specific ProGuard rules here.

# Keep Room entities and DAOs
-keep class com.watchlater.model.** { *; }
-keep class com.watchlater.data.local.** { *; }

# Keep Gson model classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep backup models
-keep class com.watchlater.utils.BackupPayload { *; }
-keep class com.watchlater.utils.WatchItemBackup { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose
-dontwarn androidx.compose.**
