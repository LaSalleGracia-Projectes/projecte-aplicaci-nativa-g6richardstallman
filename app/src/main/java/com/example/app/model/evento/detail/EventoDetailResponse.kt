package com.example.app.model.evento.detail

import com.example.app.model.Evento
import com.example.app.model.OrganizadorEvento
import com.example.app.model.TipoEntrada
import com.google.gson.annotations.SerializedName

data class EventoDetailResponse(
    @SerializedName("evento") val evento: EventoDetalle,
    @SerializedName("status") val status: String
)

data class EventoDetalle(
    @SerializedName("id") val id: Int,
    @SerializedName("titulo") val titulo: String?,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("fechaEvento") val fechaEvento: String?,
    @SerializedName("hora") val hora: String?,
    @SerializedName("ubicacion") val ubicacion: String?,
    @SerializedName("categoria") val categoria: String?,
    @SerializedName("imagen") val imagen: String?,
    @SerializedName("esOnline") val esOnline: Boolean,
    @SerializedName("isFavorito") val isFavorito: Boolean,
    @SerializedName("organizador") val organizador: OrganizadorDetail?,
    @SerializedName("tipos_entrada") val tipos_entrada: List<TipoEntrada>?
)

data class OrganizadorDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("user") val user: UserDetail?
)

data class UserDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("email") val email: String?
) 