package com.example.reproductor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorModoBottomSheet(
    viewModel: ReproductorViewModel,
    onDismissRequest: () -> Unit
) {
    val modoActual = viewModel.temaActual.modo

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = " Seleccionar Modo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Iteramos sobre todos los modos que existen
            TipoModo.values().forEach { modo ->
                val esElActual = modoActual == modo

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (esElActual) Color(0xFF4FC3F7).copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            viewModel.cambiarModo(modo)
                            onDismissRequest() // Cierra el menú al elegir
                        }
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = modo.icono, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(15.dp))
                    Text(
                        text = modo.titulo,
                        fontSize = 16.sp,
                        fontWeight = if (esElActual) FontWeight.Bold else FontWeight.Normal,
                        color = if (esElActual) Color(0xFF4FC3F7) else Color.LightGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}