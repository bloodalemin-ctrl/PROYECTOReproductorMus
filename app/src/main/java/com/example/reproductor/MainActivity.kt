package com.example.reproductor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reproductor.ui.theme.ReproductorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReproductorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReproductorTheme {
                val estilo = viewModel.temaActual

                // ¡Adiós al Box global que amontonaba los botones!
                if (estilo.esNokia) {

                    // 1. EL DISEÑO NOKIA
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(estilo.fondo)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(250.dp),
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "MODO: ${estilo.nombre}", color = estilo.acento, style = MaterialTheme.typography.headlineSmall)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { }, colors = ButtonDefaults.buttonColors(estilo.acento)) { Text("<<") }
                                Spacer(modifier = Modifier.width(16.dp))
                                FloatingActionButton(onClick = { }, containerColor = estilo.acento) { Text("▶") }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(onClick = { }, colors = ButtonDefaults.buttonColors(estilo.acento)) { Text(">>") }
                            }

                            // Botón exclusivo para la pantalla Nokia
                            Button(
                                onClick = { viewModel.cambiarTema() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .navigationBarsPadding(),
                                colors = ButtonDefaults.buttonColors(containerColor = estilo.acento)
                            ) {
                                Text("CAMBIAR A MODO WMP", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                } else {

                    // 2. DISEÑO WINDOWS MEDIA PLAYER
                    // Aquí le pasamos la orden de cambiar de tema hacia adentro de la pantalla
                    WmpThemeScreen(
                        onCambiarTema = { viewModel.cambiarTema() }
                    )

                }
            }
        }
    }
}