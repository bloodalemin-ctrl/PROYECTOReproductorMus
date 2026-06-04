package com.example.reproductor

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
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

// ====================================================================
// LISTA DE MODOS DISPONIBLES
// ====================================================================
enum class TipoModo(val titulo: String, val icono: String) {
    NOKIA("Nokia XpressMusic", "📱"),
    WINDOWS("Windows Media Player", "💽"),
    CLASSIC_POD("Classic Pod (Próximamente)", "🎧"),
    GAMMING("GammingModo (Próximamente)", "🕹️")
}

data class EstadoTema(val modo: TipoModo = TipoModo.NOKIA)

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

    var currentVolume by mutableFloatStateOf(1f)
    var isShuffleEnabled by mutableStateOf(false)
    var isRepeatOne by mutableStateOf(false)

    private val sharedPreferences = application.getSharedPreferences("BibliotecaPrefs", Context.MODE_PRIVATE)

    val listaCanciones = mutableStateListOf<Cancion>()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                if (maxVol > 0) {
                    currentVolume = vol / maxVol
                }
            }
        }
    }

    init {
        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        currentVolume = if (maxVol > 0) currentVol / maxVol else 1f

        application.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

        val sessionToken = SessionToken(application, ComponentName(application, ReproductorService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            exoPlayer = controller
            setupPlayer(controller)
            cargarPlaylistHibrida(application, controller)

            isPlaying = controller.isPlaying
            duration = controller.duration.coerceAtLeast(0L)
            currentPosition = controller.currentPosition

            isShuffleEnabled = controller.shuffleModeEnabled
            isRepeatOne = controller.repeatMode == Player.REPEAT_MODE_ONE

            if (controller.mediaMetadata.title != null) {
                currentTitle = controller.mediaMetadata.title.toString()
                currentArtist = controller.mediaMetadata.artist?.toString() ?: "Artista Desconocido"
            }

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

    private fun arrancarRelojProgreso(player: Player) {
        detenerRelojProgreso()
        jobProgreso = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
                delay(30L)
            }
        }
    }

    private fun detenerRelojProgreso() {
        jobProgreso?.cancel()
        jobProgreso = null
    }

    private fun cargarPlaylistHibrida(app: Application, player: Player) {
        try {
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

            val uriLocal = Uri.parse("android.resource://${app.packageName}/raw/cancion_retro")
            listaCanciones.add(Cancion("Pista Local Retro", "Nokia Demo", uriLocal))

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

            val persistentes = cargarCancionesLocalesPersistentes()
            persistentes.forEach { item ->
                listaCanciones.add(Cancion(
                    titulo = item.mediaMetadata.title?.toString() ?: "Local",
                    artista = item.mediaMetadata.artist?.toString() ?: "Dispositivo",
                    uri = item.localConfiguration?.uri
                ))
            }

            val mediaItems = listaCanciones.map { cancion ->
                MediaItem.Builder()
                    .setUri(cancion.uri!!)
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
        val posicionDeInsercion = player.mediaItemCount
        guardarCancionesLocalesPersistentes(uris)

        uris.forEach { uri ->
            val nombreArchivo = obtenerNombreArchivo(uri)
            val nuevaCancion = Cancion(nombreArchivo, "Memoria del Teléfono", uri)
            listaCanciones.add(nuevaCancion)

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(nombreArchivo)
                        .setArtist("Memoria del Teléfono")
                        .build()
                )
                .build()

            player.addMediaItem(mediaItem)
        }

        player.seekTo(posicionDeInsercion, 0L)
        player.prepare()
        player.play()
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "Audio Local"
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    nombre = it.getString(index)
                    nombre = nombre.substringBeforeLast(".")
                }
            }
        }
        return nombre
    }

    private fun guardarCancionesLocalesPersistentes(uris: List<Uri>) {
        val contenidoResolver = getApplication<Application>().contentResolver
        val conjuntoExistente = sharedPreferences.getStringSet("lista_uris", emptySet()) ?: emptySet()
        val nuevoConjunto = conjuntoExistente.toMutableSet()

        uris.forEach { uri ->
            try {
                contenidoResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                nuevoConjunto.add(uri.toString())
            } catch (e: Exception) { e.printStackTrace() }
        }
        sharedPreferences.edit().putStringSet("lista_uris", nuevoConjunto).apply()
    }

    private fun cargarCancionesLocalesPersistentes(): List<MediaItem> {
        val conjuntoUris = sharedPreferences.getStringSet("lista_uris", emptySet()) ?: emptySet()
        val listaItems = mutableListOf<MediaItem>()

        conjuntoUris.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val nombreReal = obtenerNombreArchivo(uri)
                val metadatos = MediaMetadata.Builder().setTitle(nombreReal).setArtist("Memoria del Teléfono").build()
                val item = MediaItem.Builder().setUri(uri).setMediaMetadata(metadatos).build()
                listaItems.add(item)
            } catch (e: Exception) { e.printStackTrace() }
        }
        return listaItems
    }

    fun cambiarVolumen(nuevoVolumen: Float) {
        currentVolume = nuevoVolumen
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (nuevoVolumen * maxVol).toInt(), 0)
    }

    fun alternarReproduccion() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        isShuffleEnabled = !isShuffleEnabled
        player.shuffleModeEnabled = isShuffleEnabled
    }

    fun toggleRepeat() {
        val player = exoPlayer ?: return
        isRepeatOne = !isRepeatOne
        player.repeatMode = if (isRepeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
    }

    fun reproducirCancionEnPosicion(index: Int) {
        val player = exoPlayer ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.prepare()
            player.play()
        }
    }

    // NUEVA FUNCIÓN QUE REEMPLAZA A LA ANTERIOR
    fun cambiarModo(nuevoModo: TipoModo) {
        temaActual = EstadoTema(modo = nuevoModo)
    }

    override fun onCleared() {
        super.onCleared()
        detenerRelojProgreso()
        getApplication<Application>().unregisterReceiver(volumeReceiver)
        MediaController.releaseFuture(controllerFuture)
    }
}