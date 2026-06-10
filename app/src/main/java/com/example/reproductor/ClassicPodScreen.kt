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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class CoquetteViewMode { Disco, Cassette, Visualizer }

@Composable
fun ClassicPodScreen(
    viewModel: ReproductorViewModel,
    onAbrirBiblioteca: () -> Unit,
    onAbrirModos: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer ?: return
    val isPlaying = viewModel.isPlaying

    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration

    var isDraggingProgreso by remember { mutableStateOf(false) }
    var progresoLocal by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    var currentView by remember { mutableStateOf(CoquetteViewMode.Disco) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) viewModel.agregarCancionesLocales(uris)
        }
    )

    // Paleta de Colores Coquette
    val rosaPastelFondo = Color(0xFFFFC0CB)
    val rosaChicleMedio = Color(0xFFFF85A2)
    val rosaFucsiaChasis = Color(0xFFF50057)
    val rosaOscuroBorde = Color(0xFFC2185B)
    val blancoClickWheel = Color(0xFFF9F9F9)
    val grisIconosWheel = Color(0xFF8E8E93)
    val wmpScreenBg = Color(0xFF0D0206)

    val gradientePantallaGeneral = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF0F5), rosaPastelFondo)
    )

    val chasisGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFF94B9), rosaChicleMedio, rosaFucsiaChasis)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientePantallaGeneral)
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(25.dp, RoundedCornerShape(24.dp))
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .background(chasisGradient, RoundedCornerShape(24.dp))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (delta > 15f) onAbrirBiblioteca()
                    }
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // CABECERA ESTILO IPOD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 10.dp, top = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("iPod Nano", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                Text("🌸", fontSize = 12.sp)
            }

            // PANTALLA LCD INDEPENDIENTE FIJA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.5.dp, rosaOscuroBorde.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .background(wmpScreenBg)
                    .clickable {
                        currentView = when (currentView) {
                            CoquetteViewMode.Disco -> CoquetteViewMode.Cassette
                            CoquetteViewMode.Cassette -> CoquetteViewMode.Visualizer
                            CoquetteViewMode.Visualizer -> CoquetteViewMode.Disco
                        }
                    }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                    label = "CoquetteAnimationSwitch"
                ) { targetMode ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (targetMode) {
                            CoquetteViewMode.Disco -> DiscoCoquette(isPlaying)
                            CoquetteViewMode.Cassette -> CassetteCoquette(isPlaying, rosaPastelFondo)
                            CoquetteViewMode.Visualizer -> VisualizerOndasCoquette(isPlaying, rosaChicleMedio)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("👆 Toca la pantalla para cambiar animación", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))

            // TITULO DE LA CANCIÓN (Marquee)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(0.25f))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val textoBase = "${viewModel.currentTitle} - ${viewModel.currentArtist}"
                val textoInfinito = "$textoBase          •          ".repeat(10)
                Text(
                    text = textoInfinito,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BARRA DE TIEMPO / PROGRESO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val textoTiempo = if (isDraggingProgreso) (progresoLocal * duration).toLong() else currentPosition
                Text(formatTimeCoquette(textoTiempo), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

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
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(0.35f)
                    )
                )
                Text(formatTimeCoquette(duration), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ====================================================================
            // SECCIÓN CENTRAL INTERACTIVA (BOTONES ARRIBA + CLICK WHEEL)
            // ====================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Izquierdo Superior: Repetir
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isRepeatOne) Color.White.copy(0.3f) else Color.Black.copy(0.15f))
                        .clickable {
                            viewModel.toggleRepeat()
                            val msj = if (viewModel.isRepeatOne) "Repetir esta canción" else "Repetición desactivada"
                            Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (viewModel.isRepeatOne) "🔂" else "🔁", fontSize = 16.sp)
                }

                // LA CLICK WHEEL
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(12.dp, CircleShape)
                        .background(blancoClickWheel, CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.03f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // VOLUMEN +
                    IpodWmpWheelButton(
                        text = "＋",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
                        color = grisIconosWheel
                    ) {
                        try {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                            viewModel.cambiarVolumen(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat())
                        } catch (_: SecurityException) {}
                    }

                    // VOLUMEN -
                    IpodWmpWheelButton(
                        text = "－",
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                        color = grisIconosWheel
                    ) {
                        try {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                            viewModel.cambiarVolumen(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat())
                        } catch (_: SecurityException) {}
                    }

                    // RETROCEDER (⏮)
                    IpodWmpWheelButton(
                        text = "⏮",
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                        color = grisIconosWheel
                    ) {
                        if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() else exoPlayer.seekTo(0)
                    }

                    // AVANZAR (⏭)
                    IpodWmpWheelButton(
                        text = "⏭",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        color = grisIconosWheel
                    ) {
                        if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext()
                    }

                    // BOTÓN CENTRAL DE REPRODUCCIÓN
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Brush.verticalGradient(listOf(rosaChicleMedio, rosaFucsiaChasis)), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { viewModel.alternarReproduccion() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Botón Derecho Superior: Aleatorio (Shuffle)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isShuffleEnabled) Color.White.copy(0.3f) else Color.Black.copy(0.15f))
                        .clickable {
                            viewModel.toggleShuffle()
                            val msj = if (viewModel.isShuffleEnabled) "Modo aleatorio encendido" else "Modo aleatorio apagado"
                            Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔀", fontSize = 16.sp, modifier = Modifier.graphicsLayer(alpha = if (viewModel.isShuffleEnabled) 1f else 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SLIDER DE VOLUMEN CON ICONOS CANVAS DEGRADADOS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                AestheticSpeakerIcon(isHighVolume = false, modifier = Modifier.padding(end = 8.dp))

                Slider(
                    value = viewModel.currentVolume,
                    onValueChange = { viewModel.cambiarVolumen(it) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(0.3f)
                    )
                )

                AestheticSpeakerIcon(isHighVolume = true, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BOTÓN CARGAR MÚSICA
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth().height(40.dp).shadow(2.dp, RoundedCornerShape(10.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("📁 CARGAR MÚSICA DEL DISPOSITIVO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BOTÓN CAMBIAR MODO
            Button(
                onClick = onAbrirModos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, rosaChicleMedio),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🎨 CAMBIAR MODO", color = rosaFucsiaChasis, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

// ====================================================================
// ICONO DE BOCINA DIBUJADO CON CANVAS VECTORIAL DEGRADADO
// ====================================================================
@Composable
fun AestheticSpeakerIcon(isHighVolume: Boolean, modifier: Modifier = Modifier) {
    val aestheticGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF8A2BE2), Color(0xFFFF00FF), Color(0xFFFF7F50))
    )
    Canvas(modifier = modifier.size(22.dp)) {
        val speakerPath = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.35f)
            lineTo(size.width * 0.3f, size.height * 0.35f)
            lineTo(size.width * 0.55f, size.height * 0.1f)
            lineTo(size.width * 0.55f, size.height * 0.9f)
            lineTo(size.width * 0.3f, size.height * 0.65f)
            lineTo(size.width * 0.1f, size.height * 0.65f)
            close()
        }
        drawPath(
            path = speakerPath,
            brush = aestheticGradient,
            style = Stroke(width = 4f, join = StrokeJoin.Round)
        )

        if (isHighVolume) {
            val wave1 = Path().apply {
                moveTo(size.width * 0.7f, size.height * 0.35f)
                quadraticBezierTo(size.width * 0.85f, size.height * 0.5f, size.width * 0.7f, size.height * 0.65f)
            }
            drawPath(path = wave1, brush = aestheticGradient, style = Stroke(width = 4f, cap = StrokeCap.Round))

            val wave2 = Path().apply {
                moveTo(size.width * 0.85f, size.height * 0.25f)
                quadraticBezierTo(size.width * 1.05f, size.height * 0.5f, size.width * 0.85f, size.height * 0.75f)
            }
            drawPath(path = wave2, brush = aestheticGradient, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
    }
}

// ====================================================================
// COMPONENTES DE BOTONES
// ====================================================================
@Composable
fun IpodWmpWheelButton(text: String, modifier: Modifier = Modifier, color: Color, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ====================================================================
// ANIMACIONES DEL LCD
// ====================================================================
@Composable
fun DiscoCoquette(isPlaying: Boolean) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) { while (isPlaying) { delay(16); angulo += 3f; if (angulo >= 360f) angulo = 0f } }
    Box(
        modifier = Modifier.size(140.dp).graphicsLayer(rotationZ = angulo).shadow(5.dp, CircleShape).background(Brush.linearGradient(colors = listOf(Color(0xFFFFD1DC), Color(0xFFFF85A2), Color(0xFFFFF0F5), Color(0xFFFFB6C1))), CircleShape).border(1.dp, Color.White.copy(0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(128.dp).background(Brush.radialGradient(colors = listOf(Color.Transparent, Color(0xFFFFC0CB).copy(alpha = 0.2f), Color(0xFFE1BEE7).copy(alpha = 0.2f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape).border(1.dp, Color.White.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(16.dp).background(Color(0xFF0D0206), CircleShape))
            }
        }
    }
}

@Composable
fun CassetteCoquette(isPlaying: Boolean, colorAcento: Color) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) { while (isPlaying) { delay(16); angulo += 4f; if (angulo >= 360f) angulo = 0f } }
    Box(modifier = Modifier.size(220.dp, 120.dp).background(Color(0xFF1E0A11), RoundedCornerShape(12.dp)).border(1.5.dp, colorAcento.copy(alpha = 0.4f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(180.dp, 70.dp).background(Color(0xFF2D141E), RoundedCornerShape(8.dp)).border(1.dp, colorAcento.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(70.dp, 25.dp).background(Color.Black, RoundedCornerShape(4.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 25.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                EngranajeCoquette(angulo, colorAcento); EngranajeCoquette(angulo, colorAcento)
            }
        }
    }
}

@Composable
fun EngranajeCoquette(angulo: Float, color: Color) {
    Box(modifier = Modifier.size(26.dp).graphicsLayer(rotationZ = angulo).background(Color.Black, CircleShape).border(1.5.dp, color.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.width(2.dp).height(26.dp).background(color.copy(alpha = 0.6f)))
        Box(Modifier.width(26.dp).height(2.dp).background(color.copy(alpha = 0.6f)))
    }
}

// ====================================================================
// ECUALIZADOR DINÁMICO EN FORMA DE CORAZÓN (Latido Global)
// ====================================================================
@Composable
fun VisualizerOndasCoquette(isPlaying: Boolean, color: Color) {
    val totalBarras = 23

    val matrizCorazon = remember {
        listOf(
            0.20f, 0.45f, 0.75f, 0.95f, 1.00f, 0.85f, 0.65f, 0.45f, 0.30f, 0.45f, 0.65f, 0.85f,
            1.00f, 0.95f, 0.75f, 0.45f, 0.20f
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "latido_corazon")
    val factorLatido by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isPlaying) 1.0f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulso"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val paddingLateral = (totalBarras - matrizCorazon.size) / 2

        repeat(totalBarras) { indice ->
            val dentroDelCorazon = indice >= paddingLateral && indice < paddingLateral + matrizCorazon.size

            val alturaObjetivo = if (dentroDelCorazon) {
                val alturaBase = 90.dp * matrizCorazon[indice - paddingLateral]
                if (isPlaying) alturaBase * factorLatido else alturaBase * 0.2f
            } else {
                0.dp
            }

            val alturaAnimada by animateDpAsState(
                targetValue = alturaObjetivo,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing),
                label = "anim_barra"
            )

            if (alturaAnimada > 0.dp) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(alturaAnimada)
                        .background(color, RoundedCornerShape(3.dp))
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ====================================================================
// UTILIDADES
// ====================================================================
fun formatTimeCoquette(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}