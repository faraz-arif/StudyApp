# Study: Native Android Assessment Planner 🎓

## 📝 Overview
Study is a premium, flagship-grade native Android application designed to serve as an intelligent, secure, and highly optimized academic assistant. In a landscape saturated with generic organizers, Study solves a critical pain point for students: the lack of a centralized, secure hub that integrates academic subject tracking, dynamic task scheduling, study-session noise reduction, and robust note-taking.

## ✨ Core Features
*   **Zero-Trust Local Encryption:** All student records, credentials, notes, and backup archives are encrypted locally using hardware-backed cryptography, preventing unauthorized extraction. It utilizes SQLCipher for database-level table encryption, combined with AES-256-GCM symmetric encryption for backup exports.
*   **Intelligent Study Quiet-Time (DND):** Through deep integration with the system's NotificationManager, the app toggles the device's Do Not Disturb mode during scheduled study periods. This is executed through modern scheduling workers, yielding zero impact on battery life or system memory.
*   **Dynamic Task Management:** A stateful schedule engine allows users to add, update, and track exams, quizzes, and homework assignments linked directly to academic subject nodes.
*   **Hardware-Aware Fluid UI:** The application features seamless fluid UI rendering that automatically matches high refresh rates (up to 120Hz) on high-end hardware. It fully adopts Material Design 3 guidelines with shared element transitions and custom canvas-rendered confetti micro-interactions upon task completion.
*   **Offline-First Resilience:** Full offline availability ensures students can access class notes, track schedules, and back up data without needing an internet connection.

## 🛠️ Technical Stack & Architecture
The project is structured according to enterprise-grade native Android conventions, emphasizing decoupling, type safety, and security.

*   **Language:** Built 100% using idiomatic Kotlin, utilizing Coroutines for asynchronous work and Flow/StateFlow for reactive data streams.
*   **UI Toolkit:** The modern UI is built entirely in Jetpack Compose using a declarative, componentized layout tree.
*   **Architecture Pattern:** Strict MVVM (Model-View-ViewModel) decoupling UI states from business logic.
*   **Data Layer:** Room Persistence Library utilizing custom type converters, secured transparently via SQLCipher. List views implement dynamic limit/offset Room queries linked to Compose's lazy-list scrolling states for incremental pagination.
*   **Security:** EncryptedSharedPreferences is used to generate and store a hardware-backed random database passphrase via Android Keystore APIs.
*   **Background Processing:** Managed through WorkManager and AlarmManager.

## 🚀 Getting Started

### Prerequisites
*   Android Studio (Latest Stable Build)
*   Android SDK API Level 26+

### Installation
1. Clone the repository to your local machine:
   ```bash
   git clone [https://github.com/faraz-arif/StudyApp.git](https://github.com/faraz-arif/StudyApp.git)
