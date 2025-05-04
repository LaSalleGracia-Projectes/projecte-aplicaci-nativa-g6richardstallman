package com.example.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.app.utils.TokenManager
import com.example.app.util.SessionManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        Log.d("MyApplication", "Aplicación inicializada correctamente")
        
        try {
            Log.d("MyApplication", "Inicializando SessionManager...")
            SessionManager.init(this)
            Log.d("MyApplication", "SessionManager inicializado correctamente")
            
            Log.d("MyApplication", "Inicializando TokenManager...")
            TokenManager.init(this)
            Log.d("MyApplication", "TokenManager inicializado correctamente")
        } catch (e: Exception) {
            Log.e("MyApplication", "Error al inicializar managers: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}