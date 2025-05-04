package com.example.app.util

import android.util.Log
import com.example.app.model.Evento
import com.example.app.model.tickets.EventoCompra

/**
 * Obtiene la URL completa de la imagen para un Evento.
 * Prioriza usar imagen_url si está disponible, de lo contrario construye la URL
 * basada en el campo imagen y las constantes de la aplicación.
 *
 * @return URL completa de la imagen
 */
fun Evento.getImageUrl(): String {
    // Diagnóstico: Imprimir valores actuales
    Log.d("ImageUtils", "Diagnóstico - imagenUrl: $imagenUrl")
    Log.d("ImageUtils", "Diagnóstico - imagen: $imagen")
    Log.d("ImageUtils", "Diagnóstico - STORAGE_URL: ${Constants.STORAGE_URL}")
    
    // Intentar usar imagenUrl si está disponible
    if (!imagenUrl.isNullOrEmpty()) {
        Log.d("ImageUtils", "Usando imagen_url: $imagenUrl")
        return imagenUrl
    }
    
    // Si no, construir la URL basada en el campo imagen
    val url = when {
        !imagen.isNullOrEmpty() && imagen.startsWith("http") -> imagen
        !imagen.isNullOrEmpty() -> "${Constants.STORAGE_URL}${imagen}"
        else -> "${Constants.STORAGE_URL}${Constants.DEFAULT_EVENT_IMAGE}"
    }
    
    Log.d("ImageUtils", "URL construida: $url")
    return url
}

/**
 * Obtiene la URL completa de la imagen para un EventoCompra.
 * Esta versión es específica para la estructura EventoCompra usada en los tickets.
 *
 * @return URL completa de la imagen
 */
fun EventoCompra.getImageUrl(): String {
    // Diagnóstico: Imprimir valores actuales
    Log.d("ImageUtils", "Diagnóstico - imagen (EventoCompra): $imagen")
    Log.d("ImageUtils", "Diagnóstico - STORAGE_URL: ${Constants.STORAGE_URL}")
    
    return imagen?.let { img ->
        when {
            img.startsWith("http") -> img
            img.isNotEmpty() -> "${Constants.STORAGE_URL}${img}"
            else -> "${Constants.STORAGE_URL}${Constants.DEFAULT_EVENT_IMAGE}"
        }
    } ?: "${Constants.STORAGE_URL}${Constants.DEFAULT_EVENT_IMAGE}"
} 