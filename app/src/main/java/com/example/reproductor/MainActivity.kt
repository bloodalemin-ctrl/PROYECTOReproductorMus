package com.example.reproductor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.reproductor.ui.theme.ReproductorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ReproductorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReproductorTheme {
                val estilo = viewModel.temaActual

                if (estilo.esNokia) {

                    SeccionNokia(viewModel = viewModel)
                } else {

                    WmpThemeScreen(viewModel = viewModel)
                }
            }
        }
    }
}