
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

data class EstadoTema(val esNokia: Boolean = false)

class ReproductorViewModel(application: Application) : AndroidViewModel(application) {

    var exoPlayer by mutableStateOf<Player?>(null)
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    var isPlaying by mutableStateOf(false)
    var temaActual by mutableStateOf(EstadoTema())

    var currentTitle by mutableStateOf("Conectando al sistema...")
    var currentArtist by mutableStateOf("Espere por favor")

    // Control de volumen sincronizado
    var currentVolume by mutableFloatStateOf(1f)

    // NUEVAS VARIABLES DE ESTADO: Controladas directamente por el ViewModel

    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    private var jobProgreso: Job? = null
    // ====================================================================

    // VAR DE ALMACENAMIENTO PERSISTENTE
    private val sharedPreferences = application.getSharedPreferences("BibliotecaPrefs", Context.MODE_PRIVATE)


    // Interceptor de botones físicos del celular

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
        // Inicializamos el volumen leyendo cómo está el celular en ese momento
        val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        currentVolume = if (maxVol > 0) currentVol / maxVol else 1f

        // Registramos el interceptor en Android para escuchar la acción de subir/bajar volumen
        application.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

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

            // Inyecta las canciones mp3 cargadas al arrancar la App a la cola
            playlistFinal.addAll(cargarCancionesLocalesPersistentes())

            player.setMediaItems(playlistFinal)
            player.prepare()
            player.playWhenReady = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Funcion para extraer el nombre real del archivo mp3
    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "Archivo Local"
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

    fun agregarCancionesLocales(uris: List<Uri>) {
        val player = exoPlayer ?: return
        val posicionDeInsercion = player.mediaItemCount

        // Para guardar las caniones siempre en la cola
        guardarCancionesLocalesPersistentes(uris)

        // Se inyecta el titulo extraido
        val nuevosItems = uris.map { uri ->
            val nombreReal = obtenerNombreArchivo(uri)
            val metadatos = MediaMetadata.Builder()
                .setTitle(nombreReal)
                .setArtist("Memoria del Teléfono")
                .build()
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(metadatos)
                .build()
        }

        player.addMediaItems(nuevosItems)
        player.seekTo(posicionDeInsercion, 0L)
        player.prepare()
        player.play()
    }

    // Funcion que hace que Android no olvide los permisos temporales y los haga permanentes
    private fun guardarCancionesLocalesPersistentes(uris: List<Uri>) {
        val contenidoResolver = getApplication<Application>().contentResolver
        val conjuntoExistente = sharedPreferences.getStringSet("lista_uris", emptySet()) ?: emptySet()
        val nuevoConjunto = conjuntoExistente.toMutableSet()

        uris.forEach { uri ->
            try {
                // Con esto se le pide a Android que nos de acceso permanente de lectura
                contenidoResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                nuevoConjunto.add(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                val metadatos = MediaMetadata.Builder()
                    .setTitle(nombreReal)
                    .setArtist("Memoria del Teléfono")
                    .build()
                val item = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(metadatos)
                    .build()
                listaItems.add(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return listaItems
    }

    // Funcion para cambiar el volumen
    fun cambiarVolumen(nuevoVolumen: Float) {
        currentVolume = nuevoVolumen
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Le mandamos la instrucción directa a la tarjeta de sonido del celular para modificar el hardware
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (nuevoVolumen * maxVol).toInt(), 0)
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
        // Apagamos el interceptor al destruir el ViewModel para evitar fugas de memoria
        getApplication<Application>().unregisterReceiver(volumeReceiver)
        MediaController.releaseFuture(controllerFuture)
    }
}

