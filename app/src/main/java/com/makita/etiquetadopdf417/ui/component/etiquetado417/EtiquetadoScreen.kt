import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.makita.etiquetadopdf417.ui.theme.GreenMakita
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@Composable
fun EtiquetadoScreen417() {
  //  var text by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var responseMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    var selectedPrinterName by remember { mutableStateOf("") }

    var item by remember { mutableStateOf("") }
    var serieInicio by remember { mutableStateOf("") }
    var serieFinal by remember { mutableStateOf("") }
    var letraFabrica by remember { mutableStateOf("") }
    var ean by remember { mutableStateOf("") }

    val (printers, setPrinters) = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    var selectedDevice: BluetoothDevice? by remember { mutableStateOf(null) }

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Fondo degradado
    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF00909E),
                    Color(0xFF80CBC4)
                )
            )
        )
        .padding(10.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .background(Color.White, shape = RoundedCornerShape(30.dp)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Titulo()
            Separar()

            EscanearCodigo(
                text,
                onValueChange = { newValue -> text = newValue },
                responseMessage = responseMessage,
                onScanSuccess = {
                    Log.d("*MAKITA*", "Escaneo correcto")
                    responseMessage = "" // Limpiar mensajes de error
                },
                onScanError = {
                    Log.d("*MAKITA*", "Error en el escaneo")
                    responseMessage = "El código escaneado es incorrecto."
                },
                focusRequester = focusRequester
            )

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
                            Log.d("*MAKITA*" , "Cierra modal")
                            Text("Cerrar" )
                        }
                    }
                )
            }

            if (selectedPrinterName.isNotEmpty()) {
                Log.d("*MAKITA*" , "Impresora Seleccionada $selectedPrinterName")
                Text("Impresora seleccionada: $selectedPrinterName", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    selectedDevice?.let { device ->

                        item =  text.text

                        Log.d("*MAKITA*" , "este es el resultado de extractedText $item")
                        val printerLanguage = "ZPL"  // Cambiar según el lenguaje soportado por la impresora

                        val codigoCargador = (item.trim() ?: "").padEnd(20, '0') + (serieInicio?.trim() ?: "").padEnd(10, '0') + (serieFinal?.trim() ?: "").padEnd(10, '0') + (letraFabrica?.trim() ?: "").padEnd(13, '0') + "0" + (serieInicio?.trim() ?: "").padEnd(10, '0') // CodigoChile"0000000000" +    // Codigo Comercial"000000000000000000000000000000"  // Nro Proforma
                        printDataToBluetoothDevice(device, item, context, printerLanguage, serieInicio , codigoCargador,"AA")
                        item = ""
                        serieInicio = ""
                        serieFinal = ""
                        letraFabrica = ""
                        ean = ""

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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearCodigo(
    scannedText: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    responseMessage: String,
    onScanSuccess: () -> Unit,
    onScanError: () -> Unit,
    focusRequester: FocusRequester
) {
    // Estado para controlar si los campos deben mostrarse
    var isScanned by remember { mutableStateOf(false) }
    var pdf417Image by remember { mutableStateOf<Bitmap?>(null) }

    Log.d("*MAKITA*", "EscanearCodigo ${scannedText.text}")

    Column(
      //  horizontalAlignment = Alignment.CenterHorizontally, // Alineación horizontal de los elementos
        verticalArrangement = Arrangement.spacedBy(1.dp), // Espaciado entre los elementos verticalmente
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        // Campo de texto para escanear
        TextField(
            value = scannedText,
            label = { Text("Escanear código", color = Color(0xFF00909E)) },
            onValueChange = onValueChange,
            readOnly = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .focusRequester(focusRequester),
            singleLine = true,
        )

        // Mostrar mensaje de error si el código es incorrecto
        if (responseMessage.isNotEmpty()) {
            Text(
                text = responseMessage,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar los campos solo si se ha escaneado un código
        if (isScanned) {
            OutlinedTextField(
                value = scannedText.text.substring(0, 20).trim(),
                onValueChange = { /* No se permite la edición */ },
                label = { Text("ITEM", color = GreenMakita, fontWeight = FontWeight.Bold ) },
                readOnly = true, // Campo de solo lectura
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontSize = 18.sp, // Tamaño del texto
                    color = Color.Red, // Color del texto
                    fontFamily = FontFamily.Serif, // Familia de fuentes
                    fontWeight = FontWeight.Bold // Peso de la fuente
                ),
                enabled = false,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color(0xFF00909E),
                    unfocusedBorderColor = Color.Gray
                )
            )

            // Espacio entre campos
            Spacer(modifier = Modifier.height(16.dp))

            // Fila para "Serie Desde" y "Serie Hasta"
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre los elementos
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = scannedText.text.substring(20, 29),
                    onValueChange = { /* No se permite la edición */ },
                    label = {
                        Text("SERIE DESDE",
                            color = GreenMakita,
                            fontWeight = FontWeight.Bold )},
                    readOnly = true, // Campo de solo lectura
                    modifier = Modifier
                        .weight(1f) // Asigna un 50% del ancho disponible
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp),// Definir altura
                    textStyle = TextStyle(
                        fontSize = 18.sp, // Tamaño del texto
                        color = Color.Red, // Color del texto
                        fontFamily = FontFamily.Serif, // Familia de fuentes
                        fontWeight = FontWeight.Bold // Peso de la fuente
                    ),
                    enabled = false,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00909E),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = scannedText.text.substring(20, 29),
                    onValueChange = { /* No se permite la edición */ },
                    label = { Text("SERIE HASTA" , color = GreenMakita,
                        fontWeight = FontWeight.Bold ) },
                    readOnly = true, // Campo de solo lectura
                    modifier = Modifier
                        .weight(1f) // Asigna un 50% del ancho disponible
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp),// Definir altura
                    textStyle = TextStyle(
                        fontSize = 18.sp, // Tamaño del texto
                        color = Color.Red, // Color del texto
                        fontFamily = FontFamily.Serif, // Familia de fuentes
                        fontWeight = FontWeight.Bold // Peso de la fuente
                    ),
                    enabled = false,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF00909E),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                    value = scannedText.text.substring(39).trim(),
            onValueChange = { /* No se permite la edición */ },
            label = {
                Text("EAN",
                    color = GreenMakita ,
                    fontWeight = FontWeight.Bold)},
            textStyle = TextStyle(
                fontSize = 18 .sp, // Tamaño del texto
                color = Color.Red, // Color del texto
                fontFamily = FontFamily.Serif, // Familia de fuentes
                fontWeight = FontWeight.Bold // Peso de la fuente
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenMakita,     // Borde al enfocar en GreenMakita
                    unfocusedBorderColor = GreenMakita,   // Borde sin enfoque en GreenMakita
                )
            )
        }
    }




    // Llamar a onScanSuccess cuando el escaneo sea correcto
    LaunchedEffect(scannedText.text) {
        // Solo validar si el texto no está vacío y ha cambiado
        if (scannedText.text.isNotEmpty()) {
            if (scannedText.text.length > 50) {
                onScanSuccess()
                isScanned = true // Hacer visibles los campos de texto
                pdf417Image = generarPDF417(scannedText.text) // Generar y mostrar el PDF417
            } else {
                onScanError() // Invocar el callback de error
                isScanned = false
                pdf417Image = null // Limpiar la imagen si el escaneo no es válido
            }
        }
    }
}


