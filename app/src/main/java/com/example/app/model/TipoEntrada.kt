package com.example.app.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo para los tipos de entrada de un evento
 */
data class TipoEntrada(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("idTipoEntrada") val idTipoEntrada: Int? = null,
    @SerializedName("idEvento") val idEvento: Int? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("precio") val precio: String? = null,
    @SerializedName("cantidad_disponible") val cantidadDisponible: Int? = null,
    @SerializedName("entradas_vendidas") val entradasVendidas: Int? = 0,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("es_ilimitado") val esIlimitado: Boolean? = false,
    @SerializedName("activo") val activo: Boolean? = true,
    @SerializedName("disponibilidad") val disponibilidad: Int? = null
) {
    /**
     * Obtiene el ID efectivo del tipo de entrada.
     * Usa idTipoEntrada primero, luego id como fallback.
     */
    fun getEffectiveId(): Int {
        return idTipoEntrada ?: id ?: 0
    }
    
    /**
     * Convierte el precio a Double de forma segura
     */
    fun getPrecioDouble(): Double {
        return precio?.toDoubleOrNull() ?: 0.0
    }
} 