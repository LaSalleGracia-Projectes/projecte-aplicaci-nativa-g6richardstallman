package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.CompraRequest
import com.example.app.model.EntradaCompra
import com.example.app.model.TipoEntrada
import com.example.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComprarEntradasViewModel : ViewModel() {
    private val TAG = "ComprarEntradasVM"
    private val apiService = RetrofitClient.apiService
    
    // Estados de UI
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var eventoId by mutableStateOf<Int?>(null)
    
    // Tipos de entrada
    var tiposEntrada by mutableStateOf<List<TipoEntrada>>(emptyList())
    
    // Cantidades seleccionadas por tipo de entrada
    private var cantidadesSeleccionadas = mutableMapOf<Int, Int>()
    
    // Estados para el proceso de compra
    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog
    
    private val _compraProcesando = MutableStateFlow(false)
    val compraProcesando: StateFlow<Boolean> = _compraProcesando
    
    private val _compraExitosa = MutableStateFlow(false)
    val compraExitosa: StateFlow<Boolean> = _compraExitosa
    
    private val _mensajeCompra = MutableStateFlow("")
    val mensajeCompra: StateFlow<String> = _mensajeCompra
    
    /**
     * Carga los tipos de entrada para un evento
     */
    fun loadTiposEntrada(eventoIdStr: String) {
        Log.d(TAG, "Cargando tipos de entrada para evento: $eventoIdStr")
        val idEvento = eventoIdStr.toIntOrNull()
        if (idEvento == null) {
            error = "ID de evento inválido"
            return
        }
        
        this.eventoId = idEvento
        
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                
                val response = withContext(Dispatchers.IO) {
                    apiService.getTiposEntrada(eventoIdStr)
                }
                
                if (response.isSuccessful) {
                    val tiposEntradaResponse = response.body()
                    Log.d(TAG, "Respuesta exitosa: ${tiposEntradaResponse != null}")
                    
                    if (tiposEntradaResponse != null) {
                        tiposEntrada = tiposEntradaResponse.tiposEntrada ?: emptyList()
                        
                        // Inicializar cantidades
                        cantidadesSeleccionadas.clear()
                        tiposEntrada.forEach { tipo ->
                            cantidadesSeleccionadas[tipo.id ?: 0] = 0
                        }
                        
                        Log.d(TAG, "Tipos de entrada cargados: ${tiposEntrada.size}")
                    } else {
                        error = "No se encontraron tipos de entrada para este evento"
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al cargar tipos de entrada: $errorBody")
                    error = "Error al cargar tipos de entrada: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al cargar tipos de entrada", e)
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
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
                tipo.cantidadDisponible != null -> tipo.cantidadDisponible
                else -> 100 // Valor por defecto si no hay información
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
            // Recargar datos si la compra fue exitosa
            eventoId?.toString()?.let { loadTiposEntrada(it) }
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
                
                if (eventoId == null) {
                    _mensajeCompra.value = "ID de evento no válido"
                    _compraProcesando.value = false
                    return@launch
                }
                
                // Crear lista de entradas a comprar
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
                val compraRequest = CompraRequest(eventoId!!, entradas)
                Log.d(TAG, "Enviando petición de compra: $compraRequest")
                
                val response = withContext(Dispatchers.IO) {
                    apiService.comprarEntradas("Bearer $token", compraRequest)
                }
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Compra realizada exitosamente")
                    _compraExitosa.value = true
                    _mensajeCompra.value = "¡Tu compra se realizó correctamente!"
                    
                    // Limpiar cantidades
                    cantidadesSeleccionadas.clear()
                    tiposEntrada.forEach { tipo ->
                        cantidadesSeleccionadas[tipo.id ?: 0] = 0
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error en la compra: $errorBody")
                    
                    val error = when (response.code()) {
                        401 -> "Sesión expirada. Inicia sesión nuevamente."
                        403 -> "No tienes permiso para realizar esta acción."
                        404 -> "Evento o entradas no disponibles."
                        else -> "Error al procesar la compra: ${response.message()}"
                    }
                    _mensajeCompra.value = error
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al realizar compra", e)
                _mensajeCompra.value = "Error: ${e.message}"
            } finally {
                _compraProcesando.value = false
            }
        }
    }
} 