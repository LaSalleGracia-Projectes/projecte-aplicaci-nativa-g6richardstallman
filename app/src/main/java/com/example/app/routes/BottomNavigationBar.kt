package com.example.app.routes

import android.util.Log
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.app.R
import com.example.app.util.SessionManager

// Enumeración para los tipos de usuario
enum class UserType {
    ALL,           // Todos los usuarios
    PARTICIPANT,   // Solo participantes
    ORGANIZER      // Solo organizadores
}

// Definición de los elementos del menú de navegación
data class NavItem(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector,
    val userType: UserType
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    userRole: String = SessionManager.getUserRole() ?: "participante"
) {
    Log.d("BottomNavigationBar", "Iniciando BottomNavigationBar con userRole: '$userRole'")
    
    val normalizedRole = userRole.trim().lowercase()
    Log.d("BottomNavigationBar", "Rol normalizado: $normalizedRole")
    
    val items = remember(normalizedRole) {
        when (normalizedRole) {
            "organizador" -> {
                Log.d("BottomNavigationBar", "Creando menú de Organizador")
                listOf(
                    NavItem(
                        route = "eventos",
                        titleResId = R.string.events,
                        icon = Icons.Default.Home,
                        userType = UserType.ALL
                    ),
                    NavItem(
                        route = "mis_eventos",
                        titleResId = R.string.my_events,
                        icon = Icons.Default.ConfirmationNumber,
                        userType = UserType.ORGANIZER
                    ),
                    NavItem(
                        route = Routes.CrearEvento.route,
                        titleResId = R.string.create_event,
                        icon = Icons.Default.Add,
                        userType = UserType.ORGANIZER
                    ),
                    NavItem(
                        route = "perfil",
                        titleResId = R.string.profile,
                        icon = Icons.Default.Person,
                        userType = UserType.ALL
                    )
                ).also {
                    Log.d("BottomNavigationBar", "Menú de Organizador creado con ${it.size} items")
                }
            }
            else -> {
                Log.d("BottomNavigationBar", "Creando menú de Participante")
                listOf(
                    NavItem(
                        route = "eventos",
                        titleResId = R.string.events,
                        icon = Icons.Default.Home,
                        userType = UserType.ALL
                    ),
                    NavItem(
                        route = "mis_tickets",
                        titleResId = R.string.my_tickets,
                        icon = Icons.Default.ConfirmationNumber,
                        userType = UserType.PARTICIPANT
                    ),
                    NavItem(
                        route = "favoritos",
                        titleResId = R.string.favorites,
                        icon = Icons.Default.Favorite,
                        userType = UserType.PARTICIPANT
                    ),
                    NavItem(
                        route = "perfil",
                        titleResId = R.string.profile,
                        icon = Icons.Default.Person,
                        userType = UserType.ALL
                    )
                ).also {
                    Log.d("BottomNavigationBar", "Menú de Participante creado con ${it.size} items")
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier.height(60.dp),
        containerColor = Color.White,
        contentColor = Color(0xFFE53935)
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.titleResId),
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(text = stringResource(id = item.titleResId)) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFE53935),
                    selectedTextColor = Color(0xFFE53935),
                    indicatorColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}