@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package com.example.reproductor

import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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

@Composable
fun WmpThemeScreen(viewModel: ReproductorViewModel) {
    val context = LocalContext.current

    // ====================================================================
    // PARCHE DE SEGURIDAD: Espera a que el servicio se conecte para no crashear
    // ====================================================================
    val exoPlayer = viewModel.exoPlayer ?: return

    val isPlaying = viewModel.isPlaying

    // ====================================================================
    // SOLUCIÓN AL CONGELAMIENTO: Vinculación directa al reloj central del ViewModel
    // ====================================================================
    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume) }

    // Sincroniza el Slider con el volumen real del Huawei al abrir la pantalla
    LaunchedEffect(exoPlayer) {
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
    }

    // ====================================================================
    // LANZADOR DEL EXPLORADOR DE ARCHIVOS (Para cargar música local)
    // ====================================================================
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.agregarCancionesLocales(uris)
            }
        }
    )

    // PALETA DE COLORES RETRO WMP (Skeuomórfica)
    val wmpNostalgiaBlue = Color(0xFF3864A6) // Azul clásico WMP
    val wmpElectricBlue = Color(0xFF4FC3F7)  // Acento brillante
    val metallicSilver = Color(0xFFB0bec5)   // Gris metálico base
    val metallicLight = Color(0xFFeceff1)    // Brillo metálico
    val metallicDark = Color(0xFF78909c)     // Sombra metálica
    val wmpScreenBg = Color(0xFF000511)      // Fondo LCD casi negro
    val wmpScreenBorder = Color(0xFF5D7BAA) // Borde azul grisáceo LCD

    // Degradado para el chasis metálico
    val chasisGradient = Brush.verticalGradient(
        colors = listOf(metallicLight, metallicSilver, metallicDark)
    )

    // Fondo de la app (fuera del reproductor)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238)) // Un gris azulado oscuro de fondo de escritorio
            .navigationBarsPadding() // Evita los botones de navegación
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom // Ancla el reproductor abajo
    ) {
        // EL REPRODUCTOR FÍSICO (Con bordes y bisel)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(15.dp, RoundedCornerShape(20.dp)) // Sombra proyectada
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp)) // Brillo del borde externo
                .background(chasisGradient, RoundedCornerShape(20.dp))
                .padding(12.dp), // Espaciado interno del chasis
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BARRA DE TÍTULO DEL SKIN (Muy 2000s)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Windows Media Player Classic", color = Color(0xFF1A237E), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                Text("v9.0", color = Color(0xFF1A237E), fontSize = 11.sp)
            }

            // PANTALLA LCD INCRUSTADA (Contiene Casete y EQ)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(3.dp, wmpScreenBorder, RoundedCornerShape(10.dp)) // Borde biselado interno
                    .background(wmpScreenBg)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CASETE (Dentro de la pantalla)
                Box(modifier = Modifier.padding(bottom = 10.dp)) {
                    CassetteRetroWmp(isPlaying, wmpElectricBlue)
                }

                // ECUALIZADOR (Dentro de la pantalla, integrado abajo)
                VisualizerOndasRetroWmp(isPlaying, wmpElectricBlue)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // INFORMACIÓN DE LA PISTA (Marquee en caja de texto biselada)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.dp, metallicDark, RoundedCornerShape(5.dp)) // Borde hundido
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

            // BARRA DE PROGRESO Y TIEMPOS (Estilo integrado y funcional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatTimeRetroWmp(currentPosition), color = Color(0xFF1A237E), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { val newPos = (it * duration).toLong(); exoPlayer.seekTo(newPos) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = wmpNostalgiaBlue,
                        inactiveTrackColor = Color.Black.copy(0.3f)
                    )
                )
                Text(formatTimeRetroWmp(duration), color = Color(0xFF1A237E), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            // BOTONES DE REPRODUCCIÓN (Físicos/3D)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RetroWmpControlButton("⏮", metallicSilver, wmpNostalgiaBlue, 45.dp, 16.sp) {
                    if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() else exoPlayer.seekTo(0)
                }

                Spacer(Modifier.width(20.dp))

                // Botón Play/Pause central grande y brillante
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

                Spacer(Modifier.width(20.dp))

                RetroWmpControlButton("⏭", metallicSilver, wmpNostalgiaBlue, 45.dp, 16.sp) {
                    if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext()
                }
            }

            // CONTROL DE VOLUMEN
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            ) {
                Text("🔈", color = Color(0xFF1A237E), fontSize = 16.sp)
                Slider(
                    value = volume,
                    onValueChange = { volume = it; try { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVolume).toInt(), 0) } catch (_: SecurityException) {} },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = metallicSilver, activeTrackColor = wmpNostalgiaBlue, inactiveTrackColor = Color.Black.copy(0.3f))
                )
                Text("🔊", color = Color(0xFF1A237E), fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(15.dp))

            // BOTÓN PARA CARGAR ARCHIVOS LOCALES
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

            // BOTONES SIMÉTRICOS (Nokia/Windows)
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

    Row(Modifier.fillMaxWidth().height(65.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
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