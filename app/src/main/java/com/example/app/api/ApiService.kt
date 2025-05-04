package com.example.app.api

import com.example.app.model.Evento
import com.example.app.model.evento.EventoRequest
import com.example.app.model.evento.detail.EventoDetailResponse
import com.example.app.model.login.LoginRequest
import com.example.app.model.login.LoginResponse
import com.example.app.model.login.GoogleAuthRequest
import com.example.app.model.logout.LogoutResponse
import com.example.app.model.register.RegisterRequest
import com.example.app.model.register.RegisterResponse
import com.example.app.model.resetpassword.ResetPasswordRequest
import com.example.app.model.resetpassword.ResetPasswordResponse
import com.example.app.model.evento.EventoResponse
import com.example.app.model.favoritos.FavoritoRequest
import com.example.app.model.favoritos.FavoritosResponse
import com.example.app.model.favoritos.FavoritoCheckResponse
import com.example.app.model.CategoriasResponse
import com.example.app.model.TiposEntradaResponse
import com.example.app.model.CompraRequest
import com.example.app.model.CompraResponse
import com.example.app.model.EventosResponse
import com.example.app.model.HistorialComprasResponse
import com.example.app.model.response.GenericResponse
import com.example.app.model.response.MisEventosResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import com.google.gson.annotations.SerializedName
import com.example.app.model.evento.CrearEventoResponse
import com.example.app.model.ProfileResponse
import com.example.app.model.favoritos.OrganizadorFavoritoRequest
import com.example.app.model.favoritos.OrganizadoresFavoritosResponse
import retrofit2.http.PUT
import com.example.app.model.tickets.TicketsResponse
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Query
import retrofit2.http.Headers
import okhttp3.ResponseBody

