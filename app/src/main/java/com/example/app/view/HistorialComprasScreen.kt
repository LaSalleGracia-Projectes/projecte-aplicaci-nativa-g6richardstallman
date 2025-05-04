package com.example.app.view

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.app.routes.BottomNavigationBar
import com.example.app.routes.Routes
import com.example.app.viewmodel.CompraItem
import com.example.app.viewmodel.HistorialComprasViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// Colores consistentes con la app
private val primaryColor = Color(0xFFE53935)  // Rojo del logo
private val backgroundColor = Color.White
private val textPrimaryColor = Color.Black
private val textSecondaryColor = Color.DarkGray
private val surfaceColor = Color(0xFFF5F5F5)  // Gris muy claro para fondos
private val successColor = Color(0xFF4CAF50)  // Verde para estados exitosos
private val warningColor = Color(0xFFFFA000)  // Ámbar para estados pendientes

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialComprasScreen(
    navController: NavController,
    viewModel: HistorialComprasViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HistorialComprasViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val compras by viewModel.compras.collectAsState()
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    val errorMessageState by remember { derivedStateOf { viewModel.errorMessage } }
    val isDownloading by viewModel.isDownloadingPdf.collectAsState()
    val downloadMessage by viewModel.downloadMessage.collectAsState()
    val shouldNavigateToLogin by viewModel.shouldNavigateToLogin.collectAsState()
    val permissionNeeded by viewModel.permissionNeeded.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Manejar navegación al login si es necesario
    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            // Navegar a la pantalla de login
            navController.navigate(Routes.Login.route) {
                popUpTo(Routes.HistorialCompras.route) { inclusive = true }
            }
            viewModel.resetShouldNavigateToLogin()
        }
    }

    // Launcher para solicitar permisos
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, intentar descargar de nuevo
            viewModel.resetPermissionNeeded()
            // Volver a intentar la última descarga
            // Aquí tendríamos que guardar el ID de la última compra que se intentó descargar
        } else {
            Toast.makeText(
                context,
                "Permiso rechazado. No se podrá guardar la factura.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Manejar solicitudes de permisos
    LaunchedEffect(permissionNeeded) {
        permissionNeeded?.let {
            requestPermissionLauncher.launch(it)
            viewModel.resetPermissionNeeded()
        }
    }

    // Mostrar snackbar con mensaje de descarga
    LaunchedEffect(downloadMessage) {
        downloadMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = it)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "MIS COMPRAS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color(0xFFE53935)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFFE53935)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                userRole = com.example.app.util.SessionManager.getUserRole() ?: "participante"
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = backgroundColor)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = primaryColor
                )
            } else if (errorMessageState != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessageState ?: "Error desconocido",
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { viewModel.loadHistorialCompras() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Reintentar", color = Color.White)
                    }
                }
            } else if (compras.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No hay compras",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(16.dp),
                        tint = Color.LightGray
                    )

                    Text(
                        text = "No tienes compras",
                        style = MaterialTheme.typography.titleLarge,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    Text(
                        text = "Aquí aparecerán las entradas que compres para los eventos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { navController.navigate(Routes.Eventos.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar eventos",
                                tint = Color.White
                            )
                            Text(text = "EXPLORAR EVENTOS", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(compras) { compra ->
                        CompraCard(
                            compra = compra,
                            onCompraClick = { /* Detalles de la compra */ },
                            onDownloadClick = { 
                                viewModel.downloadFactura(compra.id_compra)
                            },
                            isDownloading = isDownloading,
                            downloadMessageValue = downloadMessage
                        )
                    }
                    
                    // Espacio final para evitar que el último elemento quede bajo la barra de navegación
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CompraCard(
    compra: CompraItem,
    onCompraClick: (CompraItem) -> Unit,
    onDownloadClick: () -> Unit,
    isDownloading: Boolean,
    downloadMessageValue: String?
) {
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    val fechaFormateada = formateoFechaCompra(compra.fecha_compra)
    val colorEstado = when (compra.estado.lowercase()) {
        "pagada" -> successColor
        "pendiente" -> warningColor
        else -> Color.Gray
    }
    val imagenEvento = compra.evento?.imagen ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCompraClick(compra) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = compra.evento?.nombre ?: "Evento desconocido",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = textPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = fechaFormateada,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondaryColor
                    )
                }
                
                if (imagenEvento.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(data = imagenEvento)
                                .apply(block = { crossfade(true) })
                                .build()
                        ),
                        contentDescription = "Imagen del evento",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.LightGray.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondaryColor
                    )
                    
                    Text(
                        text = formatoMoneda.format(compra.total),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = textPrimaryColor
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorEstado)
                    )
                    
                    Text(
                        text = compra.estado.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorEstado
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { onDownloadClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isDownloading && downloadMessageValue != "Factura descargada correctamente"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else if (downloadMessageValue == "Factura descargada correctamente") {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Factura descargada",
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Descargar factura",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = when {
                            isDownloading -> "DESCARGANDO..."
                            downloadMessageValue == "Factura descargada correctamente" -> "FACTURA DESCARGADA"
                            else -> "DESCARGAR FACTURA"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun formateoFechaCompra(fechaCompra: String): String {
    return try {
        val formatoOriginal = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        formatoOriginal.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = formatoOriginal.parse(fechaCompra)
        
        val formatoMostrado = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
        formatoMostrado.format(fecha!!)
    } catch (e: Exception) {
        fechaCompra // Devolver la fecha original si hay un error
    }
}

class HistorialComprasViewModelFactory(private val application: android.app.Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistorialComprasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistorialComprasViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 