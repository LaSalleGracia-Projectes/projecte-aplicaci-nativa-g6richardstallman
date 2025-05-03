package com.example.app.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.view.*
import com.example.app.view.favoritos.FavoritosScreen
import com.example.app.view.register.OrganizadorScreen
import com.example.app.view.register.ParticipanteScreen
import com.example.app.view.register.RegisterScreen
import com.example.app.viewmodel.LoginViewModel
import com.example.app.viewmodel.ProfileViewModel
import com.example.app.viewmodel.RegisterViewModel
import com.example.app.viewmodel.TicketsViewModel
import kotlinx.coroutines.delay
import android.util.Log
import com.example.app.util.SessionManager
import android.widget.Toast
import android.content.Context
import com.example.app.model.Organizador
import com.example.app.view.organizador.OrganizadorDetailScreen
import com.example.app.view.EventosCategoriaScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = determineStartDestination()
) {
    val sharedRegisterViewModel: RegisterViewModel = viewModel()
    val sharedLoginViewModel: LoginViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    
    // Verificar token al inicio y en cada cambio de ruta
    LaunchedEffect(Unit) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Lista de rutas que requieren autenticación
            val protectedRoutes = listOf(
                Routes.Eventos.route,
                Routes.MisEventos.route,
                Routes.Perfil.route,
                Routes.Favoritos.route
            )
            
            if (destination.route in protectedRoutes) {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.d("AppNavHost", "Ruta protegida sin token, navegando a Login")
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
    
    // Verificar token al inicio
    LaunchedEffect(Unit) {
        val token = SessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Log.d("AppNavHost", "No hay token válido, navegando a Login")
            navController.navigate(Routes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // Observar estados de navegación
    val isRegisterSuccessful by sharedRegisterViewModel.isRegisterSuccessful.collectAsState()
    val isLogoutSuccessful by sharedLoginViewModel.isLogoutSuccessful.collectAsState()
    val shouldNavigateToLogin by profileViewModel.shouldNavigateToLogin.collectAsState()

    // Efecto para navegar después del registro exitoso
    LaunchedEffect(isRegisterSuccessful) {
        if (isRegisterSuccessful) {
            val userRole = SessionManager.getUserRole() ?: "participante"
            Log.d("AppNavHost", "Registro exitoso, rol en SessionManager: $userRole")
            
            delay(1500)
            navController.navigate(Routes.Eventos.route) {
                popUpTo(0) { inclusive = true }
            }
            sharedRegisterViewModel.resetSuccessState()
        }
    }

    // Efecto para manejar la navegación después del logout
    LaunchedEffect(shouldNavigateToLogin) {
        if (shouldNavigateToLogin) {
            Log.d("AppNavHost", "Detectada señal de logout, redirigiendo a login")
            SessionManager.clearSession()
            navController.navigate(Routes.Login.route) {
                popUpTo(0) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            // Resetear el estado para evitar navegaciones repetidas
            profileViewModel.resetShouldNavigateToLogin()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Routes.ForgotPassword.route) },
                navController = navController,
                viewModel = sharedLoginViewModel
            )
        }
        
        composable(Routes.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateToLogin = { 
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.Eventos.route) {
            EventosScreen(
                onEventoClick = { evento ->
                    val eventoId = evento.getEventoId()
                    Log.d("Navigation", "Clicked on evento with id: $eventoId")
                    val idString = eventoId.toString()
                    
                    if (eventoId > 0) {
                        try {
                            val route = Routes.EventoDetalle.createRoute(idString)
                            Log.d("Navigation", "Created route: $route")
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                            Log.d("Navigation", "Successfully navigated to route: $route")
                        } catch (e: Exception) {
                            Log.e("Navigation", "Error navigating to route", e)
                            Toast.makeText(
                                navController.context,
                                "Error al abrir detalle de evento: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.e("Navigation", "Invalid evento ID: $eventoId")
                        Toast.makeText(
                            navController.context,
                            "ID de evento inválido: $eventoId",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                navController = navController
            )
        }
        
        composable(
            route = "evento/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventoId = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: -1
            EventoDetailScreen(
                navController = navController,
                eventoId = eventoId.toString()
            )
        }
        
        composable(Routes.Register.route) {
            RegisterScreen(
                navController = navController,
                viewModel = sharedRegisterViewModel
            )
        }
        
        composable(Routes.RegisterOrganizador.route) {
            OrganizadorScreen(viewModel = sharedRegisterViewModel)
        }
        
        composable(Routes.RegisterParticipante.route) {
            ParticipanteScreen(viewModel = sharedRegisterViewModel)
        }

        // Rutas adicionales para el menú de navegación
        composable(Routes.MisEventos.route) {
            MisEventosScreen(
                navController = navController,
                onEventoClick = { evento ->
                    val eventoId = evento.getEventoId()
                    Log.d("Navigation", "Clicked on evento from MisEventos with id: $eventoId")
                    val idString = eventoId.toString()
                    
                    if (eventoId > 0) {
                        try {
                            val route = Routes.EventoDetalle.createRoute(idString)
                            Log.d("Navigation", "MisEventos - Created route: $route")
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                            Log.d("Navigation", "MisEventos - Successfully navigated to route: $route")
                        } catch (e: Exception) {
                            Log.e("Navigation", "MisEventos - Error navigating to detail route", e)
                            Toast.makeText(
                                navController.context,
                                "Error al abrir detalle de evento: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.e("Navigation", "MisEventos - Invalid evento ID: $eventoId")
                        Toast.makeText(
                            navController.context,
                            "ID de evento inválido: $eventoId",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onCreateEventoClick = {
                    navController.navigate(Routes.CrearEvento.route)
                },
                onEditEventoClick = { evento ->
                    val eventoId = evento.getEventoId()
                    Log.d("Navigation", "===== INICIO EDICIÓN DESDE MIS EVENTOS =====")
                    Log.d("Navigation", "Evento a editar - ID: $eventoId, Título: ${evento.titulo}")
                    Log.d("Navigation", "Tipo de ID: ${eventoId.javaClass.name}")
                    
                    if (eventoId > 0) {
                        try {
                            val idStr = eventoId.toString()
                            Log.d("Navigation", "ID convertido a String: '$idStr'")
                            
                            val route = Routes.EditarEvento.createRoute(idStr)
                            Log.d("Navigation", "Ruta de edición creada: $route")
                            
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                            Log.d("Navigation", "Navegación a edición completada")
                        } catch (e: Exception) {
                            Log.e("Navigation", "Error al crear ruta de edición", e)
                            Toast.makeText(
                                navController.context,
                                "Error al abrir pantalla de edición: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.e("Navigation", "Error: ID de evento inválido: $eventoId")
                        Toast.makeText(
                            navController.context,
                            "No se puede editar: ID de evento inválido ($eventoId)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDeleteEventoClick = { evento ->
                    Log.d("Navigation", "===== INICIO ELIMINACIÓN DESDE MIS EVENTOS =====")
                    Log.d("Navigation", "Evento a eliminar - ID: ${evento.getEventoId()}, Título: ${evento.titulo}")
                    // No se requiere ninguna acción adicional, ya que la eliminación se maneja en el ViewModel
                    // La propia pantalla MisEventosScreen contiene la lógica de confirmación y eliminación
                }
            )
        }
        
        composable(Routes.CrearEvento.route) {
            CrearEventoScreen(navController = navController)
        }
        
        composable(
            route = Routes.EditarEvento.route,
            arguments = listOf(
                navArgument("eventoId") { 
                    type = NavType.StringType 
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val eventoIdString = backStackEntry.arguments?.getString("eventoId")
            Log.d("Navigation", "EditarEvento recibió eventoId string: '$eventoIdString'")
            
            // Procesar el ID fuera del composable
            val eventoId = procesarEventoId(eventoIdString, navController.context)
            Log.d("Navigation", "Pasando a EditarEventoScreen ID: $eventoId")
            
            EditarEventoScreen(
                navController = navController,
                eventoId = eventoId
            )
        }
        
        composable(Routes.MisTickets.route) {
            val context = LocalContext.current
            MisTicketsScreen(
                navController = navController,
                viewModel = viewModel(
                    factory = TicketsViewModelFactory(context.applicationContext as android.app.Application)
                )
            )
        }
        
        composable(Routes.Favoritos.route) {
            FavoritosScreen(
                navController = navController,
                viewModel = viewModel()
            )
        }
        
        composable(Routes.Perfil.route) {
            ProfileScreen(
                navController = navController
            )
        }
        
        composable(Routes.HistorialCompras.route) {
            HistorialComprasScreen(
                navController = navController
            )
        }
        
        composable(
            route = Routes.OrganizadorDetalle.route,
            arguments = listOf(
                navArgument("organizadorId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val organizadorId = backStackEntry.arguments?.getInt("organizadorId") ?: 0
            // Aquí podrías cargar el objeto Organizador si tu API lo permite
            // Temporalmente pasamos un organizador con datos mínimos
            val organizador = Organizador(id = organizadorId, nombre = "Organización #$organizadorId", telefonoContacto = "", user = null)
            OrganizadorDetailScreen(
                navController = navController,
                organizador = organizador
            )
        }

        // Ruta para eventos por categoría
        composable(
            route = Routes.EventosCategoria.route,
            arguments = listOf(navArgument("categoria") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoria = backStackEntry.arguments?.getString("categoria") ?: ""
            EventosCategoriaScreen(
                categoria = categoria,
                navController = navController
            )
        }
    }
}

// Función para determinar la ruta inicial basada en si hay token
private fun determineStartDestination(): String {
    val token = SessionManager.getToken()
    return if (token.isNullOrEmpty()) {
        Log.d("AppNavHost", "Iniciando en Login porque no hay token")
        Routes.Login.route
    } else {
        Log.d("AppNavHost", "Iniciando en Eventos porque hay token")
        Routes.Eventos.route
    }
}

// Función auxiliar para procesar el ID del evento
private fun procesarEventoId(eventoIdString: String?, context: Context): Int {
    return eventoIdString?.let {
        try {
            // Limpieza adicional del ID antes de convertir
            val cleanId = it.trim()
            Log.d("Navigation", "ID limpiado: '$cleanId'")
            
            val id = cleanId.toIntOrNull()
            if (id == null) {
                Log.e("Navigation", "Error: No se pudo convertir '$cleanId' a Int")
                Toast.makeText(
                    context,
                    "Error: ID de evento inválido",
                    Toast.LENGTH_LONG
                ).show()
                -1
            } else if (id <= 0) {
                Log.e("Navigation", "Error: ID de evento <= 0: $id")
                Toast.makeText(
                    context,
                    "Error: ID de evento inválido ($id)",
                    Toast.LENGTH_LONG
                ).show()
                -1
            } else {
                Log.d("Navigation", "ID de evento válido: $id")
                id
            }
        } catch (e: Exception) {
            Log.e("Navigation", "Excepción al procesar ID de evento: $it", e)
            Toast.makeText(
                context,
                "Error al procesar ID de evento: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            -1
        }
    } ?: run {
        Log.e("Navigation", "Error: ID de evento es nulo")
        Toast.makeText(
            context,
            "Error: ID de evento no especificado",
            Toast.LENGTH_LONG
        ).show()
        -1
    }
} 