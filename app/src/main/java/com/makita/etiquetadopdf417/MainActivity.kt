package com.makita.etiquetadopdf417

import EtiquetadoScreen417
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.makita.etiquetadopdf417.ui.component.etiquetado417.ImprimirEtiqueta

import com.makita.etiquetadopdf417.ui.theme.EtiquetadoPDF417Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EtiquetadoPDF417Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Log.d("*MAKITA*", "Iniciamos MainActivity - UbiAppTheme")
                    val navController = rememberNavController()

                    // Configuramos el NavHost
                    SetupNavGraph(navController = navController)
                }
            }
        }
    }
}

@Composable
fun SetupNavGraph(navController: androidx.navigation.NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "etiquetado" // Cambiamos el punto de inicio
    ) {
        // Agregamos la ruta para EtiquetadoScreen417
        composable("etiquetado") {
            EtiquetadoScreen417(navController)
        }

        composable("imprimir-etiqueta/{itemAnterior}/{serieDesde}/{serieHsasta}/{letraFabrica}/{ean}/{itemEquivalencia}/{cargador}/{bateria}")
        { backStackEntry ->
            val itemAnterior = backStackEntry.arguments?.getString("itemAnterior") ?: ""
            val serieDesde = backStackEntry.arguments?.getString("serieDesde") ?: ""
            val serieHasta = backStackEntry.arguments?.getString("serieHsasta") ?: ""
            val letraFabrica = backStackEntry.arguments?.getString("letraFabrica") ?: ""
            val ean = backStackEntry.arguments?.getString("ean") ?: ""


            val itemEquivalencia = backStackEntry.arguments?.getString("itemEquivalencia") ?: ""
            val cargador = backStackEntry.arguments?.getString("cargador") ?: ""
            val bateria = backStackEntry.arguments?.getString("bateria") ?: ""


            // Llamada al composable principal con los valores
            ImprimirEtiqueta(navController , contexto = LocalContext.current, itemAnterior,serieDesde,serieHasta,letraFabrica,ean, itemEquivalencia ,cargador , bateria)
        }

    }
}


