@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.example.reproductor

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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private enum class GamingViewMode { ViniloQuest, AlbumPixel, Visualizador }

@Composable
fun GammingModoScreen(
    viewModel: ReproductorViewModel,
    onAbrirBiblioteca: () -> Unit,
    onAbrirModos: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = viewModel.exoPlayer ?: return
    val isPlaying = viewModel.isPlaying

    val currentPosition = viewModel.currentPosition
    val duration = viewModel.duration

    var currentView by remember { mutableStateOf(GamingViewMode.ViniloQuest) }

    var isDraggingProgreso by remember { mutableStateOf(false) }
    var progresoLocal by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) viewModel.agregarCancionesLocales(uris)
        }
    )

    // COLORES DEL ENTORNO
    val bgGamerDark = Color(0xFF080914)
    val screenBg = Color(0xFF020208)
    val blueNeonBorder = Color(0xFF0066FF)
    val coinGold = Color(0xFFFFD700)

    // COLORES PARA BOTONES CIRCULARES (Basados en la 1ra foto)
    val btnRedBase = Color(0xFFD32F2F); val btnRedDark = Color(0xFF880E4F); val btnRedLight = Color(0xFFFF8A80)
    val btnBlueBase = Color(0xFF1976D2); val btnBlueDark = Color(0xFF0D47A1); val btnBlueLight = Color(0xFF82B1FF)
    val btnGreenBase = Color(0xFF388E3C); val btnGreenDark = Color(0xFF1B5E20); val btnGreenLight = Color(0xFFB9F6CA)
    val btnPurpleBase = Color(0xFF7B1FA2); val btnPurpleDark = Color(0xFF4A148C); val btnPurpleLight = Color(0xFFEA80FC)

    // COLORES PARA BOTONES RECTANGULARES (Basados en la 4ta foto)
    val blockGreenBase = Color(0xFF4CAF50); val blockGreenDark = Color(0xFF1B5E20); val blockGreenLight = Color(0xFFA5D6A7)
    val blockOrangeBase = Color(0xFFFF9800); val blockOrangeDark = Color(0xFFE65100); val blockOrangeLight = Color(0xFFFFE082)

    // Lista de colores fosfo para el volumen segmentado
    val listaColoresVolumen = listOf(
        Color(0xFFFF073A), Color(0xFFFF4500), Color(0xFFFF8C00), Color(0xFFFFD700),
        Color(0xFFFFEA00), Color(0xFFADFF2F), Color(0xFF7FFF00), Color(0xFF39FF14),
        Color(0xFF00FF7F), Color(0xFF00FA9A), Color(0xFF00FFFF), Color(0xFF00BFFF),
        Color(0xFF0045FF), Color(0xFFCC00FF), Color(0xFFFF007F), Color(0xFFFF00AA)
    )

    val hpBarGradient = Brush.horizontalGradient(listOf(Color(0xFFFF073A), Color(0xFFFFEA00), Color(0xFF39FF14), Color(0xFF00FFFF)))
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGamerDark)
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
            // 1. HUD SUPERIOR
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("❤❤❤🖤", color = Color(0xFFFF073A), fontSize = 12.sp)
                    Text("HP [||||||||]", color = Color(0xFF39FF14), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Text("PIXEL QUEST", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Column(horizontalAlignment = Alignment.End) {
                    Text("🪙 9999", color = coinGold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("MP [||||||  ]", color = Color(0xFF00FFFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. PANTALLA PRINCIPAL ANIMADA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, CutCornerShape(8.dp))
                    .padding(4.dp)
                    .background(blueNeonBorder, CutCornerShape(6.dp))
                    .padding(3.dp)
                    .background(screenBg, CutCornerShape(4.dp))
                    .clickable {
                        currentView = when(currentView) {
                            GamingViewMode.ViniloQuest -> GamingViewMode.AlbumPixel
                            GamingViewMode.AlbumPixel -> GamingViewMode.Visualizador
                            GamingViewMode.Visualizador -> GamingViewMode.ViniloQuest
                        }
                    }
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱ 10:09", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("STAGE 1", color = coinGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("LVL 99", color = Color(0xFF39FF14), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    // Render central
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        AnimatedContent(targetState = currentView, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "ViewSwitch") { target ->
                            when (target) {
                                GamingViewMode.ViniloQuest -> VinylQuestVisualizer(isPlaying)
                                GamingViewMode.AlbumPixel -> PixelAlbum(Color(0xFFFF007F))
                                GamingViewMode.Visualizador -> PixelBarsVisualizer(isPlaying, Color(0xFF39FF14))
                            }
                        }
                    }

                    // Datos de la pista
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("PISTA: ${viewModel.currentTitle.uppercase()}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text("ARTISTA: ${viewModel.currentArtist.uppercase()}", color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // 🔸 Slider de Vida con THUMB DE CORAZÓN PIXELADO 🔸
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val textoTiempo = if (isDraggingProgreso) (progresoLocal * duration).toLong() else currentPosition
                        Text(formatTimeGaming(textoTiempo), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(24.dp), contentAlignment = Alignment.CenterStart) {
                            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.Black, CutCornerShape(2.dp)).padding(2.dp).background(hpBarGradient, CutCornerShape(1.dp)))

                            Slider(
                                value = if (isDraggingProgreso) progresoLocal else if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { isDraggingProgreso = true; progresoLocal = it },
                                onValueChangeFinished = {
                                    val nuevaPosicion = (progresoLocal * duration).toLong()
                                    viewModel.currentPosition = nuevaPosicion
                                    exoPlayer.seekTo(nuevaPosicion)
                                    coroutineScope.launch { delay(200); isDraggingProgreso = false }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Transparent,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Black.copy(0.7f)
                                ),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color.Black, CutCornerShape(4.dp)) // Contorno negro
                                            .padding(2.dp)
                                            .background(Color(0xFFFF073A), CutCornerShape(2.dp)), // Corazón Rojo Fosfo
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Brillo pixelado blanco arriba a la izquierda
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(top = 2.dp, start = 2.dp, end = 8.dp, bottom = 8.dp)
                                                .background(Color.White.copy(alpha = 0.6f))
                                        )
                                    }
                                }
                            )
                        }
                        Text(formatTimeGaming(duration), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("👆 TOCA LA PANTALLA PARA CAMBIAR ANIMACIÓN", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(12.dp))

            // TEXTO CENTRAL
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("GAMING", color = coinGold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. BOTONERA DE REPRODUCCIÓN (EFECTO PIXEL OCTAGONAL)
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0F24), CutCornerShape(12.dp)).border(3.dp, Color(0xFF1E3A8A), CutCornerShape(12.dp)).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bucle (Verde)
                val iconRepeat = if (viewModel.isRepeatOne) "🔂" else "🔁"
                val rptBase = if (viewModel.isRepeatOne) btnGreenLight else btnGreenBase
                PixelCircleButton(iconRepeat, rptBase, btnGreenDark, btnGreenLight, 48.dp) {
                    viewModel.toggleRepeat()
                    Toast.makeText(context, if (viewModel.isRepeatOne) "BUCLE COMPLETO" else "BUCLE OFF", Toast.LENGTH_SHORT).show()
                }

                // Anterior (Rojo)
                PixelCircleButton("⏮", btnRedBase, btnRedDark, btnRedLight, 55.dp) {
                    if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPrevious() else exoPlayer.seekTo(0)
                }

                // Central Play/Pausa (Azul)
                val playText = if (isPlaying) "⏸" else "▶"
                PixelCircleButton(playText, btnBlueBase, btnBlueDark, btnBlueLight, 75.dp) {
                    viewModel.alternarReproduccion()
                }

                // Siguiente (Azul)
                PixelCircleButton("⏭", btnBlueBase, btnBlueDark, btnBlueLight, 55.dp) {
                    if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNext()
                }

                // Aleatorio (Morado)
                val iconShuffle = "🔀"
                val shfBase = if (viewModel.isShuffleEnabled) btnPurpleLight else btnPurpleBase
                PixelCircleButton(iconShuffle, shfBase, btnPurpleDark, btnPurpleLight, 48.dp) {
                    viewModel.toggleShuffle()
                    Toast.makeText(context, if (viewModel.isShuffleEnabled) "MODO AZAR" else "MODO LINEAL", Toast.LENGTH_SHORT).show()
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // 4. BARRA DE VOLUMEN MULTICOLOR
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0F24), CutCornerShape(8.dp)).border(3.dp, Color(0xFF1E3A8A), CutCornerShape(8.dp)).padding(horizontal = 15.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("❤", color = Color(0xFFFF073A), fontSize = 16.sp)
                Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(20.dp), contentAlignment = Alignment.CenterStart) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val casillasActivas = (viewModel.currentVolume * 16).toInt()
                        repeat(16) { i ->
                            val colorCasilla = if (i < casillasActivas) listaColoresVolumen[i] else Color(0xFF1B1E36)
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black).padding(1.dp).background(colorCasilla))
                        }
                    }
                    Slider(
                        value = viewModel.currentVolume,
                        onValueChange = { viewModel.cambiarVolumen(it) },
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
                    )
                }
                Text("🔊", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(15.dp))

            // 5. BOTÓN: CARGAR MÚSICA
            PixelBlockButton(
                texto = "📁 CARGAR MÚSICA",
                mainColor = blockGreenBase,
                darkColor = blockGreenDark,
                lightColor = blockGreenLight,
                modifier = Modifier.fillMaxWidth().height(55.dp)
            ) {
                filePickerLauncher.launch(arrayOf("audio/*"))
            }

            Spacer(modifier = Modifier.height(15.dp))

            // 6. BOTÓN: CAMBIAR MODO
            PixelBlockButton(
                texto = "🎨 CAMBIAR MODO",
                mainColor = blockOrangeBase,
                darkColor = blockOrangeDark,
                lightColor = blockOrangeLight,
                modifier = Modifier.fillMaxWidth().height(55.dp)
            ) {
                onAbrirModos()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ADORNOS INFERIORES
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(12) { Box(modifier = Modifier.size(6.dp).background(Color.DarkGray)) } }
        }
    }
}

