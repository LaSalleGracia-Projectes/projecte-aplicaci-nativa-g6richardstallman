package com.example.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Formatea una fecha en formato ISO (yyyy-MM-dd) a un formato más legible
 * @param dateString La fecha en formato ISO
 * @param showYear Si se debe mostrar el año en el formato
 * @param locale El locale para traducir la fecha
 * @return La fecha formateada
 */
fun formatDate(dateString: String, showYear: Boolean = true, locale: Locale = Locale.getDefault()): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val pattern = when (locale.language) {
            "es" -> if (showYear) "dd 'de' MMMM 'de' yyyy" else "dd 'de' MMMM"
            "ca" -> if (showYear) "dd 'de' MMMM 'de' yyyy" else "dd 'de' MMMM"
            "en" -> if (showYear) "MMMM dd, yyyy" else "MMMM dd"
            else -> if (showYear) "dd MMMM yyyy" else "dd MMMM"
        }
        date.format(DateTimeFormatter.ofPattern(pattern, locale))
    } catch (e: DateTimeParseException) {
        dateString
    }
} 