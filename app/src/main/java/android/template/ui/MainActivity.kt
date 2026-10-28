/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.template.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.template.R

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    
    // Yahan neeche "YAHAN_APNI_API_KEY_DAALEIN" ko hata kar apni asli API key likhein
    private val apiKey = "YAHAN_APNI_API_KEY_DAALEIN" 
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userText = results?.get(0) ?: ""
            statusText.text = "Soch rahi hoon..."
            askButkiAI(userText)
        } else {
            statusText.text = "Main aapse milne aa rahi hoon..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusTextView)
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
            statusText.text = "Screen par tap karein aur Butki se baat karein!"
            
            findViewById<android.view.View>(android.R.id.content).setOnClickListener {
                startListening()
            }
        }
    }

    private fun startListening() {
        statusText.text = "Boliye, main sun rahi hoon..."
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }
        speechRecognizerLauncher.launch(intent)
    }

    private fun askButkiAI(userText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = "Tumhara naam Butki hai, tum ek bahut sweet aur caring female AI assistant ho. User ne kaha: '$userText'. Ek dost ki tarah Hindi mein chota, pyara aur supportive jawab do."
                val response = generativeModel.generateContent(prompt)
                val reply = response.text ?: "Mujhe kuch samajh nahi aaya."
                
                withContext(Dispatchers.Main) {
                    statusText.text = "Butki bol rahi hai..."
                    speak(reply)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Internet connection check karein!"
                    speak("Mujhe lagta hai internet mein kuch pareshani hai.")
                }
            }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        statusText.text = "Screen par tap karein aur baat karein!"
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
