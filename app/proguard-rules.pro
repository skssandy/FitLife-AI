-keepattributes Signature
-keepattributes *Annotation*
-keep class com.fitlife.ai.** { *; }
-keepclassmembers class com.fitlife.ai.** { *; }
-keep class io.github.jan.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
