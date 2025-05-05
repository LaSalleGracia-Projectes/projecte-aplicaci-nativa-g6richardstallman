package com.example.app.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.app.MyApplication

object SessionManager {
    private const val PREF_NAME = "EventFlixPrefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ROLE = "user_role"
    private var prefs: SharedPreferences? = null
    private var initialized = false
    
    // Cache de token para reducir accesos a disco
    private var cachedToken: String? = null
    private var cachedRole: String? = null
    
    fun init(context: Context) {
        try {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            initialized = true
            // Inicializar cache
            cachedToken = prefs?.getString(KEY_TOKEN, null)
            cachedRole = prefs?.getString(KEY_USER_ROLE, null)
            Log.d("SessionManager", "Inicializado con éxito, token en cache: ${cachedToken?.take(10)}")
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al inicializar: ${e.message}")
            initialized = false
        }
    }
    
    fun saveToken(token: String) {
        try {
            if (!initialized || prefs == null) {
                Log.e("SessionManager", "No inicializado, no se puede guardar el token")
                return
            }
            
            if (token.isBlank()) {
                Log.e("SessionManager", "Intento de guardar token vacío ignorado")
                return
            }
            
            Log.d("SessionManager", "Guardando token: ${token.take(10)}...")
            
            // Actualizar caché inmediatamente
            cachedToken = token
            
            // Guardar en disco
            prefs?.edit()?.putString(KEY_TOKEN, token)?.apply()
            
            // Verificar que se guardó correctamente
            val savedToken = prefs?.getString(KEY_TOKEN, null)
            if (savedToken != token) {
                Log.e("SessionManager", "Verificación fallida: token no se guardó correctamente")
            } else {
                Log.d("SessionManager", "Token guardado y verificado correctamente")
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al guardar token: ${e.message}")
        }
    }
    
    fun getToken(): String? {
        try {
            if (!initialized || prefs == null) {
                Log.e("SessionManager", "No inicializado, no se puede recuperar el token")
                return null
            }
            
            // Si hay token en caché, devolverlo primero
            if (!cachedToken.isNullOrBlank()) {
                Log.d("SessionManager", "Recuperando token de cache: ${cachedToken?.take(10)}...")
                return cachedToken
            }
            
            // Si no hay en caché, leer de disco
            val token = prefs?.getString(KEY_TOKEN, null)
            
            Log.d("SessionManager", "Recuperando token de disco: ${token?.take(10)}")
            
            // Actualizar la caché
            cachedToken = token
            
            return token
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al recuperar token: ${e.message}")
            return null
        }
    }
    
    fun saveUserRole(role: String) {
        Log.d("SessionManager", "Guardando rol: $role")
        try {
            if (!initialized || prefs == null) {
                Log.e("SessionManager", "No inicializado, no se puede guardar el rol")
                return
            }
            // Actualizar caché
            cachedRole = role
            // Guardar en disco
            prefs?.edit()?.putString(KEY_USER_ROLE, role)?.apply()
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al guardar rol: ${e.message}")
        }
    }
    
    fun getUserRole(): String? {
        // Si tenemos el rol en caché, lo devolvemos directamente
        if (cachedRole != null) {
            return cachedRole
        }
        
        try {
            if (!initialized || prefs == null) {
                Log.e("SessionManager", "No inicializado, no se puede recuperar el rol")
                return null
            }
            val role = prefs?.getString(KEY_USER_ROLE, null)
            Log.d("SessionManager", "Recuperando rol de disco: $role")
            // Actualizar caché
            cachedRole = role
            return role
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al recuperar rol: ${e.message}")
            return null
        }
    }
    
    fun clearSession() {
        Log.d("SessionManager", "Limpiando sesión")
        try {
            // Limpiar caché inmediatamente
            cachedToken = null
            cachedRole = null
            
            if (!initialized || prefs == null) {
                Log.e("SessionManager", "No inicializado, no se puede limpiar la sesión")
                return
            }
            
            // Limpiar disco usando commit para que sea inmediato
            prefs?.edit()?.clear()?.commit()
            Log.d("SessionManager", "Sesión limpiada correctamente (caché y disco)")
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al limpiar sesión: ${e.message}")
        }
    }
    
    /**
     * Limpia la sesión de forma síncrona
     */
    fun clearSessionSync() {
        Log.d("SessionManager", "Limpiando sesión de forma síncrona")
        try {
            // Limpiar caché en memoria
            cachedToken = null
            cachedRole = null
            
            // Limpiar SharedPreferences de forma síncrona
            if (initialized && prefs != null) {
                val editor = prefs?.edit()
                editor?.remove(KEY_TOKEN)
                editor?.remove(KEY_USER_ROLE)
                editor?.apply() // apply es asíncrono pero es más seguro que commit
                
                // Forzar commit para garantizar que se guarde inmediatamente
                editor?.commit()
                
                Log.d("SessionManager", "Sesión limpiada correctamente (síncrono)")
            } else {
                Log.w("SessionManager", "No se pudo limpiar la sesión: no inicializada o prefs nulo")
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al limpiar sesión de forma síncrona: ${e.message}")
        }
    }
    
    fun isLoggedIn(): Boolean {
        // Verificación rápida con caché
        if (cachedToken != null) {
            return true
        }
        
        try {
            if (!initialized || prefs == null) {
                Log.e("SessionManager", "No inicializado, no se puede verificar si está logueado")
                return false
            }
            
            // Solo si no hay caché, accedemos a disco
            val token = prefs?.getString(KEY_TOKEN, null)
            // Actualizar caché
            cachedToken = token
            
            val isLogged = token != null
            Log.d("SessionManager", "Verificando si está logueado: $isLogged")
            return isLogged
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al verificar si está logueado: ${e.message}")
            return false
        }
    }
    
    /**
     * Verifica si hay un token válido en la sesión
     * @return true si hay un token, false si no
     */
    fun hasValidToken(): Boolean {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            Log.d("SessionManager", "No hay token válido en la sesión")
            return false
        }
        Log.d("SessionManager", "Hay un token válido en la sesión")
        return true
    }
    
    fun isInitialized(): Boolean {
        return initialized && prefs != null
    }
    
    fun getUserLanguage(): String {
        // Usar LanguageManager para obtener el idioma configurado
        return try {
            // En caso de no tener contexto, devolvemos el idioma por defecto
            if (!initialized || prefs == null) {
                Log.d("SessionManager", "No inicializado, devolviendo idioma por defecto")
                return LanguageManager.SPANISH
            }
            
            // Obtener el contexto desde SharedPreferences
            val context = prefs?.all?.get("dummy")?.javaClass?.classLoader?.let {
                Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null)
            } as? Context
            
            if (context != null) {
                LanguageManager.getLanguage(context)
            } else {
                Log.d("SessionManager", "Contexto no disponible, devolviendo idioma por defecto")
                LanguageManager.SPANISH
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error al obtener idioma: ${e.message}")
        // Por defecto, devolvemos español
            LanguageManager.SPANISH
        }
    }
}