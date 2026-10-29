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
