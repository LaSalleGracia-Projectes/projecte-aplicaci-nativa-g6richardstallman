package com.example.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.Evento
import com.example.app.model.Organizador
import com.example.app.api.OrganizadorDetalle
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import com.example.app.model.favoritos.OrganizadorFavoritoRequest
import com.example.app.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.example.app.model.OrganizadorUserInfo

class OrganizadorDetailViewModel : ViewModel() {
    // Constante para logs
    companion object {
        private const val TAG = "OrganizadorDetailVM"
    }
    
    var eventos by mutableStateOf<List<Evento>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isError by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var avatarUrl by mutableStateOf<String?>(null)
        private set
    var organizadorDetalle by mutableStateOf<OrganizadorDetalle?>(null)
        private set
    var isFavorito by mutableStateOf(false)
        private set
    var puedeMarcarFavorito by mutableStateOf(false)
        private set
    
    private val _toggleFavoritoLoading = MutableStateFlow(false)
    val toggleFavoritoLoading: StateFlow<Boolean> = _toggleFavoritoLoading
    
    // Flow para mensajes de error
    private val _errorMessage = MutableSharedFlow<String>()

    fun loadEventos(organizadorId: Int) {
        isLoading = true
        isError = false
        viewModelScope.launch {
            try {
                // Verificar si el usuario puede marcar favorito
                puedeMarcarFavorito = SessionManager.isLoggedIn() && SessionManager.getUserRole() == "participante"
                
                // Cargar detalles del organizador (incluyendo verificación de favorito)
                loadOrganizadorDetalle(organizadorId)
                
                // Cargar eventos
                val response = RetrofitClient.apiService.getAllEventos()
                if (response.isSuccessful) {
                    val eventosResponse = response.body()?.eventos
                    eventos = eventosResponse?.filter { it.organizador?.id == organizadorId } ?: emptyList()
                    Log.d("OrganizadorVM", "Eventos cargados: ${eventos.size}")
                } else {
                    Log.e("OrganizadorVM", "Error al cargar eventos: ${response.code()}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                isError = true
                Log.e("OrganizadorVM", "Error al cargar eventos", e)
                isLoading = false
            }
        }
    }
    
    fun checkFavorito(organizadorId: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.d(TAG, "⚠️ No hay token disponible para verificar favorito")
                    return@launch
                }

                Log.d(TAG, "🔍 Verificando si organizador $organizadorId es favorito")
                _toggleFavoritoLoading.value = true

                withContext(Dispatchers.IO) {
                    try {
                        // Primer método: Verificar directamente con la API específica
                        val response = RetrofitClient.apiService.checkOrganizadorFavorito(
                            token = "Bearer $token",
                            idOrganizador = organizadorId
                        )

                        if (response.isSuccessful) {
                            val responseData = response.body()
                            Log.d(TAG, "�� Respuesta recibida: $responseData")
                            
                            // Analizar la respuesta JSON directamente para verificar
                            val responseJson = response.body()?.toString() ?: ""
                            Log.d(TAG, "📌 Respuesta JSON completa: $responseJson")
                            
                            val isFavorito = responseData?.isFavorito ?: false
                            Log.d(TAG, "📊 Estado favorito según API: $isFavorito")
                            
                            withContext(Dispatchers.Main) {
                                Log.d(TAG, "⚠️ Actualizando estado favorito de ${this@OrganizadorDetailViewModel.isFavorito} a $isFavorito")
                                this@OrganizadorDetailViewModel.isFavorito = isFavorito
                            }
                        } else {
                            Log.e(TAG, "❌ Error al verificar favorito: ${response.code()} - ${response.errorBody()?.string()}")
                            
                            // Método alternativo: Verificar en la lista completa de favoritos
                            Log.d(TAG, "🔄 Intentando método alternativo: verificar en lista completa")
                            try {
                                val favoritosResponse = RetrofitClient.apiService.getOrganizadoresFavoritos("Bearer $token")
                                if (favoritosResponse.isSuccessful) {
                                    val listaFavoritos = favoritosResponse.body()?.organizadores ?: emptyList()
                                    val encontrado = listaFavoritos.any { it.id == organizadorId }
                                    Log.d(TAG, "📊 Organizador encontrado en lista favoritos: $encontrado")
                                    
                                    withContext(Dispatchers.Main) {
                                        Log.d(TAG, "⚠️ Actualizando estado favorito (método alternativo) de ${this@OrganizadorDetailViewModel.isFavorito} a $encontrado")
                                        this@OrganizadorDetailViewModel.isFavorito = encontrado
                                    }
                                } else {
                                    Log.e(TAG, "❌ Error al obtener lista favoritos: ${favoritosResponse.code()}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error en método alternativo", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Excepción al verificar favorito", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error general en checkFavorito", e)
            } finally {
                _toggleFavoritoLoading.value = false
            }
        }
    }
    
    fun toggleFavorito(organizadorId: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.d(TAG, "⚠️ No hay token disponible para toggle favorito")
                    _errorMessage.emit("Debes iniciar sesión para gestionar favoritos")
                    return@launch
                }

                Log.d(TAG, "🔄 Iniciando toggle favorito para organizador $organizadorId")
                _toggleFavoritoLoading.value = true
                
                // Guardar estado actual para revertir en caso de error
                val estadoActual = isFavorito
                Log.d(TAG, "📊 Estado actual de favorito: $estadoActual")
                
                // 1. Verificar estado actual en el servidor
                Log.d(TAG, "🔍 Verificando estado actual en servidor")
                val checkResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.checkOrganizadorFavorito(
                        token = "Bearer $token",
                        idOrganizador = organizadorId
                    )
                }
                
                if (!checkResponse.isSuccessful) {
                    Log.e(TAG, "❌ Error al verificar estado favorito: ${checkResponse.code()} - ${checkResponse.errorBody()?.string()}")
                    _errorMessage.emit("Error al verificar estado de favorito")
                    _toggleFavoritoLoading.value = false
                    return@launch
                }
                
                val esFavoritoEnServidor = checkResponse.body()?.isFavorito ?: false
                Log.d(TAG, "📊 Estado en servidor: esFavorito=$esFavoritoEnServidor")
                
                // Actualizar estado local según servidor (por si hay discrepancia)
                if (estadoActual != esFavoritoEnServidor) {
                    Log.d(TAG, "⚠️ Discrepancia detectada: local=$estadoActual, servidor=$esFavoritoEnServidor")
                    isFavorito = esFavoritoEnServidor
                }
                
                // 2. Realizar acción basada en estado del servidor
                // Actualización optimista para UX mejorada
                isFavorito = !esFavoritoEnServidor
                
                val response = if (esFavoritoEnServidor) {
                    // Ya es favorito, hay que quitarlo
                    Log.d(TAG, "🤍 Quitando organizador de favoritos")
                    withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.removeOrganizadorFavorito(
                            token = "Bearer $token",
                            idOrganizador = organizadorId
                        )
                    }
                } else {
                    // No es favorito, hay que añadirlo
                    Log.d(TAG, "❤️ Añadiendo organizador a favoritos")
                    withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.addOrganizadorFavorito(
                            token = "Bearer $token",
                            request = OrganizadorFavoritoRequest(organizadorId)
                        )
                    }
                }
                
                // 3. Verificar resultado y notificar
                if (response.isSuccessful) {
                    val mensaje = response.body()?.message ?: "Operación completada"
                    Log.d(TAG, "✅ Operación favorito exitosa: $mensaje")
                    
                    // Verificación final para confirmar estado
                    checkFavorito(organizadorId)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "❌ Error en operación favorito: ${response.code()} - $errorMsg")
                    
                    // Revertir el cambio optimista en caso de error
                    Log.d(TAG, "🔄 Revirtiendo a estado anterior: $estadoActual")
                    isFavorito = estadoActual
                    _errorMessage.emit("Error al actualizar favorito: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción en toggleFavorito", e)
                _errorMessage.emit("Error: ${e.message}")
            } finally {
                _toggleFavoritoLoading.value = false
            }
        }
    }
    
    // Función para convertir el OrganizadorDetalle a Organizador si es necesario
    fun getOrganizador(): Organizador? {
        return organizadorDetalle?.let {
            val org = Organizador(
                id = it.id,
                nombre = it.nombre,
                telefonoContacto = it.telefonoContacto,
                direccionFiscal = it.direccionFiscal,
                cif = it.cif,
                nombreUsuario = it.nombreUsuario,
                user = it.user,
                avatarUrl = it.avatarUrl,
                isFavorite = isFavorito
            )
            
            // Log para verificar que el estado de favorito se está asignando correctamente
            Log.d("OrganizadorVM", "🔵 Organizador ID=${org.id} creado con isFavorite=${org.isFavorite}")
            
            org
        }
    }

    private fun loadOrganizadorDetalle(id: Int) {
        viewModelScope.launch {
            try {
                // Determinar si necesitamos enviar un token
                val tokenHeader = if (SessionManager.isLoggedIn()) {
                    "Bearer ${SessionManager.getToken()}"
                } else {
                    null
                }
                
                val response = RetrofitClient.apiService.getOrganizadorById(id.toString(), tokenHeader)
                if (response.isSuccessful) {
                    organizadorDetalle = response.body()?.organizador
                    avatarUrl = organizadorDetalle?.avatarUrl
                    Log.d("OrganizadorVM", "Organizador cargado: ${organizadorDetalle?.nombre}")
                    
                    // Intentamos primero tomar el valor is_favorite directamente de la respuesta
                    // Usar propiedad correcta del OrganizadorDetalle
                    organizadorDetalle?.let {
                        if (isFavorito != it.isFavorite) {
                            Log.d("OrganizadorVM", "Actualizando estado favorito desde detalle: $isFavorito a ${it.isFavorite}")
                            isFavorito = it.isFavorite
                        }
                    }
                    
                    // Verificar si es favorito directamente desde la API específica
                    if (puedeMarcarFavorito) {
                        checkFavorito(id)
                    }
                } else {
                    errorMessage = "Error al cargar organizador: ${response.code()}"
                    isError = true
                    Log.e("OrganizadorVM", errorMessage ?: "")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                isError = true
                Log.e("OrganizadorVM", "Error al cargar organizador", e)
            } finally {
                isLoading = false
            }
        }
    }
} 