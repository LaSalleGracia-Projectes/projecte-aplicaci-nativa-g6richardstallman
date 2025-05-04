package com.example.app.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.app.R
import com.example.app.model.Evento
import com.example.app.util.formatDate
import com.example.app.viewmodel.EventoViewModel
import kotlinx.coroutines.launch
import com.example.app.routes.BottomNavigationBar
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.util.Log
import com.example.app.util.SessionManager
import com.example.app.util.Constants
import com.example.app.util.getImageUrl
import com.example.app.routes.Routes
import com.example.app.components.EventoCard
import com.example.app.api.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventosScreen(
    onEventoClick: (Evento) -> Unit,
    navController: NavController,
    viewModel: EventoViewModel = viewModel()
) {
    val eventos = viewModel.eventos
    val isLoading = viewModel.isLoading
    val isError = viewModel.isError
    val errorMessage = viewModel.errorMessage
    val userRole = SessionManager.getUserRole() ?: "participante"
    
    // Actualizar eventos cada vez que se muestra la pantalla
    LaunchedEffect(Unit) {
        Log.d("EventosScreen", "Actualizando lista de eventos...")
        viewModel.fetchEventos()
    }
    
    Log.d("EventosScreen", "Rol del usuario: $userRole")

    // Colores consistentes con la app
    val primaryColor = Color(0xFFE53935)
    val backgroundColor = Color.White
    val textPrimaryColor = Color.Black
    val textSecondaryColor = Color.DarkGray
    val successColor = Color(0xFF4CAF50)

    // Estado para el texto de búsqueda
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var preciosEventos by remember { mutableStateOf<Map<Long, Pair<Double?, Double?>>>(emptyMap()) }

    // Filtrar eventos basados en el texto de búsqueda
    val filteredEventos = remember(eventos, searchText) {
        if (searchText.isEmpty()) {
            eventos
        } else {
            eventos.filter { evento ->
                evento.titulo?.contains(searchText, ignoreCase = true) == true ||
                evento.descripcion?.contains(searchText, ignoreCase = true) == true ||
                evento.categoria?.contains(searchText, ignoreCase = true) == true
            }
        }
    }

    // Estado para el botón de scroll hacia arriba
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    // Modificar la lógica de visibilidad de la barra de búsqueda
    val showSearchBar by remember {
        derivedStateOf {
            showScrollToTop || searchText.isNotEmpty()
        }
    }

    // Cargar precios cuando cambian los eventos
    LaunchedEffect(eventos) {
        eventos.forEach { evento ->
            val id = evento.getEventoId().toLong()
            if (id > 0 && !preciosEventos.containsKey(id)) {
                coroutineScope.launch {
                    try {
                        val minResponse = RetrofitClient.apiService.getPrecioMinimoEvento(id)
                        val maxResponse = RetrofitClient.apiService.getPrecioMaximoEvento(id)
                        val min = minResponse.evento.precio_minimo?.toDoubleOrNull()
                        val max = maxResponse.evento.precio_maximo?.toDoubleOrNull()
                        preciosEventos = preciosEventos + (id to (min to max))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    Scaffold(
        // Barra superior
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "EVENTOS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor,
                        modifier = Modifier.padding(start = 8.dp)
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,  // Fondo blanco para la barra superior
                    titleContentColor = primaryColor
                )
            )
        },
        // Barra de navegación inferior
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
            // Pantalla de carga
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            } 
            // Pantalla de error
            else if (isError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error desconocido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            } 
            // Lista de eventos
            else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Barra de búsqueda
                    AnimatedVisibility(
                        visible = showSearchBar,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                placeholder = { Text("Buscar eventos") },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchText = "" }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar búsqueda",
                                                tint = Color(0xFFE53935) // Color rojo del logo
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE53935),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                    focusedLabelColor = Color(0xFFE53935),
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                        }
                    }
                    
                    // Lista de eventos
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredEventos) { evento ->
                            val precios = preciosEventos[evento.getEventoId().toLong()]
                            EventoCard(
                                evento = evento,
                                onClick = { onEventoClick(evento) },
                                primaryColor = primaryColor,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                successColor = successColor,
                                navController = navController,
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