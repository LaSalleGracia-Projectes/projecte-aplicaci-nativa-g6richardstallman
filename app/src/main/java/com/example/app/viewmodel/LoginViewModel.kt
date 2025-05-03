package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.app.model.login.LoginRequest
import com.example.app.model.login.LoginResponse
import com.example.app.util.GoogleUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.example.app.util.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.lang.reflect.Type
import com.example.app.model.login.GoogleAuthRequest

class LoginViewModel : ViewModel() {
    // Campos del formulario
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    
    // Estados de la UI
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    // Estado del login
    private val _isLoginSuccessful = MutableStateFlow(false)
    val isLoginSuccessful = _isLoginSuccessful.asStateFlow()
    
    // Estado para la redirección a registro de participante después de autenticación con Google
    private val _shouldNavigateToParticipanteRegister = MutableStateFlow(false)
    val shouldNavigateToParticipanteRegister = _shouldNavigateToParticipanteRegister.asStateFlow()
    
    // Datos de sesión
    var token by mutableStateOf<String?>(null)
    var user by mutableStateOf<com.example.app.model.User?>(null)
    var googleAccount by mutableStateOf<GoogleSignInAccount?>(null)

    // Estado del logout
    private val _isLogoutSuccessful = MutableStateFlow(false)
    val isLogoutSuccessful = _isLogoutSuccessful.asStateFlow()

