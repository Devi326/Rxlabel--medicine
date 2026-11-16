# RxLabel 💊 - Medicine Info & Reminder App

**RxLabel** is an Android application designed to help users identify medication labels, search official drug details via openFDA, set timely medicine reminders, and sync account data across devices.

---

## 🌟 Key Features

- 📷 **OCR Label Scanning**: Snap a photo with your Camera or pick from Gallery to extract text from medicine packages using **Google ML Kit Text Recognition**.
- 🔍 **openFDA Drug Lookup**: Instant search for drug indications, dosage instructions, warnings, active ingredients, and manufacturer details powered by the **openFDA API**.
- ⏰ **Medication Reminders**: Schedule local pill & medicine alarms with **AlarmManager** and store reminder schedules locally using **Room Database**.
- 👤 **Google Authentication**: Sign in with **Firebase Auth & Google Sign-In** to back up and sync account preferences.
- 🎨 **Material Design UI**: Modern interface with tabbed navigation, expandable info cards, and dynamic UI state handling.

---

## 🛠 Tech Stack & Libraries

- **Language**: Kotlin & Java
- **Architecture**: MVVM / Repository pattern with ViewBinding
- **AI / Vision**: [Google ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition)
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & [Gson](https://github.com/google/gson)
- **Local Storage**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Authentication**: [Firebase Auth](https://firebase.google.com/docs/auth) with Google Sign-In
- **Image Loading**: [Glide](https://github.com/bumptech/glide)
- **UI Components**: Material Components 3, ViewBinding, ConstraintLayout, RecyclerView

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug / Hedgehog or newer
- **JDK**: Java 17
- **Android SDK**: Min SDK 24 (Android 7.0), Target SDK 35 / Compile SDK 37

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Devi326/Rxlabel--medicine.git
   cd Rxlabel--medicine
   ```

2. **Open in Android Studio**:
   Open Android Studio and select `Open an existing project`, then choose the cloned project folder.

3. **Build & Run**:
   Sync Gradle project files and run on an Android device or emulator.

---

## 📋 License & Disclaimer

*⚠️ **Disclaimer**: This app retrieves information from openFDA for informational purposes only. It does not provide professional medical advice. Always consult a healthcare professional before taking any medication.*
