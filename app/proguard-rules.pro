# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\klulc\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include line and the file path to fit your setup.

# Keep Kotlin reflect & coroutines metadata
-keepclassmembers class * extends kotlin.jvm.internal.Lambda {
    <fields>;
}

# Keep Compose models & preview
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Keep Room Database, DAOs, Entities, and Enums
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep class com.example.moneymanager.core.data.** { *; }

# Keep ViewModels and UI data models
-keep class com.example.moneymanager.ui.viewmodel.** { *; }
