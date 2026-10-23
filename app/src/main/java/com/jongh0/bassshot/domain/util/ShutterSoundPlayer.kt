package com.jongh0.bassshot.domain.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ShutterSoundPlayer {

    private const val SAMPLE_RATE = 44100
    private var pcmData: ShortArray? = null

    init {
        // 부드럽고 절제된 기계식 카메라 셔터 사운드 (40ms, 귀가 아프지 않은 30% 감쇠 볼륨)
        val durationMs = 40
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 900Hz의 부드러운 감쇠 클릭 파형
            val envelope = exp(-t * 120.0)
            val freq = 950.0 - (t * 3000.0)
            val wave = sin(2.0 * PI * freq * t)
            val sample = (wave * envelope * Short.MAX_VALUE * 0.30).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        pcmData = buffer
    }

    fun playGentleShutterSound() {
        val data = pcmData ?: return
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(data, 0, data.size)
            audioTrack.play()
            audioTrack.setNotificationMarkerPosition(data.size)
            audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    track?.release()
                }
                override fun onPeriodicNotification(track: AudioTrack?) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
