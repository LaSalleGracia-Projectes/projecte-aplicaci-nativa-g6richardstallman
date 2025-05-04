package com.example.app.viewmodel.favoritos

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.ErrorResponse
import com.example.app.model.Evento
import com.example.app.model.Organizador
import com.example.app.model.favoritos.FavoritoRequest
import com.example.app.model.favoritos.OrganizadorFavoritoRequest
import com.example.app.util.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class FavoritosViewModel : ViewModel() {
    // Constante para logs
    companion object {
        private const val TAG = "FavoritosViewModel"
    }
    
    // Lista de favoritos
    var favoritos by mutableStateOf<List<Evento>>(emptyList())
        private set
    
    // Lista de organizadores favoritos
    var organizadoresFavoritos by mutableStateOf<List<Organizador>>(emptyList())
        private set
    
    // UI
    var isLoading by mutableStateOf(false)
        private set
    
    var isLoadingOrganizadores by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var isError by mutableStateOf(false)
        private set

    // Token
    private val token: String
        get() = "Bearer ${SessionManager.getToken() ?: ""}"

    init {
        loadFavoritos()
        loadOrganizadoresFavoritos()
    }

    fun loadFavoritos() {
        viewModelScope.launch {
            isLoading = true
            isError = false
            errorMessage = null
            
            if (!SessionManager.isLoggedIn()) {
                isLoading = false
                isError = true
                errorMessage = "Debes iniciar sesión para ver tus favoritos"
                favoritos = emptyList()
                return@launch
            }
            
            try {
                Log.d("FavoritosViewModel", "Cargando favoritos...")
                val response = RetrofitClient.apiService.getFavoritos(token)
                if (response.isSuccessful) {
                    response.body()?.let { favoritosResponse ->
                        val eventosFromResponse = when {
                            // Primera opción: eventos en el campo "eventos"
                            favoritosResponse.eventos != null && favoritosResponse.eventos.isNotEmpty() -> {
                                Log.d("FavoritosViewModel", "Obteniendo eventos del campo 'eventos'")
                                favoritosResponse.eventos
                            }
                            // Segunda opción: favoritos con campo "evento" anidado
                            favoritosResponse.favoritos != null && favoritosResponse.favoritos.isNotEmpty() -> {
                                Log.d("FavoritosViewModel", "Obteniendo eventos del campo 'favoritos'")
                                // Mapear cada favorito a su evento
                                favoritosResponse.favoritos.mapNotNull { it.evento }
                            }
                            // No se encontraron datos
                            else -> {
                                Log.d("FavoritosViewModel", "No se encontraron favoritos en la respuesta")
                                emptyList()
                            }
                        }
                        
                        // Actualizar el estado
                        favoritos = eventosFromResponse
                        Log.d("FavoritosViewModel", "Favoritos cargados: ${favoritos.size}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = if (errorBody != null) {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } else null
                    errorMessage = errorResponse?.message ?: "Error al cargar favoritos"
                    isError = true
                    Log.e("FavoritosViewModel", "Error al cargar favoritos: $errorMessage")
                    favoritos = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
                isError = true
                favoritos = emptyList()
                Log.e("FavoritosViewModel", "Excepción al cargar favoritos", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun loadOrganizadoresFavoritos() {
        viewModelScope.launch {
            isLoadingOrganizadores = true
            
            if (!SessionManager.isLoggedIn()) {
                isLoadingOrganizadores = false
                organizadoresFavoritos = emptyList()
                return@launch
            }
            
            try {
                Log.d("FavoritosViewModel", "Cargando organizadores favoritos...")
                val response = RetrofitClient.apiService.getOrganizadoresFavoritos(token)
                if (response.isSuccessful) {
                    response.body()?.let { favoritosResponse ->
                        organizadoresFavoritos = favoritosResponse.organizadores
                        Log.d("FavoritosViewModel", "Organizadores favoritos cargados: ${organizadoresFavoritos.size}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = if (errorBody != null) {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } else null
                    Log.e("FavoritosViewModel", "Error al cargar organizadores favoritos: ${errorResponse?.message}")
                    organizadoresFavoritos = emptyList()
                }
            } catch (e: Exception) {
                Log.e("FavoritosViewModel", "Excepción al cargar organizadores favoritos", e)
                organizadoresFavoritos = emptyList()
            } finally {
                isLoadingOrganizadores = false
            }
        }
    }

    fun toggleFavorito(eventoId: Int) {
        viewModelScope.launch {
            if (!SessionManager.isLoggedIn()) {
                errorMessage = "Debes iniciar sesión para gestionar favoritos"
                isError = true
                return@launch
            }
            
            try {
                Log.d("FavoritosViewModel", "Verificando estado de favorito para evento $eventoId")
                val response = RetrofitClient.apiService.checkFavorito(token, eventoId)
                if (response.isSuccessful) {
                    val isFavorito = response.body()?.isFavorito ?: false
                    Log.d("FavoritosViewModel", "Estado actual: isFavorito=$isFavorito")
                    
                    if (isFavorito) {
                        removeFavorito(eventoId)
                    } else {
                        addFavorito(eventoId)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = if (errorBody != null) {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } else null
                    errorMessage = errorResponse?.message ?: "Error desconocido"
                    isError = true
                    Log.e("FavoritosViewModel", "Error al verificar favorito: $errorMessage")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
                isError = true
                Log.e("FavoritosViewModel", "Excepción al cambiar favorito", e)
            }
        }
    }

    private fun addFavorito(eventoId: Int) {
        viewModelScope.launch {
            try {
                val request = FavoritoRequest(eventoId)
                Log.d("FavoritosViewModel", "Añadiendo evento a favoritos: $eventoId")
                val response = RetrofitClient.apiService.agregarFavorito(token, request)
                
                if (response.isSuccessful) {
                    Log.d("FavoritosViewModel", "Evento añadido a favoritos correctamente")
                    loadFavoritos()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = if (errorBody != null) {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } else null
                    errorMessage = errorResponse?.message ?: "Error al añadir a favoritos"
                    isError = true
                    Log.e("FavoritosViewModel", "Error al añadir a favoritos: $errorMessage")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
                isError = true
                Log.e("FavoritosViewModel", "Excepción al añadir favorito", e)
            }
        }
    }

    private fun removeFavorito(eventoId: Int) {
        viewModelScope.launch {
            try {
                Log.d("FavoritosViewModel", "Eliminando evento de favoritos: $eventoId")
                val response = RetrofitClient.apiService.eliminarFavorito(token, eventoId)
                
                if (response.isSuccessful) {
                    Log.d("FavoritosViewModel", "Evento eliminado de favoritos correctamente")
                    loadFavoritos()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorResponse = if (errorBody != null) {
                        Gson().fromJson(errorBody, ErrorResponse::class.java)
                    } else null
                    errorMessage = errorResponse?.message ?: "Error al eliminar de favoritos"
                    isError = true
                    Log.e("FavoritosViewModel", "Error al eliminar de favoritos: $errorMessage")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
                isError = true
                Log.e("FavoritosViewModel", "Excepción al eliminar favorito", e)
            }
        }
    }

    fun toggleOrganizadorFavorito(organizador: Organizador) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.d(TAG, "No hay token disponible para toggleOrganizadorFavorito")
                    errorMessage = "Debes iniciar sesión para gestionar favoritos"
                    isError = true
                    return@launch
                }

                Log.d(TAG, "🔄 Iniciando toggle organizador favorito: ${organizador.id} - ${organizador.nombre}")
                isLoading = true
                
                // 1. Verificar estado actual en el servidor
                Log.d(TAG, "🔍 Verificando estado actual en servidor para organizador ${organizador.id}")
                val checkResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.checkOrganizadorFavorito(
                        token = "Bearer $token",
                        idOrganizador = organizador.id
                    )
                }
                
                if (!checkResponse.isSuccessful) {
                    Log.e(TAG, "❌ Error al verificar estado favorito: ${checkResponse.code()} - ${checkResponse.errorBody()?.string()}")
                    errorMessage = "Error al verificar estado de favorito"
                    isError = true
                    return@launch
                }
                
                val esFavoritoEnServidor = checkResponse.body()?.isFavorito ?: false
                Log.d(TAG, "📊 Estado en servidor para organizador ${organizador.id}: esFavorito=$esFavoritoEnServidor")
                
                // 2. Realizar acción basada en estado actual del servidor
                val response = if (esFavoritoEnServidor) {
                    // Ya es favorito, hay que quitarlo
                    Log.d(TAG, "🤍 Quitando organizador ${organizador.id} de favoritos")
                    val result = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.removeOrganizadorFavorito(
                            token = "Bearer $token",
                            idOrganizador = organizador.id
                        )
                    }
                    
                    // Actualización inmediata del UI si la operación fue exitosa
                    if (result.isSuccessful) {
                        Log.d(TAG, "✅ Organizador eliminado de favoritos, actualizando UI")
                        // Eliminar el organizador de la lista actual
                        organizadoresFavoritos = organizadoresFavoritos.filter { it.id != organizador.id }
                    }
                    
                    result
                } else {
                    // No es favorito, hay que añadirlo
                    Log.d(TAG, "❤️ Añadiendo organizador ${organizador.id} a favoritos")
                    val result = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.addOrganizadorFavorito(
                            token = "Bearer $token",
                            request = OrganizadorFavoritoRequest(organizador.id)
                        )
                    }
                    
                    // Actualización inmediata del UI si la operación fue exitosa
                    if (result.isSuccessful) {
                        Log.d(TAG, "✅ Organizador añadido a favoritos, actualizando UI")
                        // Añadir el organizador a la lista actual si no está ya
                        organizadoresFavoritos = organizadoresFavoritos + organizador
                    }
                    
                    result
                }
                
                // 3. Verificar resultado y notificar
                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Operación favorito completada: ${response.body()?.message}")
                    
                    // Recargar la lista completa para sincronizar con servidor
                    loadOrganizadoresFavoritos()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "❌ Error en operación favorito: ${response.code()} - $errorMsg")
                    errorMessage = "Error al actualizar favorito: $errorMsg"
                    isError = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción en toggleOrganizadorFavorito", e)
                errorMessage = "Error: ${e.message}"
                isError = true
            } finally {
                isLoading = false
            }
        }
    }
} 