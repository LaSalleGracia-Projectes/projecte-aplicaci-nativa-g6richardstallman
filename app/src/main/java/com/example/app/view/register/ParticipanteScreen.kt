package com.example.app.view.register

// Importaciones de Compose y Material3
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.viewmodel.RegisterViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.util.Log

@Composable
fun ParticipanteScreen(
    viewModel: RegisterViewModel
) {
    val context = LocalContext.current
    
    // Cargar datos de Google Auth si existen
    LaunchedEffect(Unit) {
        val sharedPrefs = com.example.app.MyApplication.appContext.getSharedPreferences(
            "GoogleAuthData",
            android.content.Context.MODE_PRIVATE
        )
        
        val isFromGoogle = sharedPrefs.getBoolean("is_from_google", false)
        if (isFromGoogle) {
            val email = sharedPrefs.getString("google_email", "") ?: ""
            val nombre = sharedPrefs.getString("google_nombre", "") ?: ""
            val apellido1 = sharedPrefs.getString("google_apellido1", "") ?: ""
            val token = sharedPrefs.getString("google_token", null)
            
            Log.d("PARTICIPANTE_SCREEN", "Datos detectados de Google Auth: Email=$email, Nombre=$nombre")
            
            // Establecer datos de Google en el ViewModel
            viewModel.setGoogleAuthData(
                email = email,
                name = nombre,
                apellido1 = apellido1,
                apellido2 = "",
                token = token
            )
            
            // Limpiar preferencias después de usarlas
            with(sharedPrefs.edit()) {
                clear()
                apply()
            }
        }
    }
    
    // Mostramos logs para depuración
    val isFromGoogleAuth = viewModel.isFromGoogleAuth
    Log.d("PARTICIPANTE_SCREEN", "==== INICIANDO PANTALLA PARTICIPANTE ====")
    Log.d("PARTICIPANTE_SCREEN", "Datos precargados: Email=${viewModel.email}, Nombre=${viewModel.name}")
    Log.d("PARTICIPANTE_SCREEN", "¿Es de Google Auth? $isFromGoogleAuth")
    Log.d("PARTICIPANTE_SCREEN", "Token Google: ${viewModel.googleToken?.take(10) ?: "null"}...")
    
    // Mostrar requisitos de contraseña
    var showPasswordRequirements by remember { mutableStateOf(false) }
    
    // Validación de campos
    val dniValid = viewModel.dni.isEmpty() || !viewModel.isDniError
    val telefonoValid = viewModel.telefono.isEmpty() || !viewModel.isTelefonoError
    
    // Verificar si se puede activar el botón
    val allFieldsFilled = viewModel.dni.isNotEmpty() && viewModel.telefono.isNotEmpty()
    val allFieldsValid = !viewModel.isDniError && !viewModel.isTelefonoError
    val buttonEnabled = allFieldsFilled && allFieldsValid
    
    Log.d("PARTICIPANTE_SCREEN", "Estado botón: allFieldsFilled=$allFieldsFilled, allFieldsValid=$allFieldsValid")
    Log.d("PARTICIPANTE_SCREEN", "DNI=${viewModel.dni}, Teléfono=${viewModel.telefono}")
    Log.d("PARTICIPANTE_SCREEN", "DNI válido=$dniValid, Teléfono válido=$telefonoValid")

    // Monitorear registro exitoso
    val isRegistrationSuccessful by viewModel.isRegisterSuccessful.collectAsState()
    
    LaunchedEffect(isRegistrationSuccessful) {
        if (isRegistrationSuccessful) {
            Log.d("PARTICIPANTE_SCREEN", "==== REGISTRO EXITOSO DETECTADO ====")
        }
    }
    
    // Pantalla principal
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo de la app
            item {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "EventFlix Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Título de la pantalla
            item {
                androidx.compose.material3.Text(
                    text = stringResource(id = R.string.participant_data_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Formulario
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Resumen de datos personales
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.LightGray.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.basic_information),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "${stringResource(id = R.string.name)}: ${viewModel.name} ${viewModel.apellido1} ${viewModel.apellido2}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "${stringResource(id = R.string.email)}: ${viewModel.email}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }
                    }
                    
                    // Campo para DNI
                    OutlinedTextField(
                        value = viewModel.dni,
                        onValueChange = { input -> 
                            // Convertir la entrada a mayúsculas y filtrar caracteres no válidos
                            val formattedInput = input.uppercase().take(9).filter { it.isDigit() || it.isLetter() }
                            viewModel.dni = formattedInput
                            
                            if (formattedInput.isNotEmpty()) {
                                // Validar DNI
                                val dniPattern = "[0-9]{8}[A-Z]".toRegex()
                                val isValid = formattedInput.matches(dniPattern)
                                
                                if (!isValid) {
                                    viewModel.dniErrorMessage = context.getString(R.string.dni_invalid)
                                    viewModel.isDniError = true
                                } else {
                                    viewModel.isDniError = false
                                    viewModel.dniErrorMessage = ""
                                }
                            } else {
                                viewModel.isDniError = false
                                viewModel.dniErrorMessage = ""
                            }
                        },
                        label = { Text(stringResource(id = R.string.dni_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        isError = viewModel.isDniError,
                        supportingText = {
                            if (viewModel.isDniError) {
                                Text(
                                    text = viewModel.dniErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color.Gray,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Campo para teléfono
                    OutlinedTextField(
                        value = viewModel.telefono,
                        onValueChange = { input -> 
                            // Solo permitir dígitos y limitar a 9 caracteres
                            val formattedInput = input.take(9).filter { it.isDigit() }
                            viewModel.telefono = formattedInput
                            
                            if (formattedInput.isNotEmpty()) {
                                if (formattedInput.length != 9) {
                                    viewModel.telefonoErrorMessage = context.getString(R.string.phone_digits_left, 9 - formattedInput.length)
                                    viewModel.isTelefonoError = true
                                } else {
                                    viewModel.isTelefonoError = false
                                    viewModel.telefonoErrorMessage = ""
                                }
                            } else {
                                viewModel.isTelefonoError = false
                                viewModel.telefonoErrorMessage = ""
                            }
                        },
                        label = { Text(stringResource(id = R.string.phone_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        isError = viewModel.isTelefonoError,
                        supportingText = {
                            if (viewModel.isTelefonoError) {
                                Text(
                                    text = viewModel.telefonoErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color.Gray,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

            // Botón para completar registro
                Button(
                    onClick = {
                            if (buttonEnabled) {
                                viewModel.onRegisterClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                            .height(48.dp),
                    enabled = buttonEnabled,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF333333),
                            disabledContainerColor = Color.Gray
                    )
                ) {
                    Text(
                            text = stringResource(id = R.string.complete_registration),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
                }
            }
        }
    }
    
    // Mensaje de error
    if (viewModel.isError) {
        val errorMessageState by viewModel.errorMessage.collectAsState()
        Log.e("PARTICIPANTE_SCREEN", "Mostrando error: $errorMessageState")
        AlertDialog(
            onDismissRequest = { 
                viewModel.clearError() 
                Log.d("PARTICIPANTE_SCREEN", "Error cerrado por usuario")
            },
            title = { 
                androidx.compose.material3.Text(
                    text = stringResource(id = R.string.error),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                ) 
            },
            text = { 
                androidx.compose.material3.Text(
                    text = errorMessageState ?: "",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearError() 
                    Log.d("PARTICIPANTE_SCREEN", "Error confirmado por usuario")
                }) {
                    androidx.compose.material3.Text(
                        text = stringResource(id = R.string.accept),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }
            },
            containerColor = Color.White,
            iconContentColor = Color(0xFFE53935)
        )
    }
    
    // Mensaje de éxito
    val isRegisterSuccessful by viewModel.isRegisterSuccessful.collectAsState()
    if (isRegisterSuccessful) {
        Log.d("PARTICIPANTE_SCREEN", "Mostrando diálogo de éxito")
        AlertDialog(
            onDismissRequest = { /* No hacer nada, la navegación se maneja en AppNavHost */ },
            title = { 
                Text(
                    text = stringResource(id = R.string.registration_success_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                ) 
            },
            text = { 
                Text(
                    text = stringResource(id = R.string.participant_registration_success),
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                // Quitamos la acción del botón para evitar interferencias con la navegación automática
                TextButton(onClick = { 
                    /* No hacer nada, la navegación se maneja en AppNavHost */ 
                    Log.d("PARTICIPANTE_SCREEN", "Confirmado diálogo de éxito")
                }) {
                    Text(
                        text = stringResource(id = R.string.done),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    )
                }
            },
            containerColor = Color.White,
            iconContentColor = Color(0xFF4CAF50)
        )
    }
    
    // Indicador de carga
    if (viewModel.isLoading) {
        Log.d("PARTICIPANTE_SCREEN", "Mostrando indicador de carga")
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