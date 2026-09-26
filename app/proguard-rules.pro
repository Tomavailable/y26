# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# --- Kotlin Coroutines Proguard Rules ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Room Database Proguard Rules ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.limits.Limit
# Keep our database entities and models because they are mapped dynamically
-keep class com.example.data.model.** { *; }
-keep class com.example.data.dao.** { *; }

# --- Serialized Models & Entities ---
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# --- Jetpack Compose Rules ---
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.compose.foundation.lazy.layout.DefaultLazyKey {}

