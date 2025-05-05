package com.example.app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import com.example.app.api.RetrofitClient
import com.example.app.model.register.RegisterRequest
import com.example.app.model.register.RegisterResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.example.app.model.ErrorResponse

class RegisterViewModel : ViewModel() {
    // Datos básicos
    var name by mutableStateOf("")
    var apellido1 by mutableStateOf("")
    var apellido2 by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var role by mutableStateOf("participante")

    // Datos de organizador
    var nombreOrganizacion by mutableStateOf("")
    var telefonoContacto by mutableStateOf("")

    // Datos de participante
    var dni by mutableStateOf("")
    var telefono by mutableStateOf("")

    // Datos de Google Auth
    var isFromGoogleAuth by mutableStateOf(false)
    var googleToken by mutableStateOf<String?>(null)

    // Estados de error básicos
    var isNameError by mutableStateOf(false)
    var isApellido1Error by mutableStateOf(false)
    var isApellido2Error by mutableStateOf(false)
    var isEmailError by mutableStateOf(false)
    var isPasswordError by mutableStateOf(false)
    var isConfirmPasswordError by mutableStateOf(false)


    // Estados de error organizador
    var isNombreOrganizacionError by mutableStateOf(false)
    var isTelefonoContactoError by mutableStateOf(false)

    // Estados de error participante
    var isDniError by mutableStateOf(false)
    var isTelefonoError by mutableStateOf(false)

    // Mensajes de error
    var nameErrorMessage by mutableStateOf("")
    var apellido1ErrorMessage by mutableStateOf("")
    var apellido2ErrorMessage by mutableStateOf("")
    var emailErrorMessage by mutableStateOf("")
    var passwordErrorMessage by mutableStateOf("")
    var confirmPasswordErrorMessage by mutableStateOf("")
    var nombreOrganizacionErrorMessage by mutableStateOf("")
    var telefonoContactoErrorMessage by mutableStateOf("")
    var dniErrorMessage by mutableStateOf("")
    var telefonoErrorMessage by mutableStateOf("")

    // Estados de la UI
    var isLoading by mutableStateOf(false)
    private val _isRegisterSuccessful = MutableStateFlow(false)
    val isRegisterSuccessful: StateFlow<Boolean> = _isRegisterSuccessful

    // Estado de error
    var isError by mutableStateOf(false)
        private set

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Mensajes de debug
    private val _debugMessage = MutableStateFlow<String>("")
    val debugMessage: StateFlow<String> = _debugMessage

    // Patrones de validación
    private val nameRegex = "^[A-Za-zÁ-ÿ\\s]{2,}$"
    private val dniRegex = "^[0-9]{8}[A-Z]$"
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    private val phoneRegex = "^[0-9]{9}$"
    private val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[.@#$%^&+=!*()_\\-])(?=\\S+$).{8,}$"

    // Funciones de formato
    fun formatName(input: String): String {
        return input.trim().split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }
    }

    // Validaciones
    fun isValidName(name: String): Boolean {
        return name.matches(Regex(nameRegex))
    }

    fun isValidEmail(email: String): Boolean {
        return Pattern.matches(emailRegex, email)
    }

