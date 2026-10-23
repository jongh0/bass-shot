package com.jongh0.bassshot.domain.util

import android.media.AudioAttributes
import android.media.MediaActionSound
import android.media.SoundPool
import java.io.File

object ShutterSoundPlayer {

    // 중간 정도 볼륨 (50%)
    private const val DEFAULT_VOLUME = 0.5f

    // 안드로이드 및 삼성 갤럭시 기본 카메라 셔터 사운드 파일 경로 후보
    private val CANDIDATE_SOUND_PATHS = listOf(
        "/system/media/audio/ui/camera_click.ogg",
        "/product/media/audio/ui/camera_click.ogg",
        "/system_ext/media/audio/ui/camera_click.ogg",
        "/system/sounds/ui/camera_click.ogg",
        "/system/media/audio/ui/Camera_click.ogg",
        "/system/media/audio/ui/Shutter.ogg",
        "/system/media/audio/ui/camera_shutter.ogg"
    )

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0

    @Volatile
    private var isLoaded: Boolean = false

    private val mediaActionSound: MediaActionSound by lazy {
        MediaActionSound().apply {
            try {
                load(MediaActionSound.SHUTTER_CLICK)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        try {
            val soundFile = CANDIDATE_SOUND_PATHS
                .map { File(it) }
                .firstOrNull { it.exists() && it.canRead() }

            if (soundFile != null) {
                val pool = SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()

                pool.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) {
                        isLoaded = true
                    }
                }

                soundId = pool.load(soundFile.absolutePath, 1)
                soundPool = pool
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 갤럭시 기본 카메라 셔터음(camera_click.ogg)을 중간 크기(0.5f)로 재생
     * (시스템 사운드 파일 직접 접근이 제한될 경우 MediaActionSound로 안전 폴백)
     */
    fun playDefaultShutterSound() {
        try {
            if (isLoaded && soundPool != null && soundId > 0) {
                soundPool?.play(soundId, DEFAULT_VOLUME, DEFAULT_VOLUME, 1, 0, 1.0f)
            } else {
                mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    /**
     * 하위 호환용 별칭
     */
    fun playGentleShutterSound() = playDefaultShutterSound()

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            mediaActionSound.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
