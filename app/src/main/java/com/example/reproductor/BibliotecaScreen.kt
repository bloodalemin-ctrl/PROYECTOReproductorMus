package com.example.reproductor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibliotecaBottomSheet(
    viewModel: ReproductorViewModel,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF121212), // Fondo oscuro premium estilo streaming
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.6f) // Opacidad de fondo al desplegarse
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // TÍTULO DE LA HOJA
            Text(
                text = "🎵 Mi Biblioteca",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            val canciones = viewModel.listaCanciones

            if (canciones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay canciones cargadas.\nUsa el botón de la carpeta para añadir música.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // LISTA OPTIMIZADA DESLIZABLE (LazyColumn actúa como RecyclerView)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(canciones) { index, cancion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.reproducirCancionEnPosicion(index)
                                    onDismissRequest() // Oculta el panel tras seleccionar pista
                                }
                                .padding(vertical = 10.dp, horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Validar si la pista de la lista coincide con la que suena en background
                            val esLaActual = viewModel.currentTitle == cancion.titulo

                            Text(
                                text = if (esLaActual) "▶ " else "🎵 ",
                                fontSize = 18.sp,
                                color = if (esLaActual) Color(0xFF4FC3F7) else Color.Gray
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cancion.titulo,
                                    fontSize = 16.sp,
                                    fontWeight = if (esLaActual) FontWeight.Bold else FontWeight.Normal,
                                    color = if (esLaActual) Color(0xFF4FC3F7) else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cancion.artista,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF222222))
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp)) // Amortiguador para barras de navegación
        }
    }
}