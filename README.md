# JARVIS AI - Personal Android AI Assistant

JARVIS AI is a native Android application built with **Kotlin**, **Jetpack Compose**, and the official **Google Gemini API**. Designed for zero-rupee personal use, it allows natural voice conversations in **Hindi, Hinglish, and English** and controls phone features via safe, user-permissioned actions.

---

## 🌟 Key Features

1. **Google Gemini API Key Setup**:
   - First-launch setup with Google AI Studio key (`https://aistudio.google.com/`).
   - Hardware-backed **Android Keystore** encryption (AES-256-GCM) stored locally.
   - Show/Hide toggle, Save, Validate Connection, Change, and Delete controls.
   - Zero-cloud-leak guarantee: API key is never uploaded to Firebase or third-party servers.

2. **Multilingual Voice & Chat AI**:
   - Understands and speaks **Hindi (हिंदी)**, **Hinglish**, and **English**.
   - Integrated Android `SpeechRecognizer` with real-time speech telemetry and RMS sound levels.
   - Natural spoken feedback via Android `TextToSpeech` with speech rate and pitch calibration.
   - Global **STOP** button and instant voice trigger `"Jarvis, stop"` to halt speech and pending actions immediately.

3. **Futuristic HUD UI**:
   - Arc Reactor animated visualizer with dynamic pulse, rotation, and audio reactions.
   - Real-time status indicators (IDLE, LISTENING, THINKING, SPEAKING, EXECUTING, WAITING_FOR_CONFIRMATION, STOPPED).
   - Cyberpunk cyan/deep-navy theme.

4. **Complete Permission Manager**:
   - Dedicated screen with live verification of all runtime and system permissions:
     - Microphone (Audio Recording)
     - Contacts (Contact Lookup)
     - Phone (Direct Calls)
     - Calendar (Upcoming Events)
     - Notification Listener (Notification Reader)
     - Android Accessibility Service (Screen Reader & Controls)
     - System Notifications (Alerts)
     - Text-To-Speech Engine & Haptics
   - Clear Hindi/Hinglish explanations and direct Grant/Enable buttons.

5. **Accessibility Service & Screen Reading**:
   - Native `JarvisAccessibilityService` inspects visible on-screen text, performs back gestures, and clicks accessible controls.
   - Protects user privacy: strictly skips password fields and secure elements.
   - In-depth Accessibility Tutorial screen in Hindi/Hinglish.

6. **Phone Action Dispatcher**:
   - Launch installed apps (YouTube, Chrome, WhatsApp, Settings, Maps, Camera, Calculator, Dialer, etc.).
   - Find contacts and initiate phone calls or open dialer safely.
   - Prepare WhatsApp messages with recipient and message body.
   - Read upcoming calendar events.
   - Summarize active notifications.

7. **Local Privacy (Zero-Rupee Target)**:
   - On-device SQLite storage using **Room Database**.
   - No advertisements, no subscriptions, no Firebase sign-in requirements.

---

## 🛠️ Build and Installation

### Generating Debug APK
In AI Studio or your local terminal:
```bash
gradle assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

---

## 🚀 Publishing to GitHub & Downloading the APK

### Step 1: Push Project to GitHub
1. Create a new public repository on [GitHub](https://github.com/new) named `JARVIS-AI`.
2. Push your project to the repository using Git:
   ```bash
   git add .
   git commit -m "Initial commit: JARVIS AI native Android assistant"
   git branch -M main
   git remote add origin https://github.com/<your-username>/JARVIS-AI.git
   git push -u origin main
   ```
*(Alternatively, click **Export to GitHub** in the top-right settings menu of Google AI Studio).*

### Step 2: Automated APK Build via GitHub Actions
- Once pushed, GitHub Actions automatically triggers the workflow defined in `.github/workflows/android-apk.yml`.
- The workflow:
  - Sets up JDK 17 and Gradle.
  - Compiles the APK via `./gradlew --no-daemon assembleDebug`.
  - Packages and uploads `app-debug.apk` as a downloadable artifact named `JARVIS-AI-APK`.

### Step 3: How to Download the Built APK
1. Open your repository on GitHub.
2. Click on the **Actions** tab at the top.
3. Click on the latest workflow run (e.g., *"Build Android APK"*).
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Click **JARVIS-AI-APK** to download the ZIP file containing `app-debug.apk`.
6. Transfer `app-debug.apk` to your Android phone and install!

---

## 🔒 Security & Privacy Notice
All requests sent to Gemini are processed in accordance with Google's API terms of service. Credentials are encrypted on-device via the Android hardware Keystore.
