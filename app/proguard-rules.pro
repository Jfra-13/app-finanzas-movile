# R8 rules for the release build (isMinifyEnabled = true).
#
# Only what this app actually needs: Retrofit, OkHttp, Room, Hilt and Vico all
# ship their own consumer rules inside their artifacts, so nothing is repeated
# here. What those do NOT cover is Gson, which maps JSON onto our own classes
# by reflection.

# Readable stack traces from release crashes: keep line numbers while hiding
# the original file name behind "SourceFile".
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit reads generic return types (ApiResponse<T>) and method annotations
# at runtime; both are stripped by default.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# --- Gson ---
# Gson derives JSON keys from field names, so every class it (de)serializes
# must keep them. Methods are still shrunk: only the fields are pinned.
-keep class com.example.finanzas_independientes_app.data.remote.dto.** { <fields>; }
-keep class com.example.finanzas_independientes_app.core.network.ApiResponse { <fields>; }
-keep class com.example.finanzas_independientes_app.core.network.ErrorDetail { <fields>; }
-keep class com.example.finanzas_independientes_app.core.network.ErrorEnvelope { <fields>; }
