@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package com.example.reproductor

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class WmpViewMode { DiscoOnly, CassetteOnly, VisualizerOnly }

@Composable
fun WmpThemeScreen(
    viewModel: ReproductorViewModel,
    onAbrirBiblioteca: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer ?: return
    val isPlaying = viewModel.isPlaying

    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration
    
    var isDraggingProgreso by remember { mutableStateOf(false) }
    var progresoLocal by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    var currentWmpView by remember { mutableStateOf(WmpViewMode.DiscoOnly) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) viewModel.agregarCancionesLocales(uris)
        }
    )

    val wmpNostalgiaBlue = Color(0xFF3864A6) 
    val wmpElectricBlue = Color(0xFF4FC3F7)  
    val metallicSilver = Color(0xFFB0bec5)   
    val metallicLight = Color(0xFFeceff1)    
    val metallicDark = Color(0xFF78909c)     
    val wmpScreenBg = Color(0xFF000511)      
    val wmpScreenBorder = Color(0xFF5D7BAA) 

    val chasisGradient = Brush.verticalGradient(
        colors = listOf(metallicLight, metallicSilver, metallicDark)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238)) 
            .navigationBarsPadding() 
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom 
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(15.dp, RoundedCornerShape(20.dp)) 
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp)) 
                .background(chasisGradient, RoundedCornerShape(20.dp))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (delta > 15f) onAbrirBiblioteca()
                    }
                )
                .padding(12.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Windows Media Player Classic", color = Color(0xFF1A237E), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                Text("v9.0", color = Color(0xFF1A237E), fontSize = 11.sp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(3.dp, wmpScreenBorder, RoundedCornerShape(10.dp)) 
                    .background(wmpScreenBg)
                    .clickable {
                        currentWmpView = when (currentWmpView) {
                            WmpViewMode.DiscoOnly -> WmpViewMode.CassetteOnly
                            WmpViewMode.CassetteOnly -> WmpViewMode.VisualizerOnly
                            WmpViewMode.VisualizerOnly -> WmpViewMode.DiscoOnly
                        }
                    }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = currentWmpView,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                    label = "WmpAnimationSwitch"
                ) { targetMode ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (targetMode) {
                            WmpViewMode.DiscoOnly -> DiscoRetroWmp(isPlaying)
                            WmpViewMode.CassetteOnly -> CassetteRetroWmp(isPlaying, wmpElectricBlue)
                            WmpViewMode.VisualizerOnly -> VisualizerOndasRetroWmp(isPlaying, wmpElectricBlue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("👆 Toca la pantalla para cambiar animación", color = Color(0xFF1A237E).copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.dp, metallicDark, RoundedCornerShape(5.dp)) 
                    .background(Color.Black.copy(0.2f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val textoBase = "${viewModel.currentTitle} - ${viewModel.currentArtist}"
                val textoInfinito = "$textoBase          •          ".repeat(10)
                Text(
                    text = textoInfinito,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val textoTiempo = if (isDraggingProgreso) (progresoLocal * duration).toLong() else currentPosition
                Text(formatTimeRetroWmp(textoTiempo), color = Color(0xFF1A237E), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                
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
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = wmpNostalgiaBlue,
                        inactiveTrackColor = Color.Black.copy(0.3f)
                    )
                )
                Text(formatTimeRetroWmp(duration), color = Color(0xFF1A237E), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconRepeat = if (viewModel.isRepeatOne) "🔂" else "🔁"
                val bgRepeat = if (viewModel.isRepeatOne) wmpNostalgiaBlue else metallicSilver 
                RetroWmpControlButton(iconRepeat, bgRepeat, Color.White, 40.dp, 16.sp) {
                    viewModel.toggleRepeat()
                    val msj = if (viewModel.isRepeatOne) "Repetir esta canción" else "Repetición apagada"
                    Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                }
                
                Spacer(Modifier.width(10.dp))

                RetroWmpControlButton("⏮", metallicSilver, wmpNostalgiaBlue, 45.dp, 16.sp) {
                    if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() else exoPlayer.seekTo(0)
                }

                Spacer(Modifier.width(15.dp))

                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(5.dp, CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color.White, wmpNostalgiaBlue)),
                            CircleShape
                        )
                        .border(2.dp, Color.White.copy(0.7f), CircleShape)
                        .clickable { viewModel.alternarReproduccion() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPlaying) "⏸" else "▶",
                        color = Color.White,
                        fontSize = 32.sp,
                        modifier = Modifier.graphicsLayer(shadowElevation = 5f)
                    )
                }

                Spacer(Modifier.width(15.dp))

                RetroWmpControlButton("⏭", metallicSilver, wmpNostalgiaBlue, 45.dp, 16.sp) {
                    if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext()
                }
                
                Spacer(Modifier.width(10.dp))
                
                val bgShuffle = if (viewModel.isShuffleEnabled) wmpNostalgiaBlue else metallicSilver 
                RetroWmpControlButton("🔀", bgShuffle, Color.White, 40.dp, 16.sp) {
                    viewModel.toggleShuffle()
                    val msj = if (viewModel.isShuffleEnabled) "Modo aleatorio encendido" else "Modo aleatorio apagado"
                    Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            ) {
                Text("🔈", color = Color(0xFF1A237E), fontSize = 16.sp)
                Slider(
                    value = viewModel.currentVolume,
                    onValueChange = { viewModel.cambiarVolumen(it) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = metallicSilver, activeTrackColor = wmpNostalgiaBlue, inactiveTrackColor = Color.Black.copy(0.3f))
                )
                Text("🔊", color = Color(0xFF1A237E), fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp)
                    .height(40.dp)
                    .shadow(2.dp, RoundedCornerShape(10.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = metallicSilver),
                border = androidx.compose.foundation.BorderStroke(1.dp, metallicDark),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("📁 CARGAR MÚSICA DEL DISPOSITIVO", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.cambiarTema() },
                    modifier = Modifier.weight(1f).height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(2.dp, metallicDark),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("MODO NOKIA", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(45.dp).shadow(4.dp, RoundedCornerShape(20.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = wmpNostalgiaBlue),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("MODO WINDOWS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

// COMPONENTES VISUALES

@Composable
fun DiscoRetroWmp(isPlaying: Boolean) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) { while (isPlaying) { delay(16); angulo += 3f; if (angulo >= 360f) angulo = 0f } }

    Box(
        modifier = Modifier.size(150.dp).graphicsLayer(rotationZ = angulo).shadow(6.dp, CircleShape).background(
                Brush.linearGradient(colors = listOf(Color(0xFFCFD8DC), Color(0xFF90A4AE), Color(0xFFECEFF1), Color(0xFF78909C), Color(0xFFCFD8DC), Color(0xFFECEFF1))), CircleShape).border(1.dp, Color.White.copy(0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(138.dp).background(Brush.radialGradient(colors = listOf(Color.Transparent, Color(0xFFB2DFDB).copy(alpha = 0.2f), Color(0xFFE1BEE7).copy(alpha = 0.2f), Color(0xFFB3E5FC).copy(alpha = 0.2f), Color.Transparent)), CircleShape).border(0.5.dp, Color.Black.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(45.dp).background(Color.White.copy(alpha = 0.15f), CircleShape).border(1.5.dp, Color(0xFF90A4AE).copy(0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(18.dp).background(Color(0xFF000511), CircleShape).border(1.dp, Color.Black, CircleShape))
            }
        }
    }
}

@Composable
fun CassetteRetroWmp(isPlaying: Boolean, colorAcento: Color) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) { while (isPlaying) { delay(16); angulo += 4f; if (angulo >= 360f) angulo = 0f } }

    Box(modifier = Modifier.size(240.dp, 130.dp).background(Color(0xFF111111), RoundedCornerShape(12.dp)).border(2.dp, colorAcento.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(200.dp, 75.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).border(1.dp, colorAcento.copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(80.dp, 30.dp).background(Color.Black, RoundedCornerShape(4.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                EngranajeRetroWmp(angulo, colorAcento)
                EngranajeRetroWmp(angulo, colorAcento)
            }
        }
    }
}

@Composable
fun EngranajeRetroWmp(angulo: Float, color: Color) {
    Box(modifier = Modifier.size(30.dp).graphicsLayer(rotationZ = angulo).background(Color.Black, CircleShape).border(2.dp, color.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.width(2.dp).height(30.dp).background(color.copy(alpha = 0.7f)))
        Box(Modifier.width(30.dp).height(2.dp).background(color.copy(alpha = 0.7f)))
        Box(Modifier.size(10.dp).background(color.copy(alpha = 0.7f), CircleShape))
    }
}

@Composable
fun VisualizerOndasRetroWmp(isPlaying: Boolean, color: Color) {
    var alturas by remember { mutableStateOf(List(20) { 5.dp }) }
    LaunchedEffect(isPlaying) { if (isPlaying) { while (true) { alturas = List(20) { (10..60).random().dp }; delay(150) } } else { alturas = List(20) { 5.dp } } }

    Row(Modifier.fillMaxWidth().height(100.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        alturas.forEach { alturaObjetivo ->
            val alturaAnimada by animateDpAsState(targetValue = alturaObjetivo, animationSpec = tween(durationMillis = if (isPlaying) 150 else 500, easing = LinearEasing), label = "eq_bar_retro")
            Box(Modifier.weight(1f).height(alturaAnimada).background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
        }
    }
}

@Composable
fun RetroWmpControlButton(text: String, bgColor: Color, textColor: Color, size: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit, onClick: () -> Unit) {
    val buttonGradient = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.1f)))

    Box(modifier = Modifier
        .size(size)
        .shadow(3.dp, CircleShape)
        .background(bgColor, CircleShape)
        .background(buttonGradient, CircleShape)
        .border(1.dp, Color.White.copy(0.5f), CircleShape)
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(text, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

fun formatTimeRetroWmp(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}