# ⚽ Keepie Counter

An Android app that uses real-time computer vision to count your keepie-uppies (soccer ball juggling).

## How It Works

The app combines two ML Kit detection models running simultaneously:

- **Ball Tracking** — Detects the soccer ball and tracks its vertical trajectory to identify upward motion
- **Kick Detection** — Uses pose detection (33 body landmarks) to identify kick motions via ankle velocity and knee extension angle

A keepie-uppie is counted when both signals correlate within a ±200ms window, with debounce logic to prevent double-counting.

## Tech Stack

- **Kotlin** + Jetpack Compose (Material 3)
- **CameraX** for real-time camera pipeline
- **ML Kit** Object Detection (ball) + Pose Detection (kicks)
- **Room** for session history and personal bests
- **Hilt** for dependency injection
- **MVVM** + Clean Architecture

## Getting Started

1. Clone the repo
2. Open in Android Studio (Arctic Fox or later)
3. Sync Gradle — Android Studio will download the SDK and dependencies
4. Run on a physical device (camera required)

## Project Structure

```
app/src/main/java/com/keepiecounter/
├── detection/        # CV detection logic
│   ├── ball/         # Ball tracking with ML Kit Object Detection
│   ├── pose/         # Kick detection with ML Kit Pose Detection
│   └── counter/      # Combined counting algorithm
├── data/             # Room database and repositories
├── domain/model/     # Domain models
├── di/               # Hilt dependency injection modules
└── ui/               # Jetpack Compose screens
    ├── camera/       # Main camera screen with counter overlay
    ├── history/      # Session history and personal bests
    └── theme/        # Material 3 theming
```

## License

MIT