    fun isValidDNI(dni: String): Boolean {
        if (!dni.matches(Regex(dniRegex))) return false

        val letters = "TRWAGMYFPDXBNJZSQVHLCKE"
        val number = dni.substring(0, 8).toInt()
        val letter = dni.last()

        return letters[number % 23] == letter
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex(phoneRegex))
    }

    fun isValidPassword(password: String): Boolean {
        // Verificar longitud mínima
        if (password.length < 8) return false

        // Verificar que contiene al menos un dígito
        if (!password.any { it.isDigit() }) return false

        // Verificar que contiene al menos una letra minúscula
        if (!password.any { it.isLowerCase() }) return false

        // Verificar que contiene al menos una letra mayúscula
        if (!password.any { it.isUpperCase() }) return false

        // Verificar que contiene al menos un carácter especial
        val specialChars = "!@#$%^&*()_-+={}[]|:;'<>,.?/~`\\"
        if (!password.any { it in specialChars || it == '.' }) return false

        // Verificar que no contiene espacios
        if (password.contains(" ")) return false

        return true
    }

    fun doPasswordsMatch(): Boolean {
        return password == confirmPassword && password.isNotEmpty()
    }

    fun isValidNombreOrganizacion(nombre: String): Boolean {
        // Permitir espacios en el nombre de la organización
        // Solo verificar que no esté vacío y tenga al menos 3 caracteres en total
        return nombre.trim().isNotEmpty() && nombre.trim().length >= 3
    }

    // Validación de campos
    fun validateField(field: String, value: String) {
        when (field) {
            "name" -> isNameError = !isValidName(value)
            "apellido1" -> isApellido1Error = !isValidName(value)
            "apellido2" -> isApellido2Error = !isValidName(value)
            "email" -> isEmailError = !isValidEmail(value)
            "dni" -> isDniError = !isValidDNI(value)
            "telefono" -> isTelefonoError = !isValidPhone(value)
            "password" -> isPasswordError = !isValidPassword(value)
            "confirmPassword" -> isConfirmPasswordError = !doPasswordsMatch()
            "nombreOrganizacion" -> isNombreOrganizacionError = !isValidNombreOrganizacion(value)
            "telefonoContacto" -> isTelefonoContactoError = !isValidPhone(value)
        }
    }

    fun validateAllFields(): Boolean {
        var isValid = true

        // Validar campos básicos
        if (name.isBlank()) {
            isNameError = true
            nameErrorMessage = "El nombre es requerido"
            isValid = false
            mostrarMensaje("ERROR: Nombre vacío")
        } else {
            isNameError = false
            nameErrorMessage = ""
        }

        if (apellido1.isBlank()) {
            isApellido1Error = true
            apellido1ErrorMessage = "El primer apellido es requerido"
            isValid = false
            mostrarMensaje("ERROR: Apellido1 vacío")
        } else {
            isApellido1Error = false
            apellido1ErrorMessage = ""
        }

        // Validar email
        if (email.isBlank()) {
            isEmailError = true
            emailErrorMessage = "El email es requerido"
            isValid = false
            mostrarMensaje("ERROR: Email vacío")
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            isEmailError = true
            emailErrorMessage = "Email inválido"
            isValid = false
            mostrarMensaje("ERROR: Email inválido: $email")
        } else {
            isEmailError = false
            emailErrorMessage = ""
        }

        // Validar contraseña
        if (password.isBlank()) {
            isPasswordError = true
            passwordErrorMessage = "La contraseña es requerida"
            isValid = false
            mostrarMensaje("ERROR: Contraseña vacía")
        } else if (password.length < 6) {
            isPasswordError = true
            passwordErrorMessage = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
            mostrarMensaje("ERROR: Contraseña demasiado corta: ${password.length} caracteres")
        } else {
            isPasswordError = false
            passwordErrorMessage = ""
        }

        // Validar confirmación de contraseña
        if (confirmPassword != password) {
            isConfirmPasswordError = true
            confirmPasswordErrorMessage = "Las contraseñas no coinciden"
            isValid = false
            mostrarMensaje("ERROR: Las contraseñas no coinciden")
        } else {
            isConfirmPasswordError = false
            confirmPasswordErrorMessage = ""
        }

        // Validar campos específicos según el rol
        when (role.lowercase()) {
            "organizador" -> {
                val validOrg = validateOrganizadorFields()
                if (!validOrg) {
                    mostrarMensaje("ERROR: Validación de campos de organizador falló")
                }
                isValid = isValid && validOrg
            }
            "participante" -> {
                val validPart = validateParticipanteFields()
                if (!validPart) {
                    mostrarMensaje("ERROR: Validación de campos de participante falló")
                }
                isValid = isValid && validPart
            }
        }

        mostrarMensaje("Resultado final de validación: ${if (isValid) "VÁLIDO" else "INVÁLIDO"}")
        return isValid
    }

    fun validateOrganizadorFields(): Boolean {
        var isValid = true

        if (nombreOrganizacion.isBlank()) {
            isNombreOrganizacionError = true
            nombreOrganizacionErrorMessage = "El nombre de la organización es requerido"
            isValid = false
        } else {
            isNombreOrganizacionError = false
            nombreOrganizacionErrorMessage = ""
        }

        if (telefonoContacto.isBlank()) {
            isTelefonoContactoError = true
            telefonoContactoErrorMessage = "El teléfono de contacto es requerido"
            isValid = false
        } else if (!telefonoContacto.matches(Regex("^[0-9]{9}$"))) {
            isTelefonoContactoError = true
            telefonoContactoErrorMessage = "El teléfono debe tener 9 dígitos"
            isValid = false
        } else {
            isTelefonoContactoError = false
            telefonoContactoErrorMessage = ""
        }

        return isValid
    }

    fun validateParticipanteFields(): Boolean {
        var isValid = true

        if (dni.isBlank()) {
            isDniError = true
            dniErrorMessage = "El DNI es requerido"
            isValid = false
            mostrarMensaje("ERROR: DNI vacío")
        } else if (!dni.matches(Regex("^[0-9]{8}[A-Z]$"))) {
            isDniError = true
            dniErrorMessage = "DNI inválido (8 números y 1 letra)"
            isValid = false
            mostrarMensaje("ERROR: Formato de DNI inválido: $dni")
        } else {
            // Validar la letra del DNI
            val letters = "TRWAGMYFPDXBNJZSQVHLCKE"
            val number = dni.substring(0, 8).toInt()
            val letter = dni.last()
            if (letters[number % 23] != letter) {
                isDniError = true
                dniErrorMessage = "La letra del DNI no es correcta"
                isValid = false
                mostrarMensaje("ERROR: Letra de DNI incorrecta. Calculada: ${letters[number % 23]}, Proporcionada: $letter")
            } else {
                isDniError = false
                dniErrorMessage = ""
            }
        }

        if (telefono.isBlank()) {
            isTelefonoError = true
            telefonoErrorMessage = "El teléfono es requerido"
            isValid = false
            mostrarMensaje("ERROR: Teléfono vacío")
        } else if (!telefono.matches(Regex("^[0-9]{9}$"))) {
            isTelefonoError = true
            telefonoErrorMessage = "El teléfono debe tener 9 dígitos"
            isValid = false
            mostrarMensaje("ERROR: Formato de teléfono inválido: $telefono (longitud: ${telefono.length})")
        } else {
            isTelefonoError = false
            telefonoErrorMessage = ""
        }

        return isValid
    }

    // Función para limpiar error
    fun clearError() {
        isError = false
        _errorMessage.value = null
    }

    // Función para establecer error
    fun setError(message: String) {
        isError = true
        _errorMessage.value = message
    }

    private fun clearFields() {
        name = ""
        apellido1 = ""
        apellido2 = ""
        email = ""
        password = ""
        confirmPassword = ""
        nombreOrganizacion = ""
        telefonoContacto = ""
        dni = ""
        telefono = ""

        // Limpiar estados de error
        isNameError = false
        isApellido1Error = false
        isApellido2Error = false
        isEmailError = false
        isPasswordError = false
        isConfirmPasswordError = false
        isNombreOrganizacionError = false
        isTelefonoContactoError = false
        isDniError = false
        isTelefonoError = false

        // Limpiar mensajes de error
        nameErrorMessage = ""
        apellido1ErrorMessage = ""
        apellido2ErrorMessage = ""
        emailErrorMessage = ""
        passwordErrorMessage = ""
        confirmPasswordErrorMessage = ""
        nombreOrganizacionErrorMessage = ""
        telefonoContactoErrorMessage = ""
        dniErrorMessage = ""
        telefonoErrorMessage = ""
    }

    fun mostrarMensaje(mensaje: String) {
        _debugMessage.value = mensaje
        Log.d("REGISTRO_DEBUG", mensaje)
    }

    fun mapRoleToBackend(localizedRole: String): String {
        return when (localizedRole.lowercase()) {
            "organizer", "organizador", "organisateur", "organizzatore" -> "organizador"
            "participant", "participante", "participe", "partecipante" -> "participante"
            else -> localizedRole.lowercase()
        }
    }

    fun onRegisterClick() {
        viewModelScope.launch {
            try {
                // Mapear el rol antes de cualquier validación o uso
                role = mapRoleToBackend(role)
                mostrarMensaje("Iniciando registro de $role...")
                
                isLoading = true
                clearError()
                
                if (!validateAllFields()) {
                    setError("Por favor, corrige los errores en el formulario")
                    return@launch
                }

                // Asegurarse de que el rol siempre esté en minúsculas
                role = role.lowercase()
                
                // Crear objeto de solicitud según el rol
                val registerRequest = when (role) {
                    "participante" -> {
                        mostrarMensaje("Creando solicitud para participante")
                        RegisterRequest.createParticipante(
                            nombre = name,
                            apellido1 = apellido1,
                            apellido2 = apellido2 ?: "",
                            email = email,
                            password = password,
                            dni = dni,
                            telefono = telefono
                        )
                    }
                    "organizador" -> {
                        mostrarMensaje("Creando solicitud para organizador")
                        RegisterRequest.createOrganizador(
                            nombre = name,
                            apellido1 = apellido1,
                            apellido2 = apellido2 ?: "",
                            email = email,
                            password = password,
                            nombreOrganizacion = nombreOrganizacion,
                            telefonoContacto = telefonoContacto
                        )
                    }
                    else -> {
                        mostrarMensaje("Rol no reconocido: ${role}")
                        setError("Rol no válido")
                        isLoading = false
                        return@launch
                    }
                }

                // Mostrar los valores exactos que se están enviando
                mostrarMensaje("Enviando solicitud de registro con role: ${registerRequest.role}")
                mostrarMensaje("nombreOrganizacion: '${registerRequest.nombreOrganizacion}' (longitud: ${registerRequest.nombreOrganizacion.length})")
                mostrarMensaje("telefonoContacto: '${registerRequest.telefonoContacto}' (longitud: ${registerRequest.telefonoContacto.length})")
                mostrarMensaje("JSON completo: ${Gson().toJson(registerRequest)}")
                
                try {
                    val response = RetrofitClient.apiService.registerUser(registerRequest)
                    
                    if (response.isSuccessful) {
                        val registerResponse = response.body()
                        mostrarMensaje("Registro exitoso: ${registerResponse}")
                        
                        // Guardar token y datos de usuario en SessionManager
                        if (registerResponse != null) {
                            // Determinar qué token usar (puede ser token o accessToken)
                            val tokenToSave = when {
                                registerResponse.token != null -> {
                                    mostrarMensaje("Usando 'token' de la respuesta")
                                    registerResponse.token
                                }
                                registerResponse.accessToken != null -> {
                                    mostrarMensaje("Usando 'access_token' de la respuesta")
                                    registerResponse.accessToken
                                }
                                else -> {
                                    mostrarMensaje("No se encontró token en la respuesta, intentando iniciar sesión")
                                    // Si no hay token, intentamos hacer login automáticamente
                                    loginAfterRegistration()
                                    null
                                }
                            }
                            
                            // Si tenemos un token, guardarlo
                            if (tokenToSave != null) {
                                mostrarMensaje("Guardando token: ${tokenToSave?.safeSubstring()}")
                                com.example.app.util.SessionManager.saveToken(tokenToSave)
                                
                                // Guardar rol de usuario
                                mostrarMensaje("Guardando rol: $role")
                                com.example.app.util.SessionManager.saveUserRole(role.lowercase())
                            }
                            
                            // Guardar user_id si está disponible
                            if (registerResponse.user != null) {
                                mostrarMensaje("Usuario registrado con ID: ${registerResponse.user.id}")
                            }
                        } else {
                            mostrarMensaje("Respuesta vacía del servidor")
                        }
                        
                        _isRegisterSuccessful.value = true
                        clearFields()
                    } else {
                        val errorCode = response.code()
                        val errorBody = response.errorBody()?.string()
                        mostrarMensaje("Error en el registro: Código $errorCode - Body: $errorBody")
                        
                        when (errorCode) {
                            422 -> {
                                // Unprocessable Content - Validación fallida
                                try {
                                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                    mostrarMensaje("Error response parseada: $errorResponse")
                                    mostrarMensaje("Mensajes de validación: ${errorResponse.messages}")
                                    
                                    val mensajeError = when {
                                        !errorResponse.messages.isNullOrEmpty() -> {
                                            val errores = mutableListOf<String>()
                                            errorResponse.messages.forEach { (campo, msgs) ->
                                                errores.add("${campo}: ${msgs.joinToString()}")
                                            }
                                            errores.joinToString("\n")
                                        }
                                        errorResponse.message != null -> errorResponse.message
                                        errorResponse.error != null -> errorResponse.error
                                        else -> "Error de validación. Revisa tus datos."
                                    }
                                    
                                    setError(mensajeError)
                                    mostrarMensaje("Error de validación desglosado: $mensajeError")
                                } catch (e: Exception) {
                                    setError("Error de validación: ${e.message}")
                                    mostrarMensaje("Error al procesar respuesta de error: ${e.message}")
                                    mostrarMensaje("Error body original: $errorBody")
                                }
                            }
                            409 -> setError("El correo electrónico ya está registrado")
                            500 -> setError("Error en el servidor. Inténtalo más tarde")
                            else -> setError("Error en el registro: ${response.message()}")
                        }
                    }
                } catch (e: Exception) {
                    mostrarMensaje("Excepción al hacer la petición: ${e.message}")
                    setError("Error de conexión: ${e.message}")
                } finally {
                    isLoading = false
                }
            } catch (e: Exception) {
                mostrarMensaje("Error general: ${e.message}")
                setError("Error de conexión: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun getPasswordRequirements(): List<String> {
        return listOf(
            "Al menos 8 caracteres",
            "Al menos una letra mayúscula",
            "Al menos una letra minúscula",
            "Al menos un número",
            "Al menos un carácter especial (@#$%^&+=)"
        )
    }

    // Función para resetear el estado de registro exitoso
    fun resetSuccessState() {
        _isRegisterSuccessful.value = false
    }

    fun registerParticipante() {
        viewModelScope.launch {
            isLoading = true
            mostrarMensaje("==== INICIANDO REGISTRO DE PARTICIPANTE ====")
            Log.d("REGISTRO_DEBUG", "==== INICIANDO REGISTRO DE PARTICIPANTE ====")
            Log.d("REGISTRO_DEBUG", "Email: $email, Nombre: $name, Apellido1: $apellido1")
            Log.d("REGISTRO_DEBUG", "DNI: $dni, Teléfono: $telefono")
            Log.d("REGISTRO_DEBUG", "¿Es de Google Auth? $isFromGoogleAuth, Token Google: ${googleToken?.safeSubstring()}")
            
            // Validar todos los campos antes del registro
            val camposBasicosValidos = validarCamposBasicos()
            
            // Validar campos específicos de participante
            if (!validateParticipanteFields()) {
                Log.e("REGISTRO_DEBUG", "ERROR: La validación de campos de participante falló")
                Log.e("REGISTRO_DEBUG", "isDniError: $isDniError, isTelefonoError: $isTelefonoError")
                isLoading = false
                return@launch
            }
            
            if (!camposBasicosValidos) {
                Log.e("REGISTRO_DEBUG", "ERROR: La validación de campos básicos falló")
                isLoading = false
                return@launch
            }
            
            try {
                // Preparar datos de registro
                val request: RegisterRequest
                
                if (isFromGoogleAuth && googleToken != null) {
                    // Crear solicitud con información de Google Auth
                    Log.d("REGISTRO_DEBUG", "Registrando con datos de Google")
                    
                    // Verificar que tengamos todos los datos necesarios
                    if (name.isBlank() || apellido1.isBlank() || email.isBlank()) {
                        Log.e("REGISTRO_DEBUG", "ERROR: Faltan datos básicos de usuario para registro con Google")
                        setError("Faltan datos básicos del usuario. Por favor, inicie sesión con Google nuevamente.")
                        isLoading = false
                        return@launch
                    }
                    
                    request = RegisterRequest.createWithGoogleAuth(
                        nombre = name,
                        apellido1 = apellido1,
                        apellido2 = apellido2,
                        email = email,
                        googleToken = googleToken,
                        dni = dni,
                        telefono = telefono,
                        role = "participante"
                    )
                    Log.d("REGISTRO_DEBUG", "Request con Google creada correctamente")
                    Log.d("REGISTRO_DEBUG", "Detalles de request con Google: ${Gson().toJson(request)}")
                } else {
                    // Si es registro normal pero está faltando algún campo obligatorio
                    if (name.isBlank() || apellido1.isBlank() || email.isBlank() || password.isBlank()) {
                        Log.e("REGISTRO_DEBUG", "ERROR: Campos obligatorios vacíos en registro normal")
                        mostrarMensaje("Faltan datos obligatorios para el registro. Por favor complete todos los campos.")
                        setError("Faltan datos obligatorios: Nombre, Apellido, Email y Contraseña son requeridos")
                        isLoading = false
                        return@launch
                    }
                    
                    // Registro normal
                    Log.d("REGISTRO_DEBUG", "Registrando con formulario normal")
                    request = RegisterRequest.createParticipante(
                        nombre = name,
                        apellido1 = apellido1,
                        apellido2 = apellido2,
                        email = email,
                        password = password,
                        dni = dni,
                        telefono = telefono
                    )
                    Log.d("REGISTRO_DEBUG", "Request normal creada correctamente")
                    Log.d("REGISTRO_DEBUG", "Detalles de request normal: ${Gson().toJson(request)}")
                }
                
                // Verificar que los campos obligatorios tengan valor
                val camposFaltantes = mutableListOf<String>()
                if (request.nombre.isBlank()) camposFaltantes.add("Nombre")
                if (request.apellido1.isBlank()) camposFaltantes.add("Apellido")
                if (request.email.isBlank()) camposFaltantes.add("Email")
                if (request.password.isNullOrBlank() && (request.googleToken.isNullOrBlank() || !isFromGoogleAuth)) camposFaltantes.add("Contraseña")
                
                if (camposFaltantes.isNotEmpty()) {
                    Log.e("REGISTRO_DEBUG", "ERROR: Faltan campos obligatorios en la petición: $camposFaltantes")
                    mostrarMensaje("Faltan campos obligatorios: ${camposFaltantes.joinToString(", ")}")
                    setError("Por favor complete los siguientes campos: ${camposFaltantes.joinToString(", ")}")
                    isLoading = false
                    return@launch
                }
                
                // Realizar la solicitud al servidor
                Log.d("REGISTRO_DEBUG", "Enviando solicitud al servidor: ${Gson().toJson(request)}")
                
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        Log.d("REGISTRO_DEBUG", "Ejecutando llamada a registerUser...")
                        val resp = RetrofitClient.apiService.registerUser(request)
                        Log.d("REGISTRO_DEBUG", "Llamada completada, código: ${resp.code()}")
                        resp
                    } catch (e: Exception) {
                        Log.e("REGISTRO_DEBUG", "EXCEPCIÓN en llamada a registerUser: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }
                }
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    
                    if (response.isSuccessful) {
                        val registrationResponse = response.body()
                        Log.d("REGISTRO_DEBUG", "Registro EXITOSO: ${Gson().toJson(registrationResponse)}")
                        mostrarMensaje("Registro exitoso: ${Gson().toJson(registrationResponse)}")
                        
                        // Si hay un token en la respuesta, guardarlo (para cualquier tipo de registro)
                        if (registrationResponse?.token != null) {
                            Log.d("REGISTRO_DEBUG", "Guardando token de respuesta: ${registrationResponse.token?.safeSubstring()}")
                            com.example.app.util.SessionManager.saveToken(registrationResponse.token)
                            com.example.app.util.SessionManager.saveUserRole("participante")
                        } else if (isFromGoogleAuth && googleToken != null) {
                            // Si no hay token en la respuesta pero estamos usando Google Auth, usar ese token
                            Log.d("REGISTRO_DEBUG", "Usando token de Google: ${googleToken?.safeSubstring()}")
                            com.example.app.util.SessionManager.saveToken(googleToken!!)
                            com.example.app.util.SessionManager.saveUserRole("participante")
                        }
                        
                        Log.d("REGISTRO_DEBUG", "Estableciendo isRegisterSuccessful a true")
                        _isRegisterSuccessful.value = true
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("REGISTRO_DEBUG", "ERROR en el registro: ${response.code()} - $errorBody")
                        mostrarMensaje("Error en el registro: $errorBody")
                        
                        try {
                            if (errorBody != null) {
                                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                                val errorMsg = when {
                                    errorResponse.message != null -> errorResponse.message
                                    errorResponse.error != null -> errorResponse.error
                                    !errorResponse.messages.isNullOrEmpty() -> {
                                        val errores = mutableListOf<String>()
                                        errorResponse.messages.forEach { (campo, msgs) ->
                                            errores.add("${campo}: ${msgs.joinToString()}")
                                        }
                                        errores.joinToString("\n")
                                    }
                                    else -> "Error en el registro"
                                }
                                Log.e("REGISTRO_DEBUG", "Mensaje de error: $errorMsg")
                                setError(errorMsg)
                            } else {
                                Log.e("REGISTRO_DEBUG", "Error sin cuerpo: ${response.code()}")
                                setError("Error en el registro: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("REGISTRO_DEBUG", "Error al procesar respuesta de error: ${e.message}")
                            setError("Error en el registro: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("REGISTRO_DEBUG", "EXCEPCIÓN durante el registro: ${e.message}")
                e.printStackTrace()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoading = false
                    setError("Error de conexión: ${e.message ?: "Error desconocido"}")
                    mostrarMensaje("Excepción durante el registro: ${e.message}")
                }
            } finally {
                Log.d("REGISTRO_DEBUG", "==== FINALIZANDO REGISTRO DE PARTICIPANTE ====")
            }
        }
    }

    // Método adicional para validar campos básicos
    private fun validarCamposBasicos(): Boolean {
        val camposFaltantes = mutableListOf<String>()
        
        // En registro con Google, no validamos password
        if (!isFromGoogleAuth) {
            if (password.isBlank()) {
                camposFaltantes.add("Contraseña")
                isPasswordError = true
                passwordErrorMessage = "La contraseña es obligatoria"
            } else {
                isPasswordError = false
                passwordErrorMessage = ""
            }
        }
        
        // Validar resto de campos básicos
        if (name.isBlank()) {
            camposFaltantes.add("Nombre")
            isNameError = true
            nameErrorMessage = "El nombre es obligatorio"
        } else {
            isNameError = false
            nameErrorMessage = ""
        }
        
        if (apellido1.isBlank()) {
            camposFaltantes.add("Primer apellido")
            isApellido1Error = true
            apellido1ErrorMessage = "El primer apellido es obligatorio"
        } else {
            isApellido1Error = false
            apellido1ErrorMessage = ""
        }
        
        if (email.isBlank()) {
            camposFaltantes.add("Email")
            isEmailError = true
            emailErrorMessage = "El email es obligatorio"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            camposFaltantes.add("Email válido")
            isEmailError = true
            emailErrorMessage = "Email inválido"
        } else {
            isEmailError = false
            emailErrorMessage = ""
        }
        
        if (camposFaltantes.isNotEmpty()) {
            Log.e("REGISTRO_DEBUG", "ERROR: Faltan campos básicos: ${camposFaltantes.joinToString(", ")}")
            setError("Por favor complete los siguientes campos: ${camposFaltantes.joinToString(", ")}")
            return false
        }
        
        return true
    }

    private fun loginAfterRegistration() {
        viewModelScope.launch {
            try {
                mostrarMensaje("Intentando login automático después del registro")
                isLoading = true
                
                // Crear objeto de login con las credenciales del registro
                val loginRequest = com.example.app.model.login.LoginRequest(
                    email = email,
                    password = password
                )
                
                // Realizar petición de login
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.app.api.RetrofitClient.apiService.loginUser(loginRequest)
                }
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    mostrarMensaje("Login automático exitoso: $loginResponse")
                    
                    if (loginResponse != null) {
                        // Obtener token
                        val token = loginResponse.accessToken ?: loginResponse.token
                        
                        if (token != null) {
                            mostrarMensaje("Guardando token del login: ${token?.safeSubstring()}")
                            com.example.app.util.SessionManager.saveToken(token)
                            
                            // Guardar rol
                            val userRole = role.lowercase()
                            mostrarMensaje("Guardando rol del login: $userRole")
                            com.example.app.util.SessionManager.saveUserRole(userRole)
                        } else {
                            mostrarMensaje("No se encontró token en la respuesta de login")
                        }
                    }
                } else {
                    mostrarMensaje("Error en login automático: ${response.code()}")
                }
            } catch (e: Exception) {
                mostrarMensaje("Error al realizar login automático: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun setGoogleAuthData(
        email: String, 
        name: String, 
        apellido1: String, 
        apellido2: String?, 
        token: String?
    ) {
        Log.d("REGISTRO_DEBUG", "==== SETTING GOOGLE AUTH DATA ====")
        Log.d("REGISTRO_DEBUG", "Email: $email")
        Log.d("REGISTRO_DEBUG", "Nombre: $name")
        Log.d("REGISTRO_DEBUG", "Apellido1: $apellido1")
        Log.d("REGISTRO_DEBUG", "Apellido2: ${apellido2 ?: "null"}")
        Log.d("REGISTRO_DEBUG", "Token: ${token?.safeSubstring()}")
        
        if (email.isBlank() || name.isBlank() || apellido1.isBlank()) {
            Log.e("REGISTRO_DEBUG", "ERROR: Datos incompletos de Google Auth")
            setError("No se recibieron todos los datos necesarios del login con Google")
            return
        }
        
        // Guardar los datos del usuario de Google
        this.email = email
        this.name = name
        this.apellido1 = apellido1
        this.apellido2 = apellido2 ?: ""
        this.googleToken = token
        this.isFromGoogleAuth = true

        // Generar una contraseña segura para Google Auth
        this.password = generateRandomPassword()
        this.confirmPassword = this.password
        Log.d("REGISTRO_DEBUG", "Generada contraseña aleatoria para Google Auth: ${this.password.safeSubstring()}")
        
        // Verificar si el token está en SessionManager o si necesitamos guardarlo
        if (token != null && token.isNotEmpty()) {
            try {
                Log.d("REGISTRO_DEBUG", "Guardando token de Google en SessionManager")
                com.example.app.util.SessionManager.saveToken(token)
            } catch (e: Exception) {
                Log.e("REGISTRO_DEBUG", "Error al guardar el token en SessionManager: ${e.message}")
            }
        } else {
            Log.w("REGISTRO_DEBUG", "No hay token para guardar en SessionManager")
        }
        
        // Indicar que estamos usando datos de Google
        mostrarMensaje("Datos de Google cargados: $email, $name, $apellido1")
        Log.d("REGISTRO_DEBUG", "Datos de Google establecidos correctamente en el ViewModel")
    }

    // Método para generar contraseña aleatoria segura
    private fun generateRandomPassword(): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9') + "!@#$%^&*()-_=+[]{}|;:,.<>?".toList()
        val passwordLength = 12
        return List(passwordLength) { chars.random() }.joinToString("")
    }

    // Función de utilidad para tomar de forma segura los primeros caracteres de un string posiblemente nulo
    private fun String?.safeSubstring(length: Int = 10): String {
        if (this == null) return "null"
        if (this.isEmpty()) return "vacío"
        if (this.length <= length) return this
        return this.substring(0, length) + "..."
    }
} 