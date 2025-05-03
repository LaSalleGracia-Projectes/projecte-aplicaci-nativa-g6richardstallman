package com.example.app.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "id")
    val id: String? = null,
    
    @SerializedName("nombre")
    @ColumnInfo(name = "nombre")
    val nombre: String? = null,
    
    @SerializedName("apellido1")
    @ColumnInfo(name = "apellido1")
    val apellido1: String? = null,
    
    @SerializedName("apellido2")
    @ColumnInfo(name = "apellido2")
    val apellido2: String? = null,
    
    @SerializedName("email")
    @ColumnInfo(name = "email")
    val email: String? = null,
    
    @SerializedName("role")
    @ColumnInfo(name = "role")
    val role: String? = null,
    
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
    
    @SerializedName("avatar")
    val avatar: String? = null
) {
    val avatarUrl: String?
        get() = avatar
}