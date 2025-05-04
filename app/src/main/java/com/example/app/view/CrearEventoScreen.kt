package com.example.app.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app.R
import com.example.app.routes.BottomNavigationBar
import com.example.app.util.SessionManager
import com.example.app.viewmodel.CrearEventoViewModel
import java.util.*
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEventoScreen(
    navController: NavController,
    viewModel: CrearEventoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val userRole = SessionManager.getUserRole() ?: "participante"

    // Lista de categorías disponibles
    val categorias = listOf(
        "Concierto",
        "Festival",
        "Teatro",
        "Deportes",
        "Conferencia",
        "Exposición",
        "Taller",
        "Otro"
    )

    // Estado para el dropdown de categorías
    var expandedCategoria by remember { mutableStateOf(false) }
    
    // Estado para el diálogo de selección de imagen
    var showImagePickerDialog by remember { mutableStateOf(false) }
    
    // Estado para monitorear si la creación fue exitosa
    val isCreationSuccessful by viewModel.isCreationSuccessful.collectAsState()
    
    // Efecto para manejar la navegación después de crear el evento
    LaunchedEffect(isCreationSuccessful) {
        if (isCreationSuccessful) {
            Toast.makeText(context, "Evento creado con éxito", Toast.LENGTH_LONG).show()
            navController.popBackStack()
            viewModel.resetCreationState()
        }
    }
    
    // Seleccionar imagen de la galería
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateImagenUri(it) }
    }
    
    // Capturar imagen con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.imageUri?.let { viewModel.onImageSelected(it) }
        }
    }

    // Permiso para la galería
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false) -> {
                // Permiso concedido en Android 13+
                imagePicker.launch("image/*")
            }
            permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false) -> {
                // Permiso concedido en Android 12-
                imagePicker.launch("image/*")
            }
            else -> {
                Toast.makeText(context, "Se necesita permiso para acceder a la galería", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Permiso para la cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraPermissionGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false)
        val storagePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false)
        } else {
            permissions.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false)
        }
        
        if (cameraPermissionGranted && storagePermissionGranted) {
            viewModel.createImageUri(context)?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Se necesitan permisos para usar la cámara", Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndRequestGalleryPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                galleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            }
            else -> {
                galleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }
    
    fun checkAndRequestCameraPermission() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        cameraPermissionLauncher.launch(permissionsToRequest)
    }
    
    // Diálogo para elegir entre cámara y galería
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Seleccionar imagen") },
            text = { Text("¿Cómo quieres añadir una imagen?") },
            confirmButton = {
                Button(
                    onClick = {
                        showImagePickerDialog = false
                        checkAndRequestCameraPermission()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Tomar foto")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showImagePickerDialog = false
                        checkAndRequestGalleryPermission()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Galería")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(id = R.string.create_event),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE53935),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, userRole = userRole)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Campos del formulario con estilo unificado
            OutlinedTextField(
                value = viewModel.titulo,
                onValueChange = { viewModel.updateTitulo(it) },
                label = { Text("Título del evento*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    focusedLabelColor = Color(0xFFE53935)
                )
            )

            OutlinedTextField(
                value = viewModel.descripcion,
                onValueChange = { viewModel.updateDescripcion(it) },
                label = { Text("Descripción*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    focusedLabelColor = Color(0xFFE53935)
                )
            )

            // Selector de fecha con validación
            OutlinedTextField(
                value = viewModel.fechaEvento,
                onValueChange = {},
                label = { Text("Fecha del evento") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(year, month, day, 0, 0, 0)
                                selectedCalendar.set(Calendar.MILLISECOND, 0)
                                
                                val today = Calendar.getInstance()
                                today.set(Calendar.HOUR_OF_DAY, 0)
                                today.set(Calendar.MINUTE, 0)
                                today.set(Calendar.SECOND, 0)
                                today.set(Calendar.MILLISECOND, 0)
                                
                                if (selectedCalendar.before(today)) {
                                    Toast.makeText(context, "No puedes seleccionar una fecha pasada", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.fechaEvento = String.format("%04d-%02d-%02d", year, month + 1, day)
                                }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.DateRange, "Seleccionar fecha", tint = Color(0xFFE53935))
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    focusedLabelColor = Color(0xFFE53935),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color.Black
                )
            )

            // Selector de hora con validación
            OutlinedTextField(
                value = viewModel.hora,
                onValueChange = {},
                label = { Text("Hora del evento") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                selectedCalendar.set(Calendar.MINUTE, minute)
                                
                                val now = Calendar.getInstance()
                                
                                // Si la fecha seleccionada es hoy, verificar que la hora no sea pasada
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val today = dateFormat.format(now.time)
                                
                                if (viewModel.fechaEvento == today && 
                                    (selectedCalendar.get(Calendar.HOUR_OF_DAY) < now.get(Calendar.HOUR_OF_DAY) || 
                                    (selectedCalendar.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY) && 
                                     selectedCalendar.get(Calendar.MINUTE) < now.get(Calendar.MINUTE)))) {
                                    Toast.makeText(context, "No puedes seleccionar una hora pasada para hoy", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.hora = String.format("%02d:%02d", hour, minute)
                                }
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.Schedule, "Seleccionar hora", tint = Color(0xFFE53935))
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    focusedLabelColor = Color(0xFFE53935),
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledTextColor = Color.Black
                )
            )

            OutlinedTextField(
                value = viewModel.ubicacion,
                onValueChange = { viewModel.ubicacion = it },
                label = { Text("Ubicación") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    focusedLabelColor = Color(0xFFE53935)
                )
            )

            // Selector de categoría con dropdown y estado de carga
            ExposedDropdownMenuBox(
                expanded = expandedCategoria,
                onExpandedChange = { expandedCategoria = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = viewModel.categoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        if (viewModel.isLoadingCategorias) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFE53935)
                            )
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        focusedLabelColor = Color(0xFFE53935)
                    ),
                    enabled = !viewModel.isLoadingCategorias
                )
                if (!viewModel.isLoadingCategorias) {
                    ExposedDropdownMenu(
                        expanded = expandedCategoria,
                        onDismissRequest = { expandedCategoria = false }
                    ) {
                        viewModel.categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria) },
                                onClick = {
                                    viewModel.categoria = categoria
                                    expandedCategoria = false
                                }
                            )
                        }
                    }
                }
            }

            if (viewModel.errorCategorias != null) {
                Text(
                    text = viewModel.errorCategorias!!,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Sección para subir imagen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showImagePickerDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (viewModel.imagenUri != null) {
                        // Mostrar imagen seleccionada
                        AsyncImage(
                            model = viewModel.imagenUri,
                            contentDescription = "Imagen del evento",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Botón para eliminar la imagen
                        IconButton(
                            onClick = { viewModel.updateImagenUri(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Eliminar imagen",
                                tint = Color.White
                            )
                        }
                    } else {
                        // Mostrar opción para subir imagen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Subir imagen",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Añadir imagen del evento",
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (viewModel.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE57373))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error al crear el evento:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Analizar si el error contiene contenido JSON para mostrarlo mejor formateado
                        val errorText = viewModel.error!!
                        if (errorText.contains("{") && errorText.contains("}")) {
                            // Mostrar mensajes de error formateados sin usar try-catch
                            val validationErrors = if (errorText.contains("Error de validación:")) {
                                errorText.substringAfter("Error de validación:")
                            } else {
                                errorText
                            }
                            
                            Text(
                                text = "Se han detectado los siguientes problemas:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Extraer cada campo con error
                            if (validationErrors.contains("fecha")) {
                                Text(
                                    text = "• Fecha: La fecha debe ser posterior a hoy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            
                            if (validationErrors.contains("es_online")) {
                                Text(
                                    text = "• Es online: El campo debe ser true o false",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            
                            if (validationErrors.contains("tipos_entrada")) {
                                Text(
                                    text = "• Tipos de entrada: Formato incorrecto, debe ser un array",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            
                            if (validationErrors.contains("nombre")) {
                                Text(
                                    text = "• Nombre: El nombre del tipo de entrada es obligatorio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            
                            if (validationErrors.contains("precio")) {
                                Text(
                                    text = "• Precio: El precio del tipo de entrada es obligatorio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            
                            if (validationErrors.contains("es_ilimitado")) {
                                Text(
                                    text = "• Es ilimitado: Debe especificar si las entradas son ilimitadas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        } else {
                            // Mostrar el error texto plano
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }

            // Selector online / presencial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evento online",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                Switch(
                    checked = viewModel.esOnline,
                    onCheckedChange = { viewModel.esOnline = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFE53935),
                        checkedTrackColor = Color(0xFFE53935).copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Sección de tipos de entrada (solo visible si el evento no es online)
            if (!viewModel.esOnline) {
                Text(
                    text = "TIPOS DE ENTRADA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Añade al menos un tipo de entrada para tu evento",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Lista de tipos de entrada
                viewModel.tiposEntrada.forEachIndexed { index, tipoEntrada ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Cabecera con título y botón eliminar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tipo de entrada ${index + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                
                                if (viewModel.tiposEntrada.size > 1) {
                                    IconButton(onClick = { viewModel.removeTipoEntrada(index) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar tipo de entrada",
                                            tint = Color(0xFFE53935)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Nombre del tipo de entrada
                            OutlinedTextField(
                                value = tipoEntrada.nombre,
                                onValueChange = { newValue -> 
                                    val updatedTipos = viewModel.tiposEntrada.toMutableList()
                                    updatedTipos[index] = tipoEntrada.copy(nombre = newValue)
                                    viewModel.tiposEntrada = updatedTipos
                                },
                                label = { Text("Nombre") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE53935),
                                    focusedLabelColor = Color(0xFFE53935)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Precio
                            OutlinedTextField(
                                value = tipoEntrada.precio.toString(),
                                onValueChange = { newValue -> 
                                    val precio = newValue.toDoubleOrNull() ?: 0.0
                                    val updatedTipos = viewModel.tiposEntrada.toMutableList()
                                    updatedTipos[index] = tipoEntrada.copy(precio = precio)
                                    viewModel.tiposEntrada = updatedTipos
                                },
                                label = { Text("Precio (€)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE53935),
                                    focusedLabelColor = Color(0xFFE53935)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Descripción
                            OutlinedTextField(
                                value = tipoEntrada.descripcion ?: "",
                                onValueChange = { newValue -> 
                                    val updatedTipos = viewModel.tiposEntrada.toMutableList()
                                    updatedTipos[index] = tipoEntrada.copy(descripcion = newValue)
                                    viewModel.tiposEntrada = updatedTipos
                                },
                                label = { Text("Descripción (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE53935),
                                    focusedLabelColor = Color(0xFFE53935)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Switch para entradas ilimitadas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Entradas ilimitadas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Switch(
                                    checked = tipoEntrada.esIlimitado,
                                    onCheckedChange = { newValue -> 
                                        val updatedTipos = viewModel.tiposEntrada.toMutableList()
                                        updatedTipos[index] = tipoEntrada.copy(esIlimitado = newValue)
                                        viewModel.tiposEntrada = updatedTipos
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFE53935),
                                        checkedTrackColor = Color(0xFFE53935).copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            // Campo de cantidad disponible (solo visible si no es ilimitado)
                            if (!tipoEntrada.esIlimitado) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = (tipoEntrada.cantidadDisponible ?: 100).toString(),
                                    onValueChange = { newValue -> 
                                        val cantidad = newValue.toIntOrNull() ?: 0
                                        val updatedTipos = viewModel.tiposEntrada.toMutableList()
                                        updatedTipos[index] = tipoEntrada.copy(cantidadDisponible = cantidad)
                                        viewModel.tiposEntrada = updatedTipos
                                    },
                                    label = { Text("Cantidad disponible") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFE53935),
                                        focusedLabelColor = Color(0xFFE53935)
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Botón para añadir otro tipo de entrada
                Button(
                    onClick = { viewModel.addTipoEntrada() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFE53935)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir tipo de entrada",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir tipo de entrada")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = { 
                    viewModel.crearEventoConImagen(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "CREAR EVENTO",
                        modifier = Modifier.padding(8.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mostrar errores si existen
            viewModel.error?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            // Espacio al final para evitar que el último elemento quede bajo la barra de navegación
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}