-keep class com.timely.msminutes.receiver.** { *; }
-keep class com.timely.msminutes.service.** { *; }
-keep class com.timely.msminutes.data.** { *; }

# ThemeUtil accesses these private NumberPicker fields via reflection.
# Keep them so R8 does not rename or remove them in release builds.
-keepclassmembers class android.widget.NumberPicker {
    private android.graphics.drawable.Drawable mSelectionDivider;
    private android.graphics.Paint mSelectorWheelPaint;
}