@Composable
fun Titulo() {
    Text(
        text = "GENERAR ETIQUETA ITEM Z ",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .width(500.dp)
            .height(70.dp)
            .padding(top = 40.dp)
            .padding(start = 50.dp),
        color = Color(0xFF00909E)
    )
}

@Composable
fun Separar() {
    Divider(
        color = Color(0xFFFF7F50),
        thickness = 2.dp,
        modifier = Modifier
            .padding(vertical = 16.dp)
            .padding(10.dp),
    )
}

private fun generarPDF417(texto: String): Bitmap? {
    return try {
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.encodeBitmap(texto, BarcodeFormat.PDF_417, 600, 300) // Puedes ajustar las dimensiones según necesites
    } catch (e: Exception) {
        Log.e("EscanearCodigo", "Error generando PDF417: ${e.message}")
        null
    }
}


fun startBluetoothDiscovery(
    context: Context,
    bluetoothAdapter: BluetoothAdapter?,
    setDevices: (List<BluetoothDevice>) -> Unit
): BroadcastReceiver? {
    if (bluetoothAdapter == null) {
        Log.e("*MAKITA00*", "El adaptador Bluetooth es nulo.")
        return null
    }

    // Lista para almacenar dispositivos encontrados
    val foundDevices = mutableListOf<BluetoothDevice>()

    // Receptor para dispositivos encontrados
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("*MAKITA00*","BluetoothDevice.ACTION_FOUND ${BluetoothDevice.ACTION_FOUND}")
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
                        Log.w("MAKITA00", "Permiso BLUETOOTH_CONNECT no otorgado. Usando nombre vacío.")
                        ""
                    }

                    // Filtrar impresoras Zebra
                    if (isZebraPrinter(deviceName) && !foundDevices.contains(it))
                    // if (  !foundDevices.contains(it))
                    {
                        Log.e("*MAKITA00*", "Dispositivos encontrados. $deviceName")
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
        Log.e("BluetoothDiscovery", "Permisos insuficientes para escaneo Bluetooth.")
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
        Log.e("BluetoothDiscovery", "Error al iniciar el descubrimiento: ${e.message}")
        context.unregisterReceiver(receiver)
        return null
    }

    return receiver
}

