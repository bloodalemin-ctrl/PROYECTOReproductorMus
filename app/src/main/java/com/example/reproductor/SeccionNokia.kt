@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
package com.example.reproductor

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NokiaViewMode { Album, Cassette, Visualizer }

@Composable
fun SeccionNokia(
    viewModel: ReproductorViewModel,
    onAbrirBiblioteca: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer ?: return
    val isPlaying = viewModel.isPlaying

    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration

    var currentView by remember { mutableStateOf(NokiaViewMode.Album) }
    var mostrarMenu by remember { mutableStateOf(false) }
    
    var isDraggingProgreso by remember { mutableStateOf(false) }
    var progresoLocal by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope() 

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) viewModel.agregarCancionesLocales(uris)
        }
    )

    val blackBackground = Color(0xFF050505)
    val redXpress = Color(0xFFCC0000)
    val redBright = Color(0xFFFF3333)
    val redDark = Color(0xFF660000)
    val grayText = Color(0xFFAAAAAA)
    val lcdScreen = Color(0xFF1A0000)
    val glossyRedButton = Brush.verticalGradient(listOf(redBright, redXpress, redDark))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(blackBackground)
                .navigationBarsPadding()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (delta > 15f) onAbrirBiblioteca()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(8) { Box(modifier = Modifier.size(width = 6.dp, height = 12.dp).background(Color(0xFF222222), CircleShape)) }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).border(4.dp, redXpress, RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(lcdScreen, Color.Black)))
                    .clickable {
                        currentView = when(currentView) {
                            NokiaViewMode.Album -> NokiaViewMode.Cassette
                            NokiaViewMode.Cassette -> NokiaViewMode.Visualizer
                            NokiaViewMode.Visualizer -> NokiaViewMode.Album
                        }
                    }.padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(" 🔋", color = redBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("MUSIC", color = redBright, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("00:00", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = currentView, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "NokiaViewSwitch") { target ->
                            when (target) {
                                NokiaViewMode.Album -> NokiaAlbum()
                                NokiaViewMode.Cassette -> NokiaCassette(isPlaying, redBright)
                                NokiaViewMode.Visualizer -> NokiaVisualizer(isPlaying, redBright)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.currentTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(viewModel.currentArtist, color = grayText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val textoTiempo = if (isDraggingProgreso) (progresoLocal * duration).toLong() else currentPosition
                        Text(formatTimeNokia(textoTiempo), color = redBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        
                        Slider(
                            value = if (isDraggingProgreso) progresoLocal else if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { 
                                isDraggingProgreso = true
                                progresoLocal = it 
                            },
                            onValueChangeFinished = {
                                val nuevaPosicion = (progresoLocal * duration).toLong()
                                viewModel.currentPosition = nuevaPosicion 
                                exoPlayer.seekTo(nuevaPosicion)
                                coroutineScope.launch {
                                    delay(200)
                                    isDraggingProgreso = false
                                }
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(20.dp),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = redBright, inactiveTrackColor = redDark)
                        )
                        Text(formatTimeNokia(duration), color = redBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("👆 Toca la pantalla para cambiar animación", color = grayText, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(15.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Options", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { mostrarMenu = true })
                Text("NOKIA", color = grayText.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("Back", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(40.dp)).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconRepeat = if (viewModel.isRepeatOne) "🔂" else "🔁"
                val bgRepeat = if (viewModel.isRepeatOne) redDark else Color.Black 
                val borderRepeat = if (viewModel.isRepeatOne) redBright else Color.DarkGray 
                Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(bgRepeat).border(1.dp, borderRepeat, CircleShape)
                    .clickable { 
                        viewModel.toggleRepeat()
                        val msj = if (viewModel.isRepeatOne) "Repetir esta canción" else "Repetición apagada"
                        Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                    },
                    contentAlignment = Alignment.Center) { Text(iconRepeat, fontSize = 18.sp) }

                Box(modifier = Modifier.size(55.dp, 50.dp).clip(RoundedCornerShape(25.dp)).background(glossyRedButton).border(1.dp, Color(0xFFFF6666).copy(0.5f), RoundedCornerShape(25.dp))
                    .clickable { if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() else exoPlayer.seekTo(0) },
                    contentAlignment = Alignment.Center) { Text("⏮", color = Color.White, fontSize = 18.sp) }

                Box(modifier = Modifier.size(80.dp, 60.dp).shadow(8.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(glossyRedButton).border(2.dp, Color(0xFFFF6666).copy(0.4f), RoundedCornerShape(30.dp))
                    .clickable { viewModel.alternarReproduccion() },
                    contentAlignment = Alignment.Center) { Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 30.sp) }

                Box(modifier = Modifier.size(55.dp, 50.dp).clip(RoundedCornerShape(25.dp)).background(glossyRedButton).border(1.dp, Color(0xFFFF6666).copy(0.5f), RoundedCornerShape(25.dp))
                    .clickable { if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext() },
                    contentAlignment = Alignment.Center) { Text("⏭", color = Color.White, fontSize = 18.sp) }
                    
                val iconShuffle = "🔀"
                val bgShuffle = if (viewModel.isShuffleEnabled) redDark else Color.Black 
                val borderShuffle = if (viewModel.isShuffleEnabled) redBright else Color.DarkGray 
                Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(bgShuffle).border(1.dp, borderShuffle, CircleShape)
                    .clickable { 
                        viewModel.toggleShuffle()
                        val msj = if (viewModel.isShuffleEnabled) "Modo aleatorio encendido" else "Modo aleatorio apagado"
                        Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                    },
                    contentAlignment = Alignment.Center) { Text(iconShuffle, fontSize = 18.sp) }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(20.dp)).padding(horizontal = 15.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔈", color = grayText, fontSize = 18.sp)
                Slider(
                    value = viewModel.currentVolume,
                    onValueChange = { viewModel.cambiarVolumen(it) },
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = redBright, inactiveTrackColor = redDark)
                )
                Text("🔊", color = grayText, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("📁 CARGAR MÚSICA DEL DISPOSITIVO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = redXpress),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("MODO NOKIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.cambiarTema() },
                    modifier = Modifier.weight(1f).height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF222222)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("MODO WINDOWS", color = grayText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(12) { Box(modifier = Modifier.size(width = 6.dp, height = 12.dp).background(Color(0xFF222222), CircleShape)) } }
        }

        if (mostrarMenu) {
            Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).padding(30.dp)) {
                Text("MENU", color = redBright, fontWeight = FontWeight.Bold, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "🎵 Pistas",
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mostrarMenu = false
                            onAbrirBiblioteca()
                        }
                        .padding(vertical = 15.dp),
                    fontSize = 18.sp
                )
                HorizontalDivider(color = Color.DarkGray)

                Text(
                    text = "❌ Cerrar Menú",
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarMenu = false }
                        .padding(vertical = 15.dp),
                    fontSize = 18.sp
                )
                HorizontalDivider(color = Color.DarkGray)
            }
        }
    }
}

