package com.example.fitnessrepcounter.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Low-latency audio playback using SoundPool.
 * All sounds are preloaded at init for instant playback during workouts.
 */
class AudioRepository(private val context: Context) {

    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(3) // max simultaneous sounds
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    // Sound IDs — loaded from raw resources
    // NOTE: These will use generated tones until actual sound files are added
    private var tickSoundId: Int = 0
    private var chimeSoundId: Int = 0
    private var errorSoundId: Int = 0
    private var loaded = false

    /**
     * Preload all sounds. Call once during setup.
     * Requires sound files in res/raw/:
     *  - tick.ogg (rep counted)
     *  - chime.ogg (set completed)
     *  - error_tone.ogg (invalid rep)
     *
     * If files don't exist yet, playback calls are no-ops.
     */
    fun preload() {
        val tickId = context.resources.getIdentifier("tick", "raw", context.packageName)
        val chimeId = context.resources.getIdentifier("chime", "raw", context.packageName)
        val errorId = context.resources.getIdentifier("error_tone", "raw", context.packageName)

        if (tickId != 0) {
            tickSoundId = soundPool.load(context, tickId, 1)
        }
        if (chimeId != 0) {
            chimeSoundId = soundPool.load(context, chimeId, 1)
        }
        if (errorId != 0) {
            errorSoundId = soundPool.load(context, errorId, 1)
        }
        loaded = (tickId != 0 || chimeId != 0 || errorId != 0)
    }

    /** Play the rep-counted tick */
    fun playTick() {
        if (loaded && tickSoundId != 0) {
            soundPool.play(tickSoundId, 0.8f, 0.8f, 1, 0, 1f)
        }
    }

    /** Play the set-completed chime */
    fun playChime() {
        if (loaded && chimeSoundId != 0) {
            soundPool.play(chimeSoundId, 0.7f, 0.7f, 1, 0, 1f)
        }
    }

    /** Play the invalid-rep error tone */
    fun playError() {
        if (loaded && errorSoundId != 0) {
            soundPool.play(errorSoundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    /** Release SoundPool resources. Call in onDestroy. */
    fun release() {
        soundPool.release()
    }
}
