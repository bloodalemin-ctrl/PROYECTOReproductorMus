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
import androidx.compose.ui.unit.dp
import com.example.reproductor.ui.theme.ReproductorTheme

class MainActivity : ComponentActivity() {

    // Obtenemos el motor (ViewModel)
    private val viewModel: ReproductorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReproductorTheme {
                val estilo = viewModel.temaActual

                // Usamos un Box principal para poder poner el botón de cambiar tema "flotando" encima
                Box(modifier = Modifier.fillMaxSize()) {

                    // --- EL CEREBRO PARA CAMBIAR DE PANTALLA ---
                    if (estilo.esNokia) {

                        // 1. EL DISEÑO (El código original que tenías)
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

                                // Un espacio vacío para que el botón flotante no tape los controles
                                Spacer(modifier = Modifier.height(70.dp))
                            }
                        }

                    } else {

                        // 2. ¡TU DISEÑO DE WINDOWS MEDIA PLAYER!
                        // Al fin mandamos a llamar a tu función estrella
                        WmpThemeScreen()

                    }

                    // --- EL BOTÓN MÁGICO PARA INTERCAMBIAR ---
                    // Lo dejamos flotando hasta abajo para que funcione en ambas pantallas
                    Button(
                        onClick = { viewModel.cambiarTema() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            // CORRECCIÓN DE COPILOT: Evita que la barra de navegación del celular lo tape
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp)
                            .fillMaxWidth(0.8f), // Que no ocupe todo el ancho para que se vea bonito
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("CAMBIAR A MODO ${if (estilo.esNokia) "WMP" else "NOKIA"}")
                    }
                }
            }
        }
    }
}