@Composable fun NokiaAlbum() { Box(modifier = Modifier.size(140.dp).background(Color(0xFF111111), RoundedCornerShape(8.dp)).border(2.dp, Color(0xFF660000), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("💿", fontSize = 70.sp) } }

@Composable
fun NokiaVisualizer(isPlaying: Boolean, color: Color) {
    var alturas by remember { mutableStateOf(List(12) { 5.dp }) }
    LaunchedEffect(isPlaying) { if (isPlaying) { while (true) { alturas = List(12) { (10..100).random().dp }; delay(200) } } else { alturas = List(12) { 5.dp } } }
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
    LaunchedEffect(isPlaying) { while (isPlaying) { delay(16); angulo += 4f; if (angulo >= 360f) angulo = 0f } }
    Box(modifier = Modifier.size(220.dp, 130.dp).background(Color(0xFF111111), RoundedCornerShape(12.dp)).border(2.dp, colorAcento.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(180.dp, 70.dp).background(Color(0xFF222222), RoundedCornerShape(8.dp)).border(1.dp, colorAcento, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(90.dp, 35.dp).background(Color.Black, RoundedCornerShape(4.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                NokiaEngranaje(angulo, colorAcento); NokiaEngranaje(angulo, colorAcento)
            }
        }
    }
}

@Composable fun NokiaEngranaje(angulo: Float, color: Color) { Box(modifier = Modifier.size(30.dp).graphicsLayer(rotationZ = angulo).background(Color.Black, CircleShape).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) { Box(Modifier.width(2.dp).height(30.dp).background(color)); Box(Modifier.width(30.dp).height(2.dp).background(color)); Box(Modifier.size(10.dp).background(color, CircleShape)) } }
fun formatTimeNokia(ms: Long): String { if (ms < 0) return "00:00"; val totalSeconds = ms / 1000; return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60) }