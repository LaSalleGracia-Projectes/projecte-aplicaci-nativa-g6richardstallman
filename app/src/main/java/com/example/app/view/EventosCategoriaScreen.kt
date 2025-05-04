package com.example.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.R
import com.example.app.viewmodel.EventosCategoriaViewModel
import com.example.app.routes.Routes
import com.example.app.model.evento.CategoriaEvento

/**
 * Función auxiliar para obtener la categoría normalizada a partir de un string
 */
private fun obtenerCategoriaNormalizada(context: android.content.Context, categoriaValor: String): String {
    return try {
        val resourceId = context.resources.getIdentifier(
            "categoria_" + categoriaValor.lowercase()
                .replace(" ", "_")
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("y", "").replace("&", "")
                .trim('_'),
            "string",
            context.packageName
        )
        if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            categoriaValor
        }
    } catch (e: Exception) {
        categoriaValor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventosCategoriaScreen(
    categoria: String,
    navController: NavController,
    viewModel: EventosCategoriaViewModel = viewModel()
) {
    val eventos = viewModel.eventos
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val context = LocalContext.current

    // Cargar eventos al entrar
    LaunchedEffect(categoria) {
        viewModel.loadEventosByCategoria(categoria)
    }

    val primaryColor = Color(0xFFE53935)
    val backgroundColor = Color.White
    val textPrimaryColor = Color.Black
    val textSecondaryColor = Color.DarkGray
    val successColor = Color(0xFF4CAF50)

    // Obtener el título localizado de la categoría
    val categoriaTitulo = remember(categoria) {
        // Intentar encontrar una categoría que coincida
        val categoriaEnum = CategoriaEvento.fromApiValue(categoria)
        if (categoriaEnum != null) {
            categoriaEnum.getLocalizedValue(context)
        } else {
            // Si no se encuentra la categoría, usar el método auxiliar
            obtenerCategoriaNormalizada(context, categoria)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = categoriaTitulo.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = stringResource(id = R.string.back),
                                tint = primaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = primaryColor,
                        navigationIconContentColor = primaryColor
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (errorMessage != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error: $errorMessage", 
                            color = Color.Red, 
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (eventos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(id = R.string.no_events_in_category),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondaryColor
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(eventos) { evento ->
                            EventoCard(
                                evento = evento,
                                onClick = {
                                    navController.navigate(Routes.EventoDetalle.createRoute(evento.getEventoId().toString()))
                                },
                                primaryColor = primaryColor,
                                textPrimaryColor = textPrimaryColor,
                                textSecondaryColor = textSecondaryColor,
                                successColor = successColor,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}
