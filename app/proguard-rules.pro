# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Extra R8 shrink/optimize: flatten package structure and allow member
# access modification for more aggressive inlining/merging.
-repackageclasses ''
-allowaccessmodification

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep jcifs-ng SMB classes
-keep class jcifs.** { *; }
-dontwarn jcifs.**

# Keep BouncyCastle security provider (required by jcifs-ng for NTLM/MD4)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# SLF4J is used by jcifs-ng but we don't include an implementation
# Suppress warnings for missing SLF4J implementation
-dontwarn org.slf4j.**
-dontwarn javax.naming.**

# Keep data classes for serialization
-keepclassmembers class com.photoframe.core.model.** {
    <fields>;
}

# Keep Coil 3 image loading
-keep class coil3.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
