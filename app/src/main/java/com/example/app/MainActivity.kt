package com.example.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.app.routes.AppNavHost
import com.example.app.ui.theme.AppTheme
import com.example.app.util.LanguageManager

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Aplicar el idioma guardado antes de adjuntar el contexto base
        LanguageManager.applyLanguage(newBase)
        super.attachBaseContext(newBase)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegurar que el idioma se aplique correctamente
        LanguageManager.applyLanguage(this)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}