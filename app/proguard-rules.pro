# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all attributes needed for reflection
-keepattributes *Annotation*,Signature,Exception

# Google Play Services / AdMob
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.ads.** { *; }

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Keep app specific classes
-keep class com.techseedrive.quickstatussaver.** { *; }
-keepclassmembers class com.techseedrive.quickstatussaver.** { *; }

# WebView
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}