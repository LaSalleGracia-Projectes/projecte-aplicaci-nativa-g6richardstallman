package com.example.app.routes

import com.example.app.util.isValidEventoId
import com.example.app.util.getEventoIdErrorMessage

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")
    object RegisterOrganizador : Routes("register/organizador")
    object RegisterParticipante : Routes("register/participante")
    object Eventos : Routes("eventos")
    object MisEventos : Routes("mis_eventos")
    object CrearEvento : Routes("crear_evento")
    object MisTickets : Routes("mis_tickets")
    object Favoritos : Routes("favoritos")
    object Perfil : Routes("perfil")
    object HistorialCompras : Routes("historial_compras")
    
    // Ruta con parámetros
    object EventoDetalle : Routes("evento/{id}") {
        fun createRoute(id: String) = "evento/$id"
    }
    
    object EditarEvento : Routes("editar_evento/{eventoId}") {
        fun createRoute(eventoId: String): String {
            val id = eventoId.trim()
            
            android.util.Log.d("Routes", "Creando ruta EditarEvento con ID: '$id' (${id.javaClass.name})")
            
            if (!id.isValidEventoId()) {
                val errorMsg = id.getEventoIdErrorMessage()
                android.util.Log.e("Routes", errorMsg)
                throw IllegalArgumentException(errorMsg)
            }
            
            val rutaFinal = "editar_evento/$id"
            android.util.Log.d("Routes", "Ruta final creada: $rutaFinal")
            return rutaFinal
        }
    }
    
    object ForgotPassword : Routes("forgot_password")
    
    /** Ruta para detalle de organizador */
    object OrganizadorDetalle : Routes("organizador/{organizadorId}") {
        fun createRoute(organizadorId: Int): String = "organizador/$organizadorId"
    }

    object EventosCategoria : Routes("eventos_categoria/{categoria}") {
        fun createRoute(categoria: String) = "eventos_categoria/$categoria"
    }
    
    /** Ruta para comprar entradas */
    object ComprarEntradas : Routes("comprar_entradas/{eventoId}") {
        fun createRoute(eventoId: String): String {
            val id = eventoId.trim()
            
            android.util.Log.d("Routes", "Creando ruta ComprarEntradas con ID: '$id'")
            
            if (!id.isValidEventoId()) {
                val errorMsg = id.getEventoIdErrorMessage()
                android.util.Log.e("Routes", errorMsg)
                throw IllegalArgumentException(errorMsg)
            }
            
            return "comprar_entradas/$id"
        }
    }
}

// Función de utilidad para manejar tokens de forma segura
fun String?.safeTokenDisplay(maxLength: Int = 10): String {
    if (this.isNullOrEmpty()) return "vacío"
    return this.substring(0, minOf(maxLength, this.length)) + "..."
}