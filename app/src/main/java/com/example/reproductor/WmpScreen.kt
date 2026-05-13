@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
package com.example.reproductor

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

// --- IMPORTACIONES NUEVAS PARA EL CICLO DE VIDA ---
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class PlayerView { Album, Cassette, Visualizer }

@Composable
fun WmpThemeScreen() {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(
                Uri.parse("android.resource://${context.packageName}/raw/cancion_retro")
            )
            setMediaItem(mediaItem)
            prepare()
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentView by remember { mutableStateOf(PlayerView.Cassette) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // --- MAGIA DEL VOLUMEN REAL DEL CELULAR ---
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)

            // Actualizamos el slider por si cambiaste el volumen con los botones físicos del celular
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume

            delay(1000L)
        }
    }

    // --- CORRECCIÓN DE COPILOT: PAUSAR AUDIO AL MINIMIZAR LA APP ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
                isPlaying = false // Cambiamos el botón a pausa automáticamente
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    val wmpGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2B5E91), Color(0xFF0D253F))
    )
    val silverColor = Color(0xFFE0E0E0)
    val accentBlue = Color(0xFF4FC3F7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A15))
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- ZONA VISUAL ---
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                label = "ViewSwitch"
            ) { target ->
                when (target) {
                    PlayerView.Album -> AlbumPlaceholder()
                    PlayerView.Cassette -> CassetteRealistico(isPlaying)
                    PlayerView.Visualizer -> VisualizerOndas(isPlaying)
                }
            }

            IconButton(
                onClick = {
                    currentView = when(currentView) {
                        PlayerView.Album -> PlayerView.Cassette
                        PlayerView.Cassette -> PlayerView.Visualizer
                        PlayerView.Visualizer -> PlayerView.Album
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).background(Color.White.copy(0.1f), CircleShape)
            ) {
                Text("🔄", color = Color.White)
            }
        }

        // --- PANEL DE CONTROL ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(wmpGradient)
                .border(2.dp, silverColor.copy(0.3f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎵 Pista Retro - Desconocido",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), color = silverColor, fontSize = 12.sp)
                    Text(formatTime(duration), color = silverColor, fontSize = 12.sp)
                }

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = {
                        val newPos = (it * duration).toLong()
                        exoPlayer.seekTo(newPos)
                        currentPosition = newPos
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = accentBlue,
                        inactiveTrackColor = Color.Black.copy(0.5f)
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                    WmpSimpleButton("⏮", silverColor) { exoPlayer.seekTo(0) }
                    Spacer(Modifier.width(25.dp))
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .background(Color.White.copy(0.1f), CircleShape)
                            .border(3.dp, accentBlue, CircleShape)
                            .clickable { isPlaying = !isPlaying },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPlaying) "⏸" else "▶", color = accentBlue, fontSize = 35.sp)
                    }
                    Spacer(Modifier.width(25.dp))
                    WmpSimpleButton("⏭", silverColor) { /* Siguiente */ }
                }

                // BARRA DE VOLUMEN REAL
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("🔈", color = silverColor, fontSize = 16.sp)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            // Conecta el slider con el hardware del celular
                            val realVolume = (it * maxVolume).toInt()
                            try {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, realVolume, 0)
                            } catch (_: SecurityException) {
                                // Ignorar en dispositivos/restricciones donde DND impide cambiar el volumen.
                            }
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = silverColor,
                            activeTrackColor = silverColor,
                            inactiveTrackColor = Color.Black.copy(0.5f)
                        )
                    )
                    Text("🔊", color = silverColor, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- SUB-VISTAS CORREGIDAS ---

@Composable
fun CassetteRealistico(isPlaying: Boolean) {
    var angulo by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(16)
            angulo += 4f
            if (angulo >= 360f) angulo = 0f
        }
    }

    Box(
        modifier = Modifier
            .size(280.dp, 180.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF333333), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(240.dp, 100.dp)
                .background(Color(0xFFE8DCC4), RoundedCornerShape(8.dp))
                .border(1.dp, Color.Black.copy(0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp, 40.dp)
                    .background(Color(0xFF111111), RoundedCornerShape(4.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(4.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Engranaje(angulo)
                Engranaje(angulo)
            }
        }
    }
}

@Composable
fun Engranaje(angulo: Float) {
    Box(
        modifier = Modifier
            .size(35.dp)
            .graphicsLayer(rotationZ = angulo)
            .background(Color.White, CircleShape)
            .border(4.dp, Color.DarkGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(4.dp).height(35.dp).background(Color.DarkGray))
        Box(Modifier.width(35.dp).height(4.dp).background(Color.DarkGray))
        Box(Modifier.size(12.dp).background(Color.DarkGray, CircleShape))
    }
}

@Composable
fun VisualizerOndas(isPlaying: Boolean) {
    var alturas by remember { mutableStateOf(List(12) { 10.dp }) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                alturas = List(12) { (30..150).random().dp }
                delay(250)
            }
        } else {
            alturas = List(12) { 10.dp }
        }
    }

    Row(
        Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        alturas.forEach { alturaObjetivo ->
            val alturaAnimada by animateDpAsState(
                targetValue = alturaObjetivo,
                animationSpec = tween(durationMillis = if (isPlaying) 250 else 500, easing = LinearEasing),
                label = "eq_bar"
            )
            Box(
                Modifier
                    .width(16.dp)
                    .height(alturaAnimada)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0277BD))),
                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    )
            )
        }
    }
}

@Composable
fun AlbumPlaceholder() {
    Box(Modifier.size(240.dp).background(Color.DarkGray, RoundedCornerShape(12.dp)).border(2.dp, Color(0xFF4FC3F7), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text("🎵 PORTADA", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WmpSimpleButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(50.dp).background(Color.White.copy(0.05f), CircleShape).border(1.dp, color.copy(0.5f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 18.sp)
    }
}

fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(showSystemUi = true)
@Composable
fun FinalPreview() { WmpThemeScreen() }