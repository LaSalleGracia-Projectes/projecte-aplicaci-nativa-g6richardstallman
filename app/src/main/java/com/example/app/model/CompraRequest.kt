package com.example.app.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo para la petición de compra de entradas
 * Compatible con el endpoint /compras del backend
 */
data class CompraRequest(
    @SerializedName("idEvento") val idEvento: Int,
    @SerializedName("entradas") val entradas: List<EntradaCompra>,
    @SerializedName("emitir_factura") val emitirFactura: Boolean = false,
    @SerializedName("metodo_pago") val metodoPago: String = "tarjeta"
)

/**
 * Modelo para cada entrada en la petición de compra
 */
data class EntradaCompra(
    @SerializedName("idTipoEntrada") val idTipoEntrada: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio") val precio: Double
)  