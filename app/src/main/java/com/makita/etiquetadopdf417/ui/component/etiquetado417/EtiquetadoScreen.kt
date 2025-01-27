
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Print

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.makita.etiquetadopdf417.ApiService
import com.makita.etiquetadopdf417.RegistraBitacoraEquisZ
import com.makita.etiquetadopdf417.RetrofitClient
import com.makita.etiquetadopdf417.RetrofitClient.apiService
import com.makita.etiquetadopdf417.ui.theme.GreenMakita
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID


@Composable
fun EtiquetadoScreen417(navController: NavHostController) {
  //  var text by remember { mutableStateOf("") }

    var text by remember { mutableStateOf(TextFieldValue("")) }
    var responseMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    var cargador by remember { mutableStateOf("") }
    var bateria by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }


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
            ButtonBluetooth(
                context,
                selectedItem ,
                cargador  ,
                bateria,
                onDeviceSelected = { device ->
                    selectedDevice = device // Actualiza el estado en el padre
                }
            )

           EscanearCodigo(
                text,
                onValueChange = { newValue -> text = newValue },
                responseMessage = responseMessage,
                onScanSuccess = {
                    Log.d("ETIQUETADO-Z", "Escaneo correcto")
                    responseMessage = "" // Limpiar mensajes de error
                },
                onScanError = { errorMessage -> // Modificado para aceptar un mensaje dinámico
                    Log.d("ETIQUETADO-Z", "Error en el escaneo: $errorMessage")
                    responseMessage = errorMessage.toString()
                },

                onClearError = { responseMessage = "" },
                focusRequester = focusRequester,
                navController = navController,
               selectedDevice = selectedDevice,
               onDeviceSelected = { selectedDevice = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ButtonBluetooth(
    context: Context,
    selectedItem : String ,
    cargador : String ,
    bateria : String,
    onDeviceSelected: (BluetoothDevice?) -> Unit) {

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val (printers, setPrinters) = remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }
        var selectedPrinterName by remember { mutableStateOf("") }

        ExtendedFloatingActionButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        (context as Activity),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        100
                    )
                } else {
                    showDialog = true
                    startBluetoothDiscovery(context, bluetoothAdapter, setPrinters)
                }
            },
            containerColor = Color(0xFF00909E),
            contentColor = Color.White,
            icon = {
                Icon(
                    Icons.Outlined.Bluetooth,
                    contentDescription = "Conectarse a Bluetooth",
                    modifier = Modifier.size(28.dp)
                )
            },
            text = {
                Text(
                    text = "Conectarse",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier
                .height(56.dp)
                .width(200.dp)
        )

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
                            onDeviceSelected(device)
                            selectedPrinterName = device.name ?: "Desconocida"
                            showDialog = false
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        // Mensaje de impresora seleccionada y botón de imprimir
        if (selectedPrinterName.isNotEmpty()) {

            Text(
                text = "IMPRESORA SELECCIONADA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GreenMakita // Usa el color personalizado
            )
            Text(
                text = selectedPrinterName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = GreenMakita // Usa el color personalizado
            )
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

    val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    } else {
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

fun prepararDatosImpresion(
    textoAnterior: String,
    serieDesde: String,
    serieHasta: String,
    letraFabrica: String,
    ean: String,
    itemEquivalente: String,
    cargador: String,
    bateria: String,
): Pair<String, String> {

    val itemNuevo = itemEquivalente.padEnd(20, '0')
    val dataPdf417 = "$itemNuevo$serieDesde$serieHasta$letraFabrica$ean$cargador"
    // Creamos una variable StringBuilder para armar el texto final
    val textoFinal = StringBuilder()


    val serieDesde = serieDesde
    val serieHasta = serieHasta
    val letraFabrica = letraFabrica
    val ean = ean

    Log.d("ETIQUETADO-Z", "dataPdf417 remacterizado: $dataPdf417")

    // Agregamos las variables separadas por comas (formato CSV)
    textoFinal.append("$textoAnterior,$serieDesde,$serieHasta,$letraFabrica,$ean,$itemEquivalente,$cargador,$bateria")
    Log.d("ETIQUETADO-Z", "textoFinal : $textoFinal")


    // Retornamos el texto final como un String
    return Pair(textoFinal.toString(), dataPdf417)
}


fun startBluetoothDiscovery(
    context: Context,
    bluetoothAdapter: BluetoothAdapter?,
    setDevices: (List<BluetoothDevice>) -> Unit
): BroadcastReceiver? {
    if (bluetoothAdapter == null) {
        Log.e("BluetoothDiscovery", "El adaptador Bluetooth es nulo.")
        return null
    }

    // Lista para almacenar dispositivos encontrados
    val foundDevices = mutableListOf<BluetoothDevice>()

    // Receptor para dispositivos encontrados
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
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
                        Log.w("BluetoothDiscovery", "Permiso BLUETOOTH_CONNECT no otorgado. Usando nombre vacío.")
                        "" // Nombre vacío si no hay permiso
                    }
                    Log.e("*MAKITA*", "isZebraPrinter. $deviceName")
                    // Filtrar impresoras Zebra
                    if (isZebraPrinter(deviceName) && !foundDevices.contains(it))
                    // if (  !foundDevices.contains(it))
                    {
                        Log.e("*MAKITA*", "isZebraPrinter. $deviceName")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearCodigo(
    scannedText: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    responseMessage: String,
    onScanSuccess: () -> Unit,
    onScanError: (Any?) -> Unit,
    onClearError: () -> Unit,
    focusRequester: FocusRequester,
    navController : NavController,
    selectedDevice: BluetoothDevice?, // Agregamos el parámetro
    onDeviceSelected: (BluetoothDevice?) -> Unit
) {
    val context = LocalContext.current
    var isScanned by remember { mutableStateOf(false) }
    var pdf417Image by remember { mutableStateOf<Bitmap?>(null) }
    var equivalenciasList by remember { mutableStateOf(listOf<String>()) }
    var cargador by remember { mutableStateOf("") }
    var bateria by remember { mutableStateOf("") }

    var itemAnterior by remember { mutableStateOf("") }
    var serieDesde by remember { mutableStateOf("") }
    var serieHasta by remember { mutableStateOf("") }
    var letraFabrica by remember { mutableStateOf("") }
    var ean by remember { mutableStateOf("") }
    var itemEquivalente by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val clearFields = {
        onValueChange(TextFieldValue(""))
        itemAnterior = ""
        serieDesde = ""
        serieHasta = ""
        ean = ""
        letraFabrica = ""
        selectedItem = ""
        cargador = ""
        bateria = ""
        onClearError() // Limpia cualquier mensaje de error adicional
        isScanned = false
    }

    Column(
      //  horizontalAlignment = Alignment.CenterHorizontally, // Alineación horizontal de los elementos
        verticalArrangement = Arrangement.spacedBy(1.dp), // Espaciado entre los elementos verticalmente
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState) // Activar scroll vertical
            .padding(horizontal = 16.dp),
        // Margen horizontal
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
            trailingIcon = {
                IconButton(
                    onClick = {
                        // Limpiar texto escaneado y mensaje de error
                        onValueChange(TextFieldValue(""))
                        itemAnterior = ""
                        serieDesde =""
                        serieHasta =""
                        ean = ""
                        letraFabrica = ""
                        selectedItem=""
                        cargador= ""
                        bateria =  ""
                        // Limpia el texto escaneado
                        onClearError() // Llama a la función para limpiar el mensaje de error
                        isScanned= false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete, // Usa el ícono de basurero predeterminado
                        contentDescription = "Limpiar",
                        tint = Color.Red
                    )
                }


            }
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
            itemAnterior = scannedText.text.substring(0, 20).trim()
            serieDesde = scannedText.text.substring(20, 29)
            serieHasta =scannedText.text.substring(20, 29)
            letraFabrica = scannedText.text.substring(38,39)
            ean = scannedText.text.substring(39,55)

            itemEquivalente = scannedText.text.substring(0, 20).trim()

            OutlinedTextField(
                value = itemAnterior,
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre los elementos
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = serieDesde,
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
                    value = serieHasta,
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
                    value = ean,
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

            Spacer(modifier = Modifier.height(16.dp))

            ClassicComboBox(
                items = equivalenciasList,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedItem.isNotEmpty()) {
                Cargador(cargador)
                Spacer(modifier = Modifier.height(16.dp))

                if (!bateria.isNullOrBlank() || !bateria.isNullOrEmpty()) {
                    Bateria(bateria)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ButtonImprimir(
                    context,
                    navController ,
                    itemAnterior ,
                    serieDesde,
                    serieHasta,
                    ean,
                    letraFabrica,
                    selectedItem ,
                    cargador  ,
                    bateria,
                    selectedDevice = selectedDevice,
                    onDeviceSelected = onDeviceSelected,
                    onPrintSuccess = clearFields // Pasamos la función de limpieza

                )
            }
        }
    }

    // Llamar a onScanSuccess cuando el escaneo sea correcto
    LaunchedEffect(scannedText.text) {
        if (scannedText.text.isNotEmpty()) {
            if (scannedText.text.length > 50) {
                // Llamada al servicio API cuando el escaneo es correcto
                try {


                    // Llamada a la API, reemplaza `getData` con la función que uses para hacer la solicitud
                    val response = apiService.getEquivalencias(scannedText.text.substring(0,20).trim()) // Asegúrate de definir este método en tu servicio

                    Log.d("*ETIQUETADO-Z" , "Response : $response"  )
                    if (response.isNotEmpty()) {
                        equivalenciasList = response.map { it.equivalenciaItem }
                        cargador = response.firstOrNull()?.cargador ?: "" // Obtén el primer elemento o una cadena vacía si la lista está vacía
                        bateria = response.firstOrNull()?.bateria ?: ""
                        onScanSuccess()
                        isScanned = true
                        pdf417Image = generarPDF417(scannedText.text)
                    } else {
                        // Si la respuesta falla, mostrar mensaje de error
                        onScanError("No se encontraron equivalencias para el código proporcionado.")
                        isScanned = false
                        pdf417Image = null

                    }
                } catch (e: Exception) {
                    // Manejo de excepciones si la llamada falla
                    Log.e("*ETIQUETADO-Z", "Error al hacer la llamada a la API: ${e.message}")
                    onScanError("Servicio no disponible")
                    isScanned = false
                    pdf417Image = null
                }
            } else {
                onScanError("Codigo incorrecto")
                isScanned = false
                pdf417Image = null
            }
        }
    }
}

@Composable
fun Titulo() {
    Column(
        modifier = Modifier
            .wrapContentHeight() // Ajusta solo a la altura del contenido
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EQUIS-Z",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicComboBox(
    items: List<String>, // Lista de elementos para el ComboBox
    selectedItem: String, // Elemento seleccionado
    onItemSelected: (String) -> Unit // Callback para manejar la selección
) {
    var expanded by remember { mutableStateOf(false) } // Controla si el menú está desplegado o no

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem, // Mostrar el elemento seleccionado
            onValueChange = {}, // No permitimos escribir manualmente
            label = { Text("SELECCIONE EQUIVALENCIA" , color = GreenMakita,
                fontWeight = FontWeight.Bold) },
            readOnly = true, // Solo lectura
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = !expanded } // Abrir o cerrar menú
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true } // Abrir menú al hacer clic
                .background(Color.White, shape = RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp), // Bordes redondeados
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White, // Fondo al estar enfocado
                unfocusedContainerColor = Color.White, // Fondo al estar desenfocado
                focusedIndicatorColor = Color(0xFFCCCCCC), // Color del borde enfocado (gris claro)
                unfocusedIndicatorColor = Color(0xFFDDDDDD), // Color del borde no enfocado (muy claro)
                cursorColor = GreenMakita // Color del cursor
            ),
            textStyle = TextStyle(
                fontSize = 18.sp, // Tamaño del texto
                color = Color.Red, // Color del texto
                fontFamily = FontFamily.Serif, // Familia de fuentes
                fontWeight = FontWeight.Bold // Peso de la fuente
            )
        )

        DropdownMenu(
            expanded = expanded, // Controla si se muestra o no
            onDismissRequest = { expanded = false }, // Cerrar menú al hacer clic fuera
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(12.dp))

        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemSelected(item) // Llamar al callback con el elemento seleccionado
                        expanded = false // Cerrar el menú
                    },
                    text = {
                        Text(
                            text = item,
                            color = Color.Black,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cargador(selectedItem: String) {

    OutlinedTextField(
        value = selectedItem,
        onValueChange = { /* No se permite la edición */ },
        label = { Text("CARGADOR ASIGNADO", color = GreenMakita, fontWeight = FontWeight.Bold ) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bateria(selectedItem: String) {

    OutlinedTextField(
        value = selectedItem,
        onValueChange = { /* No se permite la edición */ },
        label = { Text("BATERIA ASIGNADA", color = GreenMakita, fontWeight = FontWeight.Bold ) },
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
}

@Composable
fun ButtonImprimir(
    context: Context,
    navController: NavController,
    itemAnterior: String,
    serieDesde: String,
    serieHasta: String,
    ean: String,
    letraFabrica: String,
    selectedItem: String,
    cargador: String,
    bateria: String,
    selectedDevice: BluetoothDevice?,
    onDeviceSelected: (BluetoothDevice?) -> Unit,
    onPrintSuccess: () -> Unit
) {
    val (textoImpresion, dataPdf417) = prepararDatosImpresion(
        itemAnterior, serieDesde, serieHasta, letraFabrica, ean,
        selectedItem, cargador, bateria
    )

    // Log para ver los datos antes de ser pasados
    Log.d("ButtonImprimir", "Datos recibidos - itemAnterior: $itemAnterior, serieDesde: $serieDesde, serieHasta: $serieHasta, ean: $ean, letraFabrica: $letraFabrica, selectedItem: $selectedItem, cargador: $cargador, bateria: $bateria")
    Log.d("ButtonImprimir", "Texto Impresión: $textoImpresion, Data PDF417: $dataPdf417")

    // Verifica el permiso BLUETOOTH_CONNECT antes de permitir la impresión
    val hasBluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // No es necesario solicitar permiso en versiones anteriores
    }

    // Mostrar un mensaje de error si no se tiene el permiso
    if (!hasBluetoothConnectPermission) {
        Log.d("ButtonImprimir", "Permiso Bluetooth no otorgado.")
        Text("Permiso Bluetooth no otorgado. No se puede imprimir.")
        writeLogToFile(context, "Permiso Bluetooth no otorgado.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
            .background(Color.White, shape = RoundedCornerShape(30.dp)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ExtendedFloatingActionButton(
            onClick = {
                Log.d("ButtonImprimir", "Botón Imprimir presionado.")

                if (hasBluetoothConnectPermission) {
                    Log.d("ButtonImprimir", "Permiso Bluetooth otorgado.")

                    selectedDevice?.let { device ->
                        val printerLanguage = "ZPL" // Cambiar según el lenguaje soportado por la impresora
                        Log.d("ButtonImprimir", "Dispositivo seleccionado: ${device.name}, Dirección: ${device.address}")

                        printDataToBluetoothDevice(
                            device,
                            dataPdf417,
                            selectedItem,
                            context,
                            printerLanguage,
                            itemAnterior,
                            cargador,
                            serieDesde,
                            serieDesde,
                            ean ,
                            letraFabrica ,
                            bateria,
                            onPrintSuccess
                        )




                    }
                } else {
                    Log.d("ButtonImprimir", "Permiso Bluetooth no otorgado al presionar el botón.")
                    writeLogToFile(context, "Permiso Bluetooth no otorgado al presionar el botón.")
                    Toast.makeText(context, "Permiso Bluetooth no otorgado. No se puede imprimir.", Toast.LENGTH_SHORT).show()
                }
            },
            containerColor = Color(0xFF00909E),
            contentColor = Color.White,
            icon = {
                Icon(
                    Icons.Outlined.Print,
                    contentDescription = "Imprimir",
                    modifier = Modifier.size(28.dp)
                )
            },
            text = {
                Text(
                    text = "Imprimir",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier
                .height(56.dp)
                .width(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))



    }
}



/*
*   device,
    dataPdf417,
    selectedItem,
    context,
    printerLanguage,
    itemAnterior,
    cargador,
    onPrintSuccess
* */

@RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
fun printDataToBluetoothDevice(
    device: BluetoothDevice,
    data: String,
    selectedItem: String,
    context: Context,
    printerLanguage: String,
    itemAnterior: String,
    cargador: String,
    serieDesde : String,
    serieHasta: String,
    ean : String,
    letraFabrica :String,
    bateria: String,
    onPrintSuccess: () -> Unit


) {
    val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    Log.d("printDataToBluetoothDevice", "Datos recibidos - itemNuevo: $selectedItem, data: $data, cargador: $cargador")

    val data2 = selectedItem
    val CodigoConcatenado2 = data
    val CodigocomercialNN2 = cargador

    // Verificar que los datos no están vacíos antes de proceder
    Log.d("printDataToBluetoothDevice",
        "Impresión - data2: $data2, " +
                "CodigoConcatenado2: $CodigoConcatenado2, " +
                "CodigocomercialNN2: $CodigocomercialNN2")
    writeLogToFile(context, "Impresión - data2: $data2, CodigoConcatenado2: $CodigoConcatenado2, CodigocomercialNN2: $CodigocomercialNN2")

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Conectar al dispositivo Bluetooth
            Log.d("Bluetooth", "Intentando conectar al dispositivo ${device.name}, dirección ${device.address}")


            val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket.connect()

            if (bluetoothSocket.isConnected) {
                Log.d("Bluetooth", "Conexión exitosa.")

                val outputStream = bluetoothSocket.outputStream

                // Enviar los datos de impresión
                Log.d("", "Enviando datos de impresión: $CodigoConcatenado2")

                if (printerLanguage == "ZPL") {
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
                    Log.d("Bluetooth", "Datos enviados correctamente.")
                    // Preparar los datos para enviar al endpoint
                    val bitacora = RegistraBitacoraEquisZ(
                        itemAnterior = itemAnterior,
                        serieDesde = serieDesde, // Ejemplo, ajustar según tus datos
                        serieHasta = serieHasta, // Ejemplo, ajustar según tus datos
                        letraFabrica =letraFabrica,   // Ejemplo, ajustar según tus datos
                        ean = ean,
                        itemNuevo = selectedItem,
                        cargador = cargador,
                        bateria = bateria // Ejemplo, ajustar según tus datos
                    )

                    // Llamar al endpoint

                    try {
                        val response = apiService.insertaDataEquisZ(bitacora)

                        Log.d("API", "Datos insertados correctamente en la bitácora. $response")
                        Log.e("API", "Error al insertar datos: $response")
                    } catch (e: Exception) {
                        Log.e("API", "Error al llamar al endpoint: ${e.message}")
                    }
                    onPrintSuccess()

                }

                // Mensaje de éxito
                withContext(Dispatchers.Main) {
                    Log.d("ETIQUETADO-Z", "Impresión realizada con éxito.")

                    Toast.makeText(context, "Impresión Correcta", Toast.LENGTH_SHORT).show()



                }

            } else {
                // Si no se pudo conectar
                withContext(Dispatchers.Main) {
                    Log.d("Bluetooth", "No se pudo conectar al dispositivo.")
                    writeLogToFile(context, "No se pudo conectar al dispositivo Bluetooth")
                    Toast.makeText(context, "No se pudo conectar al dispositivo Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error al enviar datos: ${e.message}")
            writeLogToFile(context, "Error al enviar datos: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al enviar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            try {
                // bluetoothSocket.close()
                Log.d("Bluetooth", "Socket cerrado.")
                writeLogToFile(context, "Socket cerrado.")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error al cerrar el socket: ${e.message}")
                writeLogToFile(context, "Error al cerrar el socket: ${e.message}")
            }
        }
    }
}

private fun generarPDF417(texto: String): Bitmap? {
    return try {
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.encodeBitmap(texto, BarcodeFormat.PDF_417, 600, 300) // Puedes ajustar las dimensiones según necesites
    } catch (e: Exception) {
        Log.e("ETIQUETADO-Z", "Error generando PDF417: ${e.message}")
        null
    }
}
fun writeLogToFile(context: Context, logMessage: String) {
  /*  try {
        // Obtener el archivo de log
        val logFile = File(context.filesDir, "imprimir_logs.txt")

        // Si el archivo no existe, crearlo
        if (!logFile.exists()) {
            logFile.createNewFile()
        }

        // Abrir el archivo en modo de escritura (append)
        val writer = FileWriter(logFile, true)
        val bufferedWriter = BufferedWriter(writer)

        // Escribir el mensaje de log con fecha y hora
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        bufferedWriter.write("[$currentTime] $logMessage\n")
        bufferedWriter.close()

    } catch (e: IOException) {
        e.printStackTrace()
    }*/
}


fun deleteLogFile(context: Context): Boolean {
    val logFile = File(context.filesDir, "imprimir_logs.txt")
    return if (logFile.exists()) {
        logFile.delete()
    } else {
        false // Indica que el archivo no existía
    }
}

@Preview(showBackground = true)
@Composable
fun ProcesarSinCodigoScreenView() {
    val navController = rememberNavController()

    EtiquetadoScreen417(navController)
}


