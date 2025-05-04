package com.example.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.app.model.Evento
import com.example.app.util.formatDate
import com.example.app.util.getImageUrl
import com.example.app.util.getHoraFormateada

@Composable
fun EventoCard(
    evento: Evento,
    onClick: () -> Unit,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    successColor: Color,
    navController: NavController,
    onEditEventoClick: ((Evento) -> Unit)? = null,
    onDeleteEventoClick: ((Evento) -> Unit)? = null,
    precioMin: Double? = null,
    precioMax: Double? = null
) {
    // Usar la función de extensión para obtener la URL de la imagen
    val imageUrl = evento.getImageUrl()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Imagen del evento
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Imagen del evento",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            
            // Información del evento
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Categoría
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(primaryColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = evento.categoria ?: "Sin categoría",
                        style = MaterialTheme.typography.labelMedium,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Título del evento
                Text(
                    text = evento.titulo ?: "Sin título",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textPrimaryColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Fecha y hora
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Fecha
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Fecha",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = formatDate(evento.fechaEvento ?: "", true),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondaryColor
                        )
                    }
                    
                    // Hora
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Hora",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = evento.getHoraFormateada(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondaryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = evento.ubicacion ?: "Sin ubicación",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textSecondaryColor
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Precio
                val precioTexto = when {
                    precioMin == null || precioMax == null -> "No disponible"
                    precioMin == 0.0 && precioMax == 0.0 -> "Gratuito"
                    precioMin == precioMax -> "%.2f€".format(precioMin)
                    else -> "%.2f€ - %.2f€".format(precioMin, precioMax)
                }
                Text(
                    text = precioTexto,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (precioTexto == "Gratuito") successColor else primaryColor
                )
                // Botones de editar y eliminar (solo si se pasan los callbacks)
                if (onEditEventoClick != null || onDeleteEventoClick != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (onEditEventoClick != null) {
                            TextButton(onClick = { onEditEventoClick(evento) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar evento",
                                    tint = primaryColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Editar", color = primaryColor)
                            }
                        }
                        if (onDeleteEventoClick != null) {
                            TextButton(onClick = { onDeleteEventoClick(evento) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar evento",
                                    tint = Color.Red
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eliminar", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
} 