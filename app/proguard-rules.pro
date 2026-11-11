# DotShredzilla R8 rules.
#
# Firebase, AdMob, Play Billing, Compose, WorkManager and Credential Manager ship their own
# consumer rules, so nothing here keeps whole libraries or all composables: that kept R8 from
# shrinking and optimizing most of the app. The app reads Firestore field by field (no toObject or
# other reflection), so its model classes don't need keeping either. Add a rule here only for
# something R8 can't see, and test a minified build (minifiedDebug) on a device after changing it.

# Readable crash stack traces in Play Console (upload mapping.txt with each release)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credential Manager loads its Google Play services provider by class name
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# Firebase finds its components by class name from the manifest; keep every registrar's
# constructor so none is skipped at startup (ComponentDiscovery "Could not instantiate")
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }

# Optional classes some Google libraries reference but never need on Android
-dontwarn org.apache.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.xml.stream.**
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn com.google.android.gms.internal.**
-dontwarn com.google.android.gms.auth.**
-dontwarn com.google.common.**
-dontwarn com.google.api.client.**
-dontwarn org.conscrypt.**
