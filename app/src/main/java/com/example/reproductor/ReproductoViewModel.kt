package com.example.reproductor

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class ReproductorViewModel(application: Application) : AndroidViewModel(application) {

    var temaActual by mutableStateOf<ModoEstilo>(ModoEstilo.WMP)
        private set

    // EL MOTOR GLOBAL
    val exoPlayer = ExoPlayer.Builder(application).build().apply {
        val mediaItem = MediaItem.fromUri(
            Uri.parse("android.resource://${application.packageName}/raw/cancion_retro")
        )
        setMediaItem(mediaItem)
        prepare()
        repeatMode = ExoPlayer.REPEAT_MODE_ONE
    }

    var isPlaying by mutableStateOf(false)
        private set

    fun cambiarTema() {
        temaActual = if (temaActual is ModoEstilo.WMP) ModoEstilo.Nokia else ModoEstilo.WMP
    }

    fun alternarReproduccion() {
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        isPlaying = !isPlaying
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}