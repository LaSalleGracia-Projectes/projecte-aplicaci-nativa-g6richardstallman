package com.example.app.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Formatea un valor Double como una cadena de moneda con formato español (€)
 */
fun formatToCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    return format.format(value)
} 