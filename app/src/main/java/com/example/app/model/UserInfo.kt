package com.example.app.model

import com.google.gson.annotations.SerializedName

data class UserInfo(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("nombre") val nombre: String = "",
    @SerializedName("apellido1") val apellido1: String = "",
    @SerializedName("apellido2") val apellido2: String? = null,
    @SerializedName("email") val email: String = "",
    @SerializedName("role") val role: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
) 