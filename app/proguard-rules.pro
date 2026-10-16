-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
# Keep the extractor's reflective helpers available in optimized builds.
-keep class org.schabi.newpipe.extractor.** { *; }
