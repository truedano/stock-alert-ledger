# Strip verbose/debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Keep Kotlin Serialization metadata, serializers, and @Serializable models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.example.domain.model.**$$serializer { *; }
-keepclassmembers class com.example.domain.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.data.remote.**$$serializer { *; }
-keepclassmembers class com.example.data.remote.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.*Route* { *; }
-keep,includedescriptorclasses class com.example.*Route*$$serializer { *; }

# Keep AlertCondition hierarchy for Room JSON polymorphism
-keep class com.example.domain.model.AlertCondition { *; }
-keep class * implements com.example.domain.model.AlertCondition { *; }
