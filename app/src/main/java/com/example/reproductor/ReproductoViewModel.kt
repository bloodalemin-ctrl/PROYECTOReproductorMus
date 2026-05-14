package com.example.reproductor

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer


data class EstadoTema(val esNokia: Boolean = false)

class ReproductorViewModel(application: Application) : AndroidViewModel(application) {
    val exoPlayer = ExoPlayer.Builder(application).build()

    var isPlaying by mutableStateOf(false)

    // variable del mainactivity
    var temaActual by mutableStateOf(EstadoTema())

    // Variables compartidas para mostrar los nombres de las canciones
    var currentTitle by mutableStateOf("Cargando lista...")
    var currentArtist by mutableStateOf("Espere por favor")

    init {
        setupPlayer()
        cargarPlaylistHibrida(application)
    }

    private fun setupPlayer() {
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                currentTitle = mediaMetadata.title?.toString() ?: "Pista Desconocida"
                currentArtist = mediaMetadata.artist?.toString() ?: "Artista Desconocido"
            }
            override fun onPlayerError(error: PlaybackException) {
                currentTitle = "Error de conexión"
                currentArtist = "Revisa tu internet o recursos"
                isPlaying = false
            }
        })
    }

    private fun cargarPlaylistHibrida(app: Application) {
        try {
            // canncion desde la carpeta raw
            val uriLocal = "android.resource://${app.packageName}/raw/cancion_retro"
            val localMeta = MediaMetadata.Builder().setTitle("Pista Local Retro").setArtist("Nokia Demo").build()
            val localItem = MediaItem.Builder().setUri(uriLocal).setMediaMetadata(localMeta).build()

            // La lista de internet setlistt
            val misCanciones = listOf(
                Triple("BaileEee InolvidableEeEE", "Bad Bunny", "1zpq4gIaB9VyFw41VmIjZRfnCL1ssU84r"),
                Triple("Es un secretoOOOO", "Plan B", "1S-P_hPui-qQpqkixxqBKn1iVy4JhM642"),
                Triple("Algo me gusta de ti", "Wisin y Yandel", "1ZKNzACGZpA-aaOqJdSYlW4usR3dtINvu"),
                Triple("PasarelaAAAAA", "Daddy Yankee", "14jYGHEMy56I6I2wy_H-pijuOnQqA_j0T")
            )

            val listaInternet = misCanciones.map { (titulo, artista, id) ->
                val urlDirecta = "https://drive.google.com/uc?export=download&id=$id"
                val metadatos = MediaMetadata.Builder().setTitle(titulo).setArtist(artista).build()
                MediaItem.Builder().setUri(urlDirecta).setMediaMetadata(metadatos).build()
            }

            val playlistFinal = mutableListOf(localItem)
            playlistFinal.addAll(listaInternet)

            exoPlayer.setMediaItems(playlistFinal)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun alternarReproduccion() {
        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    // Función para cambiar de pantalla
    fun cambiarTema() {
        temaActual = EstadoTema(esNokia = !temaActual.esNokia)
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}