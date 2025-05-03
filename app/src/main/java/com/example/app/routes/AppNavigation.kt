package com.example.app.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.example.app.view.EventoDetailScreen
import com.example.app.view.EventosScreen
import com.example.app.view.LoginScreen
import com.example.app.view.ForgotPasswordScreen
import com.example.app.view.MisEventosScreen
import com.example.app.util.SessionManager
import android.util.Log
import androidx.compose.runtime.collectAsState
import com.example.app.view.CrearEventoScreen
import com.example.app.viewmodel.RegisterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.routes.safeTokenDisplay

// Variable global para rastrear si el usuario está autenticado
private var isUserAuthenticated = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Estado para el rol
    var currentRole by remember { 
        mutableStateOf(SessionManager.getUserRole() ?: "participante") 
    }
    
    // Estado para controlar la navegación
    var navigateToEventos by remember { mutableStateOf(false) }
    
    // Efecto para actualizar el rol cuando la composición se inicie
    LaunchedEffect(Unit) {
        val role = SessionManager.getUserRole()
        Log.d("AppNavigation", "LaunchedEffect - Rol inicial: $role")
        if (role != null) {
            currentRole = role
            Log.d("AppNavigation", "LaunchedEffect - currentRole actualizado a: $currentRole")
        }
    }
    
    // Observar y ejecutar navegaciones
    LaunchedEffect(navigateToEventos) {
        if (navigateToEventos) {
            navController.navigate(Routes.Eventos.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
            }
            navigateToEventos = false
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (isUserAuthenticated) Routes.Eventos.route else Routes.Login.route
    ) {
        // Pantalla de login
        composable(Routes.Login.route) {
            // Estado para controlar si el login fue exitoso
            val loginSuccess = remember { mutableStateOf(false) }
            
            // Efecto para manejar el login exitoso
            LaunchedEffect(Unit) {
                navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("login_successful")?.let { success ->
                    if (success) {
                        loginSuccess.value = true
                        isUserAuthenticated = true
                        val role = SessionManager.getUserRole() ?: ""
                        Log.d("AppNavigation", "Login exitoso - Rol obtenido: $role")
                        currentRole = role
                        Log.d("AppNavigation", "Login exitoso - currentRole actualizado a: $currentRole")
                        
                        navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("login_successful")
                    }
                }
            }
            
            // Efecto para la navegación después del login exitoso
            LaunchedEffect(loginSuccess.value) {
                if (loginSuccess.value) {
                    navController.navigate(Routes.Eventos.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                    loginSuccess.value = false
                }
            }
            
            LoginScreen(
                navController = navController,
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.ForgotPassword.route)
                }
            )
        }
        
        // Pantallas de registro
        composable(Routes.Register.route) {
            com.example.app.view.register.RegisterScreen(
                navController = navController
            )
        }
        
        composable(Routes.RegisterOrganizador.route) {
            val registerViewModel = viewModel<RegisterViewModel>()
            
            // Obtener datos guardados desde la pantalla anterior
            val savedEmail = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_email") ?: ""
            val savedName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_name") ?: ""
            val savedLastName = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_lastname") ?: ""
            val savedLastName2 = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_lastname2") ?: ""
            
            // Si vienen datos de un login con Google, actualizamos el viewModel
            LaunchedEffect(savedEmail) {
                if (savedEmail.isNotEmpty()) {
                    registerViewModel.email = savedEmail
                    registerViewModel.name = savedName
                    registerViewModel.apellido1 = savedLastName
                    registerViewModel.apellido2 = savedLastName2
                }
            }
            
            // Estado para controlar la navegación después del registro exitoso
            val registerSuccess = remember { mutableStateOf(false) }
            
            // Observar si el registro fue exitoso
            LaunchedEffect(registerViewModel.isRegisterSuccessful.collectAsState().value) {
                if (registerViewModel.isRegisterSuccessful.value) {
                    registerSuccess.value = true
                }
            }
            
            // Efecto para la navegación después del registro exitoso
            LaunchedEffect(registerSuccess.value) {
                if (registerSuccess.value) {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Register.route) { inclusive = true }
                    }
                    registerSuccess.value = false
                }
            }
            
            com.example.app.view.register.OrganizadorScreen(
                viewModel = registerViewModel
            )
        }
        
        composable(Routes.RegisterParticipante.route) {
            val registerViewModel = viewModel<RegisterViewModel>()
            
            // Obtener datos guardados desde la pantalla anterior
            val savedEmail = navController.currentBackStackEntry?.savedStateHandle?.get<String>("user_email") ?: ""
            val savedName = navController.currentBackStackEntry?.savedStateHandle?.get<String>("user_name") ?: ""
            val savedLastName = navController.currentBackStackEntry?.savedStateHandle?.get<String>("user_lastname") ?: ""
            val savedLastName2 = navController.currentBackStackEntry?.savedStateHandle?.get<String>("user_lastname2") ?: ""
            val isFromGoogleLogin = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("google_login") ?: false
            val token = navController.currentBackStackEntry?.savedStateHandle?.get<String>("token") ?: ""
            
            Log.d("REGISTRO_DEBUG", "======= PANTALLA PARTICIPANTE =======")
            Log.d("REGISTRO_DEBUG", "Datos recibidos:")
            Log.d("REGISTRO_DEBUG", "Email: $savedEmail")
            Log.d("REGISTRO_DEBUG", "Nombre: $savedName")
            Log.d("REGISTRO_DEBUG", "Apellido1: $savedLastName")
            Log.d("REGISTRO_DEBUG", "Apell1ido2: $savedLastName2")
            Log.d("REGISTRO_DEBUG", "¿Viene de Google? $isFromGoogleLogin")
            Log.d("REGISTRO_DEBUG", "Token recibido: ${token.safeTokenDisplay()}")
            
            // Si vienen datos de un login con Google, actualizamos el viewModel
            LaunchedEffect(Unit) {
                Log.d("REGISTRO_DEBUG", "Iniciando LaunchedEffect para cargar datos de Google")
                if (savedEmail.isNotEmpty() && isFromGoogleLogin) {
                    Log.d("REGISTRO_DEBUG", "Cargando datos de Google en RegisterViewModel")
                    
                    // Usar el nuevo método para establecer los datos de Google
                    registerViewModel.setGoogleAuthData(
                        email = savedEmail,
                        name = savedName,
                        apellido1 = savedLastName,
                        apellido2 = savedLastName2,
                        token = token
                    )
                    
                    // Verificar que los datos se establecieron correctamente
                    Log.d("REGISTRO_DEBUG", "Verificando datos establecidos:")
                    Log.d("REGISTRO_DEBUG", "Email: ${registerViewModel.email}")
                    Log.d("REGISTRO_DEBUG", "Nombre: ${registerViewModel.name}")
                    Log.d("REGISTRO_DEBUG", "Apellido1: ${registerViewModel.apellido1}")
                    Log.d("REGISTRO_DEBUG", "Apellido2: ${registerViewModel.apellido2}")
                    Log.d("REGISTRO_DEBUG", "Token: ${registerViewModel.googleToken?.safeTokenDisplay()}")
                    Log.d("REGISTRO_DEBUG", "¿Es de Google Auth? ${registerViewModel.isFromGoogleAuth}")
                    Log.d("REGISTRO_DEBUG", "Datos de Google cargados en RegisterViewModel")
                } else if (isFromGoogleLogin) {
                    // Si falta información importante
                    Log.e("REGISTRO_DEBUG", "ERROR: Faltan datos importantes para Google Auth")
                    Log.e("REGISTRO_DEBUG", "Email vacío: ${savedEmail.isEmpty()}")
                    Log.e("REGISTRO_DEBUG", "Nombre vacío: ${savedName.isEmpty()}")
                    Log.e("REGISTRO_DEBUG", "Token vacío: ${token.isEmpty()}")
                    registerViewModel.setError("No se recibieron todos los datos necesarios del login con Google")
                }
            }
            
            // Estado para controlar la navegación después del registro exitoso
            val registerSuccess = remember { mutableStateOf(false) }
            val destinationRoute = remember { mutableStateOf("") }
            
            // Observar si el registro fue exitoso
            LaunchedEffect(registerViewModel.isRegisterSuccessful.collectAsState().value) {
                if (registerViewModel.isRegisterSuccessful.value) {
                    Log.d("REGISTRO_DEBUG", "Registro participante EXITOSO")
                    
                    // Decidir la ruta de destino
                    destinationRoute.value = if (isFromGoogleLogin) Routes.Eventos.route else Routes.Login.route
                    Log.d("REGISTRO_DEBUG", "Ruta de destino: ${destinationRoute.value}")
                    
                    if (isFromGoogleLogin) {
                        // Si viene de Google Login, establecer isUserAuthenticated
                        isUserAuthenticated = true
                        
                        // Asegurar que el token esté guardado en SessionManager
                        val savedToken = SessionManager.getToken()
                        if (savedToken.isNullOrEmpty() && token.isNotEmpty()) {
                            Log.d("REGISTRO_DEBUG", "Token no encontrado en SessionManager, guardando token recibido")
                            SessionManager.saveToken(token)
                            SessionManager.saveUserRole("participante")
                        }
                        Log.d("REGISTRO_DEBUG", "Token guardado para navegación: ${SessionManager.getToken().safeTokenDisplay()}")
                    }
                    
                    registerSuccess.value = true
                    Log.d("REGISTRO_DEBUG", "registerSuccess establecido a true, preparando navegación")
                }
            }
            
            // Efecto para la navegación después del registro exitoso
            LaunchedEffect(registerSuccess.value) {
                if (registerSuccess.value && destinationRoute.value.isNotEmpty()) {
                    Log.d("REGISTRO_DEBUG", "Iniciando navegación a ${destinationRoute.value}")
                    navController.navigate(destinationRoute.value) {
                        // Limpiar toda la pila de navegación hasta la raíz
                        popUpTo(0) { inclusive = true }
                    }
                    registerSuccess.value = false
                    Log.d("REGISTRO_DEBUG", "Navegación completada, resetenado estado")
                }
            }
            
            com.example.app.view.register.ParticipanteScreen(
                viewModel = registerViewModel
            )
        }
        
        // Pantalla de recuperación de contraseña
        composable(Routes.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateToLogin = { 
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Pantalla principal con menú de navegación
        composable(Routes.Eventos.route) {
            Log.d("AppNavigation", "Composable Eventos - currentRole: $currentRole")
            HomeScreenWithBottomNav(
                navController = navController,
                userRole = currentRole
            )
        }
        
        composable(Routes.CrearEvento.route) {
            CrearEventoScreen(navController = navController)
        }
    }
}

@Composable
fun HomeScreenWithBottomNav(
    navController: NavController,
    userRole: String
) {
    Log.d("HomeScreenWithBottomNav", "Iniciando con userRole: $userRole")
    
    Scaffold(
        bottomBar = {
            Log.d("HomeScreenWithBottomNav", "Construyendo BottomBar con userRole: $userRole")
            BottomNavigationBar(
                navController = navController,
                userRole = userRole
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            EventosScreen(
                onEventoClick = { evento ->
                    val route = Routes.EventoDetalle.createRoute(evento.id.toString())
                    navController.navigate(route)
                },
                navController = navController
            )
        }
    }
}