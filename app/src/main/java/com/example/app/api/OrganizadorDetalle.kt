package com.example.app.api

import com.example.app.model.UserInfo
import com.google.gson.annotations.SerializedName

data class OrganizadorDetalle(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre_organizacion") val nombre: String,
    @SerializedName("telefono_contacto") val telefonoContacto: String,
    @SerializedName("direccion_fiscal") val direccionFiscal: String? = null,
    @SerializedName("cif") val cif: String? = null,
    @SerializedName("nombre_usuario") val nombreUsuario: String? = null,
    @SerializedName("user") val user: com.example.app.model.UserInfo?,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false
) {
    // Función para obtener la URL del avatar de forma segura
    fun obtenerAvatarUrl(): String {
        return when {
            !avatarUrl.isNullOrEmpty() -> {
                if (avatarUrl.startsWith("http")) {
                    avatarUrl
                } else {
                    "https://api.example.com/storage/$avatarUrl"
                }
            }
            user?.avatarUrl != null -> {
                if (user.avatarUrl.startsWith("http")) {
                    user.avatarUrl
                } else {
                    "https://api.example.com/storage/${user.avatarUrl}"
                }
            }
            else -> "https://ui-avatars.com/api/?name=${nombre.take(2)}&background=0D8ABC&color=fff&size=256"
        }
    }
} 