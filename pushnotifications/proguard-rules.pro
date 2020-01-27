## okhttp:
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

## Retrofit:
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

## Logging related:
-dontwarn org.slf4j.**

## Google:

-keep class com.google.android.gms.** { *; }

-keep class android.support.v4.app.NotificationCompat { *; }

-keep class android.support.v4.app.NotificationCompat$* { *; }

-keep class android.support.v4.app.NotificationManagerCompat { *; }

## Overzealous rule, but future proof
-keep class com.pusher.pushnotifications.** {
  *;
}

## Moshi reflect (https://github.com/square/moshi/issues/345#issuecomment-325413124)
-keep class kotlin.Metadata { *; }
