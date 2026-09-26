package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GameSoundSynthesizer {
    private const val SAMPLE_RATE = 22050

    fun playSelect() {
        playTone(frequency = 880.0, durationMs = 35, volume = 0.4f)
    }

    fun playMatchSuccess() {
        // 马里奥吃金币音效：连续上升的音符（523.25Hz(C5) -> 783.99Hz(G5)）
        playTones(
            listOf(
                Tone(523.25, 70),
                Tone(783.99, 140)
            ),
            volume = 0.5f
        )
    }

    fun playMatchFailure() {
        // 低沉沉闷的错误降调音（220Hz -> 146.83Hz）
        playTones(
            listOf(
                Tone(220.0, 90),
                Tone(146.83, 140)
            ),
            volume = 0.4f
        )
    }

    fun playVictory() {
        // 经典的胜利大和弦（C4 -> E4 -> G4 -> C5）
        playTones(
            listOf(
                Tone(261.63, 90),
                Tone(329.63, 90),
                Tone(392.00, 90),
                Tone(523.25, 240)
            ),
            volume = 0.5f
        )
    }

    private data class Tone(val freq: Double, val durationMs: Int)

    private fun playTone(frequency: Double, durationMs: Int, volume: Float) {
        playTones(listOf(Tone(frequency, durationMs)), volume)
    }

    private fun playTones(tones: List<Tone>, volume: Float) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val totalSamples = tones.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
                val buffer = ShortArray(totalSamples)

                var bufferIdx = 0
                for (tone in tones) {
                    val samplesForTone = (SAMPLE_RATE * tone.durationMs) / 1000
                    for (i in 0 until samplesForTone) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val angle = 2.0 * Math.PI * tone.freq * t
                        val value = Math.sin(angle)

                        // 渐隐（Fade Out）包络，防止音频切断处的咔哒杂音，模拟琴键渐弱
                        val fadeFactor = if (i > samplesForTone * 0.7) {
                            val remaining = samplesForTone - i
                            val fadeLength = samplesForTone * 0.3
                            remaining / fadeLength
                        } else {
                            1.0
                        }

                        val sampleValue = (value * 32767.0 * volume * fadeFactor).toInt()
                        buffer[bufferIdx++] = sampleValue.coerceIn(-32768, 32767).toShort()
                    }
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                val playDuration = tones.sumOf { it.durationMs }.toLong()
                kotlinx.coroutines.delay(playDuration + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
