package com.example.app.view

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app.model.Evento
import com.example.app.model.getImageUrl
import com.example.app.routes.BottomNavigationBar
import com.example.app.util.SessionManager
import com.example.app.viewmodel.EventoViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.app.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisEventosScreen(
    navController: NavController,
    onEventoClick: (Evento) -> Unit,
    onCreateEventoClick: () -> Unit,
    onEditEventoClick: (Evento) -> Unit,
    onDeleteEventoClick: (Evento) -> Unit = {}
) {
    val viewModel: EventoViewModel = viewModel()
    val eventos = viewModel.misEventos
    val isLoading = viewModel.isLoading
    val isError = viewModel.isError
    
    // Estado para el diálogo de confirmación de eliminación
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var eventoToDelete by remember { mutableStateOf<Evento?>(null) }
    
    // Observar el resultado de la eliminación
    val eventoEliminado by viewModel.eventoEliminadoExitosamente.collectAsState()
    
    // Observar el mensaje de éxito personalizado del backend
    val mensajeExito by viewModel.successMessage.collectAsState()
    
    // Referencia al contexto fuera del LaunchedEffect
    val context = LocalContext.current
    
    // Verificar si el usuario es organizador - Movido aquí arriba
    val userRole = SessionManager.getUserRole() ?: "participante"
    val isOrganizador = userRole == "organizador"
    
    // Efecto para mostrar mensaje cuando se elimina un evento
    LaunchedEffect(eventoEliminado) {
        if (eventoEliminado) {
            // Mostrar mensaje de éxito con el texto del backend
            Toast.makeText(
                context,
                mensajeExito,
                Toast.LENGTH_SHORT
            ).show()
            
            // Resetear el estado
            viewModel.resetEventoEliminado()
        }
    }
    
    // Efecto para mostrar mensajes de error
    LaunchedEffect(viewModel.isError) {
        if (viewModel.isError && !viewModel.errorMessage.isNullOrEmpty()) {
            // Añadir verificación explícita para el caso de "evento no existe"
            val errorMsg = viewModel.errorMessage ?: "Error desconocido"
            
            // Si el error indica que el evento ya fue eliminado, tratarlo como éxito
            if (errorMsg.contains("no existe o ya fue eliminado")) {
                // Actualizar la lista de eventos para quitar el que ya no existe
                viewModel.fetchMisEventos()
                
                // Mostrar un toast informativo en verde
                val customToast = Toast.makeText(
                    context,
                    errorMsg,
                    Toast.LENGTH_LONG
                )
                // Nota: en una implementación real, necesitaríamos una vista personalizada
                // para el Toast con fondo verde, pero omitimos eso por simplicidad
                
                customToast.show()
            } else {
                // Para otros errores, mostrar el mensaje normal
                Toast.makeText(
                    context,
                    errorMsg,
                    Toast.LENGTH_LONG
                ).show()
            }
            
            // Limpiar error después de mostrarlo
            viewModel.clearError()
            
            // Actualizar la lista de eventos también después de cualquier error
            if (isOrganizador && errorMsg.contains("eliminar")) {
                Log.d("MisEventosScreen", "Actualizando lista tras error relacionado con eliminación")
                viewModel.fetchMisEventos()
            }
        }
    }
    
    // Actualizar eventos cada vez que se muestra la pantalla o al eliminar un evento
    LaunchedEffect(Unit, eventoEliminado) {
        Log.d("MisEventosScreen", "Actualizando lista de mis eventos...")
        if (isOrganizador) {
            viewModel.fetchMisEventos()
        }
    }

    // Colores consistentes con la app
    val primaryColor = Color(0xFFE53935)
    val backgroundColor = Color.White

    // Diálogo de confirmación de eliminación
    if (showDeleteConfirmDialog && eventoToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false
                eventoToDelete = null
            },
            title = { Text("Eliminar evento") },
            text = { Text("¿Estás seguro que deseas eliminar el evento '${eventoToDelete?.titulo}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Ejecutar la eliminación
                        eventoToDelete?.let { evento ->
                            viewModel.deleteEvento(evento)
                        }
                        showDeleteConfirmDialog = false
                        eventoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmDialog = false
                        eventoToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        // Barra superior con título y botón de crear
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "MIS EVENTOS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor
                    ) 
                },
                actions = {
                    // Botón para crear nuevo evento
                    IconButton(
                        onClick = onCreateEventoClick,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Crear Evento",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = primaryColor
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                userRole = userRole
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Pantalla de carga
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
                // Pantalla de error
                isError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = viewModel.errorMessage ?: "Error desconocido",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            
                            if (viewModel.errorMessage?.contains("Solo los organizadores") == true) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Esta sección está disponible solo para organizadores.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                // Lista de eventos vacía
                eventos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No has creado ningún evento todavía",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCreateEventoClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear mi primer evento")
                            }
                        }
                    }
                }
                // Lista de eventos
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(eventos) { evento ->
                            // Agregar log especializado para el evento EVENTOSINFOTO
                            if (evento.titulo.contains("EVENTOSINFOTO", ignoreCase = true)) {
                                Log.d("MisEventosScreen", "Evento especial encontrado: '${evento.titulo}'")
                                Log.d("MisEventosScreen", "Detalles de EVENTOSINFOTO - ID: ${evento.getEventoId()}, Categoría: ${evento.categoria}")
                            }
                            
                            EventoCardConAcciones(
                                evento = evento,
                                onClick = { 
                                    val eventoId = evento.getEventoId()
                                    if (eventoId > 0) {
                                        navController.navigate("evento/$eventoId")
                                    } else {
                                        // Opcional: mostrar un Toast de error
                                    }
                                },
                                onEditClick = { onEditEventoClick(evento) },
                                onDeleteClick = { 
                                    // Mostrar diálogo de confirmación
                                    eventoToDelete = evento
                                    showDeleteConfirmDialog = true
                                },
                                primaryColor = primaryColor,
                                textPrimaryColor = Color.Black,
                                textSecondaryColor = Color.DarkGray,
                                successColor = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Función para editar evento con manejo mejorado de errores
private fun editarEvento(evento: Evento, navController: NavController) {
    // Obtener el ID correcto usando getEventoId()
    val eventoId = evento.getEventoId()
    
    // Logging detallado para debugging
    Log.d("EditEvento", "======== INICIANDO EDICIÓN DE EVENTO ========")
    Log.d("EditEvento", "ID del evento: $eventoId")
    Log.d("EditEvento", "Título: ${evento.titulo}")
    Log.d("EditEvento", "ID original: ${evento.idEvento}, idEvento: ${evento.idEvento}")
    
    // Validar ID y navegar directamente, siguiendo el mismo patrón que funciona en detalle
    try {
        val idString = eventoId.toString()
        Log.d("EditEvento", "ID convertido a string: '$idString'")
        
        val route = com.example.app.routes.Routes.EditarEvento.createRoute(idString)
        Log.d("EditEvento", "Ruta creada: $route")
        
        navController.navigate(route) {
            launchSingleTop = true
        }
        Log.d("EditEvento", "Navegación completada exitosamente")
    } catch (e: Exception) {
        Log.e("EditEvento", "Error al navegar", e)
        Toast.makeText(
            navController.context,
            "Error al abrir pantalla de edición: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

// Componente para mostrar un evento con botones de acciones
@Composable
fun EventoCardConAcciones(
    evento: Evento,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    successColor: Color
) {
    // Obtener el ID correcto para logging
    val eventoId = evento.getEventoId()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Contenido principal (clickeable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                // Imagen del evento
                AsyncImage(
                    model = evento.getImageUrl(),
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
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Categoría
                    Text(
                        text = evento.categoria.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Título del evento
                    Text(
                        text = evento.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = textPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Fecha
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Fecha",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = formatDate(evento.fechaEvento),
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondaryColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Hora
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Hora",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = formatTime(evento.hora),
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondaryColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Ubicación
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = if (evento.esOnline) "Online" else evento.ubicacion,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondaryColor
                        )
                    }
                }
            }
            
            // Barra de acciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Espacio flexible a la izquierda
                Spacer(modifier = Modifier.weight(1f))
                
                // Botón de editar (ahora al centro)
                TextButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = primaryColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar evento",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
                
                // Botón de eliminar (a la derecha)
                TextButton(
                    onClick = { 
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar evento",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

// Función auxiliar para formatear la fecha
private fun formatDate(dateString: String, includeDay: Boolean = false): String {
    try {
        val inputFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
        val date = inputFormat.parse(dateString) ?: return dateString
        
        val outputFormat = if (includeDay) {
            SimpleDateFormat(Constants.DISPLAY_DATE_WITH_DAY_FORMAT, Locale(SessionManager.getUserLanguage() ?: "es"))
        } else {
            SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT, Locale(SessionManager.getUserLanguage() ?: "es"))
        }
        
        return capitalizeWords(outputFormat.format(date))
    } catch (e: Exception) {
        Log.e("EventoCard", "Error formateando fecha: $e")
        return dateString
    }
}

// Función para capitalizar la primera letra de cada palabra sin invocar funciones @Composable
private fun capitalizeWords(text: String): String {
    return text.split(" ").joinToString(" ") { word ->
        if (word.isNotEmpty()) word.replaceFirstChar { it.uppercase() } else word
    }
}

// Función para formatear la hora en formato HH:MM
private fun formatTime(timeString: String): String {
    return try {
        // Dividir la hora por los dos puntos
        val parts = timeString.split(":")
        
        // Si tiene formato HH:MM o HH:MM:SS, extraer solo HH:MM
        if (parts.size >= 2) {
            val hour = parts[0].padStart(2, '0')
            val minute = parts[1].padStart(2, '0')
            "$hour:$minute"
        } else {
            // Si no tiene el formato esperado, devolver la original
            Log.e("EventoCard", "Formato de hora inesperado: $timeString")
            timeString
        }
    } catch (e: Exception) {
        // En caso de cualquier error, devolver la hora original
        Log.e("EventoCard", "Error formateando hora: $e")
        timeString
    }
} 