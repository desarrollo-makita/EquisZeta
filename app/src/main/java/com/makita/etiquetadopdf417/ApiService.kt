package com.makita.etiquetadopdf417

import retrofit2.http.Body
import retrofit2.http.POST

data class ProductoZeta(
    val productoZeta: String
)

// Respuesta de la API
data class Pdf417Response(
    val message: String,
    val zpl: String
)

// Interfaz para las llamadas API
interface ApiService {
    @POST("api/generar-pdf417")  // Endpoint de la API
    suspend fun generarPdf417(@Body productoZeta: ProductoZeta): Pdf417Response
}