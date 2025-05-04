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
import com.example.app.model.TiposEntradaResponse
import com.example.app.model.CompraRequest
import com.example.app.model.EntradaCompra
import com.example.app.model.OrganizadorEvento
import com.example.app.model.UserInfo
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
    // API
    private val apiService = RetrofitClient.apiService
    
    // Constante para logs
    private val TAG = "EventoDetailVM"
    
    // Evento
    var evento by mutableStateOf<Evento?>(null)
        private set

    // Lista de tipos de entrada
    var tiposEntrada by mutableStateOf<List<TipoEntrada>>(emptyList())
        private set

    // Cantidades seleccionadas por tipo de entrada
    private var cantidadesSeleccionadas = mutableMapOf<Int, Int>()

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

    /**
     * Carga los datos del evento usando la llamada a /api/eventos/{id}/detalle para obtener todos
     * los datos necesarios en una única petición.
     */
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
                Log.d(TAG, "⬇️ Obteniendo datos del evento con ID: $eventoId a través del endpoint de detalle")
                val response = apiService.getEventoDetalle(eventoId)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    
                    if (responseBody != null && responseBody.status == "success") {
                        // Obtener el evento detallado de la respuesta
                        val eventoDetalle = responseBody.evento
                        
                        // Convertir EventoDetalle a Evento
                        val eventoConvertido = Evento(
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
                        
                        // Establecer el evento para la UI
                        evento = eventoConvertido
                        
                        // Procesar tipos de entrada si existen
                        eventoDetalle.tipos_entrada?.let { tiposEntradaLista ->
                            if (tiposEntradaLista.isNotEmpty()) {
                                this@EventoDetailViewModel.tiposEntrada = tiposEntradaLista
                                Log.d(TAG, "🎟️ Tipos de entrada cargados: ${tiposEntradaLista.size}")
                                
                                // Inicializar cantidades seleccionadas
                                cantidadesSeleccionadas.clear()
                                tiposEntradaLista.forEach { tipo ->
                                    val tipoId = tipo.id ?: tipo.idTipoEntrada ?: 0
                                    cantidadesSeleccionadas[tipoId] = 0
                                }
                            }
                        }
                        
                        isLoading = false
                    } else {
                        setError("Datos del evento no disponibles o formato incorrecto")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "❌ Error al obtener evento: $errorBody")
                    setError("Error al cargar el evento: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción al obtener evento: ${e.message}", e)
                setError("Error de conexión: ${e.message}")
            }
        }
    }

    /**
     * Incrementa la cantidad de entradas seleccionadas
     */
    fun incrementarCantidad(tipoEntradaId: Int?) {
        if (tipoEntradaId == null) return

        val cantidadActual = cantidadesSeleccionadas[tipoEntradaId] ?: 0
        val tipoEntrada = tiposEntrada.find { it.id == tipoEntradaId }

        tipoEntrada?.let { tipo ->
            val disponibles = when {
                tipo.cantidadDisponible != null && tipo.entradasVendidas != null -> 
                    tipo.cantidadDisponible - tipo.entradasVendidas
                else -> 0
            }

            if (tipo.esIlimitado == true || cantidadActual < disponibles) {
                cantidadesSeleccionadas[tipoEntradaId] = cantidadActual + 1
            }
        }
    }

    /**
     * Decrementa la cantidad de entradas seleccionadas
     */
    fun decrementarCantidad(tipoEntradaId: Int?) {
        if (tipoEntradaId == null) return

        val cantidadActual = cantidadesSeleccionadas[tipoEntradaId] ?: 0
        if (cantidadActual > 0) {
            cantidadesSeleccionadas[tipoEntradaId] = cantidadActual - 1
        }
    }

    /**
     * Obtiene la cantidad de entradas seleccionadas
     */
    fun obtenerCantidad(tipoEntradaId: Int?): Int {
        if (tipoEntradaId == null) return 0
        return cantidadesSeleccionadas[tipoEntradaId] ?: 0
    }

    /**
     * Calcula el total de la compra
     */
    fun calcularTotal(): Double {
        var total = 0.0

        tiposEntrada.forEach { tipo ->
            val cantidad = cantidadesSeleccionadas[tipo.id] ?: 0
            val precio = tipo.precio?.toDoubleOrNull() ?: 0.0

            total += precio * cantidad
        }

        return total
    }

    /**
     * Verifica si hay entradas seleccionadas
     */
    fun hayEntradasSeleccionadas(): Boolean {
        return cantidadesSeleccionadas.values.any { it > 0 }
    }

    /**
     * Muestra el diálogo de pago
     */
    fun mostrarDialogoPago() {
        _mensajeCompra.value = ""
        _compraExitosa.value = false
        _compraProcesando.value = false
        _showPaymentDialog.value = true
    }

    /**
     * Cierra el diálogo de pago
     */
    fun cerrarDialogoPago() {
        _showPaymentDialog.value = false
        if (_compraExitosa.value) {
            evento?.id?.toString()?.let { loadEvento(it) }
        }
    }

    /**
     * Realiza la compra de entradas
     */
    fun realizarCompra() {
        viewModelScope.launch {
            try {
                _compraProcesando.value = true
                _mensajeCompra.value = ""

                val token = SessionManager.getToken()
                if (token.isNullOrBlank()) {
                    _mensajeCompra.value = "Debes iniciar sesión para comprar entradas"
                    _compraProcesando.value = false
                    return@launch
                }

                // Crear lista de entradas a comprar
                val eventoId = evento?.id ?: 0
                val entradas = mutableListOf<EntradaCompra>()

                cantidadesSeleccionadas.forEach { (tipoEntradaId, cantidad) ->
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

                // Realizar compra
                val compraRequest = CompraRequest(eventoId, entradas)
                val response = withContext(Dispatchers.IO) {
                    apiService.comprarEntradas("Bearer $token", compraRequest)
                }

                if (response.isSuccessful) {
                    _compraExitosa.value = true
                    _mensajeCompra.value = "¡Tu compra se realizó correctamente!"

                    // Limpiar cantidades
                    cantidadesSeleccionadas.clear()
                    tiposEntrada.forEach { tipo ->
                        cantidadesSeleccionadas[tipo.id ?: 0] = 0
                    }
                } else {
                    val error = when (response.code()) {
                        401 -> "Sesión expirada. Inicia sesión nuevamente."
                        403 -> "No tienes permiso para realizar esta acción."
                        404 -> "Evento o entradas no disponibles."
                        else -> "Error al procesar la compra: ${response.message()}"
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

    /**
     * Cambia el estado de favorito del evento
     */
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
                            val favoritoRequest = FavoritoRequest(eventoIdNum)
                            apiService.agregarFavorito("Bearer $token", favoritoRequest)
                        } else {
                            apiService.eliminarFavorito("Bearer $token", eventoIdNum)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (response != null && response.isSuccessful) {
                    evento = evento?.copy(isFavorito = nuevoEstado)
                }
            } catch (e: Exception) {
                // Error silencioso
            } finally {
                _toggleFavoritoLoading.value = false
            }
        }
    }

    /**
     * Establece un estado de error
     */
    private fun setError(message: String) {
        isLoading = false
        isError = true
        errorMessage = message
    }
}