package com.makita.etiquetadopdf417.ui.component.etiquetado417


import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.makita.etiquetadopdf417.RegistraBitacoraEquisZ
import com.makita.etiquetadopdf417.RetrofitClient.apiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

@Composable
fun ImprimirEtiqueta(
    navController: NavController,
    contexto: Context,
    itemAnterior: String,
    serieDesde: String,
    serieHasta: String,
    letraFabrica: String,
    ean: String,
    selectedItem: String,
    cargador: String,
    bateria: String
) {

    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val (printers, setPrinters) = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedPrinterName by remember { mutableStateOf("") }
    var selectedDevice: BluetoothDevice? by remember { mutableStateOf(null) }
    // Fondo degradado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00909E),
                        Color(0xFF80CBC4)
                    )
                )
            )
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()  // Ajusta a la altura del contenido
                .padding(16.dp)
                .background(Color.White, shape = RoundedCornerShape(30.dp)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            TituloImprimir()

            val (textoImpresion , dataPdf417) = prepararDatosImpresion(
                itemAnterior,
                serieDesde,
                serieHasta,
                letraFabrica,
                ean,
                selectedItem,
                cargador,
                bateria
            )

            // Guardar el archivo plano con los datos
            guardarArchivoPlano(contexto, textoImpresion, "capturaItemZ.txt")



            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick =
            {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        (context as Activity),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        100
                    )
                }
                else
                {
                    showDialog = true
                    startBluetoothDiscovery(context, bluetoothAdapter, setPrinters)
                }
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor =  Color(0xFF00909E),
                    contentColor = Color.White   // Color del texto
                )

            )
            {
                Text("Seleccionar Impresora Bluetooth")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Diálogo de selección de dispositivos
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Seleccione una impresora") },
                    text = {
                        BluetoothDeviceList(
                            deviceList = printers,

                            onDeviceSelected = { device ->
                                selectedDevice = device
                                selectedPrinterName = device.name ?: "Desconocida"
                                showDialog = false
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false })
                        {
                            Log.d("ETIQUETADO-Z" , "Cierra modal")
                            Text("Cerrar" )
                        }
                    }
                )
            }

            if (selectedPrinterName.isNotEmpty()) {
                Log.d("ETIQUETADO-Z" , "Impresora Seleccionada $selectedPrinterName")
                Text("Impresora seleccionada: $selectedPrinterName", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedDevice?.let { device ->

                        val printerLanguage = "ZPL"  // Cambiar según el lenguaje soportado por la impresora
                        //ACA DEBO HACER LA CONSULTA A LA TABLA donde ire a buscar el codigoz con su equivalencia y su cargador


                        printDataToBluetoothDevice(
                            navController,
                            device,
                            dataPdf417,
                            context,
                            printerLanguage,
                            itemAnterior,
                            serieDesde,
                            serieHasta,
                            letraFabrica,
                            ean,
                            cargador,
                            selectedItem,
                            bateria
                            )

                    }
                },

                colors = ButtonDefaults.buttonColors(
                    containerColor =  Color(0xFF00909E),
                    contentColor = Color.White   // Color del texto
                ),
                enabled = selectedDevice != null
            ) {
                Text(
                    text = "Imprimir",
                    style = TextStyle(
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                    ),

                    )
            }

        }
    }
}

@Composable
fun TituloImprimir() {
    Column(
        modifier = Modifier
            .wrapContentHeight() // Ajusta solo a la altura del contenido
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "IMPRIMIR ETIQUETA",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00909E),
            textAlign = TextAlign.Center
        )

        Divider(
            color = Color(0xFFFF7F50),
            thickness = 2.dp,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
    }
}

