package com.example.reproductor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun GammingModoScreen(
    viewModel: ReproductorViewModel,
    onAbrirBiblioteca: () -> Unit,
    onAbrirModos: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MODO GAMING", fontSize = 24.sp, color = Color.Yellow)
            Text("(Diseño en construcción)", color = Color.White)
        }
    }
}