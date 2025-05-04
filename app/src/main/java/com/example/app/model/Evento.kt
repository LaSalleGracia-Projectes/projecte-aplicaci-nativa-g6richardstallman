package com.example.app.model

import com.google.gson.annotations.SerializedName
import android.util.Log

data class Evento(
    @SerializedName(value = "id", alternate = ["idEvento"]) val id: Int? = null,
    @SerializedName("nombreEvento") val titulo: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("imagen") val imagen: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    @SerializedName("fechaEvento") val fechaEvento: String? = null,
    @SerializedName("hora") val hora: String? = null,
    @SerializedName("ubicacion") val ubicacion: String? = null,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("lugar") val lugar: String? = null,
    @SerializedName("precio") val precio: Double = 0.0,
    @SerializedName("organizador") val organizador: OrganizadorEvento? = null,
    @SerializedName("isFavorito") val isFavorito: Boolean = false,
    @SerializedName("entradas") val entradas: List<TipoEntrada> = emptyList(),
    @SerializedName("tipos_entrada") val tipos_entrada: List<TipoEntrada>? = null,
    @SerializedName("es_online") val esOnline: Boolean = false
) {
    fun getEventoId(): Int = id ?: -1
}

data class OrganizadorEvento(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre_organizacion") val nombre: String,
    @SerializedName("telefono_contacto") val telefonoContacto: String,
    @SerializedName("direccion_fiscal") val direccionFiscal: String? = null,
    @SerializedName("cif") val cif: String? = null,
    @SerializedName("user") val user: com.example.app.model.UserInfo?,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

fun Evento.getImageUrl(): String {
    Log.d("Evento", "getImageUrl() - ID: ${this.getEventoId()}, tipo: ${this.getEventoId().javaClass.name}")
    
    return if (!this.imagenUrl.isNullOrBlank()) {
        Log.d("Evento", "Usando URL directa: ${this.imagenUrl}")
        this.imagenUrl
    } else if (!this.imagen.isNullOrBlank()) {
        val baseUrl = "https://example.com/storage/" // o la URL base que corresponda
        val urlFinal = baseUrl + this.imagen
        Log.d("Evento", "Usando URL construida: $urlFinal")
        urlFinal
    } else {
        val urlDefault = "https://example.com/storage/eventos/default.jpg" // o la URL por defecto
        Log.d("Evento", "Usando URL por defecto: $urlDefault")
        urlDefault
    }
}