// ====================================================================
// COMPONENTES MAESTROS ESTILO PIXELS 16-BIT
// ====================================================================

@Composable
fun PixelCircleButton(icon: String, mainColor: Color, darkColor: Color, lightColor: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val pixelShape = CutCornerShape(25)

    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Black, pixelShape)
            .padding(3.dp)
            .background(darkColor, pixelShape)
            .padding(bottom = 4.dp, end = 3.dp)
            .background(lightColor, pixelShape)
            .padding(top = 3.dp, start = 3.dp)
            .background(mainColor, pixelShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, color = Color.White, fontSize = (size.value / 2.3).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PixelBlockButton(texto: String, mainColor: Color, darkColor: Color, lightColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val blockShape = CutCornerShape(15)

    Box(
        modifier = modifier
            .background(Color.Black, blockShape)
            .padding(4.dp)
            .background(darkColor, blockShape)
            .padding(bottom = 5.dp, end = 4.dp)
            .background(lightColor, blockShape)
            .padding(top = 4.dp, start = 4.dp)
            .background(mainColor, blockShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = Color(0xFF111111), fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
    }
}

@Composable
fun VinylQuestVisualizer(isPlaying: Boolean) {
    var angulo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) { delay(16); angulo += 2f; if (angulo >= 360f) angulo = 0f }
    }

    Box(
        modifier = Modifier.size(190.dp).graphicsLayer(rotationZ = angulo).background(Color(0xFF11131F), CutCornerShape(25)).border(4.dp, Color(0xFF0066FF), CutCornerShape(25)),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(155.dp).border(4.dp, Color(0xFF00FFFF), CutCornerShape(25)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(120.dp).border(4.dp, Color(0xFFFF007F), CutCornerShape(25)), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(85.dp).border(4.dp, Color(0xFF39FF14), CutCornerShape(25)), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(50.dp).background(Color(0xFF7F1D1D), CutCornerShape(25)).border(3.dp, Color.Black, CutCornerShape(25)), contentAlignment = Alignment.Center) {
                        Text("💿", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PixelBarsVisualizer(isPlaying: Boolean, colorNeon: Color) {
    var alturas by remember { mutableStateOf(List(12) { 6.dp }) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) { while (true) { alturas = List(12) { (12..85).random().dp }; delay(150) } }
        else { alturas = List(12) { 6.dp } }
    }
    Row(modifier = Modifier.height(100.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        alturas.forEach { alturaObjetivo ->
            val alturaAnimada by animateDpAsState(targetValue = alturaObjetivo, animationSpec = tween(durationMillis = if (isPlaying) 150 else 500, easing = LinearEasing), label = "eq")
            Box(Modifier.width(16.dp).height(alturaAnimada).background(Color.Black).padding(1.5.dp).background(colorNeon))
        }
    }
}

@Composable
fun PixelAlbum(colorNeon: Color) {
    Box(
        modifier = Modifier.size(160.dp).background(Color.Black).padding(4.dp).background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(Color.Black).padding(3.dp).background(colorNeon),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(80.dp).background(Color.Black).padding(2.dp).background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
                Text("🎵", fontSize = 40.sp)
            }
        }
    }
}

fun formatTimeGaming(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}