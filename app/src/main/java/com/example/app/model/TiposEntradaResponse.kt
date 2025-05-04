package com.example.app.model

import com.google.gson.annotations.SerializedName

data class TiposEntradaResponse(
    @SerializedName("message") val message: String,
    @SerializedName("data") val tiposEntrada: List<TipoEntrada>?,
    @SerializedName("tipos_entrada") val tiposEntradaAlternativo: List<TipoEntrada>?,
    @SerializedName("status") val status: String,
    @SerializedName("evento_id") val eventoId: Int = 0,
    @SerializedName("evento_nombre") val eventoNombre: String? = null,
    @SerializedName("evento_descripcion") val eventoDescripcion: String? = null,
    @SerializedName("evento_fecha") val eventoFecha: String? = null,
    @SerializedName("evento_hora") val eventoHora: String? = null,
    @SerializedName("evento_ubicacion") val eventoUbicacion: String? = null,
    @SerializedName("evento_lugar") val eventoLugar: String? = null,
    @SerializedName("evento_categoria") val eventoCategoria: String? = null,
    @SerializedName("evento_imagen") val eventoImagen: String? = null,
    @SerializedName("organizador") val organizador: OrganizadorBasico? = null,
    
    // Campos adicionales para capturar información del evento desde diferentes formatos de API
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("hora") val hora: String? = null,
    @SerializedName("ubicacion") val ubicacion: String? = null,
    @SerializedName("lugar") val lugar: String? = null,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("imagen") val imagen: String? = null,
    
    // Formato con evento anidado (como vemos en la respuesta actual)
    @SerializedName("evento") val eventoAnidado: EventoAnidado? = null
) {
    // Función para obtener los tipos de entrada de cualquier parte de la respuesta
    fun getTiposEntradaEfectivos(): List<TipoEntrada> {
        return when {
            // Primero intentamos obtenerlos del campo tiposEntrada (data) 
            !tiposEntrada.isNullOrEmpty() -> tiposEntrada
            
            // Segundo, intentamos del campo tipos_entrada a nivel raíz
            !tiposEntradaAlternativo.isNullOrEmpty() -> tiposEntradaAlternativo
            
            // Tercero, intentamos obtenerlos del campo evento.tipos_entrada
            eventoAnidado?.tiposEntrada != null -> eventoAnidado.tiposEntrada
            
            // Si no encontramos en ningún lugar, devolvemos una lista vacía
            else -> emptyList()
        }
    }
}

// Modelo para capturar el evento anidado que viene en la respuesta
data class EventoAnidado(
    @SerializedName("idEvento") val id: Int? = null,
    @SerializedName("nombreEvento") val nombre: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fechaEvento") val fecha: String? = null,
    @SerializedName("horaEvento") val hora: String? = null,
    @SerializedName("ubicacion") val ubicacion: String? = null,
    @SerializedName("categoria") val categoria: String? = null,
    @SerializedName("lugar") val lugar: String? = null,
    @SerializedName("imagen") val imagen: String? = null,
    @SerializedName("tipos_entrada") val tiposEntrada: List<TipoEntrada>? = null,
    @SerializedName("organizador") val organizador: OrganizadorBasico? = null
)

data class OrganizadorBasico(
    @SerializedName("idUser") val idUser: Int? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("apellido1") val apellido1: String? = null,
    @SerializedName("apellido2") val apellido2: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("organizador") val organizador: InfoOrganizador? = null
)

data class InfoOrganizador(
    @SerializedName("idOrganizador") val id: Int? = null,
    @SerializedName("nombre_organizacion") val nombre: String? = null,
    @SerializedName("telefono_contacto") val telefonoContacto: String? = null,
    @SerializedName("direccion_fiscal") val direccionFiscal: String? = null,
    @SerializedName("cif") val cif: String? = null,
    @SerializedName("user_id") val userId: Int? = null
)

data class TipoEntradaDetalle(
    @SerializedName("idTipoEntrada") val id: Int,
    @SerializedName("idEvento") val idEvento: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("precio") val precio: String,
    @SerializedName("cantidad_disponible") val cantidadDisponible: Int?,
    @SerializedName("entradas_vendidas") val entradasVendidas: Int,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("es_ilimitado") val esIlimitado: Boolean,
    @SerializedName("activo") val activo: Boolean,
    @SerializedName("disponibilidad") val disponibilidad: Int?
) 