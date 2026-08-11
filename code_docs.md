# Code Documentation

## Core Application & Services
- **TimelyApp.kt**: The main Application class that initializes notifications, themes, and strict mode.
- **service/AlarmNotificationHelper.kt**: Helper object that constructs various notification types for alarms, including full-screen intents and missed alarm alerts.
- **service/AlarmRingService.kt**: Manages the alarm ringing state, including playing sound, vibrating, handling auto-dismiss, and responding to power button actions.
- **service/AlarmDreamService.kt**: A Daydream service that allows the alarm ringing interface to be displayed when the device is docked or charging.
- **service/StopwatchService.kt**: A foreground service that manages the stopwatch state, updates the persistent notification with elapsed time, and handles lap actions.
- **service/StopwatchTileService.kt**: Quick Settings tile service providing easy access to start/stop the stopwatch from the system notification shade.
- **service/TimerService.kt**: Handles the lifecycle of a finished timer, including playing the alert sound and displaying a notification with a stop action.

## Data Layer
- **data/Alarm.kt**: Data model representing an alarm with properties for time, repetition, sound, and other settings.
- **data/AlarmRepository.kt**: Handles database operations for Alarm objects, including CRUD and asynchronous fetching.
- **data/DbHelper.kt**: SQLite database helper that manages the creation and schema migration for alarms and timers tables.
- **data/LapStore.kt**: A utility object to encode and decode stopwatch lap times into a single string for storage.
- **data/Prefs.kt**: Manages shared preferences for the application including theme settings, time format, and stopwatch state.
- **data/TimerItem.kt**: Data model for a timer, including its duration, remaining time, state, and alert settings.
- **data/TimerRepository.kt**: Manages database persistence for timer items, supporting CRUD operations and asynchronous retrieval.

## Receivers
- **receiver/AlarmReceiver.kt**: Listens for alarm broadcast intents, acquires a wake lock, and starts the alarm ringing service.
- **receiver/BootReceiver.kt**: Reschedules all active alarms and running timers when the device finishes booting.
- **receiver/NotificationActionReceiver.kt**: Intercepts user actions from notifications and forwards them to the corresponding service for handling.
- **receiver/TimerReceiver.kt**: Receives broadcast intents when a timer finishes and starts the timer service to handle the alert.
- **receiver/WidgetTickReceiver.kt**: Listens for system time changes and ticks to trigger updates for the home screen clock widget.

## UI Components
- **ui/MainActivity.kt**: The primary activity that hosts the main application tabs for alarms, timers, stopwatch, and world clock. Uses a canvas-based layout for its core UI elements.
- **ui/MainActivityPermissionHelper.kt**: Helper for managing runtime permissions required by the main activity, such as notifications.
- **ui/alarm/AlarmFragment.kt**: Fragment displaying the list of alarms using `CanvasListView` and providing options to add or modify them.
- **ui/alarm/AlarmEditActivity.kt**: Activity for creating and editing alarm details like time, repetition, sound, and labels, built entirely with canvas renderers.
- **ui/alarm/AlarmRingActivity.kt**: The activity that appears when an alarm is ringing, hosting the `AlarmRingRenderer`.
- **ui/alarm/SoundPickerHelper.kt**: Facilitates selecting a sound for alarms or timers from system ringtones or custom files.
- **ui/timer/TimerFragment.kt**: Fragment that hosts the timer list and the timer creation entry point.
- **ui/timer/TimerActionHandler.kt**: Handles user actions on timers, such as starting, pausing, and resetting.
- **ui/timer/TimerCreateDialog.kt**: A custom dialog for creating new timers with specific durations and sounds, utilizing canvas components for its interface.
- **ui/stopwatch/StopwatchFragment.kt**: Fragment that provides the stopwatch user interface, handling timing, lap recording, and interaction with the stopwatch service.
- **ui/worldclock/WorldClockFragment.kt**: Fragment displaying multiple world clocks with real-time updates using canvas rendering.
- **ui/worldclock/TimeZoneSearchActivity.kt**: Activity allowing users to search and add new time zones to the world clock list.
- **ui/settings/SettingsActivity.kt**: Activity for application settings, including time format and theme customization.
- **ui/view/SoundItem.kt**: Data class representing a selectable sound item in a list.
- **ui/view/CustomColorPickerDialog.kt**: An advanced color picker allowing for fine-grained selection of custom colors.
- **ui/view/SoundPickerDialog.kt**: Dialog for choosing alert sounds for alarms and timers.
- **view/GooglyEyesView.kt**: A custom view that draws animated googly eyes for the clock widget.
- **view/SwipeDismissView.kt**: A utility view that supports dismissal via swipe gestures.

