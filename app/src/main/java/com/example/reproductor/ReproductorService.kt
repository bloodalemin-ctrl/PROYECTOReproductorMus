package com.example.reproductor

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class ReproductorService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Construimos el reproductor que vivirá en el fondo
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Pausa la música automáticamente si te entra una llamada
            )
            .build()

        // ====================================================================
        // NUEVO: Configuración para redireccionar a la app al tocar la notificación
        // ====================================================================
        val intentAMainActivity = Intent(this, MainActivity::class.java)

        val pendingIntentAlMainActivity = PendingIntent.getActivity(
            this,
            0,
            intentAMainActivity,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // ====================================================================

        // 2. Creamos la sesión e inyectamos la actividad de redirección con .setSessionActivity
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntentAlMainActivity) // <--- Conecta el clic a tu MainActivity
            .build()
    }

    // Android usa esto para conectar los controles de la pantalla de bloqueo
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // Limpieza total de memoria al cerrar la app por completo
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}