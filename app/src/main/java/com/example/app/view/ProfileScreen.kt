package com.example.app.view

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.routes.Routes
import com.example.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import com.example.app.routes.BottomNavigationBar
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.app.R
import coil.compose.rememberAsyncImagePainter

// Colores consistentes con la app y la marca
private val primaryColor = Color(0xFFE53935)  // Rojo principal del logo usado en otras pantallas
private val secondaryDarkRed = Color(0xFF652C2D) // Tono más oscuro de rojo (#652c2d)
private val accentRed = Color(0xFFA53435) // Tono medio de rojo (#a53435)
private val backgroundColor = Color.White // Fondo blanco como en otras pantallas
private val textPrimaryColor = Color.Black // Texto negro para consistencia
private val textSecondaryColor = Color.Gray // Gris para textos secundarios, como en LoginScreen
private val surfaceColor = Color(0xFFF5F5F5)  // Gris muy claro para fondos de tarjetas
private val cardBackground = Color(0xFFFFFFFF) // Blanco puro para tarjetas
private val dividerColor = Color(0xFFE0E0E0) // Color para divisores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val profileData = viewModel.profileData
    val isEditing = viewModel.isEditing
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    
    val successState = viewModel.isUpdateSuccessful.collectAsState(initial = false)
    val shouldNavigateToLogin = viewModel.shouldNavigateToLogin.collectAsState(initial = false)
    
    // Verificación de token inmediata al inicio del composable
    val hasValidToken = remember { mutableStateOf(true) }
    
    // Verificar token inmediatamente
    LaunchedEffect(Unit) {
        val token = com.example.app.util.SessionManager.getToken()
        if (token.isNullOrEmpty()) {
            hasValidToken.value = false
            navController.navigate(com.example.app.routes.Routes.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            // Solo cargar el perfil si hay token válido
            viewModel.loadProfile()
        }
    }
    
    // Obtener el rol del usuario desde SessionManager para mostrar el menú correcto desde el inicio
    val initialUserRole = remember {
        com.example.app.util.SessionManager.getUserRole() ?: "participante"
    }
    
    // Navegar al login cuando la sesión ha expirado o cuando se cierra sesión
    LaunchedEffect(shouldNavigateToLogin.value) {
        if (shouldNavigateToLogin.value) {
            Log.d("ProfileScreen", "shouldNavigateToLogin es true, iniciando navegación a login")
            
            // Asegurarse de que la sesión esté limpia (esto debería ya estar hecho antes de este punto)
            if (com.example.app.util.SessionManager.getToken() != null) {
                Log.d("ProfileScreen", "Limpiando sesión antes de navegar")
            com.example.app.util.SessionManager.clearSession()
            }
            
            // Navegar a login limpiando el back stack completamente
            Log.d("ProfileScreen", "Navegando a la pantalla de login")
            navController.navigate(com.example.app.routes.Routes.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            Log.d("ProfileScreen", "Navegación a login completa")
            
            // Resetear el estado de navegación
            viewModel.resetShouldNavigateToLogin()
            Log.d("ProfileScreen", "shouldNavigateToLogin reiniciado a false")
        }
    }
    
    // Si no hay token válido, no mostramos nada (ya se está navegando a login)
    if (!hasValidToken.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = primaryColor
            )
        }
        return
    }
    
    // Mostrar snackbar en caso de éxito
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(successState.value) {
        if (successState.value) {
            showSnackbar = true
            snackbarMessage = "Perfil actualizado correctamente"
            viewModel.resetUpdateState()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }
    
    val avatarUrl = profileData?.avatarUrl

    LaunchedEffect(avatarUrl) {
        // Eliminado log innecesario
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "MI PERFIL",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = 1.sp
                        ),
                        color = primaryColor
                    )
                },
                actions = {
                    if (!isEditing && profileData != null) {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = "Editar perfil",
                                tint = primaryColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = primaryColor
                )
            )
        },
        bottomBar = {
            // Usar el rol guardado en SessionManager hasta que se cargue el perfil
            val userRole = viewModel.profileData?.role ?: initialUserRole
            Log.d("ProfileScreen", "Mostrando barra de navegación con rol: $userRole")
            BottomNavigationBar(
                navController = navController,
                userRole = userRole
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = secondaryDarkRed,
                    contentColor = Color.White
                )
            }
        }
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
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        errorMessage, 
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Si es error de sesión expirada, mostrar botón para ir al login
                    if (errorMessage.contains("sesión ha expirado", ignoreCase = true)) {
                        Button(
                            onClick = { viewModel.navigateToLogin() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Iniciar sesión", color = Color.White)
                        }
                    } else {
                        // Para otros errores, mostrar botón de reintentar
                        Button(
                            onClick = { viewModel.loadProfile() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reintentar", color = Color.White)
                        }
                    }
                }
            } else if (profileData != null) {
                if (isEditing) {
                    ProfileEditMode(viewModel = viewModel)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Card de perfil principal con avatar y nombre
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Avatar recibido del backend
                                if (!avatarUrl.isNullOrEmpty()) {
                                    androidx.compose.foundation.Image(
                                        painter = rememberAsyncImagePainter(avatarUrl),
                                        contentDescription = "Avatar del usuario",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .padding(8.dp)
                                            .clip(CircleShape),
                                        alignment = Alignment.Center,
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Icono de estrella por defecto
                                    Surface(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(percent = 50),
                                        color = primaryColor.copy(alpha = 0.15f),
                                        border = BorderStroke(2.dp, primaryColor.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Avatar por defecto",
                                            modifier = Modifier
                                                .padding(24.dp)
                                                .size(64.dp),
                                            tint = primaryColor
                                        )
                                    }
                                }
                                
                                // Nombre completo
                                Text(
                                    text = "${profileData.nombre ?: ""} ${profileData.apellido1 ?: ""} ${profileData.apellido2 ?: ""}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    modifier = Modifier.padding(top = 16.dp),
                                    textAlign = TextAlign.Center,
                                    color = textPrimaryColor
                                )
                                
                                // Rol
                                Text(
                                    text = when(profileData.role) {
                                        "organizador" -> "Organizador"
                                        "participante" -> "Participante"
                                        else -> profileData.role ?: ""
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = primaryColor,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                // Email
                                Text(
                                    text = profileData.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondaryColor,
                                    modifier = Modifier.padding(top = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // Tarjeta con información personal
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Información Personal",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = secondaryDarkRed,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                // Mostrar campos según el rol
                                if (profileData.role == "participante") {
                                    ProfileFieldRow("DNI", profileData.dni ?: "No especificado")
                                    HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileFieldRow("Teléfono", profileData.telefono ?: "No especificado")
                                } else if (profileData.role == "organizador") {
                                    ProfileFieldRow("Nombre de la organización", profileData.nombreOrganizacion ?: "No especificado")
                                    HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    ProfileFieldRow("Teléfono de contacto", profileData.telefonoContacto ?: "No especificado")
                                }
                            }
                        }
                        
                        // Botones de acción
                        ActionsButtonsSection(navController, viewModel, profileData)
                    }
                }
            } else {
                Text(
                    "No se pudo cargar la información del perfil",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = textPrimaryColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewMode(
    profileData: com.example.app.model.ProfileData,
    navController: NavController,
    viewModel: ProfileViewModel
) {
    // Estado para manejar la visibilidad del diálogo de confirmación
    var showDeleteDialog by remember { mutableStateOf(false) }
    // Estado para el campo de contraseña
    var password by remember { mutableStateOf("") }
    // Estado para controlar si la contraseña es visible
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Monitor del estado de eliminación exitosa
    val deleteSuccessState = viewModel.isDeleteAccountSuccessful.collectAsState(initial = false)
    val showSuccessDialog = viewModel.showDeleteSuccessDialog.collectAsState(initial = false)
    
    // Verificar si hay token válido y redirigir si es necesario
    LaunchedEffect(Unit) {
        val token = com.example.app.util.SessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.d("ProfileViewMode", "No hay token válido, redirigiendo a login")
            navController.navigate(com.example.app.routes.Routes.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    
    // Mostrar un snackbar cuando la eliminación de cuenta sea exitosa
    LaunchedEffect(deleteSuccessState.value) {
        if (deleteSuccessState.value) {
            // Ya no hacemos nada aquí porque ahora se muestra un diálogo
            viewModel.resetDeleteAccountState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sección de perfil con icono
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.size(80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryColor.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Perfil",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "${profileData.nombre ?: ""} ${profileData.apellido1 ?: ""} ${profileData.apellido2 ?: ""}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
                Text(
                    text = profileData.tipoUsuario ?: profileData.role ?: "",
                    fontSize = 16.sp,
                    color = primaryColor
                )
            }
        }
        
        HorizontalDivider(
            color = Color.LightGray,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Información personal
        ProfileSection(title = "Información Personal") {
            ProfileField("Email", profileData.email ?: "")
            
            // Mostrar campos según el rol
            if (profileData.role == "participante") {
                ProfileField("DNI", profileData.dni ?: "")
                ProfileField("Teléfono", profileData.telefono ?: "")
            } else if (profileData.role == "organizador") {
                ProfileField("Nombre de la organización", profileData.nombreOrganizacion ?: "")
                ProfileField("Teléfono de contacto", profileData.telefonoContacto ?: "")
            }
        }
        
        // Espaciador para separar el botón de cerrar sesión
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botón de historial de compras (solo para participantes)
        if (profileData.role == "participante") {
            Button(
                onClick = { 
                    navController.navigate(com.example.app.routes.Routes.HistorialCompras.route)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor, // Usar el color primario de la app (rojo)
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = "Historial de compras",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "VER HISTORIAL DE COMPRAS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Botón de cerrar sesión - solución directa
        Button(
            onClick = { 
                // Verificar token antes de proceder
                val token = com.example.app.util.SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    // Si no hay token, navegar directamente a login
                    navController.navigate(com.example.app.routes.Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    // Si hay token, proceder con el logout normal
                    viewModel.logout()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor, 
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Cerrar sesión",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "CERRAR SESIÓN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Espaciador para separar el botón de eliminación de cuenta
        Spacer(modifier = Modifier.height(16.dp))
        
        // Botón de eliminar cuenta
        Button(
            onClick = { 
                // Verificar token antes de mostrar el diálogo
                val token = com.example.app.util.SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    // Si no hay token, navegar directamente a login
                    navController.navigate(com.example.app.routes.Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    // Si hay token, mostrar el diálogo
                    showDeleteDialog = true
                    // Limpiar contraseña en caso de que haya quedado de algún intento anterior
                    password = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFAD2A2A), // Rojo más oscuro para acciones peligrosas
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar cuenta",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ELIMINAR CUENTA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Espacio al final para evitar que el contenido quede pegado a la barra inferior
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Si hay un mensaje de error, mostrarlo
    if (viewModel.errorMessage != null) {
        Text(
            text = viewModel.errorMessage ?: "",
            color = Color.Red,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
    
    // Diálogo de confirmación para eliminar cuenta
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!viewModel.isLoading) {
                    showDeleteDialog = false
                    password = ""
                    viewModel.clearError()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !viewModel.isLoading,
                dismissOnClickOutside = !viewModel.isLoading
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título con icono
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar cuenta",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Eliminar cuenta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (viewModel.isLoading) {
                        // Contenido del diálogo cuando está cargando
                        CircularProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Eliminando cuenta...",
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Contenido del diálogo normal
                        Text(
                            "Esta acción eliminará permanentemente tu cuenta y todos tus datos asociados:",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textPrimaryColor
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Lista de lo que se eliminará
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            BulletPoint("Tu perfil y datos personales")
                            BulletPoint("Historial de compras y facturas")
                            if (profileData.role == "participante") {
                                BulletPoint("Eventos favoritos")
                                BulletPoint("Organizadores favoritos")
                            } else {
                                BulletPoint("Eventos creados")
                                BulletPoint("Tipos de entrada")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Esta acción no se puede deshacer.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Campo de contraseña
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña actual") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Botones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón Cancelar
                            OutlinedButton(
                                onClick = { 
                                    showDeleteDialog = false
                                    password = ""
                                    viewModel.clearError()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = primaryColor
                                ),
                                border = BorderStroke(1.dp, primaryColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar")
                            }
                            
                            // Botón Confirmar
                            Button(
                                onClick = {
                                    viewModel.deleteAccount(password)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.White,
                                    disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = password.isNotBlank()
                            ) {
                                Text("Confirmar")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo de éxito cuando la cuenta se ha eliminado correctamente
    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Éxito",
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Cuenta eliminada con éxito",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Gracias por haber usado nuestra aplicación",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = textSecondaryColor
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            viewModel.confirmDeleteSuccess()
                            navController.navigate(Routes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(primaryColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = textPrimaryColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditMode(viewModel: ProfileViewModel) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Editar perfil",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Campos generales
        OutlinedTextField(
            value = viewModel.nombre,
            onValueChange = { viewModel.nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = primaryColor,
                unfocusedLabelColor = Color.Gray
            )
        )
        
        OutlinedTextField(
            value = viewModel.apellido1,
            onValueChange = { viewModel.apellido1 = it },
            label = { Text("Primer apellido") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = primaryColor,
                unfocusedLabelColor = Color.Gray
            )
        )
        
        OutlinedTextField(
            value = viewModel.apellido2,
            onValueChange = { viewModel.apellido2 = it },
            label = { Text("Segundo apellido") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedLabelColor = primaryColor,
                unfocusedLabelColor = Color.Gray
            )
        )
        
        // Email no editable
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedLabelColor = primaryColor,
                unfocusedLabelColor = Color.Gray,
                disabledLabelColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
        
        // Campos específicos según rol
        val profileData = viewModel.profileData
        if (profileData?.role == "participante") {
            OutlinedTextField(
                value = viewModel.dni,
                onValueChange = { viewModel.dni = it },
                label = { Text("DNI") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                )
            )
            
            OutlinedTextField(
                value = viewModel.telefono,
                onValueChange = { viewModel.telefono = it },
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                )
            )
        } else if (profileData?.role == "organizador") {
            OutlinedTextField(
                value = viewModel.nombreOrganizacion,
                onValueChange = { viewModel.nombreOrganizacion = it },
                label = { Text("Nombre de la organización") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                )
            )
            
            OutlinedTextField(
                value = viewModel.telefonoContacto,
                onValueChange = { viewModel.telefonoContacto = it },
                label = { Text("Teléfono de contacto") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                )
            )
        }
        
        // Botones de acción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.cancelEditing() },
                modifier = Modifier.weight(1f),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(primaryColor)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = primaryColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancelar")
            }
            
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar")
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = surfaceColor
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = textSecondaryColor
        )
        
        Text(
            text = value.ifEmpty { "No especificado" },
            fontSize = 16.sp,
            color = textPrimaryColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textSecondaryColor
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = textPrimaryColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsButtonsSection(
    navController: NavController,
    viewModel: ProfileViewModel,
    profileData: com.example.app.model.ProfileData
) {
    // Variable para controlar diálogo de cambio de contraseña
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    // Variables para el diálogo de eliminar cuenta
    var showDeleteDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Monitor del estado de eliminación exitosa
    val deleteSuccessState = viewModel.isDeleteAccountSuccessful.collectAsState(initial = false)
    val showSuccessDialog = viewModel.showDeleteSuccessDialog.collectAsState(initial = false)
    
    // Botón de historial de compras (solo para participantes)
    if (profileData.role == "participante") {
        Button(
            onClick = { 
                navController.navigate(com.example.app.routes.Routes.HistorialCompras.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentRed, // Tono medio de rojo para distinguir
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                Icons.Default.ReceiptLong,
                contentDescription = "Historial de compras",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "VER HISTORIAL DE COMPRAS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Botón de cambiar contraseña
    Button(
        onClick = { showChangePasswordDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = secondaryDarkRed, // Tono oscuro para botones importantes
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "Cambiar contraseña",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "CAMBIAR CONTRASEÑA",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    // Mostrar el diálogo de cambio de contraseña si es necesario
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Botón de cerrar sesión
    Button(
        onClick = { viewModel.logout() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Cerrar sesión",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "CERRAR SESIÓN",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Botón de eliminar cuenta
    Button(
        onClick = { 
            // Verificar token antes de mostrar el diálogo
            val token = com.example.app.util.SessionManager.getToken()
            if (token.isNullOrEmpty()) {
                // Si no hay token, navegar directamente a login
                navController.navigate(com.example.app.routes.Routes.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                // Si hay token, mostrar el diálogo
                showDeleteDialog = true
                // Limpiar contraseña en caso de que haya quedado de algún intento anterior
                password = ""
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFAD2A2A), // Rojo más oscuro para acciones peligrosas
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = "Eliminar cuenta",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "ELIMINAR CUENTA",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    // Espacio al final para evitar que el contenido quede pegado a la barra inferior
    Spacer(modifier = Modifier.height(24.dp))
    
    // Diálogo de confirmación para eliminar cuenta
    if (showDeleteDialog) {
        val isLoading = viewModel.isLoading
        val errorMessage = viewModel.errorMessage
        
        // Verificación de token antes de mostrar el diálogo
        DisposableEffect(Unit) {
            val token = com.example.app.util.SessionManager.getToken()
            if (token.isNullOrEmpty()) {
                // Si no hay token, cerrar el diálogo y navegar a login
                showDeleteDialog = false
                navController.navigate(com.example.app.routes.Routes.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            
            onDispose { }
        }
        
        AlertDialog(
            onDismissRequest = { 
                if (!isLoading) {
                    showDeleteDialog = false
                    password = ""
                    viewModel.clearError()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !isLoading,
                dismissOnClickOutside = !isLoading
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Título con icono
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar cuenta",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Eliminar cuenta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        // Contenido del diálogo cuando está cargando
                        CircularProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Eliminando cuenta...",
                            fontSize = 14.sp,
                            color = textSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Contenido del diálogo normal
                        Text(
                            "Esta acción eliminará permanentemente tu cuenta y todos tus datos asociados:",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textPrimaryColor
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Lista de lo que se eliminará
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            BulletPoint("Tu perfil y datos personales")
                            BulletPoint("Historial de compras y facturas")
                            if (profileData.role == "participante") {
                                BulletPoint("Eventos favoritos")
                                BulletPoint("Organizadores favoritos")
                            } else {
                                BulletPoint("Eventos creados")
                                BulletPoint("Tipos de entrada")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Esta acción no se puede deshacer.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Si hay un mensaje de error, mostrarlo
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = Color.Red,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // Campo de contraseña
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña actual") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Botones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón Cancelar
                            OutlinedButton(
                                onClick = { 
                                    showDeleteDialog = false
                                    password = ""
                                    viewModel.clearError()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = primaryColor
                                ),
                                border = BorderStroke(1.dp, primaryColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar")
                            }
                            
                            // Botón Confirmar
                            Button(
                                onClick = {
                                    viewModel.deleteAccount(password)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.White,
                                    disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                enabled = password.isNotBlank()
                            ) {
                                Text("Confirmar")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo de éxito cuando la cuenta se ha eliminado correctamente
    if (showSuccessDialog.value) {
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Éxito",
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Cuenta eliminada con éxito",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Gracias por haber usado nuestra aplicación",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = textSecondaryColor
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            viewModel.confirmDeleteSuccess()
                            navController.navigate(Routes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    val isChangingPassword = viewModel.isChangingPassword
    val isPasswordChangeSuccessful by viewModel.isPasswordChangeSuccessful.collectAsState()
    val errorMessage = viewModel.errorMessage
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Para mostrar u ocultar las contraseñas
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Resetear el estado si el diálogo se cierra
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetPasswordChangeState()
        }
    }
    
    // Si el cambio fue exitoso, cerrar automáticamente después de un tiempo
    LaunchedEffect(isPasswordChangeSuccessful) {
        if (isPasswordChangeSuccessful) {
            delay(1500)
            onDismiss()
        }
    }
    
    Dialog(
        onDismissRequest = { 
            if (!isChangingPassword) onDismiss() 
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título
                Text(
                    text = "Cambiar contraseña",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = primaryColor,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                if (isChangingPassword) {
                    // Pantalla de carga
                    CircularProgressIndicator(color = primaryColor)
                } else if (isPasswordChangeSuccessful) {
                    // Mensaje de éxito
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Éxito",
                        tint = primaryColor,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tu contraseña ha sido actualizada correctamente",
                        textAlign = TextAlign.Center,
                        color = textPrimaryColor
                    )
                } else {
                    // Campos de entrada
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contraseña actual") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (currentPasswordVisible) 
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(
                                    imageVector = if (currentPasswordVisible) 
                                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (currentPasswordVisible) 
                                        "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = primaryColor
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color.Gray
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (newPasswordVisible) 
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) 
                                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (newPasswordVisible) 
                                        "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = primaryColor
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color.Gray
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPasswordVisible) 
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) 
                                        Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) 
                                        "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = primaryColor
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color.Gray
                        ),
                        singleLine = true
                    )
                    
                    // Mostrar mensaje de error si hay alguno
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = primaryColor,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Botón Cancelar
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Cancelar",
                                color = primaryColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                        
                        // Botón Cambiar
                        Button(
                            onClick = {
                                viewModel.changePassword(
                                    currentPassword,
                                    newPassword,
                                    confirmPassword
                                )
                            },
                            modifier = Modifier
                                .weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = currentPassword.isNotEmpty() && 
                                      newPassword.isNotEmpty() && 
                                      confirmPassword.isNotEmpty()
                        ) {
                            Text(
                                text = "Cambiar",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
} 