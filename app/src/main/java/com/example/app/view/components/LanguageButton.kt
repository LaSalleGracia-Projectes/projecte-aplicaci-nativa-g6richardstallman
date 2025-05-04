package com.example.app.view.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app.R
import com.example.app.util.LanguageManager
import com.example.app.util.restartActivityWithLanguage

/**
 * Botón de idioma que se puede colocar en cualquier pantalla
 * 
 * @param modifier Modificador para personalizar el botón
 * @param tint Color del icono
 */
@Composable
fun LanguageButton(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFE53935)
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val languages = LanguageManager.getAvailableLanguages()
    val currentLanguageCode = LanguageManager.getLanguage(context)
    
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(id = R.string.change_language),
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { language ->
                val isSelected = language.code == currentLanguageCode
                
                DropdownMenuItem(
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.displayName,
                                color = if (isSelected) tint else Color.Unspecified
                            )
                            
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = tint,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        }
                    },
                    onClick = {
                        if (!isSelected) {
                            Log.d("LanguageButton", "Cambiando idioma a: ${language.code}")
                            
                            context.restartActivityWithLanguage(language.code)
                        }
                        expanded = false
                    }
                )
            }
        }
    }
} 