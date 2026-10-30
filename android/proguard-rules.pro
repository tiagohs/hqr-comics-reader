# kotlinx.serialization: keep generated serializers (navigation routes, backup, settings).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep,includedescriptorclasses class com.tiagohs.hqr.**$$serializer { *; }
-keepclassmembers class com.tiagohs.hqr.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
# Room 3 generated database implementation.
-keep class * extends androidx.room3.RoomDatabase { <init>(); }
-keep class com.tiagohs.hqr.data.db.HqrDatabase_Impl { *; }
# Ktor / OkHttp
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
# CBR and CB7 (ADR-011): the optional codecs and JDK-only classes Commons Compress and junrar reference but HQR never uses.
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.objectweb.asm.**
-dontwarn org.osgi.**
-dontwarn org.slf4j.**
