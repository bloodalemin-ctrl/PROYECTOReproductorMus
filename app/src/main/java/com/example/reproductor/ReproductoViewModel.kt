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

// ====================================================================
// MODELO DE DATOS REQUERIDO PARA LA BIBLIOTECA DESLIZABLE
// ====================================================================
data class Cancion(val titulo: String, val artista: String, val uri: Uri?)

class ReproductorViewModel(application: Application) : AndroidViewModel(application) {

    var exoPlayer by mutableStateOf<Player?>(null)
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    var isPlaying by mutableStateOf(false)
    var temaActual by mutableStateOf(EstadoTema())

    var currentTitle by mutableStateOf("Conectando al sistema...")
    var currentArtist by mutableStateOf("Espere por favor")

    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    private var jobProgreso: Job? = null

    // ====================================================================
    // LA LISTA REAL DE LA BIBLIOTECA: Sincronizada con Compose y Media3
    // ====================================================================
    val listaCanciones = mutableStateListOf<Cancion>()

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
            // Si el servicio ya contiene pistas cargadas, las recuperamos para inflar la biblioteca
            if (player.mediaItemCount > 0) {
                if (listaCanciones.isEmpty()) {
                    for (i in 0 until player.mediaItemCount) {
                        val item = player.getMediaItemAt(i)
                        listaCanciones.add(Cancion(
                            titulo = item.mediaMetadata.title?.toString() ?: "Pista $i",
                            artista = item.mediaMetadata.artist?.toString() ?: "Artista Desconocido",
                            uri = item.localConfiguration?.uri
                        ))
                    }
                }
                return
            }

            listaCanciones.clear()

            // 1. Añadir canción local fija de la carpeta raw
            val uriLocal = Uri.parse("android.resource://${app.packageName}/raw/cancion_retro")
            listaCanciones.add(Cancion("Pista Local Retro", "Nokia Demo", uriLocal))

            // 2. Definir lista remota de Google Drive
            val misCanciones = listOf(
                Triple("BaileEee InolvidableEeEE", "Bad Bunny", "1zpq4gIaB9VyFw41VmIjZRfnCL1ssU84r"),
                Triple("Es un secretoOOOO", "Plan B", "1S-P_hPui-qQpqkixxqBKn1iVy4JhM642"),
                Triple("Algo me gusta de ti", "Wisin y Yandel", "1ZKNzACGZpA-aaOqJdSYlW4usR3dtINvu"),
                Triple("PasarelaAAAAA", "Daddy Yankee", "14jYGHEMy56I6I2wy_H-pijuOnQqA_j0T")
            )

            misCanciones.forEach { (titulo, artista, id) ->
                val urlDirecta = Uri.parse("https://drive.google.com/uc?export=download&id=$id")
                listaCanciones.add(Cancion(titulo, artista, urlDirecta))
            }

            // Mapeamos nuestra lista de canciones internas al formato MediaItem exigido por Media3
            val mediaItems = listaCanciones.map { cancion ->
                MediaItem.Builder()
                    .setUri(cancion.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(cancion.titulo)
                            .setArtist(cancion.artista)
                            .build()
                    )
                    .build()
            }

            player.setMediaItems(mediaItems)
            player.prepare()
            player.playWhenReady = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun agregarCancionesLocales(uris: List<Uri>) {
        val player = exoPlayer ?: return

        uris.forEach { uri ->
            // Extraer un nombre legible del archivo para que la biblioteca se vea limpia
            val nombreArchivo = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "Audio Local"
            val nuevaCancion = Cancion(nombreArchivo, "Dispositivo", uri)

            listaCanciones.add(nuevaCancion)

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(nombreArchivo)
                        .setArtist("Dispositivo")
                        .build()
                )
                .build()

            player.addMediaItem(mediaItem)
        }

        player.prepare()
    }

    // ====================================================================
    // NUEVA FUNCIÓN: Ejecuta el salto directo desde el BottomSheet deslizable
    // ====================================================================
    fun reproducirCancionEnPosicion(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.prepare()
            player.play()
        }
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