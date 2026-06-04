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
                // Estados unificados para levantar ambos menús en la app
                var mostrarBiblioteca by remember { mutableStateOf(false) }
                var mostrarSelectorModos by remember { mutableStateOf(false) }

                val estiloActual = viewModel.temaActual.modo

                Box(modifier = Modifier.fillMaxSize()) {

                    // Ruteador central de vistas
                    when (estiloActual) {
                        TipoModo.NOKIA -> SeccionNokia(
                            viewModel = viewModel,
                            onAbrirBiblioteca = { mostrarBiblioteca = true },
                            onAbrirModos = { mostrarSelectorModos = true }
                        )
                        TipoModo.WINDOWS -> WmpThemeScreen(
                            viewModel = viewModel,
                            onAbrirBiblioteca = { mostrarBiblioteca = true },
                            onAbrirModos = { mostrarSelectorModos = true }
                        )
                        TipoModo.CLASSIC_POD -> ClassicPodScreen(
                            viewModel = viewModel,
                            onAbrirBiblioteca = { mostrarBiblioteca = true },
                            onAbrirModos = { mostrarSelectorModos = true }
                        )
                        TipoModo.GAMMING -> GammingModoScreen(
                            viewModel = viewModel,
                            onAbrirBiblioteca = { mostrarBiblioteca = true },
                            onAbrirModos = { mostrarSelectorModos = true }
                        )
                    }

                    // Menú Desplegable 1: La Biblioteca Musical
                    if (mostrarBiblioteca) {
                        BibliotecaBottomSheet(
                            viewModel = viewModel,
                            onDismissRequest = { mostrarBiblioteca = false }
                        )
                    }

                    // Menú Desplegable 2: El Selector de Diseños (Modos)
                    if (mostrarSelectorModos) {
                        SelectorModoBottomSheet(
                            viewModel = viewModel,
                            onDismissRequest = { mostrarSelectorModos = false }
                        )
                    }
                }
            }
        }
    }
}