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
import com.example.app.components.EventoCard
import com.example.app.model.Evento
import com.example.app.model.getImageUrl
import com.example.app.routes.BottomNavigationBar
import com.example.app.util.SessionManager
import com.example.app.util.Constants
import com.example.app.util.getHoraFormateada
import com.example.app.viewmodel.EventoViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.app.api.RetrofitClient
import kotlinx.coroutines.launch

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
    
    // Verificar si el usuario es organizador
    val userRole = SessionManager.getUserRole() ?: "participante"
    val isOrganizador = userRole == "organizador"
    
    val scope = rememberCoroutineScope()
    var preciosEventos by remember { mutableStateOf<Map<Long, Pair<Double?, Double?>>>(emptyMap()) }

    // Cargar precios cuando cambian los eventos
    LaunchedEffect(eventos) {
        eventos.forEach { evento ->
            val id = evento.getEventoId().toLong()
            if (id > 0 && !preciosEventos.containsKey(id)) {
                scope.launch {
                    try {
                        val minResponse = RetrofitClient.apiService.getPrecioMinimoEvento(id)
                        val maxResponse = RetrofitClient.apiService.getPrecioMaximoEvento(id)
                        val min = minResponse.evento.precio_minimo?.toDoubleOrNull()
                        val max = maxResponse.evento.precio_maximo?.toDoubleOrNull()
                        Log.d("PRECIOS_API", "Evento $id -> min: $min, max: $max")
                        preciosEventos = preciosEventos + (id to (min to max))
                    } catch (e: Exception) {
                        Log.e("PRECIOS_API", "Error obteniendo precios para evento $id", e)
                    }
                }
            }
        }
    }
    
    // Efecto para mostrar mensaje cuando se elimina un evento
    LaunchedEffect(eventoEliminado) {
        if (eventoEliminado) {
            Toast.makeText(
                context,
                mensajeExito,
                Toast.LENGTH_SHORT
            ).show()
            
            viewModel.resetEventoEliminado()
        }
    }
    
    // Efecto para mostrar mensajes de error
    LaunchedEffect(viewModel.isError) {
        if (viewModel.isError && !viewModel.errorMessage.isNullOrEmpty()) {
            val errorMsg = viewModel.errorMessage ?: "Error desconocido"
            
            if (errorMsg.contains("no existe o ya fue eliminado")) {
                viewModel.fetchMisEventos()
                
                val customToast = Toast.makeText(
                    context,
                    errorMsg,
                    Toast.LENGTH_LONG
                )
                
                customToast.show()
            } else {
                Toast.makeText(
                    context,
                    errorMsg,
                    Toast.LENGTH_LONG
                ).show()
            }
            
            viewModel.clearError()
            
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
                            val precios = preciosEventos[evento.getEventoId().toLong()]
                            EventoCard(
                                evento = evento,
                                onClick = { onEventoClick(evento) },
                                primaryColor = primaryColor,
                                textPrimaryColor = Color.Black,
                                textSecondaryColor = Color.DarkGray,
                                successColor = Color(0xFF4CAF50),
                                navController = navController,
                                onEditEventoClick = onEditEventoClick,
                                onDeleteEventoClick = {
                                    eventoToDelete = it
                                    showDeleteConfirmDialog = true
                                },
                                precioMin = precios?.first,
                                precioMax = precios?.second
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
    val eventoId = evento.getEventoId().toLong()
    
    Log.d("EditEvento", "======== INICIANDO EDICIÓN DE EVENTO ========")
    Log.d("EditEvento", "ID del evento: $eventoId")
    Log.d("EditEvento", "Título: ${evento.titulo}")
    Log.d("EditEvento", "ID original: ${evento.id}, idEvento: ${evento.id}")
    
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