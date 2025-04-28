package com.example.app.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.app.util.formatDate
import com.example.app.viewmodel.EventoDetailViewModel
import com.example.app.util.getImageUrl
import com.example.app.util.NotificationUtil
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import com.example.app.util.isValidEventoId
import com.example.app.util.getEventoIdErrorMessage
import com.example.app.routes.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoDetailScreen(
    navController: NavController,
    eventoId: String
) {
    val context = LocalContext.current
    val TAG = "EventoDetailScreen"

    // Verificar que el ID sea válido
    var idValido by remember { mutableStateOf(true) }
    
    LaunchedEffect(eventoId) {
        Log.d(TAG, "Inicializando pantalla con ID: '$eventoId' (${eventoId.javaClass.name})")
        
        // Usar extensión para validar ID
        idValido = eventoId.isValidEventoId()
        
        if (!idValido) {
            val errorMsg = eventoId.getEventoIdErrorMessage()
            Log.e(TAG, errorMsg)
        } else {
            Log.d(TAG, "ID validado correctamente")
        }
    }

    // Inicializar ViewModel
    val viewModel: EventoDetailViewModel = viewModel()
    
    // Cargar datos del evento solo si el ID es válido
    LaunchedEffect(eventoId, idValido) {
        if (idValido) {
            Log.d(TAG, "Cargando datos del evento con ID: $eventoId")
            viewModel.loadEvento(eventoId)
        } else {
            Log.w(TAG, "No se cargarán datos porque el ID no es válido: $eventoId")
        }
    }

    // Estados básicos
    val evento = viewModel.evento
    val isLoading = viewModel.isLoading
    val isError = viewModel.isError
    val errorMessage = viewModel.errorMessage
    
    // Colores de la app
    val primaryColor = Color(0xFFE53935)  // Rojo del logo
    val backgroundColor = Color.White
    val textPrimaryColor = Color.Black
    val textSecondaryColor = Color.DarkGray
    val successColor = Color(0xFF4CAF50)  // Verde para elementos gratuitos
    
    // Estados del proceso de compra
    val showPaymentDialog = viewModel.showPaymentDialog.collectAsState()
    val compraProcesando = viewModel.compraProcesando.collectAsState()
    val compraExitosa = viewModel.compraExitosa.collectAsState()
    val mensajeCompra = viewModel.mensajeCompra.collectAsState()
    
    // Mostrar notificación cuando la compra es exitosa
    LaunchedEffect(compraExitosa.value) {
        if (compraExitosa.value) {
            val titulo = viewModel.evento?.titulo ?: "evento"
            NotificationUtil.createNotificationChannel(context)
            NotificationUtil.showCompraExitosaNotification(context, titulo)
        }
    }
    
    // Formato para mostrar precios
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    
    // Mostrar diálogo de compra si es necesario
    if (showPaymentDialog.value) {
        PaymentDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.cerrarDialogoPago() },
            formatoMoneda = formatoMoneda,
            primaryColor = primaryColor
        )
    }
    
    // Pantalla principal
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Scaffold(
            // Barra superior
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = "DETALLE EVENTO",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 1.sp
                            ),
                            color = primaryColor
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Volver",
                                tint = primaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = primaryColor,
                        navigationIconContentColor = primaryColor
                    ),
                    actions = {
                        // Botón de Favorito (solo para participantes)
                        if (viewModel.puedeMarcarFavorito && evento != null) {
                            val isFavorito = evento.isFavorito
                            val toggleFavoritoLoading = viewModel.toggleFavoritoLoading.collectAsState()
                            
                            IconButton(
                                onClick = { viewModel.toggleFavorito() },
                                enabled = !toggleFavoritoLoading.value
                            ) {
                                if (toggleFavoritoLoading.value) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = primaryColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isFavorito) "Quitar de favoritos" else "Añadir a favoritos",
                                        tint = if (isFavorito) primaryColor else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Pantalla de ID inválido
                if (!idValido) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "ID de evento inválido: '$eventoId'",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Volver",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                // Pantalla de carga
                else if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } 
                // Pantalla de error
                else if (isError) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Error desconocido",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Volver",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                } 
                // Detalles del evento
                else if (evento != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Imagen del evento
                        val imageUrl = evento.getImageUrl()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Imagen del evento",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Categoría
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopStart)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(primaryColor.copy(alpha = 0.8f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = evento.categoria,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            navController.navigate(
                                                com.example.app.routes.Routes.EventosCategoria.createRoute(evento.categoria)
                                            )
                                        }
                                )
                            }
                        }
                        
                        // Contenido del evento
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-20).dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                    spotColor = Color.Black.copy(alpha = 0.2f)
                                ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                // Título
                                Text(
                                    text = evento.titulo,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Información de fecha, hora y ubicación
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF8F8F8)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        // Fecha
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Fecha",
                                                tint = primaryColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Text(
                                                text = formatDate(evento.fechaEvento, true),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = textPrimaryColor
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Hora
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Hora",
                                                tint = primaryColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Asegurar que la hora siempre tenga formato HH:MM
                                            val formattedHora = if (evento.hora.contains(":")) {
                                                val parts = evento.hora.split(":")
                                                val hours = parts[0].padStart(2, '0')
                                                val minutes = if (parts.size > 1) parts[1].padStart(2, '0') else "00"
                                                "$hours:$minutes"
                                            } else {
                                                evento.hora.padStart(2, '0') + ":00"
                                            }
                                            
                                            Text(
                                                text = formattedHora,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = textPrimaryColor
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Ubicación
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Ubicación",
                                                tint = primaryColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Text(
                                                text = evento.ubicacion,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = textPrimaryColor
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Organizador clicable
                                evento.organizador?.let { organizador ->
                                    Text(
                                        text = "Organizador",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimaryColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                navController.navigate(
                                                    Routes.OrganizadorDetalle.createRoute(organizador.id)
                                                )
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = organizador.nombre,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimaryColor
                                            )
                                            organizador.user?.let { user ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = user.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = textSecondaryColor
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Descripción
                                Text(
                                    text = "Descripción",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimaryColor
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = evento.descripcion,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textSecondaryColor
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Tipos de entrada (solo si no es online o tiene entradas)
                                if (!evento.esOnline && viewModel.tiposEntrada.isNotEmpty()) {
                                    Text(
                                        text = "Tipos de entrada",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimaryColor
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Lista de tipos de entrada
                                    viewModel.tiposEntrada.forEach { tipoEntrada ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFFF5F5F5)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                // Nombre y descripción
                                                Text(
                                                    text = tipoEntrada.nombre,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                
                                                if (tipoEntrada.descripcion != null) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = tipoEntrada.descripcion,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = textSecondaryColor
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                
                                                // Disponibilidad
                                                val disponibilidadText = if (tipoEntrada.esIlimitado) {
                                                    "Disponibilidad: Ilimitada"
                                                } else {
                                                    val disponibles = tipoEntrada.disponibilidad ?: 
                                                        (tipoEntrada.cantidadDisponible?.minus(tipoEntrada.entradasVendidas) ?: 0)
                                                    "Disponibles: $disponibles"
                                                }
                                                
                                                Text(
                                                    text = disponibilidadText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = textSecondaryColor
                                                )
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                // Fila de precio y controles
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Precio del tipo de entrada
                                                    Text(
                                                        text = formatoMoneda.format(tipoEntrada.precio.toDoubleOrNull() ?: 0.0),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = primaryColor
                                                    )
                                                    
                                                    // Cantidad actual
                                                    val cantidad = viewModel.obtenerCantidad(tipoEntrada.id)
                                                    
                                                    // Control de cantidad
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,
                                                        modifier = Modifier
                                                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            .height(44.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { viewModel.decrementarCantidad(tipoEntrada.id) },
                                                            enabled = cantidad > 0,
                                                            modifier = Modifier.size(42.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Disminuir",
                                                                tint = if (cantidad > 0) primaryColor else Color.Gray.copy(alpha = 0.5f),
                                                                modifier = Modifier.size(30.dp)
                                                            )
                                                        }
                                                        
                                                        Text(
                                                            text = if (cantidad > 0) "${cantidad}x" else "0",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 12.dp),
                                                            fontSize = 20.sp
                                                        )
                                                        
                                                        IconButton(
                                                            onClick = { viewModel.incrementarCantidad(tipoEntrada.id) },
                                                            modifier = Modifier.size(42.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Aumentar",
                                                                tint = primaryColor,
                                                                modifier = Modifier.size(30.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Mostrar total y contador de entradas
                                    if (viewModel.hayEntradasSeleccionadas()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = primaryColor.copy(alpha = 0.1f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                // Contador de entradas
                                                val totalEntradas = viewModel.tiposEntrada.sumOf { viewModel.obtenerCantidad(it.id) }
                                                Text(
                                                    text = "Entradas seleccionadas: $totalEntradas",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                // Total
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Total",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    
                                                    Text(
                                                        text = formatoMoneda.format(viewModel.calcularTotal()),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = primaryColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (!evento.esOnline && evento.entradas.isNotEmpty()) {
                                    // Fallback si no se pudieron cargar los tipos detallados pero hay entradas básicas
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Text(
                                        text = "Entradas disponibles",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimaryColor
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Mostrar mensaje simplificado si no tenemos tipos detallados
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F5)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "Este evento tiene ${evento.entradas.size} tipo(s) de entrada",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                // Botón de compra
                                Button(
                                    onClick = { viewModel.mostrarDialogoPago() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .then(
                                            if (viewModel.hayEntradasSeleccionadas()) {
                                                Modifier.shadow(8.dp, RoundedCornerShape(8.dp))
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor,
                                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                                        contentColor = Color.White,
                                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = viewModel.hayEntradasSeleccionadas(),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = if (viewModel.hayEntradasSeleccionadas()) 4.dp else 0.dp,
                                        pressedElevation = if (viewModel.hayEntradasSeleccionadas()) 8.dp else 0.dp
                                    )
                                ) {
                                    Text(
                                        text = "COMPRAR",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            fontSize = 18.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                } 
                // Evento no encontrado
                else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontró información del evento",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    viewModel: EventoDetailViewModel,
    onDismiss: () -> Unit,
    formatoMoneda: NumberFormat,
    primaryColor: Color
) {
    val compraProcesando = viewModel.compraProcesando.collectAsState()
    val compraExitosa = viewModel.compraExitosa.collectAsState()
    val mensajeCompra = viewModel.mensajeCompra.collectAsState()
    
    Dialog(onDismissRequest = {
        if (!compraProcesando.value) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título
                Text(
                    text = if (compraExitosa.value) "¡Compra Exitosa!" else "Confirmar Compra",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (compraExitosa.value) Color(0xFF4CAF50) else primaryColor
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (compraExitosa.value) {
                    // Icono de éxito
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Compra exitosa",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mensaje de éxito
                    Text(
                        text = mensajeCompra.value,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                } else if (compraProcesando.value) {
                    // Indicador de carga
                    CircularProgressIndicator(
                        color = primaryColor
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Procesando tu compra...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Resumen de la compra
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Lista de entradas seleccionadas
                        viewModel.tiposEntrada.forEach { tipoEntrada ->
                            val cantidad = viewModel.obtenerCantidad(tipoEntrada.id)
                            if (cantidad > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${tipoEntrada.nombre} x $cantidad",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    
                                    val precio = tipoEntrada.precio.toDoubleOrNull() ?: 0.0
                                    Text(
                                        text = formatoMoneda.format(precio * cantidad),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Color.LightGray
                        )
                        
                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = formatoMoneda.format(viewModel.calcularTotal()),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryColor
                            )
                        ) {
                            Text(text = "Cancelar")
                        }
                        
                        Button(
                            onClick = { viewModel.realizarCompra() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            )
                        ) {
                            Text(text = "Confirmar")
                        }
                    }
                }
                
                if (mensajeCompra.value.isNotEmpty() && !compraExitosa.value) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = mensajeCompra.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
} 