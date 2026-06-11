@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.example.reproductor

import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import androidx.compose.ui.text.font.FontStyle

private enum class CoquetteViewMode { Visualizer, Disco, Cassette }

// Forma de corazón para los botones
val HeartShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    moveTo(width / 2f, height * 0.25f)
    cubicTo(width * 0.1f, -height * 0.15f, -width * 0.15f, height * 0.4f, width / 2f, height * 0.9f)
    cubicTo(width * 1.15f, height * 0.4f, width * 0.9f, -height * 0.15f, width / 2f, height * 0.25f)
    close()
}

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
    var currentView by remember { mutableStateOf(CoquetteViewMode.Visualizer) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.agregarCancionesLocales(uris)
    }

    // ====================================================================
    // FONDO: ROSA PASTEL A ROSA MEDIO
    // ====================================================================
    val rosaFondoTop = Color(0xFFFFD1DC)
    val rosaFondoBottom = Color(0xFFFF85A2)

    val rosaFuerte = Color(0xFFE91E63)
    val blancoClickWheel = Color(0xFFFDFDFD)
    val grisIconosWheel = Color(0xFF8E8E93)
    val textShadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(2f, 2f), blurRadius = 6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(rosaFondoTop, rosaFondoBottom)))
            .draggable(
                state = rememberDraggableState { if (it > 15f) onAbrirBiblioteca() },
                orientation = Orientation.Vertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(" iPod Coquette", color = rosaFuerte, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("🌸", fontSize = 15.sp)
            }

            // ====================================================================
            // PANTALLA LCD
            // ====================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // <--- Valor ajustado para ser un poquito más pequeño
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0205))
                    .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .clickable {
                        currentView = when (currentView) {
                            CoquetteViewMode.Visualizer -> CoquetteViewMode.Disco
                            CoquetteViewMode.Disco -> CoquetteViewMode.Cassette
                            CoquetteViewMode.Cassette -> CoquetteViewMode.Visualizer
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = currentView, label = "LCDSwitch") { mode ->
                    when (mode) {
                        CoquetteViewMode.Visualizer -> NeonWaveformVisualizer(isPlaying)
                        CoquetteViewMode.Disco -> FloatingHeartsCoquette(isPlaying) // <--- LLUVIA DE CORAZONES
                        CoquetteViewMode.Cassette -> CassetteCoquette(isPlaying, Color(0xFFFFB6C1))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // INFO TRACK AESTHETIC
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(65.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = viewModel.currentTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        maxLines = 1,
                        fontFamily = FontFamily.Serif,
                        style = TextStyle(shadow = textShadow)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.currentArtist,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        maxLines = 1,
                        fontFamily = FontFamily.Serif,
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // PROGRESS BAR MINIMALISTA
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTimeCoquette(currentPosition), color = rosaFuerte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = if (isDraggingProgreso) progresoLocal else if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { isDraggingProgreso = true; progresoLocal = it },
                    onValueChangeFinished = {
                        exoPlayer.seekTo((progresoLocal * duration).toLong())
                        coroutineScope.launch { delay(200); isDraggingProgreso = false }
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    thumb = { Box(contentAlignment = Alignment.Center) { Text("💖", fontSize = 14.sp) } },
                    colors = SliderDefaults.colors(activeTrackColor = rosaFuerte.copy(0.6f), inactiveTrackColor = Color.White.copy(0.5f))
                )
                Text(formatTimeCoquette(duration), color = rosaFuerte, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // CONTROLES CENTRALES
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                // BOTÓN ALEATORIO
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(HeartShape)
                        .background(if (viewModel.isShuffleEnabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f))
                        .clickable { viewModel.toggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    ExactShuffleIcon(isActive = viewModel.isShuffleEnabled, modifier = Modifier.size(22.dp))
                }

                // CLICK WHEEL
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .shadow(8.dp, CircleShape)
                        .background(blancoClickWheel, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IpodWmpWheelButton("＋", Modifier.align(Alignment.TopCenter).padding(top = 10.dp), grisIconosWheel) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }
                    IpodWmpWheelButton("－", Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), grisIconosWheel) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    }
                    IpodWmpWheelButton("⏮", Modifier.align(Alignment.CenterStart).padding(start = 14.dp), grisIconosWheel) {
                        if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() else exoPlayer.seekTo(0)
                    }
                    IpodWmpWheelButton("⏭", Modifier.align(Alignment.CenterEnd).padding(end = 14.dp), grisIconosWheel) {
                        if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext()
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFFF4081), rosaFuerte)), CircleShape)
                            .clickable { viewModel.alternarReproduccion() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // BOTÓN REPETIR
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(HeartShape)
                        .background(if (viewModel.isRepeatOne) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f))
                        .clickable { viewModel.toggleRepeat() },
                    contentAlignment = Alignment.Center
                ) {
                    ExactRepeatIcon(isActive = viewModel.isRepeatOne, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // VOLUMEN
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VolumeSpeakerIcon(isHigh = false, modifier = Modifier.padding(end = 8.dp))

                Box(modifier = Modifier.weight(1f).height(30.dp), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val totalHearts = 8
                        val volumeLevel = (viewModel.currentVolume * totalHearts).toInt()
                        repeat(totalHearts) { i ->
                            val isActive = i < volumeLevel
                            Text("❤", color = if (isActive) Color(0xFFF30000) else Color(0xFFF30000).copy(alpha = 0.2f), fontSize = 18.sp)
                        }
                    }
                    Slider(
                        value = viewModel.currentVolume, onValueChange = { viewModel.cambiarVolumen(it) },
                        colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
                    )
                }

                VolumeSpeakerIcon(isHigh = true, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // BOTONES INFERIORES SÓLIDOS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .shadow(4.dp, RoundedCornerShape(30.dp))
                    .background(rosaFuerte, RoundedCornerShape(30.dp))
                    .clickable { filePickerLauncher.launch(arrayOf("audio/*")) },
                contentAlignment = Alignment.Center
            ) {
                Text("📁 CARGAR MÚSICA DEL DISPOSITIVO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .shadow(4.dp, RoundedCornerShape(30.dp))
                    .background(rosaFuerte, RoundedCornerShape(30.dp))
                    .clickable { onAbrirModos() },
                contentAlignment = Alignment.Center
            ) {
                Text("🎨 CAMBIAR MODO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ====================================================================
// ECUALIZADOR
// ====================================================================
@Composable
fun NeonWaveformVisualizer(isPlaying: Boolean) {
    val totalLineas = 21
    val infiniteTransition = rememberInfiniteTransition(label = "onda_luz")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "anim_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        val spacing = size.width / (totalLineas - 1)
        val centerY = size.height / 2

        for (index in 0 until totalLineas) {
            val distanceFromCenter = abs(index - (totalLineas / 2))
            val maxFactor = max(0f, 1f - (distanceFromCenter * 0.1f)) // Ajustado para verse más amplio

            val factorAltura = if (isPlaying) {
                0.3f + (sin(phase + index * 0.6f) * 0.4f)
            } else {
                0.05f
            }

            val pX = index * spacing
            val finalHeight = size.height * 0.85f * maxFactor * factorAltura

            if (finalHeight > 0) {
                drawPath(
                    path = Path().apply {
                        moveTo(pX, centerY - finalHeight / 2)
                        lineTo(pX, centerY + finalHeight / 2)
                    },
                    brush = Brush.radialGradient(listOf(Color(0xFFFFB6C1).copy(alpha = 0.8f), Color.Transparent)),
                    style = Stroke(width = 38f, cap = StrokeCap.Round)
                )

                drawPath(
                    path = Path().apply {
                        moveTo(pX, centerY - finalHeight / 2)
                        lineTo(pX, centerY + finalHeight / 2)
                    },
                    color = Color.White,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// ====================================================================
// ANIMACION: LLUVIA DE CORAZONES FLOTANTES
// ====================================================================
class HeartParticle(
    var xOffset: Float,
    var yOffset: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun FloatingHeartsCoquette(isPlaying: Boolean) {
    val heartColors = listOf(Color(0xFFFFB6C1), Color(0xFFF48FB1), Color(0xFFE91E63), Color.White)
    var tick by remember { mutableFloatStateOf(0f) }

    val particles = remember {
        List(15) {
            HeartParticle(
                xOffset = (10..90).random() / 100f,
                yOffset = (0..100).random() / 100f + 0.2f,
                speed = (2..5).random() / 1000f,
                size = (30..70).random().toFloat(),
                color = heartColors.random()
            )
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(16)
            tick += 1f
            particles.forEach {
                it.yOffset -= it.speed
                if (it.yOffset < -0.2f) {
                    it.yOffset = 1.1f
                    it.xOffset = (10..90).random() / 100f
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val currentTick = tick // Obligamos al canvas a redibujarse

        particles.forEach { p ->
            val w = p.size
            val h = p.size

            val path = Path().apply {
                moveTo(w / 2f, h * 0.25f)
                cubicTo(w * 0.1f, -h * 0.15f, -w * 0.15f, h * 0.4f, w / 2f, h * 0.9f)
                cubicTo(w * 1.15f, h * 0.4f, w * 0.9f, -h * 0.15f, w / 2f, h * 0.25f)
                close()
            }

            translate(left = p.xOffset * size.width - w / 2, top = p.yOffset * size.height - h / 2) {
                drawPath(path, color = p.color.copy(alpha = 0.6f))
            }
        }
    }
}

// ====================================================================
// ANIMACION CASSETTE
// ====================================================================
@Composable fun CassetteCoquette(isPlaying: Boolean, colorAcento: Color) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) { while (isPlaying) { delay(16); angulo += 4f; if (angulo >= 360f) angulo = 0f } }
    // CASSETTE GIGANTE PARA LLENAR EL ESPACIO
    Box(modifier = Modifier.size(300.dp, 180.dp).background(Color(0xFF1E0A11), RoundedCornerShape(16.dp)).border(2.dp, colorAcento.copy(alpha = 0.4f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(250.dp, 110.dp).background(Color(0xFF2D141E), RoundedCornerShape(12.dp)).border(1.5.dp, colorAcento.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(120.dp, 40.dp).background(Color.Black, RoundedCornerShape(6.dp)))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 35.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                EngranajeCoquette(angulo, colorAcento)
                EngranajeCoquette(angulo, colorAcento)
            }
        }
    }
}

@Composable fun EngranajeCoquette(angulo: Float, color: Color) {
    Box(modifier = Modifier.size(40.dp).graphicsLayer(rotationZ = angulo).background(Color.Black, CircleShape).border(2.dp, color.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.width(4.dp).height(40.dp).background(color.copy(alpha = 0.6f)))
        Box(Modifier.width(40.dp).height(4.dp).background(color.copy(alpha = 0.6f)))
    }
}

// ====================================================================
// ICONOS DE VOLUMEN
// ====================================================================
@Composable
fun VolumeSpeakerIcon(isHigh: Boolean, modifier: Modifier = Modifier) {
    val color = Color(0xFFD81B60) // Magenta outline
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.4f)
            lineTo(size.width * 0.35f, size.height * 0.4f)
            lineTo(size.width * 0.6f, size.height * 0.2f)
            lineTo(size.width * 0.6f, size.height * 0.8f)
            lineTo(size.width * 0.35f, size.height * 0.6f)
            lineTo(size.width * 0.1f, size.height * 0.6f)
            close()
        }
        drawPath(p, color, style = stroke)

        if (isHigh) {
            val wave1 = Path().apply {
                moveTo(size.width * 0.75f, size.height * 0.35f)
                quadraticBezierTo(size.width * 0.85f, size.height * 0.5f, size.width * 0.75f, size.height * 0.65f)
            }
            drawPath(wave1, color, style = stroke)

            val wave2 = Path().apply {
                moveTo(size.width * 0.85f, size.height * 0.25f)
                quadraticBezierTo(size.width * 1.0f, size.height * 0.5f, size.width * 0.85f, size.height * 0.75f)
            }
            drawPath(wave2, color, style = stroke)
        }
    }
}

// ====================================================================
// ICONOS DE ALEATORIO/REPETIR
// ====================================================================
@Composable
fun ExactShuffleIcon(isActive: Boolean, modifier: Modifier = Modifier) {
    val iconGradient = if (isActive) Brush.horizontalGradient(listOf(Color(0xFFDDA754), Color(0xFFF28BB1))) else SolidColor(Color(0xFFD81B60).copy(alpha = 0.5f))
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path1 = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.3f)
            lineTo(size.width * 0.3f, size.height * 0.3f)
            cubicTo(size.width * 0.5f, size.height * 0.3f, size.width * 0.5f, size.height * 0.7f, size.width * 0.7f, size.height * 0.7f)
            lineTo(size.width * 0.85f, size.height * 0.7f)
        }
        drawPath(path1, iconGradient, style = stroke)
        val head1 = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.5f)
            lineTo(size.width * 0.85f, size.height * 0.7f)
            lineTo(size.width * 0.65f, size.height * 0.9f)
        }
        drawPath(head1, iconGradient, style = stroke)

        val path2 = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.7f)
            lineTo(size.width * 0.3f, size.height * 0.7f)
            cubicTo(size.width * 0.5f, size.height * 0.7f, size.width * 0.5f, size.height * 0.3f, size.width * 0.7f, size.height * 0.3f)
            lineTo(size.width * 0.85f, size.height * 0.3f)
        }
        drawPath(path2, iconGradient, style = stroke)
        val head2 = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.1f)
            lineTo(size.width * 0.85f, size.height * 0.3f)
            lineTo(size.width * 0.65f, size.height * 0.5f)
        }
        drawPath(head2, iconGradient, style = stroke)
    }
}

