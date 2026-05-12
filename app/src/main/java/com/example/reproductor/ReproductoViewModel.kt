package com.example.reproductor

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class ReproductorViewModel(application: Application) : AndroidViewModel(application) {

    // Motor Media3 (VLC style)
    val player = ExoPlayer.Builder(application).build()

    var skinActual by mutableStateOf<Skin>(Skin.WMP)
        private set

    var estaReproduciendo by mutableStateOf(false)
        private set

    init {

        val item = MediaItem.fromUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
        player.setMediaItem(item)
        player.prepare()
    }

    fun alternarReproduccion() {
        if (player.isPlaying) player.pause() else player.play()
        estaReproduciendo = player.isPlaying
    }

    fun cambiarSkin() {
        skinActual = if (skinActual is Skin.WMP) Skin.Nokia else Skin.WMP
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}