# Gson models use reflection and JSON field names, including persisted cart data.
# Keep only the serialized data packages; do not keep the entire app.
-keep class com.example.jetpackcompose.data.remote.dto.** { *; }
-keep class com.example.jetpackcompose.data.model.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
