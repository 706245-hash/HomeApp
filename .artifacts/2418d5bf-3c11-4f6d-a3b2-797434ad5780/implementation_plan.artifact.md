# Redesign Settings and Focus Mode Scheduling

This plan outlines the refactoring of the Settings UI to a minimalist black-and-white aesthetic and a new full-screen layout. It also includes an overhaul of the Focus Mode scheduling to be more intuitive and error-proof.

## Proposed Changes

### [UI Components]

#### [MODIFY] [SettingsDialog.kt](file:///home/agnocode/AndroidStudioProjects/HomeApp/app/src/main/java/com/agnocode/minimalhomeapp/ui/components/SettingsDialog.kt)
- **Settings Layout**: Replace the `AlertDialog` with a full-screen `Surface` or `Box` to allow for a more immersive and cleaner layout.
- **Aesthetic**: Change all `Color.DarkGray` containers to `Color.Black`. Ensure text colors are either `Color.White` or `Color.LightGray` for hierarchy.
- **Focus Mode Creation**:
    - Replace the raw `HH:MM` text inputs with a dedicated `TimePicker` or a `TimePickerDialog` flow.
    - Implement validation to ensure focus modes cannot have invalid times.
    - Improve the "Add/Edit/Del" buttons visibility and touch targets.
- **Accessibility**: Ensure all interactive elements have proper contrast and touch target sizes.

#### [MODIFY] [MainActivity.kt](file:///home/agnocode/AndroidStudioProjects/HomeApp/app/src/main/java/com/agnocode/minimalhomeapp/MainActivity.kt)
- Update how `SettingsDialog` is invoked to accommodate the full-screen transition if necessary (e.g., using a `BackHandler` inside the dialog to dismiss it).

## Verification Plan

### Manual Verification
- Deploy to the device.
- Open Settings and verify it is now full-screen and strictly black and white.
- Create a new Focus Mode and verify the new time selection flow.
- Ensure it's impossible to enter an invalid time (e.g., 34:00).
- Verify that the "Del" button is clearly visible.
- Test the back button/gesture to ensure it correctly dismisses the settings.
