package com.example.reproductor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- COLORES GLOSSY ---
val WmpGlossyBlue = Brush.verticalGradient(
    colors = listOf(Color(0xFF4A89CC), Color(0xFF1E3C72), Color(0xFF0D254F))
)
val WmpSkyBlue = Color(0xFF6FB1FC)

// Tipos de vista
enum class PlayerView { Album, Cassette, Visualizer }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WmpThemeScreen() {
    var isPlaying by remember { mutableStateOf(false) }
    var currentView by remember { mutableStateOf(PlayerView.Cassette) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF02050A)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. ÁREA SUPERIOR DINÁMICA (Alternancia fluida) ---
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500))
                },
                label = "ViewTransition"
            ) { targetView ->
                when (targetView) {
                    PlayerView.Album -> AlbumView()
                    PlayerView.Cassette -> CassetteView(isPlaying)
                    PlayerView.Visualizer -> VisualizerView(isPlaying)
                }
            }

            // Botón flotante para cambiar de vista (Para que el profe vea la fluidez)
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

        // --- 2. PANEL DE CONTROL (Look de tu imagen) ---
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(WmpGlossyBlue),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Slider(value = 0.3f, onValueChange = {}, colors = SliderDefaults.colors(thumbColor = Color.White))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    WmpRoundButton("⏮")
                    Spacer(Modifier.width(20.dp))

                    // BOTÓN PLAY PRINCIPAL
                    Box(
                        modifier = Modifier.size(80.dp).background(Color.White.copy(0.1f), CircleShape).border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 35.sp)
                        }
                    }

                    Spacer(Modifier.width(20.dp))
                    WmpRoundButton("⏭")
                }
                Spacer(Modifier.height(10.dp))
                Text("VOLUMEN", color = Color.White, fontSize = 10.sp)
                Slider(value = 0.8f, onValueChange = {}, modifier = Modifier.width(200.dp))
            }
        }
    }
}

// --- VISTA 1: PORTADA DEL ÁLBUM ---
@Composable
fun AlbumView() {
    Box(Modifier.size(250.dp).background(Color.DarkGray, RoundedCornerShape(10.dp)).border(2.dp, Color.Gray, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Text("ÁLBUM ART", color = Color.White)
    }
}

// --- VISTA 2: CASSETTE CON MARQUESINA ---
@Composable
fun CassetteView(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing))
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // MARQUESINA (Título desplazándose)
        MarqueeText("Virtual Insanity - Jamiroquai - Reproduciendo en alta fidelidad...")

        Spacer(Modifier.height(20.dp))

        Box(
            Modifier.size(260.dp, 150.dp).graphicsLayer(rotationZ = if (isPlaying) angle else 0f).background(Color(0xFF222222), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row { repeat(2) { Box(Modifier.padding(20.dp).size(40.dp).background(Color.White, CircleShape)) } }
        }
    }
}

// --- VISTA 3: VISUALIZADOR DE FRECUENCIAS (Simulado) ---
@Composable
fun VisualizerView(isPlaying: Boolean) {
    Row(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 40.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        repeat(10) { index ->
            val heightAnim by animateDpAsState(
                targetValue = if (isPlaying) (50..150).random().dp else 10.dp,
                animationSpec = tween(300)
            )
            Box(Modifier.width(15.dp).height(heightAnim).background(WmpSkyBlue, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        }
    }
}

@Composable
fun MarqueeText(text: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 1000f, targetValue = -1000f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
    )
    Text(text, color = WmpSkyBlue, modifier = Modifier.offset(x = offset.dp), maxLines = 1, softWrap = false)
}

@Composable
fun WmpRoundButton(icon: String) {
    Box(Modifier.size(55.dp).background(Color.White.copy(0.15f), CircleShape).border(1.dp, Color.White.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) {
        Text(icon, color = Color.White, fontSize = 20.sp)
    }
}

@Preview(showSystemUi = true)
@Composable
fun FullPlayerPreview() { WmpThemeScreen() }