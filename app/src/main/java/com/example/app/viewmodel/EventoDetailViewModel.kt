package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.Evento
import com.example.app.model.TipoEntrada
import com.example.app.model.CompraRequest
import com.example.app.model.EntradaCompra
import com.example.app.model.favoritos.FavoritoRequest
import com.example.app.util.SessionManager
import com.example.app.util.isValidEventoId
import com.example.app.util.getEventoIdErrorMessage
import com.example.app.util.toValidEventoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson

class EventoDetailViewModel : ViewModel() {
    // Constante para logs
    private val TAG = "EventoDetailViewModel"
    
    // Evento
    var evento by mutableStateOf<Evento?>(null)
        private set
    
    // Tipos de entrada 
    var tiposEntrada by mutableStateOf<List<TipoEntrada>>(emptyList())
        private set
    
    // Cantidades seleccionadas por tipo de entrada - Convertido a estado observable
    private var _cantidadesSeleccionadas by mutableStateOf(mutableMapOf<Int, Int>())
    
    // UI
    var isLoading by mutableStateOf(true)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    
    // Estados para el proceso de compra
    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog
    
    private val _compraProcesando = MutableStateFlow(false)
    val compraProcesando: StateFlow<Boolean> = _compraProcesando
    
    private val _compraExitosa = MutableStateFlow(false)
    val compraExitosa: StateFlow<Boolean> = _compraExitosa
    
    private val _mensajeCompra = MutableStateFlow("")
    val mensajeCompra: StateFlow<String> = _mensajeCompra
    
    // Estado para indicar si la funcionalidad de favoritos está habilitada (solo para participantes)
    var puedeMarcarFavorito by mutableStateOf(false)
        private set
        
    private val _toggleFavoritoLoading = MutableStateFlow(false)
    val toggleFavoritoLoading: StateFlow<Boolean> = _toggleFavoritoLoading

    init {
        // Verificar si el usuario tiene rol de participante para habilitar favoritos
        checkRolUsuario()
    }
    
    private fun checkRolUsuario() {
        val userRole = SessionManager.getUserRole()?.lowercase() ?: ""
        puedeMarcarFavorito = userRole == "participante" && SessionManager.isLoggedIn()
    }
    
