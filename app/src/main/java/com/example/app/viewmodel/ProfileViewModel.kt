package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.ProfileData
import com.example.app.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import com.example.app.api.DeleteAccountRequest
import com.google.gson.Gson
import retrofit2.Response
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {
    private val TAG = "ProfileViewModel"
    
    // Estado del perfil
    var profileData by mutableStateOf<ProfileData?>(null)
        private set
    
    // Estado de edición
    var isEditing by mutableStateOf(false)
        private set
    
    // Campos editables
    var nombre by mutableStateOf("")
    var apellido1 by mutableStateOf("")
    var apellido2 by mutableStateOf("")
    var email by mutableStateOf("")
    
    // Campos específicos para participante
    var dni by mutableStateOf("")
    var telefono by mutableStateOf("")
    
    // Campos específicos para organizador
    var nombreOrganizacion by mutableStateOf("")
    var telefonoContacto by mutableStateOf("")
    
    // Estados de UI
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Estado de actualización
    private val _isUpdateSuccessful = MutableStateFlow(false)
    val isUpdateSuccessful = _isUpdateSuccessful.asStateFlow()
    
    // Estado para cambio de contraseña
    private val _isPasswordChangeSuccessful = MutableStateFlow(false)
    val isPasswordChangeSuccessful = _isPasswordChangeSuccessful.asStateFlow()
    
    // Estado para el proceso de cambio de contraseña
    var isChangingPassword by mutableStateOf(false)
        private set
    
    // Añadir un nuevo estado para controlar la redirección a login
    private val _shouldNavigateToLogin = MutableStateFlow(false)
    val shouldNavigateToLogin = _shouldNavigateToLogin.asStateFlow()
    
    // Estado para eliminar cuenta
    private val _isDeleteAccountSuccessful = MutableStateFlow(false)
    val isDeleteAccountSuccessful = _isDeleteAccountSuccessful.asStateFlow()
    
    // Estado para controlar si mostrar el diálogo de eliminación de cuenta
    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    val showDeleteConfirmationDialog = _showDeleteConfirmationDialog.asStateFlow()
    
    // Estado para controlar si mostrar el diálogo de éxito
    private val _showDeleteSuccessDialog = MutableStateFlow(false)
    val showDeleteSuccessDialog = _showDeleteSuccessDialog.asStateFlow()
    
    // Inicializar cargando el perfil
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            try {
                isLoading = true
                clearError()
                
                // Obtener token
                val token = com.example.app.util.SessionManager.getToken()
                Log.d("ProfileViewModel", "Token obtenido del SessionManager: $token")
                
                if (token.isNullOrEmpty()) {
                    Log.e("ProfileViewModel", "Error: No hay token disponible")
                    setError("No se ha iniciado sesión")
                    return@launch
                }
                
                // Solo una llamada a /api/profile
                Log.d("ProfileViewModel", "Realizando petición a getProfile con token: Bearer ${token.take(10)}...")
                val response = withContext(Dispatchers.IO) {
                    try {
                        com.example.app.api.RetrofitClient.apiService.getProfile("Bearer $token")
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Error al realizar petición HTTP a /api/profile: ${e.message}", e)
                        return@withContext null
                    }
                }
                
                withContext(Dispatchers.Main) {
                    if (response == null) {
                        setError("Error de conexión al servidor")
                        return@withContext
                    }
                    if (response.isSuccessful) {
                        val profileResponse = response.body()
                        Log.d("ProfileViewModel", "Respuesta exitosa de /api/profile: ${profileResponse?.message}")
                        if (profileResponse != null && profileResponse.data != null) {
                            profileData = profileResponse.data
                            Log.d("ProfileViewModel", "Datos de perfil recibidos: $profileData")
                            initEditableFields()
                        } else {
                            Log.e("ProfileViewModel", "Cuerpo de respuesta vacío o sin datos")
                            setError("No se pudo obtener la información del perfil")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ProfileViewModel", "Error ${response.code()}: $errorBody")
                        when (response.code()) {
                            401 -> {
                                setError("Tu sesión ha expirado. Por favor, inicia sesión nuevamente")
                                _shouldNavigateToLogin.value = true
                            }
                            403 -> setError("No tienes permiso para acceder a esta información")
                            404 -> setError("Perfil no encontrado")
                            500 -> setError("Error interno del servidor")
                            else -> setError("Error al cargar el perfil: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error general al cargar perfil: ${e.message}", e)
                setError("Error de conexión: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    private fun initEditableFields() {
        profileData?.let { profile ->
            nombre = profile.nombre ?: ""
            apellido1 = profile.apellido1 ?: ""
            apellido2 = profile.apellido2 ?: ""
            email = profile.email ?: ""
            
            // Campos por rol
            if (profile.role == "participante") {
                dni = profile.dni ?: ""
                telefono = profile.telefono ?: ""
            } else if (profile.role == "organizador") {
                nombreOrganizacion = profile.nombreOrganizacion ?: ""
                telefonoContacto = profile.telefonoContacto ?: ""
            }
        }
    }
    
    fun startEditing() {
        isEditing = true
    }
    
    fun cancelEditing() {
        isEditing = false
        initEditableFields() // Restaurar campos originales
        clearError()
    }   
    
    fun saveProfile() {
        viewModelScope.launch {
            try {
                isLoading = true
                clearError()
                
                // Obtener token
                val token = com.example.app.util.SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    setError("No se ha iniciado sesión")
                    return@launch
                }
                
                // Crear mapa con los datos a actualizar usando valores String explícitos
                val updateData = HashMap<String, String>()
                
                profileData?.let { profile ->
                    // Datos básicos
                    updateData["nombre"] = nombre
                    updateData["apellido1"] = apellido1
                    if (apellido2.isNotEmpty()) {
                        updateData["apellido2"] = apellido2
                    } else {
                        updateData["apellido2"] = "" // Asegurar que se envía vacío y no null
                    }
                    
                    // Añadir todos los campos específicos para cada rol,
                    // independientemente del rol del usuario actual
                    // Campos de participante
                    updateData["dni"] = dni
                    updateData["telefono"] = telefono
                    
                    // Campos de organizador
                    updateData["nombre_organizacion"] = nombreOrganizacion
                    updateData["telefono_contacto"] = telefonoContacto
                }
                
                Log.d("ProfileViewModel", "Enviando datos: $updateData")
                
                try {
                    // Realizar la petición de actualización
                    val response = withContext(Dispatchers.IO) {
                        com.example.app.api.RetrofitClient.apiService.updateProfile(
                            "Bearer $token",
                            updateData
                        )
                    }
                    
                    // Procesar la respuesta
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val profileResponse = response.body()
                            if (profileResponse != null && profileResponse.data != null) {
                                profileData = profileResponse.data
                                isEditing = false
                                _isUpdateSuccessful.value = true
                            } else {
                                setError("No se pudo actualizar la información del perfil")
                            }
                        } else {
                            val errorBody = response.errorBody()?.string() ?: ""
                            Log.e("ProfileViewModel", "Error raw: $errorBody")
                            
                            // Verificar si la respuesta es HTML (error del servidor)
                            if (errorBody.trim().startsWith("<!DOCTYPE html>") || 
                                errorBody.trim().startsWith("<html")) {
                                Log.e("ProfileViewModel", "Error del servidor (respuesta HTML)")
                                setError("Error en el servidor. Por favor, intente más tarde")
                            } else {
                                // Intentar analizar como JSON primero
                                try {
                                    // Si hay un mensaje específico en el errorBody, lo extraemos
                                    val errorMessage = if (errorBody.contains("\"message\"")) {
                                        // Extraer el mensaje del JSON usando una expresión regular simple
                                        val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                                        val matchResult = regex.find(errorBody)
                                        matchResult?.groupValues?.get(1) ?: "Error desconocido"
                                    } else {
                                        "Error: ${response.code()}"
                                    }
                                    setError(errorMessage)
                                } catch (e: Exception) {
                                    // Si no es JSON, mostramos el error tal cual
                                    setError("Error al actualizar el perfil: ${response.code()}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error en la petición HTTP: ${e.message}")
                    if (e.message?.contains("Unable to resolve host") == true) {
                        setError("No hay conexión a Internet. Verifica tu conexión e intenta nuevamente.")
                    } else {
                        setError("Error de conexión: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error general: ${e.message}")
                setError("Error inesperado: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    fun resetUpdateState() {
        _isUpdateSuccessful.value = false
    }
    
    private fun setError(message: String) {
        errorMessage = message
    }
    
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * Resetea el flag de navegación al login
     */
    fun resetShouldNavigateToLogin() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                _shouldNavigateToLogin.value = false
                Log.d("ProfileViewModel", "Flag de navegación al login reseteado: ${_shouldNavigateToLogin.value}")
            }
        }
    }
    
    fun navigateToLogin() {
        _shouldNavigateToLogin.value = true
    }
    
    fun logout() {
        Log.d(TAG, "Iniciando proceso de logout")
        viewModelScope.launch {
            try {
                // Limpiar datos de perfil y sesión
                com.example.app.util.SessionManager.clearSession()
                
                // Obtener token actual para el logout
                val token = "Bearer ${com.example.app.util.SessionManager.getToken() ?: ""}"
                Log.d(TAG, "Token para logout: ${token.takeLast(10)}...")
                
                // Llamar al API de logout
                try {
                    val response = withContext(Dispatchers.IO) {
                        com.example.app.api.RetrofitClient.apiService.logoutUser(token)
                    }
                    if (response.isSuccessful) {
                        Log.d(TAG, "Logout exitoso en el API")
                    } else {
                        Log.e(TAG, "Error en logout API: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción durante logout: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción durante logout: ${e.message}")
            } finally {
                // Siempre limpiar sesión y navegar a login independientemente de errores
                com.example.app.util.SessionManager.clearSession()
                Log.d(TAG, "Sesión limpiada localmente")
                
                withContext(Dispatchers.Main) {
                    // Activar la navegación al login en el hilo principal
                    _shouldNavigateToLogin.value = true
                    Log.d(TAG, "Activada navegación al login, valor: ${_shouldNavigateToLogin.value}")
                }
            }
        }
    }
    
    /**
     * Método para cerrar sesión con callback de navegación directa
     * @param navigationCallback El callback que se ejecutará después de cerrar sesión con la API
     */
    fun logoutAndNavigate(navigationCallback: () -> Unit) {
        viewModelScope.launch {
            try {
                isLoading = true
                clearError()
                
                Log.d("ProfileViewModel", "Iniciando proceso de logout directo")
                
                // Limpiar la sesión local inmediatamente
                com.example.app.util.SessionManager.clearSession()
                Log.d("ProfileViewModel", "Sesión limpiada localmente")
                
                // Obtener token antes de limpiarlo (si es posible)
                val token = com.example.app.util.SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.d("ProfileViewModel", "No hay token, cerrando sesión y navegando directamente")
                    withContext(Dispatchers.Main) {
                        navigationCallback()
                    }
                    return@launch
                }
                
                // Llamar al endpoint de logout
                try {
                    Log.d("ProfileViewModel", "Llamando al endpoint de logout")
                    val response = withContext(Dispatchers.IO) {
                        com.example.app.api.RetrofitClient.apiService.logoutUser("Bearer $token")
                    }
                    
                    if (response.isSuccessful) {
                        Log.d("ProfileViewModel", "Logout exitoso: ${response.body()?.message}")
                    } else {
                        Log.e("ProfileViewModel", "Error en logout: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Error al llamar logout API: ${e.message}")
                }
                
                // Independientemente de la respuesta, ejecutar el callback de navegación
                withContext(Dispatchers.Main) {
                    Log.d("ProfileViewModel", "Ejecutando callback de navegación directo")
                    navigationCallback()
                }
                
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error general en logout directo: ${e.message}")
                // Aún así, intentar ejecutar el callback de navegación
                withContext(Dispatchers.Main) {
                    navigationCallback()
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Método para cerrar sesión en segundo plano, sin bloquear la UI y sin importar el resultado
     */
    fun logoutInBackground() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ProfileViewModel", "Iniciando logout en segundo plano")
                val token = com.example.app.util.SessionManager.getToken()
                
                if (token.isNullOrEmpty()) {
                    Log.d("ProfileViewModel", "No hay token para logout en segundo plano")
                    return@launch
                }
                
                try {
                    Log.d("ProfileViewModel", "Llamando al API de logout en segundo plano")
                    val response = com.example.app.api.RetrofitClient.apiService.logoutUser("Bearer $token")
                    
                    if (response.isSuccessful) {
                        Log.d("ProfileViewModel", "Logout en segundo plano exitoso")
                    } else {
                        Log.w("ProfileViewModel", "Logout en segundo plano falló: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w("ProfileViewModel", "Error al llamar al API de logout en segundo plano: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Error general en logout en segundo plano: ${e.message}")
            }
        }
    }
    
    // Función para cambiar la contraseña
    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            try {
                isChangingPassword = true
                clearError()
                
                // Validar que las contraseñas coincidan
                if (newPassword != confirmPassword) {
                    setError("Las contraseñas no coinciden")
                    return@launch
                }
                
                // Validar que la nueva contraseña sea diferente a la actual
                if (currentPassword == newPassword) {
                    setError("La nueva contraseña debe ser diferente a la actual")
                    return@launch
                }
                
                // Validar longitud mínima
                if (newPassword.length < 6) {
                    setError("La nueva contraseña debe tener al menos 6 caracteres")
                    return@launch
                }
                
                // Obtener token
                val token = com.example.app.util.SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "Error: No hay token disponible")
                    setError("No se ha iniciado sesión")
                    return@launch
                }
                
                // Crear el objeto de datos para el cambio de contraseña
                val passwordData = mapOf(
                    "current_password" to currentPassword,
                    "new_password" to newPassword,
                    "confirm_password" to confirmPassword
                )
                
                // Realizar la petición
                val response = withContext(Dispatchers.IO) {
                    try {
                        com.example.app.api.RetrofitClient.apiService.changePassword("Bearer $token", passwordData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error HTTP al cambiar contraseña: ${e.message}", e)
                        return@withContext null
                    }
                }
                
                // Procesar la respuesta
                if (response == null) {
                    setError("Error de conexión al servidor")
                    return@launch
                }
                
                if (response.isSuccessful) {
                    // Marcar como exitoso y limpiar errores
                    _isPasswordChangeSuccessful.value = true
                    clearError()
                    Log.d(TAG, "Contraseña cambiada con éxito")
                } else {
                    // Procesar el error según el código
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error al cambiar contraseña: ${response.code()} - $errorBody")
                    
                    when (response.code()) {
                        400 -> {
                            // Intentar extraer mensaje específico
                            if (errorBody?.contains("contraseña actual no es correcta") == true) {
                                setError("La contraseña actual es incorrecta")
                            } else {
                                setError("Error en los datos enviados")
                            }
                        }
                        401 -> {
                            setError("Tu sesión ha expirado. Por favor, inicia sesión nuevamente")
                            _shouldNavigateToLogin.value = true
                        }
                        else -> setError("Error al cambiar la contraseña (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cambiar contraseña: ${e.message}", e)
                setError("Error: ${e.message}")
            } finally {
                isChangingPassword = false
            }
        }
    }
    
    // Resetea el estado de éxito del cambio de contraseña
    fun resetPasswordChangeState() {
        _isPasswordChangeSuccessful.value = false
    }
    
    /**
     * Eliminar la cuenta del usuario
     * @param password La contraseña del usuario para confirmar
     */
    fun deleteAccount(password: String) {
        // Limpiar cualquier error previo al iniciar el proceso
        clearError()
        
        // Verificar que la contraseña no esté vacía
        if (password.isBlank()) {
            setError("La contraseña es obligatoria")
            return
        }
        
        // Verificar token antes de continuar
        if (!com.example.app.util.SessionManager.hasValidToken()) {
            Log.d(TAG, "No hay token válido para eliminar cuenta, navegando a login")
            _shouldNavigateToLogin.value = true
            return
        }
        
        // Obtener token
        val token = com.example.app.util.SessionManager.getToken()
        
        viewModelScope.launch {
            try {
                isLoading = true
                
                // Crear la solicitud de eliminación
                val deleteRequest = DeleteAccountRequest(
                    password = password,
                    confirmDeletion = true
                )
                
                try {
                    Log.d(TAG, "Enviando solicitud para eliminar cuenta")
                    
                    // Llamar al endpoint para eliminar la cuenta
                    val response = withContext(Dispatchers.IO) {
                        com.example.app.api.RetrofitClient.apiService.deleteAccount(
                            "Bearer $token",
                            deleteRequest
                        )
                    }
                    
                    if (response.isSuccessful) {
                        Log.d(TAG, "Cuenta eliminada correctamente: ${response.body()?.message}")
                        
                        // Actualizar estado para mostrar el diálogo de éxito
                        _isDeleteAccountSuccessful.value = true
                        _showDeleteSuccessDialog.value = true
                        
                    } else {
                        // Manejar diferentes tipos de errores
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = when (response.code()) {
                            400 -> {
                                try {
                                    val errorResponse = Gson().fromJson<ErrorResponse>(errorBody, ErrorResponse::class.java)
                                    errorResponse?.message ?: "La contraseña proporcionada no es correcta"
                                } catch (e: Exception) {
                                    "La contraseña proporcionada no es correcta"
                                }
                            }
                            401 -> "No autorizado. Por favor, inicia sesión de nuevo."
                            422 -> {
                                try {
                                    val errorResponse = Gson().fromJson<ErrorResponse>(errorBody, ErrorResponse::class.java)
                                    errorResponse?.message ?: "Error de validación. Verifica tus datos."
                                } catch (e: Exception) {
                                    "Error de validación. Verifica tus datos."
                                }
                            }
                            500 -> "Error interno del servidor. Por favor, intenta más tarde."
                            else -> "Error al eliminar la cuenta: ${response.message()}"
                        }
                        setError(errorMessage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al eliminar cuenta: ${e.message}", e)
                    setError("Error de conexión: ${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    fun resetDeleteAccountState() {
        _isDeleteAccountSuccessful.value = false
        _showDeleteSuccessDialog.value = false
        _showDeleteConfirmationDialog.value = false
    }
    
    fun showDeleteConfirmationDialog() {
        _showDeleteConfirmationDialog.value = true
    }
    
    fun hideDeleteConfirmationDialog() {
        _showDeleteConfirmationDialog.value = false
    }
    
    fun confirmDeleteSuccess() {
        // Ocultar el diálogo primero
        _showDeleteSuccessDialog.value = false
        
        // Implementación hardcodeada directa
        Log.d(TAG, "Ejecutando eliminación de cuenta confirmada con implementación hardcodeada")
        
        // 1. Limpiar sesión directamente sin esperar (forzar limpieza)
        com.example.app.util.SessionManager.clearSessionSync()
        
        // 2. Acceso directo a las variables internas de SessionManager para asegurar la limpieza
        try {
            // Acceder directamente al campo mediante reflexión como último recurso
            val sessionManagerClass = com.example.app.util.SessionManager::class.java
            val cachedTokenField = sessionManagerClass.getDeclaredField("cachedToken")
            cachedTokenField.isAccessible = true
            cachedTokenField.set(com.example.app.util.SessionManager, null)
            
            val cachedRoleField = sessionManagerClass.getDeclaredField("cachedRole")
            cachedRoleField.isAccessible = true
            cachedRoleField.set(com.example.app.util.SessionManager, null)
            
            Log.d(TAG, "Variables internas de SessionManager limpiadas por reflexión")
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar variables internas: ${e.message}")
        }
        
        // 3. Doble verificación para asegurar que todo está limpio
        if (com.example.app.util.SessionManager.getToken() != null) {
            Log.w(TAG, "¡ALERTA! El token aún no está limpio, forzando limpieza nuevamente")
            com.example.app.util.SessionManager.clearSessionSync()
        }
        
        // 4. Forzar navegación a login inmediatamente y sin esperar
        Log.d(TAG, "Forzando navegación a login inmediatamente")
        _shouldNavigateToLogin.value = true
    }
}

// Clase de modelo simplificada para los datos de usuario
data class UserData(
    val id: Int = 0,
    val nombre: String = "",
    val apellidos: String = "",
    val email: String = "",
    val role: String = ""
)

// Clase para manejar errores de la API
data class ErrorResponse(
    val message: String?
) 