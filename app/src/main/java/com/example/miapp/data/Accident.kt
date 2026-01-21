package com.example.miapp.data

import android.graphics.Bitmap
import java.util.*

/**
 * Modelo de datos para representar un accidente de tránsito
 */
data class Accident(
    val id: String = UUID.randomUUID().toString(),
    val tipoAccidente: String,
    val fecha: Date,
    val matricula: String,
    val nombreConductor: String,
    val cedulaConductor: String,
    val observaciones: String,
    val fotos: List<Bitmap>,
    val latitud: Double,
    val longitud: Double
)