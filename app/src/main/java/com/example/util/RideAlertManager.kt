package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.models.RideOffer
import kotlinx.coroutines.*

/**
 * High-Priority Loud Booking Alert & Notification Manager for E-Ride 3 Driver App.
 *
 * Supports:
 * - Driver configurable Booking Ringtone [ON/OFF]
 * - 5 Distinct built-in sound patterns (Alert 1 - Alert 5)
 * - Live preview and sound test playback
 * - Android High-Priority heads-up system notifications and continuous vibration
 * - Device silent/muted audio state detection
 */
object RideAlertManager {
    private const val CHANNEL_ID = "eride3_high_priority_booking_alerts"
    private const val CHANNEL_NAME = "E-Ride 3 Booking Alerts"
    private const val NOTIFICATION_ID = 9011

    private var alertJob: Job? = null
    private var previewJob: Job? = null
    private var isPlaying: Boolean = false
    private var isPreviewPlaying: Boolean = false
    private var currentlyPreviewingId: String? = null
    private var currentOfferId: String? = null

    fun isAlertActive(): Boolean = isPlaying
    fun isPreviewActive(): Boolean = isPreviewPlaying
    fun getPreviewingRingtoneId(): String? = currentlyPreviewingId

    /**
     * Checks if device is in Silent mode, Vibrate mode, or notification/alarm stream is muted.
     */
    fun isDeviceSoundBlocked(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val ringerMode = audioManager.ringerMode
        val isSilentOrVibrate = ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE
        val alarmVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val ringVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val notifVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

        return isSilentOrVibrate || (alarmVol == 0 && ringVol == 0 && notifVol == 0)
    }

