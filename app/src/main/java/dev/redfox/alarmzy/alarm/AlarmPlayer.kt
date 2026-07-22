package dev.redfox.alarmzy.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibratorManager: VibratorManager? = null
    private var volumeJob: Job? = null

    fun play(
        ringtoneUri: String?,
        vibrate: Boolean,
        gradualVolume: Boolean,
        scope: CoroutineScope
    ) {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Try the configured (possibly custom, permission-backed) sound first, then
        // fall back to the device default so an alarm is never silent.
        val player = ringtoneUri?.let { preparePlayer(Uri.parse(it)) }
            ?: defaultUri?.let { preparePlayer(it) }

        mediaPlayer = player
        player?.apply {
            if (gradualVolume) {
                setVolume(0.1f, 0.1f)
                volumeJob = scope.launch {
                    for (i in 1..18) {
                        delay(1667)
                        val volume = (0.1f + (0.9f * i / 18f)).coerceAtMost(1f)
                        setVolume(volume, volume)
                    }
                }
            }

            start()
        }

        if (vibrate) {
            vibratorManager = context.getSystemService(VibratorManager::class.java)
            val pattern = longArrayOf(0, 500, 200, 500)
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
        }
    }

    /**
     * Builds and prepares a looping [MediaPlayer] for [uri], or returns null if the source
     * can't be loaded (e.g. a custom file whose read permission was lost). The player is
     * released on failure so the caller can fall back to another source.
     */
    private fun preparePlayer(uri: Uri): MediaPlayer? {
        val mp = MediaPlayer()
        return runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.prepare()
            mp
        }.getOrElse {
            runCatching { mp.release() }
            null
        }
    }

    fun stop() {
        volumeJob?.cancel()
        volumeJob = null
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibratorManager?.cancel()
        vibratorManager = null
    }
}
