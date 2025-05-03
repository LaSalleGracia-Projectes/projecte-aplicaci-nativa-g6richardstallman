package com.example.app.view

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.example.app.util.GoogleAuthHelper
import com.example.app.R
import com.example.app.viewmodel.LoginViewModel
import com.example.app.viewmodel.TicketsViewModel
import com.example.app.viewmodel.ForgotPasswordViewModel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import com.example.app.routes.Routes
import com.example.app.util.SessionManager
import com.example.app.routes.safeTokenDisplay

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    navController: NavController,
    viewModel: LoginViewModel = viewModel(),
    ticketsViewModel: TicketsViewModel = viewModel(
        factory = TicketsViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    // Estados básicos
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val isError = errorMessage != null
    val isLoginSuccessful = viewModel.isLoginSuccessful.collectAsState().value
    val shouldNavigateToParticipanteRegister = viewModel.shouldNavigateToParticipanteRegister.collectAsState().value
    
    // Configuración para Google Auth
    val context = androidx.compose.ui.platform.LocalContext.current
    val googleAuthHelper = remember { GoogleAuthHelper(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = googleAuthHelper.handleSignInResult(task)
            account?.let { viewModel.handleGoogleSignInResult(it) }
        }
    }
    
    // Asegurar que la sesión esté limpia al abrir LoginScreen
    LaunchedEffect(Unit) {
        Log.d("LoginScreen", "Comprobando y limpiando el estado de sesión")
        SessionManager.clearSession()
        viewModel.resetState()
    }
    
    // Navegar cuando el login es exitoso
    LaunchedEffect(isLoginSuccessful) {
        if (isLoginSuccessful) {
            val userRole = viewModel.user?.role ?: ""
            Log.d("LoginScreen", "Rol del usuario en login: $userRole")
            
            SessionManager.saveUserRole(userRole)
            Log.d("LoginScreen", "Rol guardado en SessionManager: ${SessionManager.getUserRole()}")
            
            navController.currentBackStackEntry?.savedStateHandle?.set("login_successful", true)
            
            viewModel.googleAccount?.let { account ->
                ticketsViewModel.googleAccount = account
            }
            
            navController.navigate("eventos") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
    
    // Navegar a la pantalla de registro de participante después de la autenticación con Google
    LaunchedEffect(shouldNavigateToParticipanteRegister) {
        if (shouldNavigateToParticipanteRegister) {
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: shouldNavigateToParticipanteRegister = true, redirigiendo a pantalla participante")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: User info: Email=${viewModel.user?.email}, Nombre=${viewModel.user?.nombre}")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Token recibido: ${viewModel.token?.take(10)}...")
            
            // Guardamos datos relevantes en el RegisterViewModel a través de la navegación
            val userEmail = viewModel.user?.email ?: ""
            val userName = viewModel.user?.nombre ?: ""
            val userLastName = viewModel.user?.apellido1 ?: ""
            val userLastName2 = viewModel.user?.apellido2 ?: ""
            val token = viewModel.token ?: ""
            
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Datos a guardar:")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Email=$userEmail")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Nombre=$userName")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Apellido1=$userLastName")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Apellido2=$userLastName2")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Token=${token.take(10)}...")
            
            // Guardar datos en el savedStateHandle antes de navegar
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Guardando datos en savedStateHandle")
            navController.getBackStackEntry("register/participante").savedStateHandle.set("google_login", true)
            navController.getBackStackEntry("register/participante").savedStateHandle.set("user_email", userEmail)
            navController.getBackStackEntry("register/participante").savedStateHandle.set("user_name", userName)
            navController.getBackStackEntry("register/participante").savedStateHandle.set("user_lastname", userLastName)
            navController.getBackStackEntry("register/participante").savedStateHandle.set("user_lastname2", userLastName2)
            navController.getBackStackEntry("register/participante").savedStateHandle.set("token", token)
            
            // Verificar que los datos se guardaron correctamente
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Verificación de datos guardados:")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Email guardado: ${navController.getBackStackEntry("register/participante").savedStateHandle.get<String>("user_email")}")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Nombre guardado: ${navController.getBackStackEntry("register/participante").savedStateHandle.get<String>("user_name")}")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Google login guardado: ${navController.getBackStackEntry("register/participante").savedStateHandle.get<Boolean>("google_login")}")
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Token guardado: ${navController.getBackStackEntry("register/participante").savedStateHandle.get<String>("token")?.safeTokenDisplay()}")
            
            // Navegar a la pantalla de registro
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Iniciando navegación a register/participante")
            navController.navigate(Routes.RegisterParticipante.route) {
                // Mantener la pantalla de login en la pila para poder volver
                popUpTo(Routes.Login.route) { inclusive = false }
            }
            
            // Reseteamos el flag para evitar navegaciones no deseadas
            Log.d("LOGIN_DEBUG", "=== FLUJO GOOGLE ===: Resetenado estado para evitar navegaciones duplicadas")
            viewModel.resetState()
        }
    }
    
    LoginContent(
        isLoading = isLoading,
        isError = isError,
        errorMessage = errorMessage,
        viewModel = viewModel,
        onNavigateToRegister = { navController.navigate("register") },
        onNavigateToForgotPassword = { navController.navigate("forgot_password") },
        onGoogleSignIn = { launcher.launch(googleAuthHelper.getSignInIntent()) }
    )
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    isError: Boolean,
    errorMessage: String?,
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo de la app
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "EventFlix Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Fit
                )
                
                // Título de login
                Text(
                    text = "Iniciar Sesión",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    ),
                    modifier = Modifier.padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
                
                // Campo de email
                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Email") },
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
                
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (viewModel.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.passwordVisible = !viewModel.passwordVisible }) {
                            Icon(
                                imageVector = if (viewModel.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (viewModel.passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Enlace olvidé contraseña
                TextButton(
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = Color(0xFFE53935)
                    )
                }
                
                // Botón iniciar sesión
                Button(
                    onClick = { viewModel.onLoginClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Separador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "O",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón de Google
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continuar con Google",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón crear cuenta
                OutlinedButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE53935))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53935)
                    )
                ) {
                    Text(
                        text = "Crear Cuenta",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Copyright
                Text(
                    text = "© 2025 EventFlix. Todos los derechos reservados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            if (isLoading) {
                LoadingOverlay()
            }
            
            if (isError) {
                ErrorDialog(
                    errorMessage = errorMessage,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun LoadingOverlay() {
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

@Composable
private fun ErrorDialog(
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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