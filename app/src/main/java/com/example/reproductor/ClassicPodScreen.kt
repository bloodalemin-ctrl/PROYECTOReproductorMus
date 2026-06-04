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
fun ClassicPodScreen(viewModel: ReproductorViewModel, onAbrirBiblioteca: () -> Unit, onAbrirModos: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MODO CLASSIC POD", fontSize = 24.sp, color = Color.Black)
            Text("(Diseño en construcción)", color = Color.Gray)
        }
    }
}