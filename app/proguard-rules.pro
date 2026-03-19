# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Jetpack Compose rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.Composer
-keepnames class androidx.compose.runtime.Composable
-keepclassmembers public class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers public class * implements androidx.compose.runtime.snapshots.SnapshotManager {
    <init>(...);
    public static final androidx.compose.runtime.snapshots.SnapshotManager$Companion Companion;
}
-keepclassmembers public class * implements androidx.compose.runtime.DerivedState {
    <init>(...);
    public static final androidx.compose.runtime.DerivedState$Companion Companion;
}
-keepclassmembers public class * implements androidx.compose.runtime.Recomposer {
    <init>(...);
    public static final androidx.compose.runtime.Recomposer$Companion Companion;
}
-keepclassmembers public class * implements androidx.compose.runtime.Composition {
    <init>(...);
    public static final androidx.compose.runtime.Composition$Companion Companion;
}
-keepclassmembers public class * implements androidx.compose.runtime.Composer$Companion {
    <init>(...);
    public static final androidx.compose.runtime.Composer$Companion INSTANCE;
}
-keepclassmembers public class * implements androidx.compose.runtime.tooling.InspectionTablesKt {
    <init>(...);
    public static final androidx.compose.runtime.tooling.InspectionTablesKt INSTANCE;
}
-keepclassmembers public class * implements androidx.compose.runtime.tooling.ComposableSingletonsKt {
    <init>(...);
    public static final androidx.compose.runtime.tooling.ComposableSingletonsKt INSTANCE;
}
-keepclassmembers public class * implements androidx.compose.runtime.tooling.PreviewActivity {
    <init>(...);
}
-keepclassmembers public class * implements androidx.compose.ui.tooling.preview.PreviewActivity {
    <init>(...);
}
-keepclassmembers public class * implements androidx.compose.ui.tooling.PreviewActivity {
    <init>(...);
}

# Keep data classes used with Firestore
# Adjust the package name if your models are elsewhere
-keep class com.FreeRave.shredzilla.navigation.ExerciseDisplayInfo { *; }
-keep class com.FreeRave.shredzilla.navigation.UserExerciseList { *; }
-keep class com.FreeRave.shredzilla.navigation.RecordedSet { *; }
-keep class com.FreeRave.shredzilla.models.ExerciseItem { *; }
-keep class com.FreeRave.shredzilla.onboarding.RestTimeOption { *; }
# Add any other data classes that are serialized/deserialized by Firestore

# General Firebase rules (often covered by consumer rules, but good to have if issues arise)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class org.json.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.xml.stream.**
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn com.google.android.gms.internal.**
-dontwarn com.google.android.gms.auth.**
-dontwarn com.google.common.**
-dontwarn com.google.api.client.**
-dontwarn org.conscrypt.**
# For Google Sign-In
-keep class com.google.android.gms.auth.api.signin.GoogleSignInOptions { *; }
-keep class com.google.android.gms.auth.api.signin.GoogleSignInAccount { *; }

# Google Mobile Ads SDK (AdMob) ProGuard rules
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
# Keep GMS Core
-keep class com.google.android.gms.common.GooglePlayServicesUtil {
    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE;
    public static java.lang.String getOpenSourceSoftwareLicenseInfo(android.content.Context);
    public static int isGooglePlayServicesAvailable(android.content.Context);
    public static boolean isUserRecoverableError(int);
    public static android.app.Dialog getErrorDialog(int, android.app.Activity, int);
}
# Keep AdMob activities
-keep public class com.google.android.gms.ads.AdActivity { *; }
-keep public class com.google.android.gms.ads.purchase.InAppPurchaseActivity { *; }
# Keep AdMob Content Provider
-keep public class com.google.android.gms.ads.identifier.AdvertisingIdProvider { *; }

# Keep specific drawable resources from being removed by resource shrinker
-keep class **.R$drawable {
    public static final int female_choose;
    public static final int male_choose;
    public static final int login_background;
    public static final int register_background;
    public static final int sec_page;
    public static final int third_page;
    public static final int forth_page;
    public static final int fifth_page;
    public static final int icon;
    public static final int iconApp;
    public static final int iconApp2;
    public static final int haahah_hahha;
    public static final int hahah_hahshs;
}
