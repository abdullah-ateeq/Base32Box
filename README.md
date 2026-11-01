# Base32Box 📦

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-orange.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-37-brightgreen.svg)

**Base32Box** is a fast, lightweight, and modern Android application for encoding and decoding data using Base32 algorithms compliant with RFC 4648. Built with modern Android development practices using Jetpack Compose, Material 3, and Kotlin Coroutines.

---

## ✨ Features

- **Standard & Extended Base32 Support**:
  - **RFC 4648 Standard**: Uses uppercase alphabet `A–Z, 2–7`.
  - **Base32 Hex (Extended)**: Uses `0–9, A–V` to preserve binary sort order.
- **Flexible Encoding & Decoding Options**:
  - Configurable padding (`=` character).
  - Uppercase and lowercase output formatting.
  - Case-insensitive decoding support.
  - Whitespace tolerance (ignores spaces, newlines, tabs).
  - Strict validation for non-zero padding bits and malformed blocks.
- **Text & File Processing**:
  - Real-time text encoding & decoding with character/byte count metrics and UTF-8 verification.
  - High-performance streaming engine for file encoding/decoding without high memory overhead.
- **Modern Material 3 UI**:
  - Dynamic Light, Dark, and System theme support.
  - Haptic feedback interactions.
  - Configurable privacy options (e.g., auto-clear data on exit).
- **Persistent Preferences**: Built on AndroidX DataStore for user settings persistence.

---

## 🛠 Tech Stack & Architecture

- **Language**: 100% [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) pattern
- **Asynchronous Operations**: Kotlin Coroutines & `StateFlow`
- **Data Storage**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Core Engine**: Custom RFC 4648 compliant Base32 streaming engine (`Base32Engine`)
- **Testing**: JUnit 4 for unit tests covering test vectors, streaming, and edge cases

---

## 📁 Project Structure

```
Base32Box/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/base32box/
│   │   │   │   ├── core/base32/       # RFC 4648 Base32 Engine & Exception handling
│   │   │   │   ├── data/preferences/  # DataStore Repository & UserPreferences
│   │   │   │   ├── ui/                # Compose Theme, ViewModels, Navigation
│   │   │   │   └── MainActivity.kt    # Main entry point
│   │   │   └── AndroidManifest.xml
│   │   └── test/                      # Unit tests (Base32EngineTest, etc.)
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml             # Version Catalog
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug / Jellyfish or newer
- **JDK**: Java 11 or higher
- **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 37

### Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/Base32Box.git
   cd Base32Box
   ```
2. Build the debug APK using Gradle Wrapper:
   ```bash
   ./gradlew app:assembleDebug
   ```

### Running Unit Tests

To run unit tests:
```bash
./gradlew app:testDebugUnitTest
```

---

## 📄 License

This project is open-source.
