package com.example.reproductor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.reproductor.ui.theme.ReproductorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReproductorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReproductorTheme {
                // Estado unificado para levantar el BottomSheet en toda la App
                var mostrarBiblioteca by remember { mutableStateOf(false) }
                val estilo = viewModel.temaActual

                Box(modifier = Modifier.fillMaxSize()) {
                    if (estilo.esNokia) {
                        SeccionNokia(
                            viewModel = viewModel,
                            onAbrirBiblioteca = { mostrarBiblioteca = true } // Pasa la señal al Nokia
                        )
                    } else {
                        WmpThemeScreen(
                            viewModel = viewModel,
                            onAbrirBiblioteca = { mostrarBiblioteca = true } // Pasa la señal al Windows
                        )
                    }

                    // Hoja deslizable global superpuesta
                    if (mostrarBiblioteca) {
                        BibliotecaBottomSheet(
                            viewModel = viewModel,
                            onDismissRequest = { mostrarBiblioteca = false }
                        )
                    }
                }
            }
        }
    }
}