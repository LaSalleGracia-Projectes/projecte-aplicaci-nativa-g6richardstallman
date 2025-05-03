package com.example.app.model

import com.google.gson.annotations.SerializedName
import android.util.Log

data class Evento(
    @SerializedName(value = "id", alternate = ["idEvento"]) val idEvento: Int? = null,
    @SerializedName("nombreEvento") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("imagen") val imagen: String,
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    @SerializedName("fechaEvento") val fechaEvento: String,
    @SerializedName("hora") val hora: String,
    @SerializedName("ubicacion") val ubicacion: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("lugar") val lugar: String,
    @SerializedName("precio") val precio: Double = 0.0,
    @SerializedName("organizador") val organizador: OrganizadorEvento?,
    @SerializedName("isFavorito") val isFavorito: Boolean = false,
    @SerializedName("entradas") val entradas: List<TipoEntrada> = emptyList(),
    @SerializedName("es_online") val esOnline: Boolean = false
) {
    fun getEventoId(): Int = idEvento ?: -1
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

data class TipoEntrada(
    @SerializedName("id") val id: Int,
    @SerializedName("tipo") val nombre: String,
    @SerializedName("precio") val precio: Double,
    @SerializedName("cantidad_disponible") val cantidadDisponible: Int?,
    @SerializedName("entradas_vendidas") val entradasVendidas: Int = 0,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("es_ilimitado") val esIlimitado: Boolean = false
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