@Composable
fun ExactRepeatIcon(isActive: Boolean, modifier: Modifier = Modifier) {
    val iconGradient = if (isActive) Brush.horizontalGradient(listOf(Color(0xFFDDA754), Color(0xFFF28BB1))) else SolidColor(Color(0xFFD81B60).copy(alpha = 0.5f))
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path1 = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.5f)
            quadraticBezierTo(size.width * 0.2f, size.height * 0.15f, size.width * 0.55f, size.height * 0.15f)
            lineTo(size.width * 0.8f, size.height * 0.15f)
        }
        drawPath(path1, iconGradient, style = stroke)
        val head1 = Path().apply {
            moveTo(size.width * 0.6f, size.height * 0.0f)
            lineTo(size.width * 0.8f, size.height * 0.15f)
            lineTo(size.width * 0.6f, size.height * 0.3f)
        }
        drawPath(head1, iconGradient, style = stroke)

        val path2 = Path().apply {
            moveTo(size.width * 0.8f, size.height * 0.5f)
            quadraticBezierTo(size.width * 0.8f, size.height * 0.85f, size.width * 0.45f, size.height * 0.85f)
            lineTo(size.width * 0.2f, size.height * 0.85f)
        }
        drawPath(path2, iconGradient, style = stroke)
        val head2 = Path().apply {
            moveTo(size.width * 0.4f, size.height * 0.7f)
            lineTo(size.width * 0.2f, size.height * 0.85f)
            lineTo(size.width * 0.4f, size.height * 1.0f)
        }
        drawPath(head2, iconGradient, style = stroke)
    }
}

// ====================================================================
// COMPONENTES AUXILIARES
// ====================================================================
@Composable
fun IpodWmpWheelButton(text: String, modifier: Modifier, color: Color, onClick: () -> Unit) {
    Box(modifier = modifier.size(40.dp).clip(CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatTimeCoquette(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}