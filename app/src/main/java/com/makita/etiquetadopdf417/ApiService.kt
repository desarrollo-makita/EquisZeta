package com.makita.etiquetadopdf417

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ProductoZeta(
    val productoZeta: String
)

// Respuesta de la API
data class Pdf417Response(
    val message: String,
    val zpl: String
)

data class EquivalenciaItem(
    @SerializedName("Id") val id: Int,
    @SerializedName("Item") val item: String,
    @SerializedName("EquivalenciaItem") val equivalenciaItem: String,
    @SerializedName("Descripcion") val descripcion: String,
    @SerializedName("Saldo") val saldo: Int,
    @SerializedName("Bateria") val bateria: String,
    @SerializedName("Cargador") val cargador: String
)


data class RegistraBitacoraEquisZ(
    val itemAnterior: String,
    val serieDesde: String,
    val serieHasta: String,
    val letraFabrica: String,
    val ean: String,
    val itemNuevo: String,
    val cargador: String,
    val bateria: String
)

// Interfaz para las llamadas API
interface ApiService {
    @POST("api/generar-pdf417")  // Endpoint de la API
    suspend fun generarPdf417(@Body productoZeta: ProductoZeta): Pdf417Response

    @GET("api/get-equivalencia-item/{item}")
    suspend fun getEquivalencias(@Path("item") item: String): List<EquivalenciaItem>


    @POST("api/inserta-data-bitacora-equisZ")  // Endpoint de la API
    suspend fun insertaDataEquisZ(@Body pdf417Request: RegistraBitacoraEquisZ): Unit
}
