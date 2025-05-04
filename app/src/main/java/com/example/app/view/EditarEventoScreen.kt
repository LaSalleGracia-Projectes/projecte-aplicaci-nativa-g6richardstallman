package com.example.app.view

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app.routes.BottomNavigationBar
import com.example.app.util.SessionManager
import com.example.app.viewmodel.EditarEventoViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarEventoScreen(
    navController: NavController,
    eventoId: Int,
    viewModel: EditarEventoViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    
    // Cargar el evento cuando se inicia la pantalla
    LaunchedEffect(eventoId) {
        Log.d("EditarEventoScreen", "LaunchedEffect con ID: $eventoId")
        if (eventoId <= 0) {
            Log.e("EditarEventoScreen", "Error: ID de evento inválido: $eventoId")
            viewModel.updateError("ID de evento inválido ($eventoId)")
            Toast.makeText(context, "ID de evento inválido: $eventoId", Toast.LENGTH_LONG).show()
            // Regresar a la pantalla anterior después de un breve retraso
            kotlinx.coroutines.delay(2000)
            navController.popBackStack()
        } else {
            Log.d("EditarEventoScreen", "Cargando evento con ID: $eventoId")
            viewModel.cargarEvento(eventoId)
        }
    }
    
    // Estado para el dropdown de categorías
    var expandedCategoria by remember { mutableStateOf(false) }
    
    // Estado para el diálogo de selección de imagen
    var showImagePickerDialog by remember { mutableStateOf(false) }
    
    // Estado para monitorear si la actualización fue exitosa
    val isUpdateSuccessful by viewModel.isUpdateSuccessful.collectAsState()
    
    // Efecto para manejar la navegación después de actualizar el evento
    LaunchedEffect(isUpdateSuccessful) {
        if (isUpdateSuccessful) {
            Toast.makeText(context, "Evento actualizado con éxito", Toast.LENGTH_LONG).show()
            navController.popBackStack()
            viewModel.resetUpdateState()
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
            viewModel.imagenCamara?.let { uri -> viewModel.onImageSelected(uri) }
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
    
    // Función para solicitar permisos de cámara
    fun checkAndRequestCameraPermission() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        cameraPermissionLauncher.launch(permissionsToRequest)
    }
    
    // Función para mostrar el selector de fecha
    val showDatePicker = {
        val year: Int
        val month: Int
        val day: Int
        
        val currentDate = if (viewModel.fechaEvento.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateFormat.parse(viewModel.fechaEvento)?.let { date ->
                    Calendar.getInstance().apply { time = date }
                } ?: Calendar.getInstance()
            } catch (e: Exception) {
                Calendar.getInstance()
            }
        } else {
            Calendar.getInstance()
        }
        
        year = currentDate.get(Calendar.YEAR)
        month = currentDate.get(Calendar.MONTH)
        day = currentDate.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)
                Log.d("EditarEventoScreen", "Fecha seleccionada: $formattedDate")
                viewModel.updateFecha(formattedDate)
            },
            year, month, day
        )
        
        // Establecer fecha mínima como hoy
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis
        
        datePickerDialog.show()
    }
    
    // Función para mostrar el selector de hora
    val showTimePicker = {
        val hour: Int
        val minute: Int
        
        val currentTime = if (viewModel.hora?.isNotEmpty() == true) {
            try {
                val parts = viewModel.hora?.split(":")
                if (parts != null && parts.size == 2) {
                    Pair(parts[0].toInt(), parts[1].toInt())
                } else {
                    val calendar = Calendar.getInstance()
                    Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                }
            } catch (e: Exception) {
                val calendar = Calendar.getInstance()
                Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            }
        } else {
            val calendar = Calendar.getInstance()
            Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        }
        
        hour = currentTime.first
        minute = currentTime.second
        
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                viewModel.updateHora(String.format("%02d:%02d", selectedHour, selectedMinute))
            },
            hour, minute, true
        )
        
        timePickerDialog.show()
    }
    
    // Función para solicitar el permiso de galería adecuado según la versión de Android
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
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "EDITAR EVENTO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack, 
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFE53935),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }
        } else if (!viewModel.eventoLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = viewModel.error ?: "Cargando evento...",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (viewModel.error != null) Color.Red else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Mostrar error si existe
                if (viewModel.error != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
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
                                text = "Error al editar el evento:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = viewModel.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                
                // Sección de información básica
                Text(
                    text = "INFORMACIÓN BÁSICA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Título del evento
                OutlinedTextField(
                    value = viewModel.titulo,
                    onValueChange = { viewModel.updateTitulo(it) },
                    label = { Text("Título del evento") },
                    placeholder = { Text("Ej. Concierto de Rock") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        focusedLabelColor = Color(0xFFE53935)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Descripción
                OutlinedTextField(
                    value = viewModel.descripcion.orEmpty(),
                    onValueChange = { viewModel.updateDescripcion(it) },
                    label = { Text("Descripción") },
                    placeholder = { Text("Describe tu evento") },
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Fecha
                OutlinedTextField(
                    value = viewModel.fechaEvento,
                    onValueChange = { viewModel.updateFecha(it) },
                    label = { Text("Fecha") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker() }) {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = "Seleccionar fecha",
                                tint = Color(0xFFE53935)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        focusedLabelColor = Color(0xFFE53935)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hora
                OutlinedTextField(
                    value = viewModel.hora.orEmpty(),
                    onValueChange = { viewModel.updateHora(it) },
                    label = { Text("Hora") },
                    placeholder = { Text("HH:MM") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker() }) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = "Seleccionar hora",
                                tint = Color(0xFFE53935)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        focusedLabelColor = Color(0xFFE53935)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Ubicación
                OutlinedTextField(
                    value = viewModel.ubicacion.orEmpty(),
                    onValueChange = { viewModel.updateUbicacion(it) },
                    label = { Text("Ubicación") },
                    placeholder = { Text("Ej. Estadio Municipal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        focusedLabelColor = Color(0xFFE53935)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Categoría (dropdown)
                ExposedDropdownMenuBox(
                    expanded = expandedCategoria,
                    onExpandedChange = { expandedCategoria = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = viewModel.categoria.orEmpty(),
                        onValueChange = {},
                        label = { Text("Categoría") },
                        placeholder = { Text("Selecciona una categoría") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoria)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            focusedLabelColor = Color(0xFFE53935)
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedCategoria,
                        onDismissRequest = { expandedCategoria = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria) },
                                onClick = {
                                    viewModel.updateCategoria(categoria)
                                    expandedCategoria = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Imagen del evento
                Text(
                    text = "IMAGEN DEL EVENTO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Selecciona una imagen para tu evento (opcional)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Selección/visualización de imagen
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (viewModel.imagenUri != null) {
                        // Mostrar imagen seleccionada nueva
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
                    } else if (viewModel.imagenUrl != null && viewModel.imagenUrl!!.isNotEmpty()) {
                        // Mostrar imagen actual del evento
                        AsyncImage(
                            model = viewModel.imagenUrl,
                            contentDescription = "Imagen actual del evento",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Botón para cambiar la imagen
                        IconButton(
                            onClick = { showImagePickerDialog = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Cambiar imagen",
                                tint = Color.White
                            )
                        }
                    } else {
                        // Mostrar opción para subir imagen
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
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
                
                // Diálogo para seleccionar imagen
                if (showImagePickerDialog) {
                    AlertDialog(
                        onDismissRequest = { showImagePickerDialog = false },
                        title = { Text("Seleccionar imagen") },
                        text = { Text("¿Cómo deseas añadir la imagen?") },
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Tipo de evento (online/presencial)
                Text(
                    text = "TIPO DE EVENTO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                        onCheckedChange = { viewModel.updateEsOnline(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE53935),
                            checkedTrackColor = Color(0xFFE53935).copy(alpha = 0.5f)
                        )
                    )
                }

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
                        text = "Edita o añade tipos de entrada para tu evento",
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

                // Botón para guardar cambios
                Button(
                    onClick = { viewModel.actualizarEvento(context) },
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
                            "GUARDAR CAMBIOS",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
} 