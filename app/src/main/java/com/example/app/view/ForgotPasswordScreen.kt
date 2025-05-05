package com.example.app.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = viewModel(),
    onNavigateToLogin: () -> Unit
) {
    // Control de navegación
    val navigateToLogin by viewModel.navigateToLogin.observeAsState(false)
    if (navigateToLogin) {
        LaunchedEffect(key1 = true) {
            viewModel.onLoginNavigated()
            onNavigateToLogin()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Título principal
            Text(
                text = stringResource(id = R.string.recover_password),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Texto explicativo
            Text(
                text = stringResource(id = R.string.recover_password_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Campo para email
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text(stringResource(id = R.string.email)) },
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo para DNI o teléfono
            OutlinedTextField(
                value = viewModel.identificador,
                onValueChange = { viewModel.onIdentificadorChange(it) },
                label = { Text(stringResource(id = R.string.dni_or_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onResetPasswordClick() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = Color(0xFFE53935),
                    unfocusedLabelColor = Color.Gray
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botón recuperar
            Button(
                onClick = { viewModel.onResetPasswordClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.recover_password),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
            
            // Mostrar contraseña si existe
            if (viewModel.recoveredPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.your_password_is),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = viewModel.recoveredPassword,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón volver
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.back_to_login),
                    color = Color(0xFFE53935),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        
        // Círculo de carga
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
        
        // Mensaje de error
        if (viewModel.isError) {
            AlertDialog(
                onDismissRequest = { /* No hacer nada */ },
                title = { 
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE53935)
                    ) 
                },
                text = { 
                    Text(
                        text = viewModel.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setError(null) }) {
                        Text(
                            text = stringResource(id = R.string.accept),
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
    }
}

@Composable
fun StepIndicator(step: Int, currentStep: Int) {
    val isActive = step <= currentStep
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = if (isActive) Color(0xFFE53935) else Color.Gray.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = step.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
    }
} 