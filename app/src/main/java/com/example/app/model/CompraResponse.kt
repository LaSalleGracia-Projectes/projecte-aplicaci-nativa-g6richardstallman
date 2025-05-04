package com.example.app.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo para la respuesta de la compra de entradas
 */
data class CompraResponse(
    @SerializedName("message") val message: String,
    @SerializedName("compra") val compra: CompraDatos? = null,
    @SerializedName("status") val status: String
)

/**
 * Datos de la compra realizada
 */
data class CompraDatos(
    @SerializedName("id") val id: Int,
    @SerializedName("total") val total: Double,
    @SerializedName("fecha") val fecha: String
) 