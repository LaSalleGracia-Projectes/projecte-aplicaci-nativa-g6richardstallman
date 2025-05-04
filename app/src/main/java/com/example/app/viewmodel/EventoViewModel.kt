package com.example.app.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.ApiService
import com.example.app.api.RetrofitClient
import com.example.app.model.Evento
import com.example.app.model.evento.EventoResponse
import com.example.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EventoViewModel : ViewModel() {
    // Estado para la lista de eventos
    var eventos by mutableStateOf<List<Evento>>(emptyList())
        private set
    
    // Estado para la lista de eventos del organizador
    var misEventos by mutableStateOf<List<Evento>>(emptyList())
        private set
    
    // Estado para indicar carga
    var isLoading by mutableStateOf(false)
        private set
    
    // Estado para errores
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    
    private val _isRegisterSuccessful = MutableStateFlow(false)
    val isRegisterSuccessful: StateFlow<Boolean> = _isRegisterSuccessful
    
    // Estado para el evento en proceso de eliminación
    var eventoEliminandoId by mutableStateOf<Int?>(null)
        private set
    
    // Estado para el resultado de la eliminación
    private val _eventoEliminadoExitosamente = MutableStateFlow<Boolean>(false)
    val eventoEliminadoExitosamente: StateFlow<Boolean> = _eventoEliminadoExitosamente
    
    // Estado para mensaje de éxito
    private val _successMessage = MutableStateFlow<String>("Evento eliminado correctamente")
    val successMessage: StateFlow<String> = _successMessage
    
    // Cargar eventos al iniciar
    init {
        fetchEventos()
    }
    
    // Función para cargar eventos
    fun fetchEventos() {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                isError = false
                
                Log.d("EVENTOS", "Cargando eventos...")
                
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAllEventos()
                }
                
                if (response.isSuccessful) {
                    val eventosResponse = response.body()
                    if (eventosResponse != null) {
                        // Filtrar eventos con fechas anteriores a hoy
                        val hoy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            LocalDate.now()
                        } else {
                            // Para versiones anteriores a Android O
                            val calendar = java.util.Calendar.getInstance()
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            sdf.format(calendar.time)
                        }
                        
                        eventos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            eventosResponse.eventos.filter { evento ->
                                try {
                                    val fechaEvento = LocalDate.parse(evento.fechaEvento, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    val fechaHoy = LocalDate.now()
                                    fechaEvento.isEqual(fechaHoy) || fechaEvento.isAfter(fechaHoy)
                                } catch (e: Exception) {
                                    Log.e("EVENTOS", "Error al parsear fecha: ${evento.fechaEvento}", e)
                                    true // Si hay error al parsear, incluirlo por defecto
                                }
                            }
                        } else {
                            // Para versiones anteriores a Android O
                            eventosResponse.eventos.filter { evento ->
                                try {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    val fechaEvento = sdf.parse(evento.fechaEvento)
                                    val fechaHoy = sdf.parse(hoy.toString())
                                    fechaEvento != null && fechaHoy != null && !fechaEvento.before(fechaHoy)
                                } catch (e: Exception) {
                                    Log.e("EVENTOS", "Error al parsear fecha: ${evento.fechaEvento}", e)
                                    true // Si hay error al parsear, incluirlo por defecto
                                }
                            }
                        }
                        
                        Log.d("EVENTOS", "Eventos cargados: ${eventos.size}")
                        
                        // Si no hay eventos, actualizar mensaje de error
                        if (eventos.isEmpty()) {
                            Log.d("EVENTOS", "No hay eventos disponibles")
                            setError("No hay eventos registrados")
                        }
                    } else {
                        setError("No se pudieron cargar los eventos: Respuesta vacía")
                    }
                } else {
                    handleErrorResponse(response)
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                isLoading = false
            }
        }
    }
    
    fun fetchMisEventos() {
        viewModelScope.launch {
            try {
                // Obtener token y rol de usuario desde SessionManager
                val token = SessionManager.getToken()
                val userRole = SessionManager.getUserRole()
                
                Log.d("MIS_EVENTOS", "Iniciando fetchMisEventos - Token: ${token?.take(10)}... Role: $userRole")
                
                // Verificar si el usuario es organizador y tiene token válido
                if (token.isNullOrEmpty()) {
                    Log.e("MIS_EVENTOS", "Error: Token no disponible")
                    setError("No hay sesión activa. Por favor, inicia sesión nuevamente.")
                    return@launch
                }
                
                if (userRole != "organizador") {
                    Log.e("MIS_EVENTOS", "Error: Usuario no es organizador ($userRole)")
                    setError("Esta función solo está disponible para organizadores")
                    return@launch
                }
                
                // Iniciar carga
                isLoading = true
                errorMessage = null
                isError = false
                
                // Ejecutar llamada a la API
                try {
                    Log.d("MIS_EVENTOS", "Realizando petición a /api/mis-eventos con token")
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.getMisEventosFromApi("Bearer $token")
                    }
                    
                    Log.d("MIS_EVENTOS", "Respuesta recibida - Status: ${response.code()}, Success: ${response.isSuccessful}")
                    
                    if (response.isSuccessful) {
                        val eventosResponse = response.body()
                        if (eventosResponse != null) {
                            // Actualizar la lista de eventos del organizador
                            misEventos = eventosResponse.eventos
                            Log.d("MIS_EVENTOS", "Eventos cargados: ${misEventos.size}")
                            
                            // Log de IDs de eventos para depuración
                            misEventos.forEachIndexed { index, evento ->
                                Log.d("MIS_EVENTOS", "Evento[$index] - ID: ${evento.id}, IDEvento: ${evento.id}, getEventoId(): ${evento.getEventoId()}, Título: ${evento.titulo}")
                            }
                            
                            if (misEventos.isEmpty()) {
                                Log.d("MIS_EVENTOS", "No hay eventos creados por este organizador")
                            }
                        } else {
                            Log.e("MIS_EVENTOS", "Respuesta del servidor vacía")
                            setError("No se pudieron cargar tus eventos")
                        }
                    } else {
                        // Manejar errores específicos por código de respuesta
                        when (response.code()) {
                            401 -> {
                                Log.e("MIS_EVENTOS", "Error 401: No autorizado")
                                setError("Tu sesión ha expirado. Por favor, inicia sesión nuevamente.")
                                SessionManager.clearSession() // Limpiar sesión por expiración
                            }
                            403 -> {
                                Log.e("MIS_EVENTOS", "Error 403: Acceso prohibido")
                                setError("No tienes permisos para acceder a esta función")
                            }
                            else -> {
                                // Extraer mensaje de error del cuerpo de la respuesta
                                val errorBody = response.errorBody()?.string()
                                Log.e("MIS_EVENTOS", "Error ${response.code()}: $errorBody")
                                
                                try {
                                    val errorResponse = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                                    val errorMessage = errorResponse?.get("message") as? String 
                                        ?: errorResponse?.get("error") as? String
                                        ?: "Error desconocido (${response.code()})"
                                    setError(errorMessage)
                                } catch (e: Exception) {
                                    setError("Error al cargar tus eventos (${response.code()})")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MIS_EVENTOS", "Excepción en la petición HTTP", e)
                    setError("Error de conexión: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("MIS_EVENTOS", "Excepción general", e)
                setError("Error inesperado: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    fun toggleFavorito(eventoId: Int) {
        viewModelScope.launch {
            try {
                Log.d("FAVORITOS", "Toggle favorito para evento $eventoId")
                
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.toggleFavorito(eventoId)
                }
                
                // Verificar el código de respuesta
                when (response.code()) {
                    200 -> {
                        // Actualizar el estado del evento en la lista local
                        eventos = eventos.map { evento ->
                            if (evento.getEventoId() == eventoId) {
                                evento.copy(isFavorito = !evento.isFavorito)
                            } else {
                                evento
                            }
                        }
                        Log.d("FAVORITOS", "Favorito actualizado correctamente")
                    }
                    else -> handleErrorResponse(response)
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }
    
    private fun handleErrorResponse(response: retrofit2.Response<*>) {
        val errorBody = response.errorBody()?.string()
        Log.e("EVENTOS", "Error: ${response.code()} - $errorBody")
        
        try {
            val errorResponse = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
            val message = errorResponse?.get("message") as? String ?: errorResponse?.get("error") as? String
            setError(message ?: "Error en la operación (${response.code()})")
        } catch (e: Exception) {
            setError("Error en la operación: ${response.message()} (${response.code()})")
        }
    }
    
    private fun handleException(e: Exception) {
        Log.e("EVENTOS", "Excepción: ${e.message}", e)
        setError("Error de conexión: ${e.message}")
    }
    
    // Función para establecer error
    private fun setError(error: String?) {
        isError = error != null
        errorMessage = error
        Log.e("EVENTOS", "ERROR: $error")
    }
    
    // Función para parsear la fecha
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            // Si hay un error al parsear, devolvemos una fecha muy lejana para que aparezca al final
            LocalDate.of(9999, 12, 31)
        }
    }
    
    // Función para parsear la hora
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseTime(timeString: String): LocalTime {
        return try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"))
        } catch (e: Exception) {
            // Si hay un error al parsear, devolvemos una hora muy tardía para que aparezca al final
            LocalTime.of(23, 59, 59)
        }
    }

    fun clearError() {
        isError = false
        errorMessage = null
    }

    // Función para eliminar un evento
    fun deleteEvento(evento: Evento) {
        viewModelScope.launch {
            try {
                // Obtener token desde SessionManager
                val token = SessionManager.getToken()
                
                Log.d("DELETE_EVENTO", "Iniciando eliminación del evento ID: ${evento.id}")
                
                // Verificar si hay token válido
                if (token.isNullOrEmpty()) {
                    Log.e("DELETE_EVENTO", "Error: Token no disponible")
                    setError("No hay sesión activa. Por favor, inicia sesión nuevamente.")
                    return@launch
                }
                
                // Iniciar proceso de eliminación
                isLoading = true
                errorMessage = null
                isError = false
                eventoEliminandoId = evento.getEventoId()
                
                // Limpiar cualquier respuesta exitosa anterior
                _eventoEliminadoExitosamente.value = false
                
                // Ejecutar llamada a la API
                try {
                    // Obtenemos el ID correcto del evento
                    val eventoId = evento.getEventoId()
                    
                    Log.d("DELETE_EVENTO", """
                        Realizando petición DELETE:
                        - Ruta: /api/eventos/$eventoId
                        - ID original: ${evento.id}
                        - IDEvento: ${evento.id}
                        - getEventoId(): $eventoId
                        - Título: ${evento.titulo}
                    """.trimIndent())
                    
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.deleteEvento(
                            id = eventoId.toString(),
                            token = "Bearer $token"
                        )
                    }
                    
                    Log.d("DELETE_EVENTO", "Respuesta recibida - Status: ${response.code()}, Success: ${response.isSuccessful}")
                    
                    if (response.isSuccessful) {
                        val deleteResponse = response.body()
                        Log.d("DELETE_EVENTO", "Cuerpo de respuesta: ${deleteResponse}")
                        
                        // Eliminar el evento de la lista de misEventos independientemente de la respuesta
                        misEventos = misEventos.filter { it.getEventoId() != evento.getEventoId() }
                        
                        // Si la eliminación fue exitosa, emitir evento de éxito
                        isError = false
                        errorMessage = null
                        
                        // Extraer los campos del backend del GenericResponse
                        val mensajeExito = deleteResponse?.message ?: "Evento eliminado correctamente"
                        val codRespuesta = deleteResponse?.code ?: "DELETED_SUCCESS"
                        val statusRespuesta = deleteResponse?.status ?: "success"
                        
                        Log.d("DELETE_EVENTO", """
                            Respuesta exitosa:
                            - Mensaje: $mensajeExito
                            - Código: $codRespuesta
                            - Estado: $statusRespuesta
                        """.trimIndent())
                        
                        // Establecer mensaje de éxito con detalles completos
                        _successMessage.value = mensajeExito
                        _eventoEliminadoExitosamente.value = true
                        
                        if (codRespuesta == "DELETED_SUCCESS" && statusRespuesta == "success") {
                            Log.d("DELETE_EVENTO", "¡Eliminación verificada! Código y estado coinciden con lo esperado")
                        } else {
                            Log.w("DELETE_EVENTO", "Respuesta con formato inesperado, pero operación exitosa")
                        }
                        
                        // Actualizar la lista completa de eventos después de una eliminación exitosa
                        Log.d("DELETE_EVENTO", "Actualizando lista de eventos tras eliminación exitosa")
                        fetchMisEventos()
                    } else {
                        // Manejar errores específicos por código de respuesta
                        when (response.code()) {
                            401 -> {
                                Log.e("DELETE_EVENTO", "Error 401: No autorizado")
                                setError("Tu sesión ha expirado. Por favor, inicia sesión nuevamente.")
                                SessionManager.clearSession() // Limpiar sesión por expiración
                            }
                            403 -> {
                                Log.e("DELETE_EVENTO", "Error 403: Acceso prohibido")
                                setError("No tienes permisos para eliminar este evento")
                            }
                            404 -> {
                                // El evento ya no existe, pero eso es lo que queríamos
                                Log.d("DELETE_EVENTO", "Error 404: Evento no encontrado - Tratando como eliminación exitosa")
                                
                                // Eliminar el evento de la lista local al ser un 404
                                misEventos = misEventos.filter { it.getEventoId() != evento.getEventoId() }
                                
                                // Tratar como éxito en lugar de error, con mensaje específico
                                isError = false
                                errorMessage = null
                                _successMessage.value = "El evento ya ha sido eliminado previamente"
                                _eventoEliminadoExitosamente.value = true
                                
                                // Actualizar la lista completa de eventos después de detectar que ya se eliminó
                                Log.d("DELETE_EVENTO", "Actualizando lista de eventos tras detectar que ya fue eliminado (404)")
                                fetchMisEventos()
                            }
                            else -> {
                                // Extraer mensaje de error del cuerpo de la respuesta
                                val errorBody = response.errorBody()?.string()
                                Log.e("DELETE_EVENTO", """
                                    Error ${response.code()}:
                                    - Cuerpo del error: $errorBody
                                    - Headers: ${response.headers()}
                                    - URL: ${response.raw().request.url}
                                    - Método: ${response.raw().request.method}
                                """.trimIndent())
                                
                                try {
                                    val errorResponse = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                                    val errorMessage = errorResponse?.get("message") as? String 
                                        ?: errorResponse?.get("error") as? String
                                        ?: "Error desconocido (${response.code()})"
                                    setError(errorMessage)
                                } catch (e: Exception) {
                                    setError("Error al eliminar el evento (${response.code()})")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DELETE_EVENTO", "Excepción en la petición HTTP", e)
                    setError("Error de conexión: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("DELETE_EVENTO", "Excepción general", e)
                setError("Error inesperado: ${e.message}")
            } finally {
                isLoading = false
                eventoEliminandoId = null
            }
        }
    }
    
    // Resetear el estado de eliminación
    fun resetEventoEliminado() {
        _eventoEliminadoExitosamente.value = false
    }
}