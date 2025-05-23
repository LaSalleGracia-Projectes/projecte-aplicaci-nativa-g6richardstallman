package com.example.app.view.register

// Importaciones de Compose y Material3
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.viewmodel.RegisterViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun OrganizadorScreen(
    viewModel: RegisterViewModel
) {
    // Definir el tipo de usuario
    LaunchedEffect(Unit) {
        viewModel.role = "Organizador"
    }
    
    // Pantalla principal
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo de la app
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "EventFlix Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Fit
                )
                
                // Título de la pantalla
                Text(
                    text = "Datos de Organizador",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    ),
                    modifier = Modifier.padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
                
                // Resumen de datos personales
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Resumen de Registro",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = "Nombre: ${viewModel.name} ${viewModel.apellido1} ${viewModel.apellido2}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                        
                        Text(
                            text = "Email: ${viewModel.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }
                
                // Campo para nombre de la organización
                OutlinedTextField(
                    value = viewModel.nombreOrganizacion,
                    onValueChange = { viewModel.nombreOrganizacion = it },
                    label = { Text("Nombre de la Organización") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = Color(0xFFE53935),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Campo para teléfono
                OutlinedTextField(
                    value = viewModel.telefonoContacto,
                    onValueChange = { 
                        // Solo permitir dígitos y limitar a 9 caracteres
                        if (it.all { char -> char.isDigit() } && it.length <= 9) {
                            viewModel.telefonoContacto = it
                        }
                    },
                    label = { Text("Teléfono de Contacto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = Color(0xFFE53935),
                        unfocusedLabelColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botón para completar registro
                Button(
                    onClick = {
                        // Validar que los campos estén completos y con formato correcto
                        if (viewModel.nombreOrganizacion.isNotEmpty() && viewModel.telefonoContacto.length == 9) {
                            viewModel.mostrarMensaje("Enviando registro de organizador con: " +
                                "nombreOrg='${viewModel.nombreOrganizacion}', " +
                                "telefonoContacto='${viewModel.telefonoContacto}'")
                                
                            viewModel.onRegisterClick()
                        } else {
                            if (viewModel.nombreOrganizacion.isEmpty()) {
                                viewModel.nombreOrganizacionErrorMessage = "El nombre de la organización es requerido"
                                viewModel.isNombreOrganizacionError = true
                            }
                            
                            if (viewModel.telefonoContacto.isEmpty()) {
                                viewModel.telefonoContactoErrorMessage = "El teléfono de contacto es requerido"
                                viewModel.isTelefonoContactoError = true
                            } else if (viewModel.telefonoContacto.length != 9) {
                                viewModel.telefonoContactoErrorMessage = "El teléfono debe tener 9 dígitos"
                                viewModel.isTelefonoContactoError = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333)
                    )
                ) {
                    Text(
                        text = "Completar Registro",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
            
            // Mensaje de error
            val errorMessage by viewModel.errorMessage.collectAsState()
            if (viewModel.isError) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(errorMessage ?: "") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Aceptar")
                        }
                    }
                )
            }
            
            // Mensaje de éxito
            val isRegisterSuccessful by viewModel.isRegisterSuccessful.collectAsState()
            if (isRegisterSuccessful) {
                AlertDialog(
                    onDismissRequest = { /* No hacer nada */ },
                    title = { 
                        Text(
                            text = "Registro Exitoso",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF4CAF50)
                        ) 
                    },
                    text = { 
                        Text(
                            text = "Tu cuenta de organizador ha sido creada correctamente.",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    confirmButton = {
                        TextButton(onClick = { /* Navegar al login */ }) {
                            Text(
                                text = "Aceptar",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFE53935)
                            )
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            
            // Indicador de carga
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE53935),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}

fun validateOrganizador(nombreOrganizacion: String, telefonoContacto: String): Boolean {
    return nombreOrganizacion.isNotEmpty() && telefonoContacto.length >= 9
}