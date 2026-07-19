# Noter is not shipped minified by default (see build.gradle.kts).
# Rules below apply if isMinifyEnabled is switched on for release builds.

-keep class com.noter.app.data.db.entity.** { *; }
