# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/agnocode/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers and source file names for R8 de-obfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Data Models for JSON Serialization (Gson)
-keep class com.agnocode.minimalhomeapp.data.model.** { *; }

# Room Entities and DAOs
-keep class com.agnocode.minimalhomeapp.data.local.entities.** { *; }
-keep class com.agnocode.minimalhomeapp.data.local.dao.** { *; }

# Hilt and Dagger
-keep class **_HiltModules { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# GSON
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
