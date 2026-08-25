package com.mosque.prayerclock.service

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.AppSettings
import com.mosque.prayerclock.data.model.PrayerType
import com.mosque.prayerclock.data.model.SoundType

class SoundManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SoundManager"
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500) // Pattern: wait, vibrate, pause, vibrate...
    }

    private var beepPlayer: MediaPlayer? = null
    private var azanPlayer: MediaPlayer? = null
    private var iqamahPlayer: MediaPlayer? = null

    // SIWAK: generator nada bip (berbeda dari ticking mp3)
    private var toneGenerator: ToneGenerator? = null
    private val beepHandler = Handler(Looper.getMainLooper())

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator = getVibrator()

    /**
     * SIWAK: Mainkan pola BIP (pip) nyaring memakai ToneGenerator — benar-benar berbeda
     * dari suara ticking (mp3). isAzan=true memberi pola lebih panjang.
     */
    private fun playBeepPattern(isAzan: Boolean) {
        try {
            stopBeepTone()
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            toneGenerator = tg
            val totalBeeps = if (isAzan) 6 else 4
            val toneMs = 300
            val gapMs = 380
            var count = 0
            val runner =
                object : Runnable {
                    override fun run() {
                        val current = toneGenerator
                        if (current !== tg) return // dibatalkan / diganti
                        if (count >= totalBeeps) {
                            stopBeepTone()
                            return
                        }
                        try {
                            tg.startTone(ToneGenerator.TONE_PROP_BEEP, toneMs)
                        } catch (e: Exception) {
                            Log.e(TAG, "startTone error", e)
                        }
                        count++
                        beepHandler.postDelayed(this, (toneMs + gapMs).toLong())
                    }
                }
            beepHandler.post(runner)
            Log.d(TAG, "Playing beep pattern (isAzan=$isAzan, beeps=$totalBeeps)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play beep pattern", e)
        }
    }

    /**
     * SIWAK: satu bunyi BIP tunggal — dipakai untuk countdown per-detik (5..1) & bip final (0),
     * agar mode "Bip/pip" berbunyi 5 detik sebelum waktu, sama seperti ticking.
     */
    fun playSingleBeep(finalBeep: Boolean = false) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            val tone = if (finalBeep) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
            val dur = if (finalBeep) 600 else 160
            tg.startTone(tone, dur)
            beepHandler.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {}
            }, (dur + 120).toLong())
        } catch (e: Exception) {
            Log.e(TAG, "playSingleBeep error", e)
        }
    }

    private fun stopBeepTone() {
        beepHandler.removeCallbacksAndMessages(null)
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing tone generator", e)
        }
        toneGenerator = null
    }

    /**
     * Play 5-second countdown ticking sound when exactly 5 seconds remain
     */
    fun playCountdownTicking(
        secondsRemaining: Int,
        prayerType: PrayerType,
        settings: AppSettings,
    ) {
        Log.d(
            TAG,
            "playCountdownTicking called: secondsRemaining=$secondsRemaining, prayerType=$prayerType, soundEnabled=${settings.soundEnabled}",
        )

        if (!settings.soundEnabled) {
            Log.d(TAG, "Countdown ticking disabled in settings")
            return
        }

        // Only start the 5-second countdown when triggered (now at 6 seconds remaining for perfect sync)
        if (secondsRemaining != 5) {
            Log.d(TAG, "Not triggering countdown - secondsRemaining=$secondsRemaining (expecting 5 from service)")
            return
        }

        // Don't start if already playing
        if (beepPlayer != null && beepPlayer?.isPlaying == true) {
            Log.d(TAG, "Countdown ticking already playing")
            return
        }

        try {
            stopAllSounds() // Stop any currently playing sounds

            // Use the 5-second countdown ticking audio
            val uri = getCountdownTickingUri()
            Log.d(TAG, "Using audio URI: $uri")

            beepPlayer =
                MediaPlayer().apply {
                    setDataSource(context, uri)
                    setOnCompletionListener {
                        Log.d(TAG, "5-second countdown ticking completed for $prayerType")
                        release()
                        beepPlayer = null
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Error playing countdown ticking: what=$what, extra=$extra")
                        release()
                        beepPlayer = null
                        true
                    }
                    setOnPreparedListener {
                        Log.d(TAG, "MediaPlayer prepared, starting playback")
                        start()
                    }
                    // Use async prepare for better error handling
                    prepareAsync()
                }

            Log.d(TAG, "Started preparing 5-second countdown ticking: $prayerType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play countdown ticking", e)
            beepPlayer?.release()
            beepPlayer = null
        }
    }

    /**
     * SIWAK: Mainkan file bip hitung mundur (beep_countdown_3s.mp3) — dipicu saat sisa 3 detik,
     * berdurasi ~3 detik sehingga berakhir tepat saat masuk waktu.
     */
    fun playBeepCountdown(prayerType: PrayerType) {
        if (beepPlayer != null && beepPlayer?.isPlaying == true) {
            return
        }
        try {
            stopAllSounds()
            val uri = getTraditionalBeepUri()
            beepPlayer =
                MediaPlayer().apply {
                    setDataSource(context, uri)
                    setOnCompletionListener {
                        release()
                        beepPlayer = null
                    }
                    setOnErrorListener { _, _, _ ->
                        release()
                        beepPlayer = null
                        true
                    }
                    setOnPreparedListener { start() }
                    prepareAsync()
                }
            Log.d(TAG, "Playing beep countdown for $prayerType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play beep countdown", e)
            beepPlayer?.release()
            beepPlayer = null
        }
    }

    /**
     * Play Azan sound based on settings
     * Returns true if sound was played or vibration triggered
     */
    fun playAzanSound(
        settings: AppSettings,
        prayerType: PrayerType,
    ): Boolean {
        if (!settings.azanSoundEnabled) {
            Log.d(TAG, "Azan sound disabled in settings")
            return false
        }

        // Check if phone is in silent/vibrate mode
        if (isPhoneInSilentMode()) {
            Log.d(TAG, "Phone in silent mode - vibrating and showing notification only")
            vibrateForPrayer()
            return true // Return true to indicate we handled it (with vibration)
        }

        Log.d(TAG, "Playing Azan sound: type=${settings.azanSoundType}")

        when (settings.azanSoundType) {
            SoundType.COUNTDOWN_TICKING -> {
                // This case is handled by playCountdownTicking at 5 seconds before
                Log.d(TAG, "Azan countdown ticking handled separately")
                return false
            }
            SoundType.TRADITIONAL_BEEP -> {
                playBeepPattern(isAzan = true)
                return true
            }
            SoundType.CUSTOM -> {
                if (settings.azanSoundUri.isNotEmpty()) {
                    try {
                        val uri = Uri.parse(settings.azanSoundUri)
                        Log.d(TAG, "Attempting to play Azan custom audio from URI: $uri")
                        playSound(azanPlayer, uri, "Azan Custom Audio", prayerType, true) { azanPlayer = null }
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse Azan custom audio URI: ${settings.azanSoundUri}, falling back to beep", e)
                        playBeepPattern(isAzan = true)
                        return true
                    }
                } else {
                    Log.w(TAG, "Azan custom audio selected but no URI provided, using beep")
                    playBeepPattern(isAzan = true)
                    return true
                }
            }
        }
    }

    /**
     * Play Iqamah sound based on settings
     * Returns true if sound was played or vibration triggered
     */
    fun playIqamahSound(
        settings: AppSettings,
        prayerType: PrayerType,
    ): Boolean {
        if (!settings.iqamahSoundEnabled) {
            Log.d(TAG, "Iqamah sound disabled in settings")
            return false
        }

        // Check if phone is in silent/vibrate mode
        if (isPhoneInSilentMode()) {
            Log.d(TAG, "Phone in silent mode - vibrating and showing notification only")
            vibrateForPrayer()
            return true // Return true to indicate we handled it (with vibration)
        }

        Log.d(TAG, "Playing Iqamah sound: type=${settings.iqamahSoundType}")

        when (settings.iqamahSoundType) {
            SoundType.COUNTDOWN_TICKING -> {
                // This case is handled by playCountdownTicking at 5 seconds before
                Log.d(TAG, "Iqamah countdown ticking handled separately")
                return false
            }
            SoundType.TRADITIONAL_BEEP -> {
                playBeepPattern(isAzan = false)
                return true
            }
            SoundType.CUSTOM -> {
                if (settings.iqamahSoundUri.isNotEmpty()) {
                    try {
                        val uri = Uri.parse(settings.iqamahSoundUri)
                        Log.d(TAG, "Attempting to play Iqamah custom audio from URI: $uri")
                        playSound(iqamahPlayer, uri, "Iqamah Custom Audio", prayerType, false) { iqamahPlayer = null }
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse Iqamah custom audio URI: ${settings.iqamahSoundUri}, falling back to beep", e)
                        playBeepPattern(isAzan = false)
                        return true
                    }
                } else {
                    Log.w(TAG, "Iqamah custom audio selected but no URI provided, using beep")
                    playBeepPattern(isAzan = false)
                    return true
                }
            }
        }
    }

    /**
     * Generic method to play a sound from URI
     */
    private fun playSound(
        player: MediaPlayer?,
        uri: Uri,
        description: String,
        prayerType: PrayerType,
        isAzan: Boolean,
        onComplete: () -> Unit,
    ) {
        // Don't start if already playing
        if (player != null && player.isPlaying) {
            Log.d(TAG, "$description already playing")
            return
        }

        try {
            // Stop any existing player
            player?.let {
                it.release()
            }

            Log.d(TAG, "Playing $description from URI: $uri")

            val newPlayer = MediaPlayer()
            
            // Use FileDescriptor for better permission handling with content URIs
            if (uri.scheme == "content") {
                try {
                    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    if (fileDescriptor != null) {
                        newPlayer.setDataSource(fileDescriptor.fileDescriptor)
                        fileDescriptor.close()
                        Log.d(TAG, "Successfully set data source using FileDescriptor")
                    } else {
                        throw IllegalArgumentException("Unable to open file descriptor for URI: $uri")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException opening file descriptor. URI permissions may not be granted.", e)
                    throw IllegalArgumentException(
                        "Cannot access audio file. Please re-select the audio file from Settings.",
                        e
                    )
                }
            } else {
                // For non-content URIs (e.g., android.resource://)
                newPlayer.setDataSource(context, uri)
            }
            
            newPlayer.apply {
                setAudioStreamType(AudioManager.STREAM_ALARM) // Use alarm stream to bypass silent mode if needed
                setOnCompletionListener {
                    Log.d(TAG, "$description completed")
                    release()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error playing $description: what=$what, extra=$extra")
                    release()
                    onComplete()
                    true
                }
                setOnPreparedListener {
                    Log.d(TAG, "$description prepared, starting playback")
                    start()
                }
                prepareAsync()
            }

            // Update the player reference based on which one we're using
            when (description) {
                "Azan Traditional Beep", "Azan Custom Audio", "Azan Fallback Beep", "Azan Default Beep" -> azanPlayer = newPlayer
                "Iqamah Traditional Beep", "Iqamah Custom Audio", "Iqamah Fallback Beep", "Iqamah Default Beep" -> iqamahPlayer = newPlayer
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play $description", e)
            player?.release()
            onComplete()
        }
    }

    /**
     * Stop all currently playing sounds
     */
    fun stopAllSounds() {
        try {
            beepPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                beepPlayer = null
            }
            azanPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                azanPlayer = null
            }
            iqamahPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                iqamahPlayer = null
            }
            stopBeepTone() // SIWAK: hentikan pola bip ToneGenerator
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sounds", e)
        }
    }

    /**
     * Get the URI for 5-second countdown ticking sound
     */
    private fun getCountdownTickingUri(): Uri =
        try {
            // Use the 5-second countdown ticking audio file
            Uri.parse("android.resource://${context.packageName}/${R.raw.countdown_ticking_5s}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting countdown ticking URI, falling back to system sound", e)
            // Fallback to system notification sound
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }

    /**
     * Get the URI for traditional beep sound
     */
    private fun getTraditionalBeepUri(): Uri =
        try {
            // SIWAK: file bip 3 detik (berbunyi saat sisa 3 detik)
            Uri.parse("android.resource://${context.packageName}/${R.raw.beep_countdown_3s}")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting traditional beep URI, falling back to system sound", e)
            // Fallback to system notification sound
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        }

    /**
     * Validate and test if a custom audio URI is valid
     */
    fun validateCustomAudioUri(uri: String): Boolean {
        if (uri.isEmpty()) return false

        return try {
            val audioUri = Uri.parse(uri)
            val player = MediaPlayer()
            player.setDataSource(context, audioUri)
            player.prepare()
            player.release()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Invalid custom audio URI: $uri", e)
            false
        }
    }

    /**
     * Check if any sound is currently playing
     */
    fun isPlaying(): Boolean {
        val beepPlaying = beepPlayer?.isPlaying == true
        val azanPlaying = azanPlayer?.isPlaying == true
        val iqamahPlaying = iqamahPlayer?.isPlaying == true
        return beepPlaying || azanPlaying || iqamahPlaying
    }

    /**
     * Check if phone is in silent or vibrate mode
     */
    private fun isPhoneInSilentMode(): Boolean {
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT, AudioManager.RINGER_MODE_VIBRATE -> true
            else -> false
        }
    }

    /**
     * Vibrate the phone for prayer notification
     */
    private fun vibrateForPrayer() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        VIBRATION_PATTERN,
                        -1, // Don't repeat
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, -1)
            }
            Log.d(TAG, "Vibration triggered for prayer notification")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }

    /**
     * Get vibrator service (handles both old and new API)
     */
    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get vibrator service", e)
            null
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopAllSounds()
        vibrator?.cancel() // Cancel any ongoing vibration
    }
}
