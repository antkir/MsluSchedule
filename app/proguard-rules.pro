# Project specific ProGuard rules
-dontobfuscate

# Kotlin
-dontnote kotlin.reflect.jvm.internal.**
-dontnote kotlin.internal.PlatformImplementationsKt

# Apache Http
-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**

# Apache POI
-dontwarn org.apache.**
-dontnote org.apache.**
-keeppackagenames org.apache.poi.ss.formula.function
-keep public class org.apache.poi.ss.** { *; }
-keep public class org.apache.poi.hssf.** { *; }

# Dagger
-dontwarn com.google.errorprone.annotations.*

# Retrofit
-keepattributes Signature
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

#Okhttp
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.*
-dontnote okhttp3.internal.platform.*

#Moshi
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-dontnote com.squareup.moshi.ClassFactory

# Material Components
-keep class com.google.android.material.** { *; }

# Firebase
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**
-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }
