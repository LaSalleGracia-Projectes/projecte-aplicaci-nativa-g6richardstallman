package com.example.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TicketsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketsViewModel::class.java)) {
            return TicketsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 