## Canvas Rendering System
- **ui/canvas/CanvasRenderer.kt**: The core interface for components that render directly to a `Canvas`, defining methods for drawing, touch handling, and accessibility.
- **ui/canvas/CanvasHostView.kt**: A specialized view that manages and draws multiple `CanvasRenderer` instances, providing a unified touch and accessibility routing layer.
- **ui/canvas/CanvasListView.kt**: A high-performance, canvas-based list component that supports scrolling, swiping, and custom item rendering without the overhead of standard RecyclerViews.
- **ui/canvas/CanvasListTouchHandler.kt** & **CanvasListSwipeHandler.kt**: Handle scrolling and swipe-to-action logic specifically for the canvas list.
- **ui/canvas/CanvasDialog.kt**: A custom dialog base that uses `CanvasHostView` to render its content.
- **ui/canvas/CanvasIcons.kt** & variants: Provide lightweight, vector-like icon drawing logic directly on the canvas.
- **ui/canvas/items/**: Contains specific item renderers for the list (e.g., `AlarmItemRenderer`, `TimerItemRenderer`, `LapItemRenderer`, `ToggleItemRenderer`).
- **ui/canvas/renderers/**: Specialized renderers for specific UI parts like `ToolbarRenderer`, `TabBarRenderer`, `FabRenderer`, `UndoBarRenderer`, and `GooglyEyesRenderer`.
- **ui/canvas/AlarmRingRenderer.kt**: Implements the immersive alarm ringing interface with animated eyes and swipe gestures.
- **ui/canvas/AnalogClockRenderer.kt**: Renders a traditional analog clock face on the canvas.
- **ui/canvas/WheelPickerRenderer.kt**: A custom wheel-style picker for selecting time or numbers.

## Utilities
- **util/AlarmScheduler.kt**: Logic for calculating the next alarm time and scheduling it with the system AlarmManager.
- **util/AlarmTimeUtil.kt**: Provides utility methods for calculating and formatting the remaining time until an alarm triggers.
- **util/AppExecutors.kt**: Centralizes thread management for disk and UI operations using a single thread executor and the main looper.
- **util/NotificationChannels.kt**: Configures notification channels for alarms, timers, and the stopwatch, ensuring appropriate priority and sound settings.
- **util/PowerButtonState.kt**: Monitors power button actions to support snooze/dismiss functionality.
- **util/RingPlayer.kt**: Manages audio playback and vibration for alerts, including support for gradual volume ramping.
- **util/SharedDrawablePool.kt**: Implements a memory-efficient cache for drawables using weak references.
- **util/ThemeApplier.kt**: Helper class that applies theme tokens to various UI components.
- **util/ThemeStore.kt**: A singleton that manages the active theme tokens and notifies listeners when the theme changes.
- **util/ThemeTokens.kt**: A data class that holds a set of color tokens for the current theme.
- **util/ThemeUtil.kt**: Provides utility functions for applying themes and tinting widgets.
- **util/TimeFormatUtil.kt**: Utility class for formatting time durations and clock times.
- **util/TimerScheduler.kt**: Manages scheduling and cancelling of timer expiration events using the system AlarmManager.
- **util/WakeLockHolder.kt**: Safely manages a wake lock during alert transitions.

## App Widget
- **widget/ClockWidgetProvider.kt**: AppWidgetProvider for the home screen clock widget.
- **widget/GooglyEyesController.kt**: Manages the logic and state for the googly eyes animation in the widget.
- **widget/GooglyEyesService.kt**: Handles background updates for the googly eyes widget.
- **widget/WidgetInfoProvider.kt**: Provides data and status information specifically for the app widgets.
- **widget/WidgetNotifier.kt**: Helper for triggering updates to all instances of the app widget.
- **widget/WidgetRenderer.kt** & **WidgetRendererExtensions.kt**: Encapsulates the canvas-based rendering logic for the app widget.
- **widget/WidgetState.kt**: Holds the visual state data for the widget during rendering.

## AppFunctions
- **appfunctions/TimelyAppFunctionService.kt**: Implementation of the AppFunctions SDK, exposing core functionality like creating alarms and starting timers to system agents and voice commands.

## Tests
- **AlarmModelTest.kt**: Unit tests for the Alarm data model.
- **AlarmSchedulerTest.kt**: Tests for the alarm scheduling logic.
- **AlarmTimeUtilTest.kt**: Tests for alarm time calculation utilities.
- **LapStoreTest.kt**: Tests for the lap time storage and encoding logic.
- **TimerItemTest.kt**: Unit tests for the TimerItem data model.
- **TimerRepositoryFakeTest.kt**: Tests for the timer repository using a fake database implementation.
