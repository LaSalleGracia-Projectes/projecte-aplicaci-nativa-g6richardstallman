package com.example.app.view

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.R
import com.example.app.util.formatToCurrency
import com.example.app.viewmodel.ComprarEntradasViewModel
import androidx.compose.runtime.collectAsState
import com.example.app.model.TipoEntrada
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprarEntradasScreen(
    navController: NavController,
    eventoId: String
) {
    val context = LocalContext.current
    val viewModel: ComprarEntradasViewModel = viewModel()
    
    // Cargar datos del evento
    LaunchedEffect(eventoId) {
        viewModel.loadTiposEntrada(eventoId)
    }
    
    // Estados
    val isLoading = viewModel.isLoading
    val tiposEntrada = viewModel.tiposEntrada
    val error = viewModel.error

    // Estados del proceso de compra
    val showPaymentDialog = viewModel.showPaymentDialog.collectAsState()
    val compraProcesando = viewModel.compraProcesando.collectAsState()
    val compraExitosa = viewModel.compraExitosa.collectAsState()
    val mensajeCompra = viewModel.mensajeCompra.collectAsState()
    
    // Formato para mostrar precios
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    
    // Mostrar diálogo de compra si es necesario
    if (showPaymentDialog.value) {
        CompraDialogoPago(
            total = viewModel.calcularTotal(),
            enProceso = compraProcesando.value,
            exito = compraExitosa.value,
            mensaje = mensajeCompra.value,
            onConfirm = { viewModel.realizarCompra() },
            onDismiss = { viewModel.cerrarDialogoPago() },
            primaryColor = Color(0xFFE53935)
        )
    }
    
    // Pantalla principal
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.buy_tickets_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE53935),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE53935))
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }
                else -> {
                    // Mostrar los tipos de entrada
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_your_tickets),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Lista de tipos de entrada con selectores de cantidad
                        tiposEntrada.forEach { tipoEntrada ->
                            CompraEntradaTipoItem(
                                tipoEntrada = tipoEntrada,
                                cantidad = viewModel.obtenerCantidad(tipoEntrada.id),
                                onIncrement = { viewModel.incrementarCantidad(tipoEntrada.id) },
                                onDecrement = { viewModel.decrementarCantidad(tipoEntrada.id) },
                                primaryColor = Color(0xFFE53935)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Mostrar total y contador de entradas
                        if (viewModel.hayEntradasSeleccionadas()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE53935).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Contador de entradas
                                    val totalEntradas = tiposEntrada.sumOf { viewModel.obtenerCantidad(it.id) }
                                    Text(
                                        text = stringResource(R.string.selected_tickets, totalEntradas),
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
                                            text = stringResource(R.string.total),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = formatoMoneda.format(viewModel.calcularTotal()),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Botón de compra
                            Button(
                                onClick = { viewModel.mostrarDialogoPago() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935),
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = viewModel.hayEntradasSeleccionadas()
                            ) {
                                Text(
                                    text = stringResource(R.string.buy_now),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        } else {
                            // Mensaje cuando no hay entradas seleccionadas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.select_at_least_one_ticket),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompraEntradaTipoItem(
    tipoEntrada: TipoEntrada,
    cantidad: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    primaryColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tipoEntrada.nombre?.takeIf { it.isNotBlank() } ?: stringResource(R.string.general_ticket),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = tipoEntrada.precio?.formatToCurrency() ?: "0,00 €",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
            
            if (!tipoEntrada.descripcion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tipoEntrada.descripcion ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mostrar disponibilidad
                val disponibilidad = when {
                    tipoEntrada.esIlimitado == true -> stringResource(R.string.unlimited_tickets)
                    tipoEntrada.cantidadDisponible != null -> stringResource(R.string.available_tickets_count, tipoEntrada.cantidadDisponible ?: 0)
                    else -> stringResource(R.string.availability_not_specified)
                }
                Text(
                    text = disponibilidad,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                // Selector de cantidad
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrement,
                        enabled = cantidad > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = stringResource(R.string.decrease),
                            tint = if (cantidad > 0) primaryColor else Color.Gray
                        )
                    }
                    
                    Text(
                        text = cantidad.toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    
                    IconButton(
                        onClick = onIncrement,
                        enabled = true,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.increase),
                            tint = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompraDialogoPago(
    total: Double,
    enProceso: Boolean,
    exito: Boolean,
    mensaje: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    AlertDialog(
        onDismissRequest = { if (!enProceso) onDismiss() },
        title = {
            Text(
                text = if (exito) stringResource(R.string.successful_purchase) else stringResource(R.string.confirm_purchase),
                fontWeight = FontWeight.Bold,
                color = if (exito) Color.Green else primaryColor
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    enProceso -> {
                        CircularProgressIndicator(color = primaryColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.processing_purchase),
                            textAlign = TextAlign.Center
                        )
                    }
                    exito -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.successful_purchase),
                            tint = Color.Green,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = mensaje,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        if (mensaje.isNotEmpty()) {
                            Text(
                                text = mensaje,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.total_to_pay, formatToCurrency(total)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!enProceso) {
                Button(
                    onClick = { if (exito) onDismiss() else onConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(if (exito) stringResource(R.string.close) else stringResource(R.string.confirm_button))
                }
            }
        },
        dismissButton = {
            if (!enProceso && !exito) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        }
    )
}

// Función para formatear un string a formato moneda
private fun String?.formatToCurrency(): String {
    val value = this?.toDoubleOrNull() ?: 0.0
    val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    return format.format(value)
}

// Función para formatear un double a formato moneda  
private fun formatToCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    return format.format(value)
} 