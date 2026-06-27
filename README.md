# FlexLock 🔒💪

FlexLock is an offline Android application that acts as a secure fitness rep counter with built-in anti-cheat validation. It uses dual-source cross-verification (motion sensors + camera-based pose landmarks) to ensure reps are counted accurately while preventing false positives or cheating.

It also features a "Soft Lock" mode to prevent accidental workout exits by suppressing the system Back button and touch events, requiring a hidden 5-second long press in the top-right corner to unlock.

---

## Key Features

1. **Dual-Source Rep Verification:**
   * **Motion Sensors:** Continuous 50Hz polling of the accelerometer and gyroscope with low-pass filtering and discrete derivative peak detection.
   * **Pose Landmarks:** Real-time extraction of key skeletal joints using Google's ML Kit Pose Detection.
2. **Temporal Window Anti-Cheat Engine:**
   * Reps are only validated if the sensor peak and pose flexion-extension cycle match within a **1.5-second sliding window**.
3. **Optimized Performance (Offline):**
   * Frame sub-sampling (processing every 3rd camera frame at ~10 FPS) to prevent thermal throttling on mid-range devices like the MediaTek Dimensity 6100+ (Realme 12 5G).
   * Fully offline: no cloud calls or API dependencies.
4. **Accidental Touch Protection (Soft Lock):**
   * Suppresses Android back handler during active workouts.
   * Renders a touch-intercepting full-screen layer.
   * Unlocks ONLY via a **5-second long press in the top-right corner**.
5. **Low-Latency Audio:**
   * Preloaded sound effects via `SoundPool` for immediate verification ticks.

---

## Tech Stack

* **Platform:** Android (Kotlin)
* **UI Framework:** Jetpack Compose + Material 3
* **Camera API:** Jetpack CameraX (with `LifecycleCameraController` and `ImageAnalysis`)
* **Machine Learning:** Google ML Kit Pose Detection (Accurate Model)
* **Architecture:** MVVM + Repository pattern with reactive Kotlin Flows
* **Audio:** `SoundPool` for game-like sonification latency

---

## Project Structure

```text
app/src/main/java/com/fitness/repcounter/
├── MainActivity.kt
├── data/
│   ├── sensor/
│   │   ├── SensorRepository.kt        # SensorManager to callbackFlow
│   │   └── SensorDataFilter.kt        # Low-pass filter + peak detection
│   ├── pose/
│   │   ├── PoseAngleCalculator.kt     # Law of Cosines for joint angles
│   │   └── PoseOverlay.kt             # Skeleton rendering on Canvas
│   └── audio/
│       └── AudioRepository.kt         # SoundPool audio preloading
├── domain/
│   └── engine/
│       ├── FusionEngine.kt            # 1.5s temporal state machine
│       └── ExerciseValidator.kt       # Flexion/Extension phase tracking
└── ui/
    ├── theme/                         # Electric lime dark-mode system
    ├── screen/
    │   ├── HomeScreen.kt              # Exercise selection menu
    │   ├── CalibrationScreen.kt       # Live check for landmark visibility
    │   └── WorkoutScreen.kt           # Rep counter, Camera preview & Soft Lock
    └── component/                     # Custom Compose elements
```

---

## How to Build

1. Open the project in Android Studio (Koala/Ladybug or newer).
2. Sync the project with Gradle Files.
3. Build and run `assembleDebug` to generate the APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Find the output APK at `FlexLock.apk` in the project root folder.
