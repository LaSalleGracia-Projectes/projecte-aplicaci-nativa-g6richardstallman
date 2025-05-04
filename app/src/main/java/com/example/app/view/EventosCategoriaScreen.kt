package com.example.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.components.EventoCard
import com.example.app.viewmodel.EventosCategoriaViewModel
import com.example.app.routes.Routes
import com.example.app.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventosCategoriaScreen(
    categoria: String,
    navController: NavController,
    viewModel: EventosCategoriaViewModel = viewModel()
) {
    val eventos = viewModel.eventos
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val scope = rememberCoroutineScope()
    var preciosEventos by remember { mutableStateOf<Map<Long, Pair<Double?, Double?>>>(emptyMap()) }

    // Cargar eventos al entrar
    LaunchedEffect(categoria) {
        viewModel.loadEventosByCategoria(categoria)
    }

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
                        preciosEventos = preciosEventos + (id to (min to max))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val primaryColor = Color(0xFFE53935)
    val backgroundColor = Color.White
    val textPrimaryColor = Color.Black
    val textSecondaryColor = Color.DarkGray
    val successColor = Color(0xFF4CAF50)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = categoria.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = primaryColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = primaryColor,
                        navigationIconContentColor = primaryColor
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (errorMessage != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $errorMessage", color = Color.Red, modifier = Modifier.padding(16.dp))
                    }
                } else if (eventos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No hay eventos en esta categoría",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondaryColor
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(eventos) { evento ->
                            val precios = preciosEventos[evento.getEventoId().toLong()]
                            EventoCard(
                                evento = evento,
                                onClick = {
                                    navController.navigate(Routes.EventoDetalle.createRoute(evento.getEventoId().toString()))
                                },
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
