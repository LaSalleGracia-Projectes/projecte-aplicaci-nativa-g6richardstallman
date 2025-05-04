package com.example.app.model.evento

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.app.R

enum class CategoriaEvento(val resourceId: Int) {
    ARTE_Y_CULTURA(R.string.categoria_arte_cultura),
    BELLEZA_Y_BIENESTAR(R.string.categoria_belleza_bienestar),
    CHARLAS_Y_CONFERENCIAS(R.string.categoria_charlas_conferencias),
    CINE(R.string.categoria_cine),
    COMEDIA(R.string.categoria_comedia),
    CONCIERTOS(R.string.categoria_conciertos),
    DEPORTES(R.string.categoria_deportes),
    DISCOTECA(R.string.categoria_discoteca),
    EDUCACION(R.string.categoria_educacion),
    EMPRESARIAL(R.string.categoria_empresarial),
    EXPOSICIONES(R.string.categoria_exposiciones),
    FAMILIAR(R.string.categoria_familiar),
    FESTIVALES(R.string.categoria_festivales),
    GASTRONOMIA(R.string.categoria_gastronomia),
    INFANTIL(R.string.categoria_infantil),
    MODA(R.string.categoria_moda),
    MUSICA_EN_DIRECTO(R.string.categoria_musica_directo),
    NETWORKING(R.string.categoria_networking),
    OCIO_NOCTURNO(R.string.categoria_ocio_nocturno),
    PRESENTACIONES(R.string.categoria_presentaciones),
    RESTAURANTES(R.string.categoria_restaurantes),
    SALUD_Y_DEPORTE(R.string.categoria_salud_deporte),
    SEMINARIOS(R.string.categoria_seminarios),
    TALLERES(R.string.categoria_talleres),
    TEATRO(R.string.categoria_teatro),
    TECNOLOGIA(R.string.categoria_tecnologia),
    TURISMO(R.string.categoria_turismo),
    VARIOS(R.string.categoria_varios);

    // Obtener el valor traducido de la categoría
    fun getLocalizedValue(context: Context): String {
        return context.getString(resourceId)
    }
    
    // Versión composable para obtener el valor traducido
    @Composable
    fun getLocalizedValue(): String {
        return stringResource(id = resourceId)
    }

    // Valor de la categoría para APIs
    fun getApiValue(): String {
        return when (this) {
            ARTE_Y_CULTURA -> "Arte y Cultura"
            BELLEZA_Y_BIENESTAR -> "Belleza y Bienestar"
            CHARLAS_Y_CONFERENCIAS -> "Charlas y Conferencias"
            CINE -> "Cine"
            COMEDIA -> "Comedia"
            CONCIERTOS -> "Conciertos"
            DEPORTES -> "Deportes"
            DISCOTECA -> "Discoteca"
            EDUCACION -> "Educación"
            EMPRESARIAL -> "Empresarial"
            EXPOSICIONES -> "Exposiciones"
            FAMILIAR -> "Familiar"
            FESTIVALES -> "Festivales"
            GASTRONOMIA -> "Gastronomía"
            INFANTIL -> "Infantil"
            MODA -> "Moda"
            MUSICA_EN_DIRECTO -> "Música en Directo"
            NETWORKING -> "Networking"
            OCIO_NOCTURNO -> "Ocio Nocturno"
            PRESENTACIONES -> "Presentaciones"
            RESTAURANTES -> "Restaurantes"
            SALUD_Y_DEPORTE -> "Salud y Deporte"
            SEMINARIOS -> "Seminarios"
            TALLERES -> "Talleres"
            TEATRO -> "Teatro"
            TECNOLOGIA -> "Tecnología"
            TURISMO -> "Turismo"
            VARIOS -> "Varios"
        }
    }

    companion object {
        // Obtener todas las categorías como valores localizados
        @Composable
        fun getAllLocalizedValues(): List<String> {
            return values().map { it.getLocalizedValue() }.sorted()
        }
        
        // Obtener todas las categorías como valores localizados con el contexto
        fun getAllLocalizedValues(context: Context): List<String> {
            return values().map { it.getLocalizedValue(context) }.sorted()
        }

        // Obtener todas las categorías como valores para la API
        fun getAllApiValues(): List<String> {
            return values().map { it.getApiValue() }.sorted()
        }

        // Obtener categoría a partir de un valor de API
        fun fromApiValue(valor: String): CategoriaEvento? {
            // Intentamos encontrar una coincidencia exacta primero
            var categoria = values().find { it.getApiValue().equals(valor, ignoreCase = true) }
            
            // Si no hay coincidencia exacta, intentamos normalizar los nombres:
            // - Eliminamos acentos
            // - Convertimos a minúsculas
            // - Eliminamos y ("y" e "i")
            if (categoria == null) {
                val valorNormalizado = valor.lowercase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace(" y ", " ").replace(" i ", " ").replace(" & ", " ")
                    .trim()
                
                categoria = values().find { 
                    val apiValueNormalizado = it.getApiValue().lowercase()
                        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                        .replace(" y ", " ").replace(" i ", " ").replace(" & ", " ")
                        .trim()
                    valorNormalizado.contains(apiValueNormalizado) || apiValueNormalizado.contains(valorNormalizado)
                }
            }
            
            return categoria
        }
    }
}