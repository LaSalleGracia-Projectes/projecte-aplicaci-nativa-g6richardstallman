package com.example.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.api.RetrofitClient
import com.example.app.model.Evento
import com.example.app.model.TipoEntrada
import com.example.app.model.TipoEntradaDetalle
import com.example.app.model.CompraRequest
import com.example.app.model.EntradaCompra
import com.example.app.util.SessionManager
import com.example.app.util.isValidEventoId
import com.example.app.util.getEventoIdErrorMessage
import com.example.app.util.toValidEventoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import androidx.core.content.FileProvider
import com.example.app.util.FileUtil
import com.google.gson.Gson
import retrofit2.Response
import java.io.File
import java.io.IOException
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.FileOutputStream
import java.io.InputStream
import com.example.app.model.evento.EventoRequest
import com.example.app.model.evento.TipoEntradaRequest
import com.example.app.model.evento.CategoriaEvento
import com.example.app.model.evento.CrearEventoResponse
import com.example.app.util.Constants

class EditarEventoViewModel : ViewModel() {
    // ID del evento a editar
    var eventoId by mutableStateOf<Int?>(null)
    
    // Datos básicos del evento
    var nombreEvento by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var fechaEvento by mutableStateOf("")
    var hora by mutableStateOf("")
    var ubicacion by mutableStateOf("")
    var categoria by mutableStateOf("")
    var esOnline by mutableStateOf(false)
    
    // URI de la imagen original y la que se toma con la cámara
    var imagen by mutableStateOf<Uri?>(null)
    var imageUri by mutableStateOf<Uri?>(null)
    
    // Datos del formulario
    var titulo by mutableStateOf("")
    var imagenUri by mutableStateOf<Uri?>(null)
    var imagenUrl by mutableStateOf("")
    
    // Tipos de entrada
    var tiposEntrada by mutableStateOf<List<TipoEntradaRequest>>(listOf())
    var tiposEntradaOriginales by mutableStateOf<List<TipoEntradaDetalle>>(listOf())
    
    // Estados de UI
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var success by mutableStateOf(false)
    var eventoLoaded by mutableStateOf(false)
    
    // Estado para las categorías disponibles
    var categorias by mutableStateOf<List<String>>(emptyList())
    var isLoadingCategorias by mutableStateOf(false)
    var errorCategorias by mutableStateOf<String?>(null)

    // Estado de éxito
    private val _isUpdateSuccessful = MutableStateFlow(false)
    val isUpdateSuccessful: StateFlow<Boolean> = _isUpdateSuccessful
    
    // TAG para logs
    private val TAG = "EditarEventoViewModel"

    init {
        cargarCategorias()
    }

