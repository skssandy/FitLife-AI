# FitLife AI ProGuard Rules

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.fitlife.ai.** {
    *** Companion;
}
-keepclasseswithmembers class com.fitlife.ai.**$$serializer {
    *** INSTANCE;
}

# Supabase
-keep class io.github.jan.supabase.** { *; }

# Ktor
-keep class io.ktor.** { *; }

# Generative AI
-keep class com.google.ai.client.generativeai.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# DataStore
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite
