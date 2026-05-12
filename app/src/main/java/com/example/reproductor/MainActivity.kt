package com.example.reproductor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
            // Mantenemos vuestro tema original de Android Studio
            ReproductorTheme {
                val estilo = viewModel.temaActual

                // Scaffold es la estructura original, la usamos de base
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Contenedor principal que reacciona al cambio de modo
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(estilo.fondo) // Aquí cambia el fondo
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // CABECERA DINÁMICA (Mitad 1: Marcador de posición)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "MODO: ${estilo.nombre}",
                                    color = estilo.acento,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }

                        // CONTROLES TÁCTILES ERGONÓMICOS
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { }, colors = ButtonDefaults.buttonColors(estilo.acento)) {
                                Text("<<")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            FloatingActionButton(onClick = { }, containerColor = estilo.acento) {
                                Text("▶")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = { }, colors = ButtonDefaults.buttonColors(estilo.acento)) {
                                Text(">>")
                            }
                        }

                        // EL BOTÓN DE CAMBIO (Vuestra especificación)
                        Button(
                            onClick = { viewModel.cambiarTema() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("CAMBIAR A MODO ${if (estilo.esNokia) "WMP" else "NOKIA"}")
                        }
                    }
                }
            }
        }
    }
}