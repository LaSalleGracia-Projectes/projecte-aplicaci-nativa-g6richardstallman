package com.example.app.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.app.R
import java.util.Locale

object LanguageManager {
    private const val LANGUAGE_PREFS = "language_preferences"
    private const val KEY_LANGUAGE = "selected_language"
    
    const val SPANISH = "es"
    const val ENGLISH = "en"
    const val CATALAN = "ca"
    
    // Obtener el idioma actual
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, SPANISH) ?: SPANISH
    }
    
    // Guardar el idioma seleccionado
    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    // Aplicar el idioma guardado a la configuración de la aplicación
    fun applyLanguage(context: Context) {
        val languageCode = getLanguage(context)
        Log.d("LanguageManager", "Aplicando idioma: $languageCode")
        updateResources(context, languageCode)
    }
    
    // Actualizar recursos de idioma
    private fun updateResources(context: Context, languageCode: String) {
        Log.d("LanguageManager", "Cambiando idioma a: $languageCode")
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
            
            // Actualizar la configuración también en los recursos
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
    
    // Obtener los idiomas disponibles (no composable)
    fun getAvailableLanguages(context: Context): List<Language> {
        return listOf(
            Language(SPANISH, context.getString(R.string.spanish)),
            Language(ENGLISH, context.getString(R.string.english)),
            Language(CATALAN, context.getString(R.string.catalan))
        )
    }
    
    // Obtener los idiomas disponibles (versión composable)
    @Composable
    fun getAvailableLanguages(): List<Language> {
        return listOf(
            Language(SPANISH, stringResource(id = R.string.spanish)),
            Language(ENGLISH, stringResource(id = R.string.english)),
            Language(CATALAN, stringResource(id = R.string.catalan))
        )
    }
    
    // Aplicar idioma y reiniciar la actividad
    fun applyLanguageAndRefresh(context: Context, languageCode: String) {
        setLanguage(context, languageCode)
        applyLanguage(context)
    }
}

// Clase para representar un idioma
data class Language(val code: String, val displayName: String) 