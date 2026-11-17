package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.example.data.local.ProductEntity
import java.util.Locale

data class ParsedVoiceMatch(
    val product: ProductEntity,
    val quantity: Int
)

class KioskAudioManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    fun playTapBeep() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        }
    }

    fun playSuccessChime() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 220)
        }
    }

    fun speak(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sellmore_tts_${System.currentTimeMillis()}")
        }
    }

    fun release() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        ttsReady = false
    }

    companion object {
        private val NUMBER_WORDS = mapOf(
            "a" to 1, "an" to 1, "one" to 1, "single" to 1,
            "two" to 2, "couple" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6
        )

        fun parseVoiceOrder(transcript: String, products: List<ProductEntity>): List<ParsedVoiceMatch> {
            val lower = transcript.lowercase(Locale.US)
            val clauses = lower.split(Regex("\\b(and|plus|with|then|,)\\b"))
            val aggregated = LinkedHashMap<String, Int>()

            for (clause in clauses) {
                val trimmed = clause.trim()
                if (trimmed.isEmpty()) continue

                val tokens = trimmed.split(Regex("\\s+"))
                var qty = 1
                for (tok in tokens) {
                    val cleanTok = tok.replace(Regex("[^a-z0-9]"), "")
                    val numeric = cleanTok.toIntOrNull()
                    if (numeric != null && numeric in 1..10) {
                        qty = numeric
                        break
                    }
                    val wordVal = NUMBER_WORDS[cleanTok]
                    if (wordVal != null) {
                        qty = wordVal
                        break
                    }
                }

                val matched = products.firstOrNull { p ->
                    trimmed.contains(p.flavorTag.lowercase(Locale.US)) ||
                        trimmed.contains(p.slotCode.lowercase(Locale.US)) ||
                        trimmed.contains(p.name.lowercase(Locale.US))
                }
                if (matched != null) {
                    aggregated[matched.id] = (aggregated[matched.id] ?: 0) + qty
                }
            }

            return aggregated.mapNotNull { (id, qty) ->
                products.firstOrNull { it.id == id }?.let { ParsedVoiceMatch(it, qty) }
            }
        }
    }
}
