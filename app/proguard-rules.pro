# Room proguard rules
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Domain & Data models
-keep class com.samadhi.meditationtimer.data.backup.model.** { *; }
-keep class com.samadhi.meditationtimer.data.local.entity.** { *; }
-keep class com.samadhi.meditationtimer.domain.model.** { *; }
