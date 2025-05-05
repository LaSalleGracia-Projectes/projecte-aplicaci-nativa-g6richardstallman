package com.example.app.view.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.viewmodel.RegisterViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavController

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = viewModel()
) {
    // Estados para mostrar/ocultar contraseñas
    var showPasswordRequirements by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Validación de campos
    val nameValid = viewModel.name.isEmpty() || !viewModel.isNameError
    val apellido1Valid = viewModel.apellido1.isEmpty() || !viewModel.isApellido1Error
    val apellido2Valid = viewModel.apellido2.isEmpty() || !viewModel.isApellido2Error
    val emailValid = viewModel.email.isEmpty() || !viewModel.isEmailError
    val passwordValid = viewModel.password.isEmpty() || !viewModel.isPasswordError
    val confirmPasswordValid = viewModel.confirmPassword.isEmpty() || !viewModel.isConfirmPasswordError
    
    // Verificar si se puede activar los botones
    val allFieldsFilled = viewModel.name.isNotEmpty() && 
                         viewModel.apellido1.isNotEmpty() && 
                         viewModel.email.isNotEmpty() && 
                         viewModel.password.isNotEmpty() && 
                         viewModel.confirmPassword.isNotEmpty()
    
    val allFieldsValid = !viewModel.isNameError && 
                         !viewModel.isApellido1Error && 
                         !viewModel.isEmailError && 
                         !viewModel.isPasswordError && 
                         !viewModel.isConfirmPasswordError &&
                         viewModel.doPasswordsMatch()
    
    val buttonsEnabled = allFieldsFilled && allFieldsValid

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White // Fondo blanco para una interfaz minimalista
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            item {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "EventFlix Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Título
            item {
                Text(
                    text = stringResource(id = R.string.register_user_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935) // Color rojo del logo
                    ),
                    modifier = Modifier.padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Campos de formulario
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // Campos comunes para ambos roles
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { 
                            viewModel.name = it
                            if (it.isNotEmpty()) {
                                viewModel.validateField("name", it)
                            } else {
                                viewModel.isNameError = false
                            }
                        },
                        label = { Text(stringResource(id = R.string.name)) },
                        isError = viewModel.isNameError,
                        supportingText = {
                            if (viewModel.isNameError) {
                                Text(
                                    text = viewModel.nameErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = viewModel.apellido1,
                        onValueChange = { 
                            viewModel.apellido1 = it
                            if (it.isNotEmpty()) {
                                viewModel.validateField("apellido1", it)
                            } else {
                                viewModel.isApellido1Error = false
                            }
                        },
                        label = { Text(stringResource(id = R.string.first_surname)) },
                        isError = viewModel.isApellido1Error,
                        supportingText = {
                            if (viewModel.isApellido1Error) {
                                Text(
                                    text = viewModel.apellido1ErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = viewModel.apellido2,
                        onValueChange = { 
                            viewModel.apellido2 = it
                            if (it.isNotEmpty()) {
                                viewModel.validateField("apellido2", it)
                            } else {
                                viewModel.isApellido2Error = false
                            }
                        },
                        label = { Text(stringResource(id = R.string.second_surname)) },
                        isError = !apellido2Valid,
                        supportingText = {
                            if (!apellido2Valid) {
                                Text(stringResource(id = R.string.surname) + " inválido")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { 
                            viewModel.email = it
                            if (it.isNotEmpty()) {
                                viewModel.validateField("email", it)
                            } else {
                                viewModel.isEmailError = false
                            }
                        },
                        label = { Text(stringResource(id = R.string.email)) },
                        isError = viewModel.isEmailError,
                        supportingText = {
                            if (viewModel.isEmailError) {
                                Text(
                                    text = viewModel.emailErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFE53935),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { 
                            viewModel.password = it
                            if (it.isNotEmpty()) {
                                viewModel.validateField("password", it)
                            } else {
                                viewModel.isPasswordError = false
                            }
                        },
                        label = { Text(stringResource(id = R.string.password)) },
                        isError = viewModel.isPasswordError,
                        supportingText = {
                            if (viewModel.isPasswordError) {
                                Text(
                                    text = viewModel.passwordErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            Row {
                                // Botón de información para requisitos de contraseña
                                IconButton(onClick = { showPasswordRequirements = !showPasswordRequirements }) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = stringResource(id = R.string.show_password_requirements),
                                        tint = Color(0xFFE53935)
                                    )
                                }
                                // Botón para mostrar/ocultar contraseña
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) stringResource(id = R.string.hide_password) else stringResource(id = R.string.show_password),
                                        tint = Color(0xFFE53935)
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
                    
                    // Mostrar los requisitos de contraseña si se hace clic en el botón de información
                    if (showPasswordRequirements) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                                    text = stringResource(id = R.string.password_requirements_title),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF333333),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Text(text = stringResource(id = R.string.password_requirement_8_chars), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                
                                Text(text = stringResource(id = R.string.password_requirement_upper), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                
                                Text(text = stringResource(id = R.string.password_requirement_lower), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                
                                Text(text = stringResource(id = R.string.password_requirement_digit), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                
                                Text(text = stringResource(id = R.string.password_requirement_special), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = viewModel.confirmPassword,
                        onValueChange = { 
                            viewModel.confirmPassword = it
                            if (it.isNotEmpty()) {
                                viewModel.validateField("confirmPassword", it)
                            } else {
                                viewModel.isConfirmPasswordError = false
                            }
                        },
                        label = { Text(stringResource(id = R.string.confirm_password)) },
                        isError = viewModel.isConfirmPasswordError,
                        supportingText = {
                            if (viewModel.isConfirmPasswordError) {
                                Text(
                                    text = viewModel.confirmPasswordErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (confirmPasswordVisible) stringResource(id = R.string.hide_password) else stringResource(id = R.string.show_password),
                                    tint = Color(0xFFE53935)
                                )
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

            // Selección de rol
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.select_role),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                viewModel.role = "organizador"
                                navController.navigate("register/organizador")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .height(48.dp),
                            enabled = buttonsEnabled,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                disabledContainerColor = Color.LightGray
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.role_organizer),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.role = "participante"
                                navController.navigate("register/participante")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                                .height(48.dp),
                            enabled = buttonsEnabled,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                disabledContainerColor = Color.LightGray
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.role_participant),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }
                    
                    // Mensaje de ayuda si los botones están deshabilitados
                    if (!buttonsEnabled) {
                        Text(
                            text = stringResource(id = R.string.complete_all_fields),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Botón para volver al login
            item {
                TextButton(
                    onClick = { 
                        navController.navigate("login") 
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.already_have_account),
                        color = Color(0xFFE53935),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
    
    // Diálogo de error
    val errorMessageState by viewModel.errorMessage.collectAsState()
    if (viewModel.isError) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { 
                androidx.compose.material3.Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                androidx.compose.material3.Text(
                    text = errorMessageState ?: "",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    androidx.compose.material3.Text(
                        text = "Aceptar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
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