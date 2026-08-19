package com.vano.n8nmobile.autoreply

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object AutoReplyTts {
    @Volatile private var tts: TextToSpeech? = null

    fun speak(context: Context, text: String) {
        val instance = tts
        if (instance == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("id", "ID")
                    tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "aiwa_autoreply_tts")
                }
            }
        } else {
            instance.language = Locale("id", "ID")
            instance.speak(text, TextToSpeech.QUEUE_ADD, null, "aiwa_autoreply_tts")
        }
    }
}
