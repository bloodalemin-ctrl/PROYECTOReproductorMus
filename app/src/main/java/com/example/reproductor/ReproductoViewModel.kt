

package com.example.reproductor

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*

data class EstadoTema(val esNokia: Boolean = false)

class ReproductorViewModel(application: Application) : AndroidViewModel(application) {

    var exoPlayer by mutableStateOf<Player?>(null)
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    var isPlaying by mutableStateOf(false)
    var temaActual by mutableStateOf(EstadoTema())

    var currentTitle by mutableStateOf("Conectando al sistema...")
    var currentArtist by mutableStateOf("Espere por favor")

    // ====================================================================
    // NUEVAS VARIABLES DE ESTADO: Controladas directamente por el ViewModel
    // ====================================================================
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    private var jobProgreso: Job? = null
    // ====================================================================

    init {
        val sessionToken = SessionToken(application, ComponentName(application, ReproductorService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            exoPlayer = controller
            setupPlayer(controller)
            cargarPlaylistHibrida(application, controller)

            // Sincronizar estado actual de inmediato al conectar o reconectar
            isPlaying = controller.isPlaying
            duration = controller.duration.coerceAtLeast(0L)
            currentPosition = controller.currentPosition

            if (controller.mediaMetadata.title != null) {
                currentTitle = controller.mediaMetadata.title.toString()
                currentArtist = controller.mediaMetadata.artist?.toString() ?: "Artista Desconocido"
            }

            // Arrancar el reloj si ya venía reproduciéndose de fondo
            if (isPlaying) arrancarRelojProgreso(controller)

        }, ContextCompat.getMainExecutor(application))
    }

    private fun setupPlayer(player: Player) {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    arrancarRelojProgreso(player)
                } else {
                    detenerRelojProgreso()
                }
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                currentTitle = mediaMetadata.title?.toString() ?: "Pista Desconocida"
                currentArtist = mediaMetadata.artist?.toString() ?: "Artista Desconocido"
                duration = player.duration.coerceAtLeast(0L)
            }
            override fun onPlayerError(error: PlaybackException) {
                currentTitle = "Error de conexión"
                currentArtist = "Revisa tu internet o recursos"
                isPlaying = false
                detenerRelojProgreso()
            }
        })
    }

    // Corrutina que actualiza los segundos en tiempo real sin congelarse
    private fun arrancarRelojProgreso(player: Player) {
        detenerRelojProgreso()
        jobProgreso = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    private fun detenerRelojProgreso() {
        jobProgreso?.cancel()
        jobProgreso = null
    }

    private fun cargarPlaylistHibrida(app: Application, player: Player) {
        try {
            if (player.mediaItemCount > 0) return

            val uriLocal = "android.resource://${app.packageName}/raw/cancion_retro"
            val localMeta = MediaMetadata.Builder().setTitle("Pista Local Retro").setArtist("Nokia Demo").build()
            val localItem = MediaItem.Builder().setUri(uriLocal).setMediaMetadata(localMeta).build()

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

            player.setMediaItems(playlistFinal)
            player.prepare()
            player.playWhenReady = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun agregarCancionesLocales(uris: List<Uri>) {
        val player = exoPlayer ?: return
        val posicionDeInsercion = player.mediaItemCount
        val nuevosItems = uris.map { uri -> MediaItem.Builder().setUri(uri).build() }

        player.addMediaItems(nuevosItems)
        player.seekTo(posicionDeInsercion, 0L)
        player.prepare()
        player.play()
    }

    fun alternarReproduccion() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun cambiarTema() {
        temaActual = EstadoTema(esNokia = !temaActual.esNokia)
    }

    override fun onCleared() {
        super.onCleared()
        detenerRelojProgreso()
        MediaController.releaseFuture(controllerFuture)
    }
}