    /**
     * Start the loud attention-grabbing ringtone, aggressive vibration,
     * and system notification for an assigned booking offer.
     */
    fun startAlert(
        context: Context,
        offer: RideOffer? = null,
        ringtoneEnabled: Boolean = true,
        ringtoneId: String = "E_RIDE_ALERT_1"
    ) {
        val appContext = context.applicationContext
        val offerId = offer?.rideId ?: "offer_${System.currentTimeMillis()}"

        // If already playing for the exact same offer, do not restart
        if (isPlaying && currentOfferId == offerId) {
            return
        }

        stopAlert(appContext)
        stopPreview()

        currentOfferId = offerId
        isPlaying = true

        // 1. Post High-Priority Android System Notification
        try {
            postBookingNotification(appContext, offer)
        } catch (_: Exception) {}

        // 2. Launch Audio & Vibration Loop on Background Dispatcher
        alertJob = CoroutineScope(Dispatchers.Default).launch {
            var toneGenerator: ToneGenerator? = null
            var ringtone: Ringtone? = null

            try {
                // Initialize ToneGenerator on STREAM_ALARM (loudest) or fallback to STREAM_RING
                if (ringtoneEnabled) {
                    try {
                        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    } catch (_: Exception) {
                        try {
                            toneGenerator = ToneGenerator(AudioManager.STREAM_RING, 100)
                        } catch (_: Exception) {}
                    }

                    // Also fallback system ringtone for maximum audibility
                    try {
                        val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                        ringtone = RingtoneManager.getRingtone(appContext, ringtoneUri)?.apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                audioAttributes = AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                                    .build()
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                isLooping = true
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Vibrator setup
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                val vibratePattern = longArrayOf(0, 450, 150, 450, 150, 600)

                // Continuous alert loop until cancelled (Accept/Reject/Expiry)
                while (isActive && isPlaying) {
                    // Trigger vibration buzz
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(VibrationEffect.createWaveform(vibratePattern, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(vibratePattern, -1)
                        }
                    } catch (_: Exception) {}

                    // Play specific acoustic pattern if sound is enabled
                    if (ringtoneEnabled) {
                        playTonePattern(toneGenerator, ringtone, ringtoneId)
                    } else {
                        // Silent mode: keep vibration ticking at intervals
                        delay(1200)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { ringtone?.stop() } catch (_: Exception) {}
                try { toneGenerator?.release() } catch (_: Exception) {}
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    vibrator?.cancel()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Preview a specific built-in ringtone for 4 seconds or until stopped.
     */
    fun previewRingtone(
        context: Context,
        ringtoneId: String,
        onFinished: () -> Unit = {}
    ) {
        val appContext = context.applicationContext
        stopPreview()

        isPreviewPlaying = true
        currentlyPreviewingId = ringtoneId

        previewJob = CoroutineScope(Dispatchers.Default).launch {
            var toneGen: ToneGenerator? = null
            var ringtone: Ringtone? = null

            try {
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                } catch (_: Exception) {
                    try { toneGen = ToneGenerator(AudioManager.STREAM_RING, 95) } catch (_: Exception) {}
                }

                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtone = RingtoneManager.getRingtone(appContext, uri)
                } catch (_: Exception) {}

                // Play 2 cycles of the tone pattern (~3-4 seconds total)
                var cycles = 0
                while (isActive && isPreviewPlaying && cycles < 2) {
                    playTonePattern(toneGen, ringtone, ringtoneId)
                    cycles++
                }
            } catch (_: Exception) {
            } finally {
                try { ringtone?.stop() } catch (_: Exception) {}
                try { toneGen?.release() } catch (_: Exception) {}
                withContext(Dispatchers.Main) {
                    isPreviewPlaying = false
                    currentlyPreviewingId = null
                    onFinished()
                }
            }
        }
    }

    /**
     * Stops any currently playing preview.
     */
    fun stopPreview() {
        isPreviewPlaying = false
        currentlyPreviewingId = null
        previewJob?.cancel()
        previewJob = null
    }

    /**
     * Test booking sound playback with vibration feedback.
     */
    fun testBookingSound(
        context: Context,
        ringtoneId: String,
        onFinished: () -> Unit = {}
    ) {
        val appContext = context.applicationContext
        stopAlert(appContext)
        stopPreview()

        previewRingtone(appContext, ringtoneId) {
            onFinished()
        }

        // Add a gentle vibration pulse for test
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (_: Exception) {}
    }

    /**
     * Stop ringtone, stop vibration, and dismiss booking notification.
     */
    fun stopAlert(context: Context? = null) {
        isPlaying = false
        currentOfferId = null
        alertJob?.cancel()
        alertJob = null

        context?.let { ctx ->
            try {
                val notificationManager = ctx.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)
            } catch (_: Exception) {}
        }
    }

    /**
     * Helper to synthesize distinctive audio patterns for the 5 ringtone styles.
     */
    private suspend fun playTonePattern(
        toneGenerator: ToneGenerator?,
        ringtone: Ringtone?,
        ringtoneId: String
    ) {
        when (ringtoneId) {
            "E_RIDE_ALERT_2" -> {
                // Alert 2: Energetic Electronic Pulse (Staccato Chimes)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 180) } catch (_: Exception) {}
                delay(220)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180) } catch (_: Exception) {}
                delay(220)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PING_RING, 260) } catch (_: Exception) {}
                delay(600)
            }
            "E_RIDE_ALERT_3" -> {
                // Alert 3: Urgent Double Siren Tone
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 320) } catch (_: Exception) {}
                delay(360)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 320) } catch (_: Exception) {}
                delay(360)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 320) } catch (_: Exception) {}
                delay(650)
            }
            "E_RIDE_ALERT_4" -> {
                // Alert 4: Melodic Digital Bell (Four-note ascending melody)
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150) } catch (_: Exception) {}
                delay(180)
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150) } catch (_: Exception) {}
                delay(180)
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220) } catch (_: Exception) {}
                delay(250)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PING_RING, 280) } catch (_: Exception) {}
                delay(700)
            }
            "E_RIDE_ALERT_5" -> {
                // Alert 5: High-Frequency Emergency Beep (Triple rapid pulse)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 140) } catch (_: Exception) {}
                delay(160)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 140) } catch (_: Exception) {}
                delay(160)
                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 240) } catch (_: Exception) {}
                delay(600)
            }
            else -> {
                // Alert 1 (Default): High-Priority Classic Chime
                try {
                    if (ringtone?.isPlaying != true) {
                        ringtone?.play()
                    }
                } catch (_: Exception) {}

                try { toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 320) } catch (_: Exception) {}
                delay(400)
                try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 320) } catch (_: Exception) {}
                delay(550)
            }
        }
    }

    private fun postBookingNotification(context: Context, offer: RideOffer?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Create High Importance Notification Channel on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud audio and vibration alerts for incoming E-Ride 3 booking requests"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttr)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val fareText = if (offer != null) "₹${offer.fare.toInt()}" else "New Request"
        val earnText = if (offer != null) " • Est. Earning: ₹${offer.driverEarning.toInt()}" else ""
        val pickup = offer?.pickupLocation ?: "Assam Service Zone"
        val drop = offer?.dropoffLocation ?: "Destination"
        val distance = if (offer != null) "${offer.distanceKm} km" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚖 NEW RIDE REQUEST • $fareText$earnText")
            .setContentText("Pickup: $pickup → Drop: $drop ($distance)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("📍 Pickup: $pickup\n🏁 Drop: $drop\n💰 Fare: $fareText$earnText\n⏱ 30 seconds countdown active!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
