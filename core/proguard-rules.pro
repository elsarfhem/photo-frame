# Add project specific ProGuard rules here.
# ProGuard rules for core module

# Keep all public APIs
-keep public class com.photoframe.core.** { public *; }

# Keep data classes
-keepclassmembers class com.photoframe.core.model.** {
    <fields>;
    <init>(...);
}

# Keep jcifs-ng SMB library
-keep class jcifs.** { *; }
-dontwarn jcifs.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
