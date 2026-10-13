# Gson instantiates DTOs and populates fields reflectively,
# so R8 must not rename or strip them.
-keep class com.m4isper.remote.model.** { *; }

# Retrofit reads generic signatures of the service interface at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod