```kotlin
package android.template.butki

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var generativeModel: GenerativeModel

    private lateinit var txtResponse: TextView
    private lateinit var btnMic: Button

    private val micLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()
                    .orEmpty()

                if (spokenText.isNotBlank()) {
                    txtResponse.text = "Aapne kaha: \"$spokenText\""
                    askButki(spokenText)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutId = resources.getIdentifier(
            "activity_main",
            "layout",
            packageName
        )

        if (layoutId == 0) {
            finish()
            return
        }

        setContentView(layoutId)

        val responseId = resources.getIdentifier(
            "txtResponse", "id", packageName
        )
        val micId = resources.getIdentifier(
            "btnMic", "id", packageName
        )

        if (responseId == 0 || micId == 0) {
            finish()
            return
        }

        txtResponse = findViewById(responseId)
        btnMic = findViewById(micId)

        tts = TextToSpeech(this, this)

        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        btnMic.setOnClickListener {
            checkAndRequestAudioPermission()
        }
    }

    private fun checkAndRequestAudioPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (
            requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else if (requestCode == 100) {
            txtResponse.text = "Butki ko microphone permission chahiye."
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "hi-IN"
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Butki sun rahi hai, boliye..."
            )
        }

        try {
            micLauncher.launch(intent)
        } catch (e: Exception) {
            txtResponse.text = "Voice input start nahi ho saka."
        }
    }

    private fun askButki(prompt: String) {
        txtResponse.text = "Butki soch rahi hai..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val personaPrompt =
                    "You are Butki, a caring, warm and friendly " +
                    "female AI assistant. Answer naturally and " +
                    "concisely in Hindi or English as appropriate.\n" +
                    "User: $prompt"

                val response =
                    generativeModel.generateContent(personaPrompt)

                val answer =
                    response.text ?: "Kuch samajh nahi aaya."

                withContext(Dispatchers.Main) {
                    txtResponse.text = answer
                    speakOut(answer)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtResponse.text =
                        "Butki error: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
        }
    }

    private fun speakOut(text: String) {
        if (::tts.isInitialized) {
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "butki_response"
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("hi", "IN"))

            if (
                result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // Device par Hindi TTS available na ho
                tts.language = Locale.getDefault()
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }
}
```
