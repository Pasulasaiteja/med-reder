package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppLanguage(val displayName: String, val code: String, val locale: Locale) {
    ENGLISH("English", "en", Locale.ENGLISH),
    HINDI("हिंदी (Hindi)", "hi", Locale.forLanguageTag("hi-IN")),
    TELUGU("తెలుగు (Telugu)", "te", Locale.forLanguageTag("te-IN"))
}

/**
 * ViewModel for MediRead AI analysis.
 * Uses Google Generative AI SDK (Gemini) directly with the API key.
 */
class MediReadViewModel : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Configurable API key (default from local.properties / BuildConfig)
    private val _apiKey = MutableStateFlow(BuildConfig.GEMINI_API_KEY)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // Configurable model (default from local.properties / BuildConfig)
    private val _modelName = MutableStateFlow(BuildConfig.GEMMA_MODEL.ifBlank { "gemma-4-26b-a4b-it" })
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    // Selected language for explanations
    private val _selectedLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    // Text to speech instance
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun updateApiKey(newKey: String) {
        _apiKey.value = newKey.trim()
    }

    fun updateModelName(newModel: String) {
        _modelName.value = newModel.trim()
    }

    fun setLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
    }

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    updateTtsLanguage()
                }
            }
        }
    }

    private fun updateTtsLanguage() {
        tts?.let { engine ->
            val result = engine.setLanguage(_selectedLanguage.value.locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.ENGLISH)
            }
        }
    }

    fun speakText(text: String) {
        if (_isSpeaking.value) {
            stopSpeaking()
            return
        }

        updateTtsLanguage()
        // Clean markdown characters for cleaner audio reading
        val cleanText = text
            .replace(Regex("[*#_`~>⚠️💊🧪💪📋📏🔬🌡️📅🔢💉⏰🔤]"), "")
            .trim()

        _isSpeaking.value = true
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "MediReadTts")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }

    private fun getGenerativeModel(): GenerativeModel {
        val currentKey = _apiKey.value.ifBlank { BuildConfig.GEMINI_API_KEY }
        val currentModel = _modelName.value.ifBlank { "gemma-4-26b-a4b-it" }
        return GenerativeModel(
            modelName = currentModel,
            apiKey = currentKey
        )
    }

    /**
     * Analyze a medicine strip image.
     */
    fun analyzeMedicineStrip(bitmap: Bitmap) {
        val key = _apiKey.value
        if (key.isBlank() || key.contains("YOUR_API_KEY")) {
            _uiState.value = UiState.Error("API key not configured. Please provide a valid Gemini API key in Settings or local.properties.")
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = getMedicinePrompt(_selectedLanguage.value)
                val model = getGenerativeModel()
                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                } ?: run {
                    _uiState.value = UiState.Error("No response received from AI. Please try again.")
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Analysis failed. Please check your internet connection and API key."
                _uiState.value = UiState.Error(msg)
            }
        }
    }

    /**
     * Analyze a prescription image.
     */
    fun analyzePrescription(bitmap: Bitmap) {
        val key = _apiKey.value
        if (key.isBlank() || key.contains("YOUR_API_KEY")) {
            _uiState.value = UiState.Error("API key not configured. Please provide a valid Gemini API key in Settings or local.properties.")
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = getPrescriptionPrompt(_selectedLanguage.value)
                val model = getGenerativeModel()
                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                } ?: run {
                    _uiState.value = UiState.Error("No response received from AI. Please try again.")
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Analysis failed. Please check your internet connection and API key."
                _uiState.value = UiState.Error(msg)
            }
        }
    }

    /**
     * Reset to initial state.
     */
    fun resetState() {
        stopSpeaking()
        _uiState.value = UiState.Initial
    }

    companion object {
        fun getMedicinePrompt(language: AppLanguage): String {
            val langInstruction = when (language) {
                AppLanguage.ENGLISH -> "Provide explanations in simple, clear English."
                AppLanguage.HINDI -> "Provide the plain-language explanation and usage instructions in Hindi (हिंदी), while preserving the original English medicine names, numbers, strengths, and units."
                AppLanguage.TELUGU -> "Provide the plain-language explanation and usage instructions in Telugu (తెలుగు), while preserving the original English medicine names, numbers, strengths, and units."
            }

            return """
You are an expert healthcare document reader for MediRead AI. Analyze this medicine packaging/strip image carefully and accurately.
$langInstruction

Extract ONLY information that is visible on the medicine packaging. Structure your response in clean markdown cards as follows:

### 💊 Medicine Information
- **Medicine Name:** [exact name printed]
- **Active Ingredient(s):** [composition & strength, e.g., Paracetamol 500mg]
- **Brand / Manufacturer:** [company name if visible]

### 📋 Usage & Purpose
- **What it is used for:** [plain-language explanation of primary use]
- **Dosage Instructions:** [only if printed on packaging; otherwise note "Consult doctor/pharmacist"]

### ⚠️ Important Warnings & Precautions
- **Visible Warnings:** [contraindications, allergy alerts, alcohol warnings]
- **Storage:** [e.g., store below 25°C, keep away from direct sunlight]

### 📅 Details
- **Expiry Date:** [read carefully or mark "Not clearly visible"]
- **Batch No:** [batch if visible]

---
⚠️ **Medical Disclaimer:** MediRead AI provides informational guidance only. It does not replace a doctor or pharmacist. Never change your medication without consulting your healthcare provider.
            """.trimIndent()
        }

        fun getPrescriptionPrompt(language: AppLanguage): String {
            val langInstruction = when (language) {
                AppLanguage.ENGLISH -> "Explain the instructions in simple, clear English."
                AppLanguage.HINDI -> "Explain the prescription in Hindi (हिंदी) in simple words, while keeping the medicine names, doses, and numbers in their original form."
                AppLanguage.TELUGU -> "Explain the prescription in Telugu (తెలుగు) in simple words, while keeping the medicine names, doses, and numbers in their original form."
            }

            return """
You are an expert prescription reader for MediRead AI. Analyze this doctor's prescription image.
$langInstruction

Extract and structure the legible information:

### 📋 Prescribed Medications
(For each medication found):
- **Medicine Name:** [name as written; if handwriting is unclear, mark "⚠️ UNVERIFIED: [best guess]"]
- **Dosage & Frequency:** [e.g., 1 tablet twice daily after meals]
- **Duration:** [e.g., 5 days]
- **Instructions:** [e.g., take with warm water]

### 🔤 Plain-Language Summary
[Explain in 2-3 friendly sentences what the patient needs to do each day]

### ⚠️ Verification Notice
- If any handwriting, medicine name, or dose is ambiguous, clearly highlight it under this section as **UNVERIFIED** and advise immediate verification with the prescribing physician.

---
⚠️ **Important Safety Notice:** Handwriting recognition can be uncertain. Please confirm all dosages and names with your doctor or pharmacist before consumption.
            """.trimIndent()
        }
    }
}
