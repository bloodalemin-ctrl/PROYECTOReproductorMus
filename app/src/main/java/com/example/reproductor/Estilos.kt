package com.example.reproductor

import androidx.compose.ui.graphics.Color


sealed class ModoEstilo(
    val nombre: String,
    val fondo: Color,
    val acento: Color,
    val esNokia: Boolean
) {
    object WMP : ModoEstilo("WMP 2003", Color(0xFF0055E5), Color(0xFF3399FF), false)
    object Nokia : ModoEstilo("Nokia Retro", Color(0xFF001244), Color(0xFFD32F2F), true)
}