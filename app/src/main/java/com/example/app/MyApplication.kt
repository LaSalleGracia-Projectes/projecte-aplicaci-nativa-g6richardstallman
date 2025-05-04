package com.example.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.example.app.utils.TokenManager
import com.example.app.util.SessionManager
import com.example.app.util.LanguageManager
import com.example.app.R
import java.util.Locale

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        
        try {
            // Aplicar el idioma configurado inmediatamente
            val languageCode = LanguageManager.getLanguage(this)
            Log.d("MyApplication", "Configurando idioma inicial: $languageCode")
            LanguageManager.applyLanguage(this)
            
            Log.d("MyApplication", getString(R.string.app_initialized_correctly))
            
            // Inicializar gestores
            Log.d("MyApplication", getString(R.string.initializing_session_manager))
            SessionManager.init(this)
            Log.d("MyApplication", getString(R.string.session_manager_initialized))
            
            Log.d("MyApplication", getString(R.string.initializing_token_manager))
            TokenManager.init(this)
            Log.d("MyApplication", getString(R.string.token_manager_initialized))
            
            // Registrar el idioma actual
            Log.d("MyApplication", getString(R.string.language_loaded, languageCode))
        } catch (e: Exception) {
            Log.e("MyApplication", getString(R.string.error_initializing_managers, e.message))
            e.printStackTrace()
        }
    }
    
    override fun attachBaseContext(base: Context) {
        // Aplicar el idioma configurado al contexto base
        val languageCode = LanguageManager.getLanguage(base)
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        
        super.attachBaseContext(base.createConfigurationContext(config))
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Mantener el idioma configurado cuando cambia la configuración del sistema
        val languageCode = LanguageManager.getLanguage(this)
        Locale.setDefault(Locale(languageCode))
        
        val config = newConfig
        config.setLocale(Locale(languageCode))
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}