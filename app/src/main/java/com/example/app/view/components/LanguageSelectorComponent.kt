package com.example.app.view.components

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R
import com.example.app.util.Language
import com.example.app.util.LanguageManager
import com.example.app.util.restartActivityWithLanguage

@Composable
fun LanguageSelectorComponent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguageCode = LanguageManager.getLanguage(context)
    val languages = LanguageManager.getAvailableLanguages()
    var expanded by remember { mutableStateOf(false) }
    
    // Obtener el nombre de visualización del idioma actual
    val currentLanguage = languages.find { it.code == currentLanguageCode }?.displayName
        ?: stringResource(id = R.string.spanish)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFE53935)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { expanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFFE53935)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentLanguage)
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        languages.forEach { language ->
                            val isSelected = language.code == currentLanguageCode
                            
                            DropdownMenuItem(
                                text = { Text(language.displayName) },
                                onClick = {
                                    if (language.code != currentLanguageCode) {
                                        // Log para diagnóstico
                                        Log.d("LanguageSelectorComponent", "Cambiando idioma a: ${language.code}")
                                        
                                        // Usar la función de extensión para cambiar el idioma y reiniciar
                                        context.restartActivityWithLanguage(language.code)
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
} 