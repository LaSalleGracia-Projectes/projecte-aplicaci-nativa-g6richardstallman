package com.example.app.viewmodel

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.tickets.TicketCompra
import com.example.app.util.SessionManager
import com.example.app.util.GoogleCalendarHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TicketsViewModel(
    private val application: android.app.Application
) : ViewModel() {
    private val TAG = "TicketsViewModel"
    
    // Estados para la descarga de PDF
    private val _isDownloadingPdf = MutableStateFlow(false)
    val isDownloadingPdf: StateFlow<Boolean> = _isDownloadingPdf.asStateFlow()
    
    private val _downloadMessage = MutableStateFlow<String?>(null)
    val downloadMessage: StateFlow<String?> = _downloadMessage.asStateFlow()
    
    // Estado para los tickets
    private val _tickets = MutableStateFlow<List<TicketCompra>>(emptyList())
    val tickets: StateFlow<List<TicketCompra>> = _tickets.asStateFlow()
    
    // Estado de carga
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    var calendarHelper: GoogleCalendarHelper? = null
    var googleAccount: GoogleSignInAccount? = null
    
    // Cargar tickets al iniciar
    init {
        loadTickets()
    }
    
    // Función para añadir evento al calendario - método de emergencia
    fun addEventToCalendarEmergency(ticket: TicketCompra) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Intentando añadir evento al calendario usando método de emergencia")
                
                // Construir los datos del evento
                val beginTime = System.currentTimeMillis()
                val endTime = beginTime + 2 * 60 * 60 * 1000 // 2 horas después
                
                // Usar ContentResolver directamente
                val contentResolver = application.contentResolver
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, 1) // Usar el calendario principal
                    put(CalendarContract.Events.TITLE, ticket.evento.nombre)
                    put(CalendarContract.Events.DESCRIPTION, "Entrada para ${ticket.evento.nombre}")
                    put(CalendarContract.Events.EVENT_LOCATION, "Ubicación del evento")
                    put(CalendarContract.Events.DTSTART, beginTime)
                    put(CalendarContract.Events.DTEND, endTime)
                    put(CalendarContract.Events.ALL_DAY, 0)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                
                withContext(Dispatchers.IO) {
                    try {
                        val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                        if (uri != null) {
                            Log.d(TAG, "Evento añadido correctamente: $uri")
                            withContext(Dispatchers.Main) {
                                setSuccessMessage("Evento añadido al calendario")
                            }
                        } else {
                            Log.e(TAG, "Error: URI nula al insertar evento")
                            withContext(Dispatchers.Main) {
                                setError("Error al añadir evento al calendario")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al insertar evento: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            setError("Error al añadir evento: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error general en método de emergencia: ${e.message}", e)
                setError("Error al añadir evento: ${e.message}")
            }
        }
    }
    
    // Función para añadir evento al calendario - método principal
    fun addEventToCalendar(ticket: TicketCompra) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando proceso de añadir evento al calendario")
                
                // Verificar si tenemos el helper inicializado
                if (calendarHelper == null) {
                    Log.e(TAG, "Error: calendarHelper es null, inicializando...")
                    calendarHelper = GoogleCalendarHelper(application.applicationContext)
                }
                
                // Intentamos con el método directo de Intents
                Log.d(TAG, "Intentando añadir evento con método de Intents")
                val success = calendarHelper?.addEventToCalendarUsingIntent(ticket) ?: false
                
                if (success) {
                    Log.d(TAG, "Evento añadido exitosamente con Intent")
                    setSuccessMessage("Evento añadido a tu calendario")
                } else {
                    // Si falla el método de Intent, intentar con el método de emergencia
                    Log.e(TAG, "Intent falló, probando método de emergencia")
                    addEventToCalendarEmergency(ticket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error global al añadir evento: ${e.message}", e)
                
                // Si falla cualquier cosa, intentar con el método de emergencia
                try {
                    Log.d(TAG, "Intentando método de emergencia después de excepción")
                    addEventToCalendarEmergency(ticket)
                } catch (e2: Exception) {
                    Log.e(TAG, "También falló el método de emergencia: ${e2.message}", e2)
                    setError("Error al añadir evento al calendario: ${e.message}")
                }
            }
        }
    }
    
    // Método original que usa la API de Google Calendar
    private suspend fun addEventToCalendarUsingAPI(ticket: TicketCompra) {
        // Todas las operaciones de Google Calendar deben ejecutarse en un hilo secundario
        withContext(Dispatchers.IO) {
            try {
                // Verificar si tenemos una cuenta Google
                if (googleAccount == null) {
                    // Intentar obtener la cuenta Google del dispositivo
                    googleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(
                        com.example.app.MyApplication.appContext
                    )
                }
                
                val account = googleAccount
                if (account == null) {
                    withContext(Dispatchers.Main) {
                        setError("No hay una cuenta de Google conectada. Por favor, inicie sesión con Google primero.")
                    }
                    return@withContext
                }

                Log.d("TicketsViewModel", "Añadiendo evento al calendario con cuenta: ${account.email}")
                
                // Verificar que la cuenta tenga los permisos necesarios
                try {
                    val calendarService = calendarHelper?.getCalendarService(account)
                    if (calendarService == null) {
                        withContext(Dispatchers.Main) {
                            setError("Error al inicializar el servicio de calendario")
                        }
                        return@withContext
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    
                    val startDate = dateFormat.parse(ticket.evento.fecha) ?: Date()
                    val startTime = timeFormat.parse(ticket.evento.hora) ?: Date()
                    
                    // Combinar fecha y hora
                    val calendar = Calendar.getInstance()
                    calendar.time = startDate
                    val timeCalendar = Calendar.getInstance()
                    timeCalendar.time = startTime
                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                    calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
                    
                    // Calcular la hora de fin (2 horas después)
                    val endCalendar = Calendar.getInstance()
                    endCalendar.timeInMillis = calendar.timeInMillis
                    endCalendar.add(Calendar.HOUR_OF_DAY, 2)
                    
                    Log.d("TicketsViewModel", "Creando evento: ${ticket.evento.nombre} - Fecha: ${dateFormat.format(calendar.time)} ${timeFormat.format(calendar.time)}")

                    val event = Event()
                        .setSummary(ticket.evento.nombre)
                        .setDescription("Entrada para ${ticket.evento.nombre}")
                        .setLocation("Ubicación del evento")
                        .setStart(EventDateTime().setDateTime(com.google.api.client.util.DateTime(calendar.time)))
                        .setEnd(EventDateTime().setDateTime(com.google.api.client.util.DateTime(endCalendar.time)))
                    
                    // Agregar recordatorios
                    val reminderOverrides = listOf(
                        com.google.api.services.calendar.model.EventReminder().setMethod("email").setMinutes(24 * 60), // 1 día antes
                        com.google.api.services.calendar.model.EventReminder().setMethod("popup").setMinutes(60) // 1 hora antes
                    )
                    val reminders = com.google.api.services.calendar.model.Event.Reminders()
                        .setUseDefault(false)
                        .setOverrides(reminderOverrides)
                    event.reminders = reminders

                    // Ejecutar en un contexto de IO para evitar bloquear el hilo principal
                    val createdEvent = calendarService.events()
                        .insert("primary", event)
                        .execute()

                    Log.d("TicketsViewModel", "Evento creado: ${createdEvent.htmlLink}")
                    withContext(Dispatchers.Main) {
                        setSuccessMessage("Evento '${ticket.evento.nombre}' añadido a tu Google Calendar")
                    }
                } catch (e: Exception) {
                    Log.e("TicketsViewModel", "Error específico en la API de Calendar: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        if (e.message?.contains("DEAD_OBJECT") == true || e.message?.contains("DeadObject") == true) {
                            setError("Error de conexión con Google Calendar. Intente nuevamente iniciando sesión con Google.")
                        } else if (e.message?.contains("PERMISSION_DENIED") == true) {
                            setError("Permiso denegado. Verifica que hayas iniciado sesión con tu cuenta Google.")
                        } else if (e.message?.contains("NetworkError") == true || e.message?.contains("Failed to connect") == true) {
                            setError("Error de red. Verifica tu conexión a internet.")
                        } else if (e.message?.contains("deadlock") == true || e.message?.contains("Calling this from your main thread") == true) {
                            setError("Error interno. Por favor, intenta de nuevo más tarde.")
                        } else {
                            setError("Error al añadir evento al calendario: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TicketsViewModel", "Error general: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    setError("Error al añadir evento al calendario: ${e.message}")
                }
            }
        }
    }
    
    // Agregar un mensaje de éxito
    private var _successMessage by mutableStateOf<String?>(null)
    val successMessage get() = _successMessage
    
    private fun setSuccessMessage(message: String) {
        _successMessage = message
        // Eliminamos el mensaje después de 3 segundos
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _successMessage = null
        }
    }
    
    // Función para cargar tickets
    fun loadTickets() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _error.value = "No hay sesión activa"
                    return@launch
                }
                
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getMisTickets("Bearer $token")
                }
                
                if (response.isSuccessful) {
                    val ticketsResponse = response.body()
                    if (ticketsResponse?.status == "success") {
                        _tickets.value = ticketsResponse.compras
                    } else {
                        _error.value = ticketsResponse?.message ?: "Error al cargar los tickets"
                    }
                } else {
                    _error.value = "Error al cargar los tickets: ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar tickets", e)
                _error.value = "Error al cargar los tickets: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Función para establecer un mensaje de error
    fun setError(error: String) {
        _error.value = error
        _isLoading.value = false
        Log.e(TAG, "Error: $error")
    }
    
    // Función para limpiar el mensaje de error
    private fun clearError() {
        _error.value = null
    }

    fun downloadEntrada(ticketId: Int) {
        viewModelScope.launch {
            try {
                _isDownloadingPdf.value = true
                _downloadMessage.value = "Iniciando descarga..."
                
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    _downloadMessage.value = "No hay sesión activa"
                    return@launch
                }
                
                _downloadMessage.value = "Conectando con el servidor..."
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.downloadEntrada("Bearer $token", ticketId)
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        _downloadMessage.value = "Descargando entrada..."
                        
                        // Verificar el tipo de contenido
                        val contentType = response.headers()["Content-Type"]
                        if (contentType?.contains("application/pdf") != true) {
                            _downloadMessage.value = "Error: El servidor no devolvió un PDF válido"
                            return@launch
                        }
                        
                        // Guardar el PDF en el almacenamiento
                        val fileName = "entrada_$ticketId.pdf"
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        )
                        downloadsDir.mkdirs()
                        
                        val file = java.io.File(downloadsDir, fileName)
                        
                        try {
                            withContext(Dispatchers.IO) {
                                file.outputStream().use { outputStream ->
                                    responseBody.byteStream().use { inputStream ->
                                        val buffer = ByteArray(4096)
                                        var bytesRead: Int
                                        var totalBytesRead: Long = 0
                                        val contentLength = responseBody.contentLength()
                                        
                                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                            outputStream.write(buffer, 0, bytesRead)
                                            totalBytesRead += bytesRead
                                            
                                            // Actualizar progreso
                                            if (contentLength > 0) {
                                                val progress = (totalBytesRead * 100 / contentLength).toInt()
                                                _downloadMessage.value = "Descargando... $progress%"
                                            }
                                        }
                                    }
                                }
                                
                                // Verificar que el archivo se creó correctamente
                                if (!file.exists() || file.length() == 0L) {
                                    throw Exception("Error al guardar el archivo")
                                }
                            }
                            
                            // Notificar al sistema del nuevo archivo
                            val contentValues = android.content.ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                            }
                            
                            val contentUri = MediaStore.Files.getContentUri("external")
                            application.contentResolver.insert(contentUri, contentValues)
                            
                            // Abrir el PDF automáticamente
                            try {
                                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                    application,
                                    "${application.packageName}.provider",
                                    file
                                )
                                
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(fileUri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                
                                // Verificar si hay alguna aplicación que pueda abrir PDFs
                                val packageManager = application.packageManager
                                if (intent.resolveActivity(packageManager) != null) {
                                    application.startActivity(intent)
                                } else {
                                    _downloadMessage.value = "¡Entrada descargada! Instala un lector de PDF para verla"
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al abrir el PDF", e)
                                _downloadMessage.value = "¡Entrada descargada! No se pudo abrir automáticamente"
                            }
                            
                            _downloadMessage.value = "¡Entrada descargada correctamente!"
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al guardar el archivo", e)
                            _downloadMessage.value = "Error al guardar la entrada: ${e.message}"
                            // Intentar eliminar el archivo si se creó
                            file.delete()
                        }
                    } else {
                        _downloadMessage.value = "Error: No se recibieron datos del servidor"
                    }
                } else {
                    when (response.code()) {
                        401 -> _downloadMessage.value = "Sesión expirada. Por favor, vuelve a iniciar sesión"
                        403 -> _downloadMessage.value = "No tienes permiso para descargar esta entrada"
                        404 -> _downloadMessage.value = "Entrada no encontrada"
                        else -> _downloadMessage.value = "Error del servidor: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al descargar entrada", e)
                _downloadMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isDownloadingPdf.value = false
            }
        }
    }
} 