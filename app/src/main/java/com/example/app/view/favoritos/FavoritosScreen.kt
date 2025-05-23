package com.example.app.view.favoritos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.app.R
import com.example.app.model.Evento
import com.example.app.model.Organizador
import com.example.app.routes.BottomNavigationBar
import com.example.app.routes.Routes
import com.example.app.util.formatDate
import com.example.app.util.getImageUrl
import com.example.app.viewmodel.favoritos.FavoritosViewModel
import com.example.app.view.EventoCard
import com.example.app.view.organizador.OrganizadorCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosScreen(
    navController: NavHostController,
    viewModel: FavoritosViewModel = viewModel()
) {
    val favoritos = viewModel.favoritos
    val isLoading = viewModel.isLoading
    val isError = viewModel.isError
    val errorMessage = viewModel.errorMessage
    
    val primaryColor = Color(0xFFE53935)
    val backgroundColor = Color.White
    val textPrimaryColor = Color.Black
    val textSecondaryColor = Color.DarkGray
    val successColor = Color(0xFF4CAF50)
    
    // Estado para controlar la pestaña seleccionada
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Efecto para recargar los favoritos cuando se muestra la pantalla
    LaunchedEffect(Unit) {
        viewModel.loadFavoritos()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.my_favorites),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor
                    ) 
                },
                actions = {
                    // Botón para recargar favoritos
                    IconButton(
                        onClick = { viewModel.loadFavoritos() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = primaryColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.reload_favorites),
                                tint = primaryColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = primaryColor
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                userRole = "Participante"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pestañas
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTabIndex])
                            .width(tabPositions[selectedTabIndex].width),
                        color = primaryColor,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.events_tab),
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTabIndex == 0) primaryColor else textSecondaryColor
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 0) primaryColor else textSecondaryColor
                        )
                    }
                )
                
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.organizers_tab),
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTabIndex == 1) primaryColor else textSecondaryColor
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 1) primaryColor else textSecondaryColor
                        )
                    }
                )
            }
            
            // Contenido según la pestaña seleccionada
            when (selectedTabIndex) {
                0 -> EventosFavoritosContent(
                    favoritos = favoritos,
                    isLoading = isLoading,
                    isError = isError,
                    errorMessage = errorMessage,
                    navController = navController,
                    primaryColor = primaryColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    successColor = successColor
                )
                1 -> OrganizadoresFavoritosContent(
                    navController = navController,
                    primaryColor = primaryColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun EventosFavoritosContent(
    favoritos: List<Evento>,
    isLoading: Boolean,
    isError: Boolean,
    errorMessage: String?,
    navController: NavHostController,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    successColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage ?: stringResource(R.string.error_unknown),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate(Routes.Favoritos.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            favoritos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.LightGray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.no_favorite_events),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate(Routes.Eventos.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text(stringResource(R.string.explore_events))
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoritos) { evento ->
                        EventoCard(
                            evento = evento,
                            onClick = {
                                navController.navigate(Routes.EventoDetalle.createRoute(evento.getEventoId().toString()))
                            },
                            primaryColor = primaryColor,
                            textPrimaryColor = textPrimaryColor,
                            textSecondaryColor = textSecondaryColor,
                            successColor = successColor,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizadoresFavoritosContent(
    navController: NavHostController,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    viewModel: FavoritosViewModel = viewModel()
) {
    val organizadores = viewModel.organizadoresFavoritos
    val isLoading = viewModel.isLoadingOrganizadores

    // Efecto para recargar cuando se muestra el contenido
    LaunchedEffect(Unit) {
        viewModel.loadOrganizadoresFavoritos()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            organizadores.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.LightGray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.no_favorite_organizers),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.organizers_explanation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate(Routes.Eventos.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text(stringResource(R.string.explore_events))
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    // Cabecera con botón de recarga
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.organizers_favorites_count, organizadores.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondaryColor
                        )
                        
                        IconButton(
                            onClick = { viewModel.loadOrganizadoresFavoritos() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.reload_favorites),
                                tint = primaryColor
                            )
                        }
                    }
                    
                    // Lista de organizadores
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(organizadores) { organizador ->
                            OrganizadorCard(
                                organizador = organizador,
                                onClick = {
                                    navController.navigate(Routes.OrganizadorDetalle.createRoute(organizador.id))
                                },
                                onFavoriteClick = {
                                    viewModel.toggleOrganizadorFavorito(organizador)
                                },
                                primaryColor = primaryColor,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor
                            )
                        }
                        
                        // Añadir espacio al final para scroll
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Botón para refrescar la lista de favoritos
                            Button(
                                onClick = { 
                                    viewModel.loadOrganizadoresFavoritos() 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.update_favorites))
                            }
                            
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventoCard(
    evento: Evento,
    onClick: () -> Unit,
    primaryColor: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    successColor: Color,
    navController: NavHostController
) {
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
            val imageUrl = evento.getImageUrl()
            
            // Imagen del evento
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.event_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
            )
            
            // Contenido del evento
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Categoría
                Text(
                    text = evento.categoria.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
                
                // Título
                Text(
                    text = evento.titulo.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = textPrimaryColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Fecha y hora
                Text(
                    text = "${formatDate(evento.fechaEvento.orEmpty())} · ${evento.hora.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondaryColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Ubicación
                Text(
                    text = evento.ubicacion.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Precio
                val precios = evento.entradas?.map { it.precio } ?: emptyList()
                val precioMinimo = if (precios.isEmpty()) 0.0 else precios.mapNotNull { it?.toDoubleOrNull() ?: 0.0 }.minOrNull() ?: 0.0
                val precioMaximo = if (precios.isEmpty()) 0.0 else precios.mapNotNull { it?.toDoubleOrNull() ?: 0.0 }.maxOrNull() ?: 0.0
                
                Text(
                    text = if (evento.entradas.isNullOrEmpty()) {
                        stringResource(R.string.not_available)
                    } else if (precioMinimo == 0.0 && precioMaximo == 0.0) {
                        stringResource(R.string.free)
                    } else if (precioMinimo == precioMaximo) {
                        "%.2f€".format(precioMinimo)
                    } else {
                        "%.2f€ - %.2f€".format(precioMinimo, precioMaximo)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (evento.entradas.isNullOrEmpty()) textSecondaryColor else if (precioMinimo == 0.0 && precioMaximo == 0.0) successColor else primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
