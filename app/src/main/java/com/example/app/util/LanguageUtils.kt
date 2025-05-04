package com.example.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.example.app.R
import java.util.Locale

/**
 * Funciones de extensión para aplicar el idioma a nivel de actividad y contexto
 */

/**
 * Función para obtener la actividad desde un contexto
 */
fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * Función para reiniciar la actividad y aplicar el nuevo idioma
 */
fun Context.restartActivityWithLanguage(languageCode: String) {
    // Guardar el nuevo idioma
    LanguageManager.setLanguage(this, languageCode)
    
    // Obtener la actividad
    val activity = this.getActivity()
    if (activity != null) {
        val logMessage = getString(R.string.restarting_activity_language, languageCode)
        Log.d("LanguageUtils", logMessage)
        
        // Aplicar el idioma inmediatamente al contexto actual
        LanguageManager.applyLanguage(activity)
        
        // Reiniciar la actividad para aplicar los cambios
        activity.finish()
        activity.startActivity(activity.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP))
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    } else {
        Log.e("LanguageUtils", getString(R.string.error_activity_not_found))
    }
}

/**
 * Función para aplicar el idioma actual a un contexto
 */
fun Context.applyCurrentLanguage(): Context {
    val languageCode = LanguageManager.getLanguage(this)
    return applyLanguage(languageCode)
}

/**
 * Función para aplicar un idioma específico a un contexto
 */
fun Context.applyLanguage(languageCode: String): Context {
    val logMessage = getString(R.string.applying_language, languageCode)
    Log.d("LanguageUtils", logMessage)
    
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    
    val config = this.resources.configuration
    
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        config.setLocale(locale)
        createConfigurationContext(config)
    } else {
        @Suppress("DEPRECATION")
        config.locale = locale
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        this
    }
} 