package com.example.app.view.organizador

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.app.model.Organizador
import coil.compose.SubcomposeAsyncImage

@Composable
fun OrganizadorCard(
    organizador: Organizador,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del organizador
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(organizador.obtenerAvatarUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar de ${organizador.nombre}",
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                error = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Avatar por defecto",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                },
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )
            
            // Información del organizador
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = organizador.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Etiqueta de favorito
                if (organizador.isFavorite) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF0000),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Favorito",
                            fontSize = 12.sp,
                            color = Color(0xFFFF0000),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                organizador.nombreUsuario?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = textSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = "Tel: ${organizador.telefonoContacto}",
                    fontSize = 14.sp,
                    color = textSecondaryColor
                )
            }
            
            // Botón de favorito
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (organizador.isFavorite) Color(0xFFFFEAEA) else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (organizador.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (organizador.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (organizador.isFavorite) Color(0xFFFF0000) else textSecondaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
} 