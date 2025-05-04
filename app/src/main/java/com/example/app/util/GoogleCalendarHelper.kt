package com.example.app.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.example.app.model.tickets.TicketCompra
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GoogleCalendarHelper(private val context: Context) {
    private val SCOPES = Collections.singleton(CalendarScopes.CALENDAR)

    suspend fun getCalendarService(account: GoogleSignInAccount): Calendar {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GoogleCalendarHelper", "Iniciando la creación del servicio de Calendar para cuenta: ${account.email}")
                
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    SCOPES
                ).setSelectedAccount(account.account)

                Log.d("GoogleCalendarHelper", "Credencial obtenida, construyendo servicio Calendar")
                
                val calendarService = Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("Tickets App")
                    .build()
                
                Log.d("GoogleCalendarHelper", "Servicio Calendar construido exitosamente")
                
                calendarService
            } catch (e: Exception) {
                Log.e("GoogleCalendarHelper", "Error al crear servicio Calendar: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * Método directo para añadir eventos al calendario usando Intent
     * Este método es más simple y no requiere la API de Google Calendar
     */
    suspend fun addEventToCalendarUsingIntent(ticket: TicketCompra): Boolean {
        return try {
            Log.d("GoogleCalendarHelper", "Intentando añadir evento usando Intent")
            
            // Parsear la fecha y hora - manejo seguro de valores nulos
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            
            // Usar fecha actual si hay problemas de parsing
            val startDate = try {
                dateFormat.parse(ticket.evento.fecha) ?: Date()
            } catch (e: Exception) {
                Log.e("GoogleCalendarHelper", "Error al parsear fecha, usando fecha actual")
                Date()
            }
            
            val startTime = try {
                timeFormat.parse(ticket.evento.hora) ?: Date()
            } catch (e: Exception) {
                Log.e("GoogleCalendarHelper", "Error al parsear hora, usando hora actual")
                Date()
            }
            
            // Combinar fecha y hora
            val calendar = java.util.Calendar.getInstance()
            calendar.time = startDate
            
            val timeCalendar = java.util.Calendar.getInstance()
            timeCalendar.time = startTime
            
            try {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, timeCalendar.get(java.util.Calendar.HOUR_OF_DAY))
                calendar.set(java.util.Calendar.MINUTE, timeCalendar.get(java.util.Calendar.MINUTE))
                calendar.set(java.util.Calendar.SECOND, timeCalendar.get(java.util.Calendar.SECOND))
            } catch (e: Exception) {
                Log.e("GoogleCalendarHelper", "Error al establecer la hora: ${e.message}")
                // Continuar con la fecha como está
            }
            
            // Hora de inicio
            val beginTime = calendar.timeInMillis
            
            // Hora de fin (2 horas después)
            calendar.add(java.util.Calendar.HOUR_OF_DAY, 2)
            val endTime = calendar.timeInMillis
            
            // Crear intent para añadir evento
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                .putExtra(CalendarContract.Events.TITLE, ticket.evento.nombre)
                .putExtra(CalendarContract.Events.DESCRIPTION, "Entrada para ${ticket.evento.nombre}")
                .putExtra(CalendarContract.Events.EVENT_LOCATION, "Ubicación del evento")
            
            // Flags importantes para asegurar que se abra correctamente
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Ejecutar en el hilo principal
            withContext(Dispatchers.Main) {
                try {
                    Log.d("GoogleCalendarHelper", "Iniciando Intent de calendario")
                    context.startActivity(intent)
                    Log.d("GoogleCalendarHelper", "Intent iniciado correctamente")
                    true
                } catch (e: Exception) {
                    Log.e("GoogleCalendarHelper", "Error al iniciar Intent: ${e.message}")
                    
                    // Intento alternativo más básico si el anterior falla
                    try {
                        Log.d("GoogleCalendarHelper", "Intentando método alternativo con Intent básico")
                        val simpleIntent = Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.Events.TITLE, ticket.evento.nombre)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        
                        context.startActivity(simpleIntent)
                        Log.d("GoogleCalendarHelper", "Intent básico iniciado correctamente")
                        true
                    } catch (e2: Exception) {
                        Log.e("GoogleCalendarHelper", "Error también con Intent alternativo: ${e2.message}")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleCalendarHelper", "Error global al preparar intent: ${e.message}")
            e.printStackTrace()
            false
        }
    }
} 