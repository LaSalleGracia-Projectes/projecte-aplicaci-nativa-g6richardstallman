package com.example.app.util

import com.example.app.model.Evento

fun Evento.getHoraFormateada(): String {
    return try {
        val partes = this.hora?.split(":") ?: return this.hora ?: ""
        if (partes.size >= 2) {
            "${partes[0].padStart(2, '0')}:${partes[1].padStart(2, '0')}"
        } else {
            this.hora ?: ""
        }
    } catch (e: Exception) {
        this.hora ?: ""
    }
} 