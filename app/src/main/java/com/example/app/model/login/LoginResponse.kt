package com.example.app.model.login

import com.example.app.model.User
import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("user") val user: User? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("userRole") val userRole: String? = null,
    @SerializedName("user_role") val userRoleAlt: String? = null,
    @SerializedName("needs_registration") val needsRegistration: Boolean? = null
)

data class Auth(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = "Bearer"
)

data class ProfileData(
    @SerializedName("idOrganizador") val idOrganizador: Int? = null,
    @SerializedName("nombre_organizacion") val nombreOrganizacion: String? = null,
    @SerializedName("telefono_contacto") val telefonoContacto: String? = null,
    @SerializedName("idParticipante") val idParticipante: Int? = null,
    @SerializedName("dni") val dni: String? = null,
    @SerializedName("telefono") val telefono: String? = null
)