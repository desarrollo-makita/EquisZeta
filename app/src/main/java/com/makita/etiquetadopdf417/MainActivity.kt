package com.makita.etiquetadopdf417

import EtiquetadoScreen417
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
            EtiquetadoScreen417()
        }

        // Puedes agregar más rutas si es necesario en el futuro
    }
}


