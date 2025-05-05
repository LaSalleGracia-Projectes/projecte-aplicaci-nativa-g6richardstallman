package com.example.app.view.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.R

/**
 * TopAppBar común con soporte para botón de idioma
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showLanguageButton: Boolean = true,
    backgroundColor: Color = Color.White,
    contentColor: Color = Color(0xFFE53935),
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = contentColor
                    )
                }
            }
        },
        actions = {
            // Añadir acciones personalizadas
            actions()
            
            // Añadir botón de idioma si se solicita
            if (showLanguageButton) {
                LanguageButton(tint = contentColor)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}

/**
 * CenterAlignedTopAppBar común con soporte para botón de idioma
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonCenterAlignedTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showLanguageButton: Boolean = true,
    backgroundColor: Color = Color.White,
    contentColor: Color = Color(0xFFE53935),
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = contentColor
                    )
                }
            }
        },
        actions = {
            // Añadir acciones personalizadas
            actions()
            
            // Añadir botón de idioma si se solicita
            if (showLanguageButton) {
                LanguageButton(tint = contentColor)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
} 