    fun cargarEvento(id: Int) {
        Log.d(TAG, "==== INICIANDO CARGA DE EVENTO PARA EDICIÓN ====")
        Log.d(TAG, "Evento ID recibido: $id (tipo: ${id.javaClass.name})")
        
        if (id <= 0) {
            Log.e(TAG, "Error: ID de evento inválido: $id")
            updateError("ID de evento inválido ($id)")
            return
        }
        
        eventoId = id
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                Log.d(TAG, "Iniciando carga de datos del evento #$id")
                
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "Error: Token no disponible")
                    updateError("No hay sesión activa. Por favor, inicia sesión.")
                    return@launch
                }

                Log.d(TAG, "Token disponible: ${token.take(10)}... [${token.length} caracteres]")
                
                // Obtener datos del evento desde la API
                Log.d(TAG, "Realizando petición a API para obtener evento #$id")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getEventoById(id.toString())
                }
                
                Log.d(TAG, "Respuesta recibida - Código: ${response.code()}, Exitosa: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val evento = response.body()?.evento
                    if (evento != null) {
                        Log.d(TAG, "Datos de evento recibidos exitosamente:")
                        Log.d(TAG, "ID: ${evento.id}, Título: ${evento.titulo}")
                        Log.d(TAG, "Fecha: ${evento.fechaEvento}, Hora: ${evento.hora}")
                        Log.d(TAG, "Categoría: ${evento.categoria}, Online: ${evento.esOnline}")
                        
                        // Llenar los datos del formulario
                        nombreEvento = evento.titulo ?: ""
                        titulo = evento.titulo ?: ""
                        descripcion = evento.descripcion ?: ""
                        fechaEvento = evento.fechaEvento ?: ""
                        hora = formatearHora(evento.hora ?: "")
                        ubicacion = evento.ubicacion ?: ""
                        categoria = evento.categoria ?: ""
                        esOnline = evento.esOnline ?: false
                        imagenUrl = evento.imagen ?: ""

                        // Cargar tipos de entrada
                        cargarTiposEntrada(id)
                        
                        eventoLoaded = true
                        Log.d(TAG, "Evento cargado correctamente")
                    } else {
                        Log.e(TAG, "Error: Respuesta exitosa pero el evento es nulo")
                        updateError("No se pudieron obtener los datos del evento")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al cargar evento:\nCódigo: ${response.code()}\nError: $errorBody")
                    updateError("Error al cargar el evento: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al cargar evento", e)
                updateError("Error al cargar el evento: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun cargarTiposEntrada(idEvento: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando carga de tipos de entrada para evento ID: $idEvento")
                
                if (idEvento <= 0) {
                    Log.e(TAG, "Error: ID de evento inválido ($idEvento) al cargar tipos de entrada")
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getTiposEntrada(idEvento.toString())
                }
                
                Log.d(TAG, "Respuesta de tipos de entrada recibida con código: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val tiposEntradaAPI = response.body()?.tiposEntrada ?: emptyList()
                    Log.d(TAG, "Tipos de entrada recibidos: ${tiposEntradaAPI.size}")

                    // Guardar los tipos originales para referencia
                    tiposEntradaOriginales = tiposEntradaAPI.map { tipo ->
                        TipoEntradaDetalle(
                            id = tipo.getEffectiveId(),
                            idEvento = tipo.idEvento ?: 0,
                            nombre = tipo.nombre ?: "",
                            precio = tipo.precio ?: "0",
                            cantidadDisponible = tipo.cantidadDisponible,
                            entradasVendidas = tipo.entradasVendidas ?: 0,
                            descripcion = tipo.descripcion,
                            esIlimitado = tipo.esIlimitado ?: false,
                            activo = tipo.activo ?: true,
                            disponibilidad = tipo.disponibilidad
                        )
                    }
                    
                    // Convertir a TipoEntradaRequest
                    tiposEntrada = tiposEntradaAPI.map { tipo ->
                        TipoEntradaRequest(
                            nombre = tipo.nombre ?: "",
                            precio = tipo.precio?.toDoubleOrNull() ?: 0.0,
                            cantidadDisponible = tipo.cantidadDisponible,
                            descripcion = tipo.descripcion,
                            esIlimitado = tipo.esIlimitado ?: false
                        )
                    }
                    
                    Log.d(TAG, "Tipos de entrada convertidos correctamente: ${tiposEntrada.size}")
                    
                    if (tiposEntrada.isEmpty() && !esOnline) {
                        // Si no hay tipos de entrada, agregar uno por defecto para eventos presenciales
                        Log.d(TAG, "No hay tipos de entrada. Agregando uno por defecto")
                        addTipoEntrada()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error al cargar tipos de entrada: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al cargar tipos de entrada", e)
            }
        }
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            isLoadingCategorias = true
            errorCategorias = null
            try {
                // Usar categorías estáticas por ahora
                categorias = listOf(
                    "Festival",
                    "Concierto",
                    "Teatro",
                    "Deportes",
                    "Conferencia",
                    "Exposición",
                    "Taller",
                    "Otro"
                )
                Log.d(TAG, "Categorías cargadas: $categorias")
            } catch (e: Exception) {
                errorCategorias = "Error al cargar las categorías: ${e.message}"
                Log.e(TAG, "Excepción al cargar categorías", e)
            } finally {
                isLoadingCategorias = false
            }
        }
    }

    // Manejo de imagen
    fun onImageSelected(uri: Uri) {
        imagen = uri
        imagenUri = uri // Actualizamos ambas variables para mantener consistencia
    }
    
    // Función para actualizar la URI de la imagen
    fun updateImagenUri(uri: Uri?) {
        imagenUri = uri
        imagen = uri // Actualizamos ambas variables para mantener consistencia
    }
    
    // Funciones para manejar tipos de entrada
    fun addTipoEntrada() {
        val tipoEntrada = TipoEntradaRequest(
            nombre = "General",
            precio = 25.0,
            cantidadDisponible = 100,
            descripcion = "Acceso General Evento",
            esIlimitado = false
        )
        tiposEntrada = tiposEntrada + tipoEntrada
    }
    
    fun removeTipoEntrada(index: Int) {
        if (index >= 0 && index < tiposEntrada.size && tiposEntrada.size > 1) {
            tiposEntrada = tiposEntrada.toMutableList().apply {
                removeAt(index)
            }
        } else if (tiposEntrada.size <= 1) {
            updateError("Debe haber al menos un tipo de entrada")
        }
    }

    fun updateTipoEntrada(index: Int, tipoEntrada: TipoEntradaRequest) {
        if (index >= 0 && index < tiposEntrada.size) {
            tiposEntrada = tiposEntrada.toMutableList().apply {
                set(index, tipoEntrada)
            }
        }
    }

    // Funciones para actualizar los campos del formulario
    fun updateTitulo(newTitulo: String) {
        Log.d(TAG, "Actualizando título: '$newTitulo'")
        titulo = newTitulo.trim()
        nombreEvento = newTitulo.trim() // Actualizar ambas propiedades
        Log.d(TAG, "Título actualizado: '$titulo', nombreEvento: '$nombreEvento'")
    }

    fun updateDescripcion(newDescripcion: String) {
        descripcion = newDescripcion
    }
    
    fun updateFecha(newFecha: String) {
        fechaEvento = formatearFecha(newFecha)
    }
    
    fun updateHora(newHora: String) {
        hora = formatearHora(newHora)
    }
    
    fun updateUbicacion(newUbicacion: String) {
        ubicacion = newUbicacion
    }
    
    fun updateCategoria(newCategoria: String) {
        categoria = newCategoria
    }
    
    fun updateEsOnline(newEsOnline: Boolean) {
        esOnline = newEsOnline
    }

    // Actualizar mensaje de error
    fun updateError(errorMsg: String?) {
        error = errorMsg
        if (errorMsg != null) {
            Log.e(TAG, "ERROR: $errorMsg")
            isLoading = false
        }
    }

    // Función para validar los campos del formulario
    private fun validarCampos(): Boolean {
        val errores = mutableListOf<String>()
        
        // Validar campos obligatorios
        Log.d(TAG, "Validando título: '$titulo', longitud: ${titulo.length}")
        if (titulo.isBlank()) {
            Log.e(TAG, "Error de validación: Título está en blanco")
            errores.add("El título es obligatorio")
        }
        
        Log.d(TAG, "Validando descripción: Longitud ${descripcion.length}")
        if (descripcion.isBlank()) {
            errores.add("La descripción es obligatoria")
        }
        
        if (fechaEvento.isBlank()) {
            errores.add("La fecha es obligatoria")
        } else {
            // Validar que la fecha sea posterior a hoy
            try {
                val formatoFecha = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
                val fechaIngresada = formatoFecha.parse(fechaEvento)
                val hoy = Calendar.getInstance().apply { 
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                
                if (fechaIngresada != null && fechaIngresada.before(hoy)) {
                    errores.add("La fecha debe ser posterior a hoy")
                }
            } catch (e: Exception) {
                errores.add("Formato de fecha incorrecto")
            }
        }
        
        if (hora.isBlank()) {
            errores.add("La hora es obligatoria")
        } else {
            // Validar formato de hora HH:MM
            val horaRegex = "^([01]?[0-9]|2[0-3]):([0-5][0-9])$"
            if (!hora.matches(Regex(horaRegex))) {
                errores.add("Formato de hora incorrecto. Debe ser HH:MM (valor actual: '$hora')")
                Log.e(TAG, "Error de validación: Formato de hora incorrecto: '$hora'. Debe coincidir con el patrón: $horaRegex")
            }
        }
        
        if (ubicacion.isBlank()) {
            errores.add("La ubicación es obligatoria")
        }
        
        if (categoria.isBlank()) {
            errores.add("La categoría es obligatoria")
                }

        // Validar tipos de entrada para eventos presenciales
        if (!esOnline) {
            if (tiposEntrada.isEmpty()) {
                errores.add("Debe especificar al menos un tipo de entrada")
            } else {
                tiposEntrada.forEachIndexed { index, tipo ->
                    if (tipo.nombre.isBlank()) {
                        errores.add("El nombre del tipo de entrada ${index + 1} es obligatorio")
                    }
                    
                    if (tipo.precio <= 0) {
                        errores.add("El precio del tipo de entrada ${index + 1} debe ser mayor a 0")
                    }
                    
                    if (!tipo.esIlimitado && (tipo.cantidadDisponible == null || tipo.cantidadDisponible <= 0)) {
                        errores.add("La cantidad disponible del tipo de entrada ${index + 1} debe ser mayor a 0")
                    }
                }
            }
                    }

        if (errores.isNotEmpty()) {
            val mensajeError = buildString {
                appendLine("Por favor, corrige los siguientes errores:")
                errores.forEach { error ->
                    appendLine(error)
                }
            }
            updateError(mensajeError.trim())
            Log.e(TAG, "Errores de validación: $errores")
            return false
        }

        return true
    }

    // Función para actualizar el evento
    fun actualizarEvento(context: Context) {
        if (eventoId == null) {
            updateError("No se ha especificado un ID de evento")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando actualización de evento #$eventoId")
                Log.d(TAG, "Datos que se enviarán:")
                Log.d(TAG, "- Título: '$titulo'")
                Log.d(TAG, "- Descripción: '${descripcion.take(20)}...'")
                Log.d(TAG, "- Fecha: '$fechaEvento'")
                Log.d(TAG, "- Hora: '$hora'")
                Log.d(TAG, "- Ubicación: '$ubicacion'")
                Log.d(TAG, "- Categoría: '$categoria'")
                Log.d(TAG, "- Es online: $esOnline")
                Log.d(TAG, "- Imagen nueva: ${imagenUri != null}")
                Log.d(TAG, "- Tipos de entrada: ${tiposEntrada.size}")
                
                // Validar que los campos obligatorios estén completos
                if (!validarCampos()) {
                    Log.e(TAG, "Validación de campos fallida")
                    return@launch
                }

                Log.d(TAG, "Validación de campos exitosa")
                
                // Obtener token
                val token = SessionManager.getToken()
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "Error: Token no disponible")
                    updateError("No hay sesión activa. Por favor, inicia sesión.")
                    return@launch
                }

                // Iniciar carga
                isLoading = true
                error = null
                
                // Crear petición según si hay nueva imagen o no
                val response = if (imagenUri != null) {
                    actualizarEventoConImagen(context, token)
                } else {
                    actualizarEventoSinImagen(token)
                    }

                Log.d(TAG, "Código de respuesta: ${response.code()}")

                    if (response.isSuccessful) {
                    val eventoResponse = response.body()
                    Log.d(TAG, "Evento actualizado exitosamente: ${eventoResponse?.message}")
                    _isUpdateSuccessful.value = true
                    } else {
                    // Procesar error de la respuesta
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e(TAG, "Error detallado al actualizar evento:\n- Código: ${response.code()}\n- Cuerpo del error: $errorBody\n- Headers: ${response.headers()}")
                    
                    try {
                        // Intentar parsear el error JSON
                        val errorJson = Gson().fromJson(errorBody, Map::class.java) as? Map<String, Any>
                        Log.e(TAG, "Error response parseado: $errorJson")

                        val errorMessages = errorJson?.get("messages") as? Map<String, List<String>>
                        if (errorMessages != null) {
                            Log.e(TAG, "Errores de validación encontrados: $errorMessages")
                            
                            val erroresFormateados = StringBuilder("Se encontraron los siguientes errores:\n")
                            errorMessages.forEach { (campo, mensajes) ->
                                mensajes.forEach { mensaje ->
                                    erroresFormateados.append("• $campo: $mensaje\n")
                                }
                            }
                            updateError(erroresFormateados.toString().trimEnd())
                        } else {
                            // Si no hay errores específicos de validación, mostrar mensaje general
                            val mensaje = errorJson?.get("message") as? String ?: "Error desconocido"
                            updateError("Error al actualizar el evento: $mensaje")
                    }
                } catch (e: Exception) {
                        Log.e(TAG, "Error al parsear respuesta de error", e)
                        updateError("Error al actualizar el evento: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al actualizar evento", e)
                updateError("Error al actualizar el evento: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun actualizarEventoConImagen(context: Context, token: String): Response<CrearEventoResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Procesar la imagen
                Log.d(TAG, "Procesando imagen para actualización")
                val imagenPart = try {
                    procesarImagen(context, imagenUri!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al procesar imagen: ${e.message}")
                    throw Exception("Error al procesar la imagen: ${e.message}")
                }
                
                // Crear un mapa de datos para el formulario multipart
                val eventoData = HashMap<String, RequestBody>()
                
                // Convertir cada campo de texto a RequestBody
                eventoData["titulo"] = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["descripcion"] = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["fecha"] = fechaEvento.toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["hora"] = hora.toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["ubicacion"] = ubicacion.toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["categoria"] = categoria.toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["es_online"] = (if (esOnline) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
                eventoData["_method"] = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
                
                // Para eventos presenciales, manejar los tipos de entrada
                val partesTiposEntradas = mutableListOf<MultipartBody.Part>()
                
                if (!esOnline) {
                    // Procesar cada tipo de entrada
                    tiposEntrada.forEachIndexed { index, tipo ->
                        // Crear las partes del formulario para los tipos de entrada
                        partesTiposEntradas.add(
                            MultipartBody.Part.createFormData(
                                "tipos_entrada[$index][nombre]", 
                                tipo.nombre
                            )
                        )
                        partesTiposEntradas.add(
                            MultipartBody.Part.createFormData(
                                "tipos_entrada[$index][precio]", 
                                tipo.precio.toString()
                            )
                        )
                        partesTiposEntradas.add(
                            MultipartBody.Part.createFormData(
                                "tipos_entrada[$index][descripcion]", 
                                tipo.descripcion ?: "Acceso General Evento"
                            )
                        )
                        partesTiposEntradas.add(
                            MultipartBody.Part.createFormData(
                                "tipos_entrada[$index][cantidad_disponible]", 
                                (tipo.cantidadDisponible ?: 100).toString()
                            )
                        )
                        partesTiposEntradas.add(
                            MultipartBody.Part.createFormData(
                                "tipos_entrada[$index][es_ilimitado]", 
                                if (tipo.esIlimitado) "1" else "0"
                            )
                        )
                    }
                }
                
                // Llamar a la API con el formato de Postman
                RetrofitClient.apiService.actualizarEventoConImagen(
                    id = eventoId.toString(),
                                token = "Bearer $token",
                    titulo = eventoData["titulo"]!!,
                    descripcion = eventoData["descripcion"]!!,
                    fecha = eventoData["fecha"]!!,
                    hora = eventoData["hora"]!!,
                    ubicacion = eventoData["ubicacion"]!!,
                    categoria = eventoData["categoria"]!!,
                    esOnline = eventoData["es_online"]!!,
                    tiposEntradas = partesTiposEntradas,
                    imagen = imagenPart,
                    method = eventoData["_method"]!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar evento con imagen: ${e.message}")
                throw e
            }
                        }
                    }

    private suspend fun actualizarEventoSinImagen(token: String): Response<CrearEventoResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Crear un mapa con los datos en el formato exacto que espera el API
                val eventoData = HashMap<String, @JvmSuppressWildcards Any>()
                eventoData["nombreEvento"] = titulo
                eventoData["descripcion"] = descripcion
                eventoData["fechaEvento"] = fechaEvento
                eventoData["hora"] = hora
                eventoData["ubicacion"] = ubicacion
                eventoData["categoria"] = categoria
                eventoData["es_online"] = esOnline
                
                // Preparar tipos de entrada en el formato que necesita Postman
                if (!esOnline) {
                    val tiposList = tiposEntrada.mapIndexed { index, tipo ->
                        mapOf(
                            "nombre" to tipo.nombre,
                            "precio" to tipo.precio,
                            "descripcion" to (tipo.descripcion ?: "Acceso General Evento"),
                            "es_ilimitado" to tipo.esIlimitado,
                            "cantidad_disponible" to (tipo.cantidadDisponible ?: 100)
                        )
                    }
                    eventoData["tipos_entrada"] = tiposList
                }
                
                Log.d(TAG, "Enviando datos para actualizar evento usando PUT: ${Gson().toJson(eventoData)}")
                
                // Llamar al método PUT para modificar el evento
                RetrofitClient.apiService.modificarEvento(
                    id = eventoId.toString(),
                    token = "Bearer $token",
                    eventoData = eventoData
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar evento sin imagen: ${e.message}")
                throw e
            }
        }
    }
    
    private fun procesarImagen(context: Context, uri: Uri): MultipartBody.Part {
        try {
            Log.d(TAG, "Procesando imagen para actualización")
            
            // Obtener información del archivo
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 2 * 1024 * 1024) { // 2MB
                            throw Exception("La imagen no puede pesar más de 2MB")
                        }
                    }
                }
            }

            // Obtener el tipo MIME
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (mimeType !in listOf("image/jpeg", "image/png", "image/gif")) {
                throw Exception("El tipo de archivo no es una imagen válida")
            }
            
            // Crear un archivo temporal real
            val tempFile = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
            
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            // Crear el RequestBody a partir del archivo real
            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            return MultipartBody.Part.createFormData("imagen", tempFile.name, requestFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar la imagen", e)
            throw Exception("Error al procesar la imagen: ${e.message}")
        }
    }
    
    fun resetUpdateState() {
        _isUpdateSuccessful.value = false
    }

    // Función para crear URI para la cámara
    fun createImageUri(context: Context): Uri? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timeStamp.jpg"
            val file = File(context.cacheDir, fileName)
            
            // Usar FileProvider en lugar de Uri.fromFile
            val authority = "${context.packageName}.provider"
            imageUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            
            return imageUri
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear URI para imagen", e)
            return null
        }
    }

    // Función para formatear la fecha asegurando que siga el formato yyyy-MM-dd
    private fun formatearFecha(fechaOriginal: String): String {
        return try {
            // Dividir por guiones para asegurar formato yyyy-MM-dd
            Log.d(TAG, "Formateando fecha original: '$fechaOriginal'")
            
            // Si la fecha ya tiene el formato correcto, simplemente validarlo
            if (fechaOriginal.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                // Validar año, mes y día
                val partes = fechaOriginal.split("-")
                val año = partes[0].toInt()
                val mes = partes[1].toInt()
                val dia = partes[2].toInt()
                
                // Verificar valores válidos (mes entre 1-12, día entre 1-31 según el mes)
                if (año >= 2023 && mes in 1..12 && dia in 1..31) {
                    Log.d(TAG, "Fecha con formato correcto: '$fechaOriginal'")
                    return fechaOriginal
                }
            }
            
            // Intentar parsear la fecha para formatearla correctamente
            val formatoEntrada = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
            val fecha = formatoEntrada.parse(fechaOriginal)
            
            if (fecha != null) {
                val formatoSalida = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
                val resultado = formatoSalida.format(fecha)
                Log.d(TAG, "Fecha formateada: '$resultado'")
                resultado
            } else {
                Log.d(TAG, "No se pudo parsear la fecha, usando original: '$fechaOriginal'")
                fechaOriginal
            }
        } catch (e: Exception) {
            // En caso de cualquier error, devolver la fecha original
            Log.e(TAG, "Error al formatear fecha: '$fechaOriginal'", e)
            fechaOriginal
        }
    }

    // Función para formatear la hora eliminando los segundos si existen
    private fun formatearHora(horaOriginal: String): String {
        return try {
            // Dividir la hora por los dos puntos
            Log.d(TAG, "Formateando hora original: '$horaOriginal'")
            val partes = horaOriginal.split(":")
            
            // Si tiene formato HH:MM o HH:MM:SS, extraer solo HH:MM
            if (partes.size >= 2) {
                val hora = partes[0].padStart(2, '0')
                val minuto = partes[1].padStart(2, '0')
                val resultado = "$hora:$minuto"
                Log.d(TAG, "Hora formateada: '$resultado'")
                resultado
            } else {
                // Si no tiene el formato esperado, devolver la original
                Log.d(TAG, "Formato de hora inesperado: '$horaOriginal', usando como está")
                horaOriginal
            }
        } catch (e: Exception) {
            // En caso de cualquier error, devolver la hora original
            Log.e(TAG, "Error al formatear hora: '$horaOriginal'", e)
            horaOriginal
        }
    }
}