fun prepararDatosImpresion(
    textoAnterior: String,
    serieDesde:String,
    serieHasta:String,
    letraFabrica : String,
    ean: String,
    itemEquivalente: String,
    cargador: String,
    bateria: String
): Pair<String, String>  {

    val itemNuevo = itemEquivalente.padEnd(20, '0')
    val dataPdf417 = "$itemNuevo$serieDesde$serieHasta$letraFabrica$ean$cargador"
    // Creamos una variable StringBuilder para armar el texto final
    val textoFinal = StringBuilder()


    val serieDesde = serieDesde
    val serieHasta = serieHasta
    val letraFabrica =letraFabrica
    val ean = ean

    Log.d("ETIQUETADO-Z","dataPdf417 remacterizado: $dataPdf417")

    // Agregamos las variables separadas por comas (formato CSV)
    textoFinal.append("$textoAnterior,$serieDesde,$serieHasta,$letraFabrica,$ean,$itemEquivalente,$cargador,$bateria")
    Log.d("ETIQUETADO-Z","textoFinal : $textoFinal")



    // Retornamos el texto final como un String
    return Pair(textoFinal.toString(), dataPdf417)
}

fun guardarArchivoPlano(
    contexto: Context,
    texto: String,
    nombreArchivo: String
) {
    try {
        // Usamos context.filesDir para acceder al almacenamiento interno de la app
        val archivo = File(contexto.filesDir, nombreArchivo)

        // Si el archivo no existe, lo creamos
        if (!archivo.exists()) {
            archivo.createNewFile()
            Log.d("guardarArchivoPlano", "Archivo creado: ${archivo.absolutePath}")
        }

        // Leemos el contenido actual del archivo
        val contenidoActual = archivo.readText()

        // Solo agregamos el texto si no está ya presente
        if (!contenidoActual.contains(texto)) {
            archivo.appendText(texto + "\n")
            Log.d("guardarArchivoPlano", "Datos guardados correctamente en el archivo: ${archivo.absolutePath}")
        } else {
            Log.d("guardarArchivoPlano", "El texto ya está presente en el archivo.")
        }
    } catch (e: Exception) {
        Log.e("guardarArchivoPlano", "Error al guardar el archivo: ${e.message}")
    }
}

fun startBluetoothDiscovery(
    context: Context,
    bluetoothAdapter: BluetoothAdapter?,
    setDevices: (List<BluetoothDevice>) -> Unit
): BroadcastReceiver? {
    if (bluetoothAdapter == null) {
        Log.e("ETIQUETADO-Z*", "El adaptador Bluetooth es nulo.")
        return null
    }

    // Lista para almacenar dispositivos encontrados
    val foundDevices = mutableListOf<BluetoothDevice>()

    // Receptor para dispositivos encontrados
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("ETIQUETADO-Z","BluetoothDevice.ACTION_FOUND ${BluetoothDevice.ACTION_FOUND}")
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    // Manejo seguro del nombre del dispositivo
                    val deviceName: String = if (ActivityCompat.checkSelfPermission(
                            context!!,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        it.name ?: "" // Obtener el nombre si hay permiso
                    } else {
                        Log.w("ETIQUETADO-Z", "Permiso BLUETOOTH_CONNECT no otorgado. Usando nombre vacío.")
                        ""
                    }

                    // Filtrar impresoras Zebra
                    if (isZebraPrinter(deviceName) && !foundDevices.contains(it))
                    {
                        Log.e("ETIQUETADO-Z", "Dispositivos encontrados. $deviceName")
                        foundDevices.add(it)
                        setDevices(foundDevices)
                    }
                }
            }
        }
    }

    // Registrar el receptor para detectar dispositivos Bluetooth
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    context.registerReceiver(receiver, filter)

    // Manejo explícito de permisos
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        != PackageManager.PERMISSION_GRANTED
    ) {
        Log.e("ETIQUETADO-Z", "Permisos insuficientes para escaneo Bluetooth.")
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
            1001
        )
        return null
    }

    try {
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e("BluetoothDiscovery", "No se pudo iniciar el descubrimiento Bluetooth.")
            context.unregisterReceiver(receiver)
            return null
        }
    } catch (e: SecurityException) {
        Log.e("ETIQUETADO-Z", "Error al iniciar el descubrimiento: ${e.message}")
        context.unregisterReceiver(receiver)
        return null
    }

    return receiver
}

private fun isZebraPrinter(deviceName: String): Boolean {
    return deviceName.contains("Zebra", ignoreCase = true) ||
            deviceName.startsWith("ZQ", ignoreCase = true)
}

@RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
fun printDataToBluetoothDevice(
    navController: NavController,
    device: BluetoothDevice,
    data: String,
    context: Context,
    printerLanguage: String,
    itemAnterior: String,
    serieDesde: String,
    serieHasta: String,
    letraFabrica : String,
    ean : String,
    itemNuevo: String,
    cargador : String,
    bateria : String

    ) {
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    val data2 =itemNuevo
    val CodigoConcatenado2 = data
    val CodigocomercialNN2  = cargador

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Conectar al dispositivo Bluetooth
            val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket.connect()

            if (bluetoothSocket.isConnected)
            {
                val outputStream = bluetoothSocket.outputStream

                // Enviar los datos de impresión
                Log.d("ETIQUETADO-Z", "printDataToBluetoothDevice $data")
                // Enviar comando de finalización si es ZPL o CPCL
                if (printerLanguage == "ZPL")
                {

                    Log.d("ETIQUETADO-Z", "Imprimir  $data")

                    val linea2 = "^XA\n " +
                            "^PW354 \n" +   // Ancho de la etiqueta (3 cm = 354 dots)
                            "^LL354 \n" +
                            "^FO50,25\n " +
                            "^ADN,15,13\n " +
                            "^FD$data2^FS\n " +
                            "^FO50,70\n " +
                            "^ADN,15,12\n " +
                            //"^B7N,5,10,2,5,N\n " +
                            //"^B7N,1,30,2,30,N\n  " +
                            //"^B7N,2,10,2,30,NY\n  " +
                            //"^FD$comercial^FS\n " +
                            "^B7N,5,10,2,20,N" +
                            "^FD$CodigoConcatenado2^FS " +
                            "^FO50,190\n " +
                            "^ADN,15,13\n " +
                            "^FD$CodigocomercialNN2^FS\n " +
                            "^XZ\n"

                    outputStream.write(linea2.toByteArray(Charsets.US_ASCII))
                    outputStream.flush()
                    // outputStream.write("^XZ".toByteArray(Charsets.US_ASCII)) // Finalizar trabajo en ZPL
                }

                // Mostrar un mensaje de éxito
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Impresión Correcta", Toast.LENGTH_SHORT).show()
                    navController.navigate("/etiquetado")
                    val request = RegistraBitacoraEquisZ(
                        itemAnterior ,
                        serieDesde,
                        serieHasta ,
                        letraFabrica ,
                        ean ,
                        itemNuevo,
                        cargador ,
                        bateria
                    )

                    try {
                        apiService.insertaDataEquisZ(request)
                        Log.d("ETIQUETADO-Z" , "inserta datos en tabla BitacoraEquisZ exitosamente")

                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al realizar la solicitud: ${e.message}", Toast.LENGTH_SHORT).show()
                        println("Error al realizar la solicitud: ${e.message}")
                    }

                }

                navController.navigate("/etiquetado")
            } else {
                // Si no se pudo conectar, mostrar un mensaje de error
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No se pudo conectar al dispositivo Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            // Manejo de errores
            withContext(Dispatchers.Main)
            {
                Log.e("Bluetooth", "Error al enviar datos: ${e.message}")

            }
        }
        finally
        {
            try
            {
                Log.e("Bluetooth", "Final de Bluetooth")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("Bluetooth", "Error al cerrar el socket: ${e.message}")
            }
        }
    }
}


@Composable
fun BluetoothDeviceList(
    deviceList: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {

    val context = LocalContext.current
    // Verifica el permiso BLUETOOTH_CONNECT en dispositivos con Android 12 (API 31) o superior
    Log.d("ETIQUETADO-Z", "BluetoothDeviceList")

    val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    } else
    {
        Log.d("ETIQUETADO-Z", "permisos OK")
        true // No se requiere permiso en versiones anteriores
    }



    // Solo muestra la lista si el permiso es otorgado o si el sistema no lo requiere
    if (hasBluetoothConnectPermission) {
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(deviceList) { device ->
                Text(
                    text = device.name ?: "Dispositivo desconocido",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceSelected(device) }
                        .padding(8.dp)
                )
            }
        }
    } else {
        // Mostrar mensaje o manejar el caso en el que no se tiene el permiso
        Log.d("ETIQUETADO-Z", " Permisos Denegados")
        Text("Permiso Bluetooth no otorgado. No se pueden mostrar los dispositivos.")
    }
}