    fun loadEvento(eventoId: String) {
        // Restablecer estados
        isLoading = true
        isError = false
        errorMessage = null

        // Validar ID
        if (!eventoId.isValidEventoId()) {
            setError("ID de evento inválido")
            return
        }

        viewModelScope.launch {
            try {
                // Hacer la solicitud a la API
                Log.d(TAG, "Obteniendo datos del evento con ID: $eventoId")
                val response = RetrofitClient.apiService.getEventoDetalle(eventoId)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    
                    if (responseBody != null && responseBody.status == "success") {
                        // Obtener el evento detallado de la respuesta
                        val eventoDetalle = responseBody.evento
                        
                        // Convertir EventoDetalle a Evento si es necesario
                        evento = Evento(
                            id = eventoDetalle.id,
                            titulo = eventoDetalle.titulo,
                            descripcion = eventoDetalle.descripcion,
                            fechaEvento = eventoDetalle.fechaEvento,
                            hora = eventoDetalle.hora,
                            ubicacion = eventoDetalle.ubicacion,
                            categoria = eventoDetalle.categoria,
                            imagen = eventoDetalle.imagen,
                            esOnline = eventoDetalle.esOnline,
                            isFavorito = eventoDetalle.isFavorito
                        )
                        
                        // Procesar tipos de entrada si existen
                        eventoDetalle.tipos_entrada?.let { tiposEntradaLista ->
                            if (tiposEntradaLista.isNotEmpty()) {
                                this@EventoDetailViewModel.tiposEntrada = tiposEntradaLista
                                Log.d(TAG, "Tipos de entrada cargados: ${tiposEntradaLista.size}")
                                
                                // Inicializar cantidades seleccionadas
                                val nuevoMapa = mutableMapOf<Int, Int>()
                                tiposEntradaLista.forEach { tipo ->
                                    val tipoId = tipo.id ?: 0 
                                    nuevoMapa[tipoId] = 0
                                }
                                _cantidadesSeleccionadas = nuevoMapa
                            }
                        }
                        
                        isLoading = false
                    } else {
                        setError("Datos del evento no disponibles o formato incorrecto")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al obtener evento: $errorBody")
                    setError("Error al cargar el evento: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al obtener evento: ${e.message}", e)
                setError("Error de conexión: ${e.message}")
            }
        }
    }
    
    fun incrementarCantidad(tipoEntradaId: Int?) {
        if (tipoEntradaId == null) {
            Log.d(TAG, "No se puede incrementar: ID de tipo de entrada es nulo")
            return
        }

        val cantidadActual = _cantidadesSeleccionadas[tipoEntradaId] ?: 0
        val tipoEntrada = tiposEntrada.find { it.id == tipoEntradaId }

        tipoEntrada?.let { tipo ->
            // Calcular disponibilidad
            val disponibles = when {
                tipo.esIlimitado == true -> Int.MAX_VALUE
                tipo.cantidadDisponible != null -> {
                    val vendidas = tipo.entradasVendidas ?: 0
                    Math.max(0, tipo.cantidadDisponible - vendidas)
                }
                else -> 10 // Si no hay información de disponibilidad, permitir hasta 10 entradas
            }

            // Comprobar si hay entradas disponibles
            if (disponibles <= 0 && tipo.esIlimitado != true) return

            // Permitir incrementar si no se ha alcanzado el límite de disponibilidad
            if (cantidadActual < disponibles) {
                // Crear un nuevo mapa para desencadenar la recomposición
                val nuevoMapa = _cantidadesSeleccionadas.toMutableMap()
                nuevoMapa[tipoEntradaId] = cantidadActual + 1
                _cantidadesSeleccionadas = nuevoMapa
                Log.d(TAG, "Incrementada cantidad para tipo $tipoEntradaId a ${cantidadActual + 1}")
            }
        }
    }
    
    fun decrementarCantidad(tipoEntradaId: Int?) {
        if (tipoEntradaId == null) return
        
        val cantidadActual = _cantidadesSeleccionadas[tipoEntradaId] ?: 0
        if (cantidadActual > 0) {
            // Crear un nuevo mapa para desencadenar la recomposición
            val nuevoMapa = _cantidadesSeleccionadas.toMutableMap()
            nuevoMapa[tipoEntradaId] = cantidadActual - 1
            _cantidadesSeleccionadas = nuevoMapa
            Log.d(TAG, "Decrementada cantidad para tipo $tipoEntradaId a ${cantidadActual - 1}")
        }
    }
    
    fun obtenerCantidad(tipoEntradaId: Int?): Int {
        if (tipoEntradaId == null) return 0
        return _cantidadesSeleccionadas[tipoEntradaId] ?: 0
    }
    
    fun calcularTotal(): Double {
        var total = 0.0
        
        tiposEntrada.forEach { tipo ->
            val cantidad = _cantidadesSeleccionadas[tipo.id] ?: 0
            val precio = tipo.precio?.toDoubleOrNull() ?: 0.0
            
            total += precio * cantidad
        }
        
        return total
    }
    
    fun hayEntradasSeleccionadas(): Boolean {
        return _cantidadesSeleccionadas.values.any { it > 0 }
    }
    
    fun mostrarDialogoPago() {
        _mensajeCompra.value = ""
        _compraExitosa.value = false
        _compraProcesando.value = false
        _showPaymentDialog.value = true
    }
    
    fun cerrarDialogoPago() {
        _showPaymentDialog.value = false
        if (_compraExitosa.value) {
            evento?.id?.toString()?.let { loadEvento(it) }
        }
    }
    
    fun realizarCompra() {
        viewModelScope.launch {
            try {
                _compraProcesando.value = true
                
                val token = SessionManager.getToken()
                if (token.isNullOrBlank()) {
                    _mensajeCompra.value = "Debes iniciar sesión para comprar entradas"
                    _compraProcesando.value = false
                    return@launch
                }
                
                // Crear lista de entradas a comprar
                val eventoId = evento?.id ?: 0
                if (eventoId <= 0) {
                    _mensajeCompra.value = "Error al identificar el evento"
                    _compraProcesando.value = false
                    return@launch
                }
                
                val entradas = mutableListOf<EntradaCompra>()
                
                for ((tipoEntradaId, cantidad) in _cantidadesSeleccionadas) {
                    if (cantidad > 0) {
                        val tipoEntrada = tiposEntrada.find { it.id == tipoEntradaId }
                        tipoEntrada?.let {
                            val precio = it.precio?.toDoubleOrNull() ?: 0.0
                            entradas.add(EntradaCompra(tipoEntradaId, cantidad, precio))
                        }
                    }
                }
                
                if (entradas.isEmpty()) {
                    _mensajeCompra.value = "No has seleccionado ninguna entrada"
                    _compraProcesando.value = false
                    return@launch
                }
                
                // Crear la petición de compra
                val compraRequest = CompraRequest(eventoId, entradas)
                
                // Realizar compra
                val response = withContext(Dispatchers.IO) {
                    try {
                        RetrofitClient.apiService.comprarEntradas("Bearer $token", compraRequest)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (response?.isSuccessful == true) {
                    _compraExitosa.value = true
                    _mensajeCompra.value = "¡Tu compra se realizó correctamente!"
                    
                    // Limpiar cantidades
                    _cantidadesSeleccionadas.clear()
                    tiposEntrada.forEach { tipo ->
                        _cantidadesSeleccionadas[tipo.id ?: 0] = 0
                    }
                } else {
                    val responseCode = response?.code() ?: -1
                    val errorBody = response?.errorBody()?.string() ?: "Error desconocido"
                    
                    val error = when (responseCode) {
                        401 -> "Sesión expirada. Inicia sesión nuevamente."
                        403 -> "No tienes permiso para realizar esta acción."
                        404 -> "Evento o entradas no disponibles."
                        422 -> "Datos inválidos: $errorBody"
                        else -> "Error al procesar la compra (Código: $responseCode)"
                    }
                    _mensajeCompra.value = error
                }
            } catch (e: Exception) {
                _mensajeCompra.value = "Error: ${e.message}"
            } finally {
                _compraProcesando.value = false
            }
        }
    }
    
    fun toggleFavorito() {
        if (!puedeMarcarFavorito || evento == null) return

        val eventoIdNum = evento?.id ?: return

        viewModelScope.launch {
            try {
                _toggleFavoritoLoading.value = true

                val token = SessionManager.getToken()
                if (token.isNullOrBlank()) {
                    return@launch
                }

                val nuevoEstado = !(evento?.isFavorito ?: false)
                
                val response = withContext(Dispatchers.IO) {
                    try {
                        if (nuevoEstado) {
                            // Añadir a favoritos
                            val favoritoRequest = FavoritoRequest(eventoIdNum)
                            RetrofitClient.apiService.agregarFavorito("Bearer $token", favoritoRequest)
                        } else {
                            // Eliminar de favoritos
                            RetrofitClient.apiService.eliminarFavorito("Bearer $token", eventoIdNum)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al actualizar favorito: ${e.message}", e)
                        null
                    }
                }

                if (response?.isSuccessful == true) {
                    evento = evento?.copy(isFavorito = nuevoEstado)
                }
            } catch (e: Exception) {
                // Error silencioso
                Log.e(TAG, "Excepción al actualizar favorito: ${e.message}", e)
            } finally {
                _toggleFavoritoLoading.value = false
            }
        }
    }
    
    private fun setError(message: String) {
        isLoading = false
        isError = true
        errorMessage = message
        Log.e(TAG, "Error: $message")
    }
}