interface ApiService {
    @POST("api/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<RegisterResponse>
    
    @POST("api/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @POST("api/logout")
    suspend fun logoutUser(
        @Header("Authorization") token: String
    ): Response<LogoutResponse>
    
    @POST("api/reset-password")
    suspend fun resetPassword(@Body resetRequest: ResetPasswordRequest): Response<ResetPasswordResponse>
    
    @GET("api/eventos")
    suspend fun getAllEventos(): Response<EventoResponse>
    
    @GET("api/eventos/{id}")
    suspend fun getEventoById(@Path("id") id: String): Response<EventoDetailResponse>
    
    @GET("api/eventos/{id}/detalle")
    suspend fun getEventoDetalle(@Path("id") id: String): Response<EventoDetailResponse>
    
    @GET("api/eventos/{id}/tipos-entrada")
    suspend fun getTiposEntrada(@Path("id") id: String): Response<TiposEntradaResponse>
    
    @GET("api/favoritos")
    suspend fun getFavoritos(
        @Header("Authorization") token: String
    ): Response<FavoritosResponse>
    
    @POST("api/favoritos")
    suspend fun agregarFavorito(
        @Header("Authorization") token: String,
        @Body request: FavoritoRequest
    ): Response<MessageResponse>
    
    @DELETE("api/favoritos/{idEvento}")
    suspend fun eliminarFavorito(
        @Header("Authorization") token: String,
        @Path("idEvento") idEvento: Int
    ): Response<MessageResponse>
    
    @GET("api/favoritos/check/{idEvento}")
    suspend fun checkFavorito(
        @Header("Authorization") token: String,
        @Path("idEvento") idEvento: Int
    ): Response<FavoritoCheckResponse>
    
    @POST("eventos/{eventoId}/favorito")
    suspend fun toggleFavorito(@Path("eventoId") eventoId: Long): Response<Unit>
    
    @GET("api/mis-eventos")
    suspend fun getMisEventosFromApi(
        @Header("Authorization") token: String
    ): Response<EventoResponse>
    
    @POST("api/eventos")
    suspend fun crearEvento(
        @Header("Authorization") token: String,
        @Body request: EventoRequest
    ): Response<CrearEventoResponse>

    @Multipart
    @POST("api/eventos")
    suspend fun crearEventoConImagen(
        @Header("Authorization") token: String,
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("fecha") fecha: RequestBody,
        @Part("hora") hora: RequestBody,
        @Part("ubicacion") ubicacion: RequestBody,
        @Part("categoria") categoria: RequestBody,
        @Part("es_online") esOnline: RequestBody,
        @Part tiposEntradas: List<MultipartBody.Part>,
        @Part imagen: MultipartBody.Part
    ): Response<CrearEventoResponse>

    @GET("api/categorias")
    suspend fun getCategorias(
        @Header("Authorization") token: String
    ): Response<CategoriasResponse>

    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>
    
    @PUT("api/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body profileData: Map<String, String>
    ): Response<ProfileResponse>
    
    @GET("api/user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): Response<com.example.app.model.User>
    
    @GET("api/compras")
    suspend fun getMisTickets(
        @Header("Authorization") token: String
    ): Response<TicketsResponse>
    
    @POST("api/compras")
    suspend fun comprarEntradas(
        @Header("Authorization") token: String,
        @Body compraRequest: CompraRequest
    ): Response<CompraResponse>

    @PUT("api/eventos/{id}")
    suspend fun actualizarEvento(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Body request: EventoRequest
    ): Response<CrearEventoResponse>

    @Multipart
    @POST("api/eventos/{id}")
    suspend fun actualizarEventoConImagen(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Part("titulo") titulo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("fecha") fecha: RequestBody,
        @Part("hora") hora: RequestBody,
        @Part("ubicacion") ubicacion: RequestBody,
        @Part("categoria") categoria: RequestBody,
        @Part("es_online") esOnline: RequestBody,
        @Part tiposEntradas: List<MultipartBody.Part>,
        @Part imagen: MultipartBody.Part,
        @Part("_method") method: RequestBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
    ): Response<CrearEventoResponse>

    @GET("api/debug/testid/{id}")
    suspend fun testId(@Path("id") id: String): Response<String>
    
    // Función extra para retornar el ID recibido, útil para depuración
    @GET("api/debug/echo/{id}")
    suspend fun echoId(@Path("id") id: String): Response<String>

    // Obtener mis eventos (del organizador logueado)
    @GET("api/mis-eventos")
    suspend fun getMisEventos(
        @Header("Authorization") token: String
    ): Response<MisEventosResponse>
    
    // Eliminar un evento
    @DELETE("api/eventos/{id}")
    suspend fun deleteEvento(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<GenericResponse>

    // Historial de compras
    @GET("api/compras")
    suspend fun getHistorialCompras(
        @Header("Authorization") token: String
    ): Response<HistorialComprasResponse>
    
    // Cambiar contraseña
    @POST("api/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body passwordData: Map<String, String>
    ): Response<GenericResponse>
    
    // Añadir un evento a favoritos
   
    // Eliminar cuenta de usuario
    @POST("account")
    @Headers("X-HTTP-Method-Override: DELETE")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Body request: DeleteAccountRequest
    ): Response<GenericResponse>

    @GET("api/organizadores/{id}")
    suspend fun getOrganizadorById(
        @Path("id") id: String,
        @Header("Authorization") token: String? = null
    ): Response<OrganizadorResponse>
    
    // Endpoints para organizadores favoritos
    @GET("api/organizadores-favoritos")
    suspend fun getOrganizadoresFavoritos(
        @Header("Authorization") token: String
    ): Response<OrganizadoresFavoritosResponse>
    
    @POST("api/organizadores-favoritos")
    suspend fun addOrganizadorFavorito(
        @Header("Authorization") token: String,
        @Body request: OrganizadorFavoritoRequest
    ): Response<MessageResponse>
    
    @DELETE("api/organizadores-favoritos/{idOrganizador}")
    suspend fun removeOrganizadorFavorito(
        @Header("Authorization") token: String,
        @Path("idOrganizador") idOrganizador: Int
    ): Response<MessageResponse>
    
    @GET("api/organizadores-favoritos/check/{idOrganizador}")
    suspend fun checkOrganizadorFavorito(
        @Header("Authorization") token: String,
        @Path("idOrganizador") idOrganizador: Int
    ): Response<FavoritoCheckResponse>

    @GET("api/eventos/categoria/{categoria}")
    suspend fun getEventosByCategoria(@Path("categoria") categoria: String): Response<EventosResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Query("email") email: String): Response<LoginResponse>

    @POST("api/auth/google/mobile")
    suspend fun handleGoogleMobile(@Body request: GoogleAuthRequest): Response<LoginResponse>

    @POST("api/auth/google/mobile/register")
    suspend fun registerWithGoogleMobile(@Body request: GoogleAuthRequest): Response<LoginResponse>

    @GET("api/compras/{id}/factura")
    suspend fun generarFactura(
        @Header("Authorization") token: String,
        @Path("id") idCompra: Int
    ): Response<GenerarFacturaResponse>

    @GET("api/factura/{id}/pdf")
    @Headers("Accept: application/pdf")
    suspend fun downloadFactura(
        @Header("Authorization") token: String,
        @Path("id") idCompra: Int
    ): Response<ResponseBody>

    @GET("api/entrada/{id}/pdf")
    @Headers("Accept: application/pdf", "Content-Type: application/pdf")
    suspend fun downloadEntrada(
        @Header("Authorization") token: String,
        @Path("id") idEntrada: Int
    ): Response<ResponseBody>

    @GET("/api/avatar")
    suspend fun getAvatar(@Header("Authorization") token: String): Response<AvatarResponse>

    // Método para actualizar eventos usando el formato correcto según Postman
    @PUT("api/eventos/{id}")
    suspend fun modificarEvento(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Body eventoData: Map<String, @JvmSuppressWildcards Any>
    ): Response<CrearEventoResponse>

    // Método DELETE para editar eventos (usando el formato que se ve en Postman)
    @DELETE("api/eventos/{id}")
    suspend fun modificarEventoDelete(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Body eventoData: Map<String, Any>
    ): Response<CrearEventoResponse>

    @GET("api/eventos/{id}/precio-minimo")
    suspend fun getPrecioMinimoEvento(@Path("id") id: Long): PrecioEventoWrapper

    @GET("api/eventos/{id}/precio-maximo")
    suspend fun getPrecioMaximoEvento(@Path("id") id: Long): PrecioEventoWrapper
}

data class FavoritosResponse(
    @SerializedName("message") val message: String,
    @SerializedName("favoritos") val eventos: List<Evento>
)

data class MessageResponse(
    @SerializedName("message") val message: String
)

data class FavoritoCheckResponse(
    @SerializedName("is_favorite") val isFavorito: Boolean,
    @SerializedName("status") val status: String = "success"
) {
    override fun toString(): String {
        return "FavoritoCheckResponse(isFavorito=$isFavorito, status=$status)"
    }
}

// Modelo para la solicitud de eliminación de cuenta
data class DeleteAccountRequest(
    @SerializedName("password") val password: String,
    @SerializedName("confirm_deletion") val confirmDeletion: Boolean,
    @SerializedName("_method") val method: String = "DELETE"
)

// Añadir la data class para la respuesta del organizador
data class OrganizadorResponse(
    @SerializedName("message") val message: String,
    @SerializedName("organizador") val organizador: OrganizadorDetalle,
    @SerializedName("status") val status: String
)

data class AvatarResponse(
    val avatar: String?,
    val avatar_url: String?
)

// Clase para la respuesta de generar factura
data class GenerarFacturaResponse(
    @SerializedName("message") val message: String,
    @SerializedName("factura") val factura: FacturaData,
    @SerializedName("status") val status: String
)

data class FacturaData(
    @SerializedName("id") val id: Int,
    @SerializedName("numero") val numero: String,
    @SerializedName("fecha_emision") val fechaEmision: String,
    @SerializedName("fecha_vencimiento") val fechaVencimiento: String,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("impuestos") val impuestos: Double,
    @SerializedName("descuento") val descuento: Double,
    @SerializedName("total") val total: Double,
    @SerializedName("estado") val estado: String
)

// Modelo para la respuesta de precio
data class PrecioResponse(val precio: Double?)

// Modelos para la respuesta de precio mínimo/máximo
data class PrecioEventoWrapper(val evento: PrecioEvento)
data class PrecioEvento(
    val precio_minimo: String?, // para el endpoint de mínimo
    val precio_maximo: String?  // para el endpoint de máximo
)