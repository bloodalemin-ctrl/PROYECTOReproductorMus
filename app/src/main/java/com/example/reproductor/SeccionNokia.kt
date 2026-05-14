package com.example.reproductor

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class NokiaViewMode { Album, Cassette, Visualizer }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SeccionNokia(viewModel: ReproductorViewModel) {
    val context = LocalContext.current

    // Conexión al Motor Central
    val exoPlayer = viewModel.exoPlayer
    val isPlaying = viewModel.isPlaying

    // Estados
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentView by remember { mutableStateOf(NokiaViewMode.Album) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
            delay(1000L)
        }
    }

    // Paleta
    val blackBackground = Color(0xFF050505)
    val redXpress = Color(0xFFCC0000)
    val redBright = Color(0xFFFF3333)
    val redDark = Color(0xFF660000)
    val grayText = Color(0xFFAAAAAA)
    val lcdScreen = Color(0xFF1A0000)

    val glossyRedButton = Brush.verticalGradient(listOf(redBright, redXpress, redDark))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(blackBackground)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // aqui todo se distribuye pata no dejar huecos
    ) {

        // --- BOCINA SUPERIOR
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(8) {
                Box(modifier = Modifier.size(width = 6.dp, height = 12.dp).background(Color(0xFF222222), CircleShape))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- PANTALLA TIPO LCD TÁCTIL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Toma el espacio disponible
                .clip(RoundedCornerShape(12.dp))
                .border(4.dp, redXpress, RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(lcdScreen, Color.Black)))
                .clickable {
                    currentView = when(currentView) {
                        NokiaViewMode.Album -> NokiaViewMode.Cassette
                        NokiaViewMode.Cassette -> NokiaViewMode.Visualizer
                        NokiaViewMode.Visualizer -> NokiaViewMode.Album
                    }
                }
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Barra iconos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FULL🔋", color = redBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("MUSICA", color = redBright, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("------", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                // CENTRO DINÁMICO
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentView,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "NokiaViewSwitch"
                    ) { target ->
                        when (target) {
                            NokiaViewMode.Album -> NokiaAlbum()
                            NokiaViewMode.Cassette -> NokiaCassette(isPlaying, redBright)
                            NokiaViewMode.Visualizer -> NokiaVisualizer(isPlaying, redBright)
                        }
                    }
                }

                // Info de la canción
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Retro ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("-------", color = grayText, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(5.dp))

                // --- slider de la cancion o carrusel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatTimeNokia(currentPosition), color = redBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = {
                            val newPos = (it * duration).toLong()
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(20.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = redBright,
                            inactiveTrackColor = redDark
                        )
                    )
                    Text(formatTimeNokia(duration), color = redBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("👆 Toca la pantalla para cambiar animación", color = grayText, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(15.dp))

        // --- Teclas de funcion
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Options", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("NOKIA", color = grayText.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Text("Back", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(15.dp))

        // --- botones de control
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(40.dp)).padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ATRASAR
            Box(
                modifier = Modifier
                    .size(width = 75.dp, height = 55.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(glossyRedButton)
                    .border(1.dp, Color(0xFFFF6666).copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                    .clickable {
                        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                        exoPlayer.seekTo(newPos)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("⏮", color = Color.White, fontSize = 22.sp)
            }

            // boton de play
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 75.dp)
                    .shadow(8.dp, RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
                    .background(glossyRedButton)
                    .border(2.dp, Color(0xFFFF6666).copy(alpha = 0.4f), RoundedCornerShape(40.dp))
                    .clickable { viewModel.alternarReproduccion() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 36.sp)
            }

            // ADELANTAR
            Box(
                modifier = Modifier
                    .size(width = 75.dp, height = 55.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(glossyRedButton)
                    .border(1.dp, Color(0xFFFF6666).copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                    .clickable {
                        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                        exoPlayer.seekTo(newPos)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("⏭", color = Color.White, fontSize = 22.sp)
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // --- CONTROL DE VOLUMEN
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp)).padding(horizontal = 15.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔈", color = grayText, fontSize = 18.sp)
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    val realVolume = (it * maxVolume).toInt()
                    try { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, realVolume, 0) } catch (_: SecurityException) {}
                },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = redBright, inactiveTrackColor = redDark)
            )
            Text("🔊", color = grayText, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(15.dp))

        // --- BOTÓN PARA CAMBIAR EL TEMA A ELEGIR (NOKIA O WINDOWS)
        Button(
            onClick = { viewModel.cambiarTema() },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("CAMBIAR A MODO WINDOWS", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- BOCINA INFERIOR
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(12) {
                Box(modifier = Modifier.size(width = 6.dp, height = 12.dp).background(Color(0xFF222222), CircleShape))
            }
        }
    }
}

// COMPONENTES EXCLUSIVOS PARA NOKIA
@Composable
fun NokiaAlbum() {
    Box(
        modifier = Modifier.size(140.dp).background(Color(0xFF111111), RoundedCornerShape(8.dp)).border(2.dp, Color(0xFF660000), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("💿", fontSize = 70.sp)
    }
}

@Composable
fun NokiaVisualizer(isPlaying: Boolean, color: Color) {
    var alturas by remember { mutableStateOf(List(12) { 5.dp }) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                alturas = List(12) { (10..100).random().dp }
                delay(200)
            }
        } else { alturas = List(12) { 5.dp } }
    }
    Row(modifier = Modifier.height(100.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        alturas.forEach { alturaObjetivo ->
            val alturaAnimada by animateDpAsState(targetValue = alturaObjetivo, animationSpec = tween(durationMillis = if (isPlaying) 200 else 500, easing = LinearEasing), label = "nokia_eq")
            Box(Modifier.width(12.dp).height(alturaAnimada).background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        }
    }
}

@Composable
fun NokiaCassette(isPlaying: Boolean, colorAcento: Color) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(16)
            angulo += 4f
            if (angulo >= 360f) angulo = 0f
        }
    }
    Box(
        modifier = Modifier.size(220.dp, 130.dp).background(Color(0xFF111111), RoundedCornerShape(12.dp)).border(2.dp, colorAcento.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(180.dp, 70.dp).background(Color(0xFF222222), RoundedCornerShape(8.dp)).border(1.dp, colorAcento, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(90.dp, 35.dp).background(Color.Black, RoundedCornerShape(4.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                NokiaEngranaje(angulo, colorAcento)
                NokiaEngranaje(angulo, colorAcento)
            }
        }
    }
}

@Composable
fun NokiaEngranaje(angulo: Float, color: Color) {
    Box(
        modifier = Modifier.size(30.dp).graphicsLayer(rotationZ = angulo).background(Color.Black, CircleShape).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(2.dp).height(30.dp).background(color))
        Box(Modifier.width(30.dp).height(2.dp).background(color))
        Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}

fun formatTimeNokia(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}