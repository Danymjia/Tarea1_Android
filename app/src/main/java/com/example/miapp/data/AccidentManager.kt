package com.example.miapp.data

import androidx.compose.runtime.mutableStateListOf

object AccidentManager {
    private val accidents = mutableStateListOf<Accident>()

    fun addAccident(accident: Accident) {
        accidents.add(0, accident)
    }

    fun getAccidents(): List<Accident> {
        return accidents
    }

    fun getAccidentById(id: String): Accident? {
        return accidents.find { it.id == id }
    }

    fun clearAll() {
        accidents.clear()
    }
}