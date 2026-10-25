# MediRead AI — Medicine & Prescription Reader

**MediRead AI** is an Android mobile app designed to help users understand medicine packaging and doctor's prescriptions using Google GenAI (Gemma & Gemini models). Users can photograph a medicine strip or prescription slip, extract legible medical details, and receive plain-language explanations with accessibility support.

---

## 🌟 Key Features

* **💊 Medicine Strip Reader:** Scans blister packs, bottles, and boxes to detect medicine name, active ingredients, strengths, printed usage instructions, storage advice, and expiry dates.
* **📋 Doctor's Prescription Reader:** Interprets printed and handwritten prescriptions, extracts dosage, frequency, and duration, and flags ambiguous handwriting as `⚠️ UNVERIFIED`.
* **🌐 Multilingual Explanations:** Real-time output language switcher for **English**, **Hindi (हिंदी)**, and **Telugu (తెలుగు)**.
* **🔊 Read Aloud (Text-to-Speech):** High-clarity TTS audio support for elderly or low-vision users.
* **📱 Responsive Layout:** Optimized for both standard smartphones (vertical card hierarchy) and tablets/landscape screens (side-by-side view with photo on the left and extracted data on the right).
* **🔑 In-App API Configuration:** Dedicated settings modal to view or update API keys and model IDs without rebuilding the app.
* **🛡️ Medical Safety Disclaimers:** Visible warnings emphasizing that the app is for informational guidance and does not replace medical professionals.

---

## 🛠️ Tech Stack

* **Platform:** Android (Min SDK 24, Target SDK 36)
* **Language & Framework:** Kotlin 2.2, Jetpack Compose, Material 3
* **AI Engine:** Google Generative AI SDK (`gemma-4-26b-a4b-it` / `gemini-1.5-flash` / `gemini-2.5-flash`)
* **Image Processing:** FileProvider camera integration, URI bitmap decoding with downsampling
* **Audio:** Android `TextToSpeech` engine with localized voice support

---

## 🚀 Setup & Installation

### 1. Clone the repository
```bash
git clone https://github.com/Pasulasaiteja/med-reder.git
cd med-reder
```

### 2. Configure API Key
Create or open `local.properties` in the root directory:
```properties
# Add your Google AI Studio API key
GEMINI_API_KEY=your_api_key_here

# Configured Model ID (supports gemma-4-26b-a4b-it, gemini-2.5-flash, etc.)
GEMMA_MODEL=gemma-4-26b-a4b-it
```

### 3. Build & Run
Open the project in **Android Studio** and click **Run**, or build via command line:
```bash
./gradlew assembleDebug
```

---

## ⚠️ Medical Safety Notice
MediRead AI is designed for educational and informational assistance. It does not provide personalized medical diagnoses, treatments, or prescriptions. Always consult a licensed physician or pharmacist for medical decisions.
