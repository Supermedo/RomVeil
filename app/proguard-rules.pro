# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.glyph.launcher.**$$serializer { *; }
-keepclassmembers class com.glyph.launcher.** {
    *** Companion;
}
-keepclasseswithmembers class com.glyph.launcher.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Data Models (Critical for JSON Parsing & Room)
-keep class com.glyph.launcher.data.remote.dto.** { *; }
-keep class com.glyph.launcher.domain.model.** { *; }
-keep class com.glyph.launcher.data.local.entity.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