    fun onLoginClick() {
        // Validar campos
        if (!validateFields()) {
            return
        }

        // Mostrar indicador de carga
        isLoading = true

        viewModelScope.launch {
            try {
                // Realizar la solicitud de inicio de sesión
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.loginUser(LoginRequest(email, password))
                }

                // Procesar la respuesta
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Log.d("LoginViewModel", "Respuesta del servidor recibida: $loginResponse")
                        Log.d("LoginViewModel", "Código de respuesta: ${response.code()}")
                        
                        if (loginResponse != null) {
                            Log.d("LoginViewModel", "Verificando token...")
                            
                            try {
                                // Verificar si el usuario existe en la respuesta
                                if (loginResponse.user == null) {
                                    setError("La respuesta no contiene información de usuario")
                                    Log.e("LoginViewModel", "Usuario es null en la respuesta")
                                    return@withContext
                                }
                                
                                // Intentar obtener el token de donde esté disponible
                                val accessToken = loginResponse.token ?: loginResponse.accessToken
                                
                                // Validar que obtuvimos un token
                                if (accessToken.isNullOrBlank()) {
                                    setError("No se recibió un token válido del servidor")
                                    Log.e("LoginViewModel", "No se pudo obtener un token válido")
                                    return@withContext
                                }
                                
                                // Guardar el token y la información del usuario
                                token = accessToken
                                user = loginResponse.user
                                
                                Log.d("LoginViewModel", "Login exitoso")
                                Log.d("LoginViewModel", "Token: $token")
                                Log.d("LoginViewModel", "Usuario: ${user?.email}")
                                
                                // Determinar el rol de usuario (con manejo seguro de nulos)
                                val userRole = when {
                                    loginResponse.role?.isNotBlank() == true -> loginResponse.role
                                    loginResponse.userRole?.isNotBlank() == true -> loginResponse.userRole
                                    loginResponse.userRoleAlt?.isNotBlank() == true -> loginResponse.userRoleAlt
                                    else -> "participante" // valor por defecto
                                }
                                Log.d("LoginViewModel", "Rol determinado: $userRole")
                                
                                // Guardar en SessionManager (con manejo seguro de excepciones)
                                try {
                                    if (!SessionManager.isInitialized()) {
                                        Log.w("LoginViewModel", "SessionManager no está inicializado, no se guardarán los datos de sesión")
                                        // Continuamos con el login aunque no se guarden los datos
                                    } else {
                                        SessionManager.saveToken(accessToken)
                                        SessionManager.saveUserRole(userRole)
                                        
                                        // Verificar que se guardó correctamente
                                        val savedToken = SessionManager.getToken()
                                        val savedRole = SessionManager.getUserRole()
                                        Log.d("LoginViewModel", "Token guardado: $savedToken")
                                        Log.d("LoginViewModel", "Rol guardado: $savedRole")
                                    }
                                    
                                    // Siempre continuamos con el login
                                    clearFields()
                                    _isLoginSuccessful.value = true
                                } catch (e: Exception) {
                                    Log.e("LoginViewModel", "Error al guardar en SessionManager: ${e.message}")
                                    // No bloqueamos el login aunque haya errores en el guardado
                                    clearFields()
                                    _isLoginSuccessful.value = true
                                }
                            } catch (e: Exception) {
                                Log.e("LoginViewModel", "Error al procesar la respuesta: ${e.message}", e)
                                setError("Error al procesar la respuesta: ${e.message ?: "Error desconocido"}")
                                return@withContext
                            }
                        } else {
                            setError("Error desconocido durante el inicio de sesión")
                        }
                    } else {
                        // Intentar obtener el mensaje de error del cuerpo de la respuesta
                        try {
                            val errorBody = response.errorBody()?.string()
                            if (errorBody != null) {
                                val errorResponse = com.google.gson.Gson().fromJson(errorBody, Map::class.java)
                                val message = errorResponse["message"] as? String ?: "Error en la comunicación con el servidor"
                                setError(message)
                            } else {
                                setError("Error en la comunicación con el servidor: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            setError("Error en la comunicación con el servidor: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejar errores
                withContext(Dispatchers.Main) {
                    isLoading = false
                    setError("Error de conexión: ${e.message ?: "Error desconocido"}")
                    Log.e("LoginViewModel", "Excepción durante el login", e)
                }
            }
        }
    }

    fun clearFields() {
        email = ""
        password = ""
    }

    fun setError(message: String) {
        errorMessage = message
    }

    fun clearError() {
        errorMessage = null
    }

    fun resetState() {
        _isLoginSuccessful.value = false
        _isLogoutSuccessful.value = false
        _shouldNavigateToParticipanteRegister.value = false
        clearError()
        // No limpiar email/password para facilitar reautenticación
    }

    fun onForgotPasswordClick() {
        // No hacer nada aquí, la navegación se maneja en la vista
    }

    fun onRegisterClick() {
        // No hacer nada aquí, la navegación se maneja en la vista
    }

    fun onLoginSuccess(userType: String) {
        viewModelScope.launch {
            try {
                // No hacer nada aquí, la navegación se maneja en la vista
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al navegar después del inicio de sesión", e)
                setError("Error al navegar: ${e.message}")
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            try {
                isLoading = true
                
                // Simular una petición de cierre de sesión
                delay(500)
                
                // Limpiar datos de sesión
                token = null
                user = null
                _isLoginSuccessful.value = false
                _isLogoutSuccessful.value = true
                
                Log.d("LoginViewModel", "Sesión cerrada correctamente")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error al cerrar sesión", e)
                setError("Error al cerrar sesión: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Función para resetear el estado de cierre de sesión
    fun resetLogoutState() {
        _isLogoutSuccessful.value = false
    }

    // Función que será llamada desde el HomeScreen
    fun performLogout() {
        viewModelScope.launch {
            try {
                isLoading = true
                
                // Obtener el token almacenado
                val token = SessionManager.getToken()
                
                if (token == null) {
                    Log.e("LoginViewModel", "No hay token para logout")
                    // Si no hay token, simplemente limpiamos los datos locales
                    clearUserData()
                    _isLogoutSuccessful.value = true
                    return@launch
                }
                
                // Llamar al endpoint de logout con el token
                try {
                    val response = RetrofitClient.apiService.logoutUser("Bearer $token")
                    
                    if (response.isSuccessful) {
                        Log.d("LoginViewModel", "Logout exitoso en el servidor")
                    } else {
                        Log.e("LoginViewModel", "Error en logout: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("LoginViewModel", "Excepción en logout: ${e.message}")
                }
                
                // Siempre limpiamos los datos locales, incluso si falla el logout en el servidor
                clearUserData()
                _isLogoutSuccessful.value = true
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error durante el logout", e)
                // Aún así intentamos limpiar los datos locales
                clearUserData()
                _isLogoutSuccessful.value = true
            } finally {
                isLoading = false
            }
        }
    }

    private fun clearUserData() {
        // Limpiar datos locales
        SessionManager.clearSession()
        token = null
        user = null
        _isLoginSuccessful.value = false
    }

    private fun validateFields(): Boolean {
        if (email.isEmpty()) {
            setError("Por favor, introduce tu correo electrónico")
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setError("Por favor, introduce un correo electrónico válido")
            return false
        }
        
        if (password.isEmpty()) {
            setError("Por favor, introduce tu contraseña")
            return false
        }
        
        // Limpiar error si todo está bien
        clearError()
        return true
    }

    fun handleGoogleSignInResult(account: GoogleUserInfo) {
        viewModelScope.launch {
            try {
                isLoading = true
                Log.d("GOOGLE_AUTH_DEBUG", "======= INICIO PROCESO GOOGLE AUTH =======")
                Log.d("GOOGLE_AUTH_DEBUG", "Cuenta recibida: Email=${account.email}, Nombre=${account.nombre}")
                
                val request = GoogleAuthRequest(
                    email = account.email,
                    nombre = account.nombre,
                    apellido1 = account.apellido1,
                    apellido2 = account.apellido2,
                    photoUrl = account.photoUrl,
                    token = account.email,
                    id = account.email.split("@")[0],
                    googleId = account.email
                )
                
                Log.d("GOOGLE_AUTH_DEBUG", "Enviando solicitud de autenticación con Google: ${Gson().toJson(request)}")
                
                try {
                    Log.d("GOOGLE_AUTH_DEBUG", "Intentando endpoint handleGoogleMobile")
                    val response: Response<LoginResponse> = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.handleGoogleMobile(request)
                    }

                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Log.d("GOOGLE_AUTH_DEBUG", "Respuesta del servidor: Código=${response.code()}")
                        
                        if (response.isSuccessful) {
                            val loginResponse = response.body()
                            Log.d("GOOGLE_AUTH_DEBUG", "Respuesta exitosa de Google: ${Gson().toJson(loginResponse)}")
                            
                            if (loginResponse != null) {
                                // Obtener token y verificar si necesita registro
                                val accessToken = loginResponse.token ?: loginResponse.accessToken
                                val needsRegistration = loginResponse.needsRegistration ?: false
                                
                                Log.d("GOOGLE_AUTH_DEBUG", "Token recibido: ${accessToken?.take(10)}...")
                                Log.d("GOOGLE_AUTH_DEBUG", "¿Necesita registro? $needsRegistration")
                                
                                if (accessToken.isNullOrBlank()) {
                                    Log.e("GOOGLE_AUTH_DEBUG", "ERROR: Token nulo o vacío en respuesta de Google")
                                    setError("No se recibió un token válido del servidor")
                                    return@withContext
                                }
                                
                                // Guardar los datos en memoria
                                token = accessToken
                                user = loginResponse.user
                                
                                // Guardar en SessionManager
                                try {
                                    if (SessionManager.isInitialized()) {
                                        SessionManager.saveToken(accessToken)
                                        
                                        // Determinar y guardar el rol
                                        val userRole = when {
                                            loginResponse.role?.isNotBlank() == true -> loginResponse.role
                                            loginResponse.userRole?.isNotBlank() == true -> loginResponse.userRole
                                            loginResponse.userRoleAlt?.isNotBlank() == true -> loginResponse.userRoleAlt
                                            user?.role?.isNotBlank() == true -> user?.role
                                            else -> "participante"
                                        }
                                        
                                        userRole?.let { 
                                            SessionManager.saveUserRole(it)
                                            Log.d("GOOGLE_AUTH_DEBUG", "Rol guardado: $it")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("GOOGLE_AUTH_DEBUG", "Error al guardar en SessionManager: ${e.message}", e)
                                }
                                
                                // Decidir la navegación basada en needs_registration
                                if (needsRegistration) {
                                    Log.d("GOOGLE_AUTH_DEBUG", "Usuario necesita completar registro...")
                                    _shouldNavigateToParticipanteRegister.value = true
                                } else {
                                    Log.d("GOOGLE_AUTH_DEBUG", "Usuario ya registrado, procediendo al login...")
                                    _isLoginSuccessful.value = true
                                }
                                
                                clearFields()
                            } else {
                                Log.e("GOOGLE_AUTH_DEBUG", "ERROR: Respuesta vacía del servidor")
                                setError("Error: La respuesta del servidor está vacía")
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("GOOGLE_AUTH_DEBUG", "Error en la respuesta: ${response.code()}, Body: $errorBody")
                            
                            try {
                                val errorJson = Gson().fromJson<Map<String, String>>(errorBody, object : TypeToken<Map<String, String>>() {}.type)
                                val errorMessage = errorJson["message"] ?: "Error en la comunicación con el servidor"
                                setError(errorMessage)
                            } catch (e: Exception) {
                                setError("Error en la comunicación con el servidor (${response.code()})")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GOOGLE_AUTH_DEBUG", "Error en la petición: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        setError("Error de conexión: ${e.message ?: "Error desconocido"}")
                    }
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_AUTH_DEBUG", "Error general: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    setError("Error inesperado: ${e.message ?: "Error desconocido"}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
                Log.d("GOOGLE_AUTH_DEBUG", "======= FIN PROCESO GOOGLE AUTH =======")
            }
        }
    }

    // Función para completar el perfil de usuario tras login con Google
    fun completarPerfilUsuario() {
        viewModelScope.launch {
            try {
                if (token.isNullOrBlank()) {
                    Log.e("LoginViewModel", "No hay token para completar perfil")
                    return@launch
                }
                
                Log.d("LoginViewModel", "Intentando completar perfil de usuario")
                isLoading = true
                
                // Crear datos mínimos del perfil
                val perfilData = mapOf(
                    "telefono" to "000000000", // Valor por defecto
                    "dni" to "00000000X",      // Valor por defecto
                    "completeProfile" to "true"
                )
                
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.updateProfile("Bearer $token", perfilData)
                }
                
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (response.isSuccessful) {
                        Log.d("LoginViewModel", "Perfil completado exitosamente")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("LoginViewModel", "Error al completar perfil: $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Log.e("LoginViewModel", "Excepción al completar perfil: ${e.message}")
                }
            }
        }
    }

    // Método para guardar temporalmente los datos de Google para el registro
    private fun saveGoogleUserDataForRegistration(
        email: String,
        nombre: String,
        apellido1: String,
        googleToken: String?
    ) {
        try {
            // Guardar en preferencias compartidas para pasar a la pantalla de registro
            val sharedPrefs = com.example.app.MyApplication.appContext.getSharedPreferences(
                "GoogleAuthData",
                android.content.Context.MODE_PRIVATE
            )
            
            with(sharedPrefs.edit()) {
                putString("google_email", email)
                putString("google_nombre", nombre)
                putString("google_apellido1", apellido1)
                putString("google_token", googleToken)
                putBoolean("is_from_google", true)
                apply()
            }
            
            Log.d("GOOGLE_AUTH_DEBUG", "Datos de Google guardados en preferencias para registro")
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH_DEBUG", "Error al guardar datos de Google: ${e.message}")
        }
    }
}