private fun isZebraPrinter(deviceName: String): Boolean {
    return deviceName.contains("Zebra", ignoreCase = true) ||
            deviceName.startsWith("ZQ", ignoreCase = true)
}


@Composable
fun BluetoothDeviceList(
    deviceList: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {

    val context = LocalContext.current
    // Verifica el permiso BLUETOOTH_CONNECT en dispositivos con Android 12 (API 31) o superior
    Log.d("*MAKITA00*", " version blue 1")

    val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    } else
    {
        Log.d("*MAKITA*", " si tiene permiso")
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
        Log.d("*MAKITA*", " permisos blue")
        Text("Permiso Bluetooth no otorgado. No se pueden mostrar los dispositivos.")
    }
}


@RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
fun printDataToBluetoothDevice(
    device: BluetoothDevice,
    data: String,
    context: Context,
    printerLanguage: String , // Lenguaje de Programacion Zebra  ZPL (ZPL, CPCL o ESC/POS)
    comercial: String,
    CodigoConcatenado: String,
    CodigocomercialNN: String
) {
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Conectar al dispositivo Bluetooth
            val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket.connect()

            if (bluetoothSocket.isConnected)
            {
                val outputStream = bluetoothSocket.outputStream

                // Enviar los datos de impresión
                Log.d("*MAKITA*", "printDataToBluetoothDevice $data")
                 // Enviar comando de finalización si es ZPL o CPCL
                if (printerLanguage == "ZPL")
                {

                    Log.d("*MAKITA*", "Imprimir  $CodigoConcatenado")

                    val linea2 = "^XA\n " +
                            "^PW354 \n" +   // Ancho de la etiqueta (3 cm = 354 dots)
                            "^LL354 \n" +
                            "^FO50,25\n " +
                            "^ADN,15,13\n " +
                            "^FD$data^FS\n " +
                            "^FO50,70\n " +
                            "^ADN,15,12\n " +
                            "^B7N,5,10,2,20,N" +
                            "^FD$CodigoConcatenado^FS " +
                            "^FO50,190\n " +
                            "^ADN,15,13\n " +
                            "^FD$CodigocomercialNN^FS\n " +
                            "^XZ\n"

                    outputStream.write(linea2.toByteArray(Charsets.US_ASCII))
                    outputStream.flush()

                }

                // Mostrar un mensaje de éxito
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Impresión Correcta", Toast.LENGTH_SHORT).show()
                }
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

@Preview(showBackground = true)
@Composable
fun PreviewEtiquetadoScreen417() {
    EtiquetadoScreen417()
}
