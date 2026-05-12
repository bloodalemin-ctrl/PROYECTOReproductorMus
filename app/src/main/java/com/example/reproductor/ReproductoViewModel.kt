package com.example.reproductor

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

class ReproductorViewModel : ViewModel() {
    // Usamos la clase que ya definimos en Estilos.kt
    var temaActual by mutableStateOf<ModoEstilo>(ModoEstilo.WMP)
        private set

    fun cambiarTema() {
        temaActual = if (temaActual is ModoEstilo.WMP) ModoEstilo.Nokia else ModoEstilo.WMP
    }
}