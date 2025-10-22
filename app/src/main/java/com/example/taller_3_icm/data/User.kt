package com.example.taller_3_icm.data

data class User(
    var nombre: String = "",
    var apellido: String = "",
    var identificacion: String = "",
    var email: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var imagenUrl: String = "",
    var status: String = "offline"
)
