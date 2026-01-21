package com.example.miapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.miapp.data.Accident
import com.example.miapp.data.AccidentManager
import com.example.miapp.ui.theme.MiAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RegisterAccidentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiAppTheme {
                RegisterAccidentScreen(onFinish = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RegisterAccidentScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Estados del formulario
    var tipoAccidente by remember { mutableStateOf("Choque") }
    var expanded by remember { mutableStateOf(false) }
    var fechaSiniestro by remember { mutableStateOf(Date()) }
    var matricula by remember { mutableStateOf("") }
    var nombreConductor by remember { mutableStateOf("") }
    var cedulaConductor by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var fotos by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }

    // URIs para cámara
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Permisos
    val permissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Cliente de ubicación
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    isLoadingLocation = false
                }
            }
        }
    }

    // Launcher para cámara
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let { uri ->
                val bitmap = context.contentResolver
                    .openInputStream(uri)?.use { inputStream ->
                        android.graphics.BitmapFactory.decodeStream(inputStream)
                    }
                bitmap?.let {
                    fotos = fotos + it
                }
            }
        }
    }

    fun createImageFile(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null)
        val imageFile = File.createTempFile(
            "ACCIDENT_${timeStamp}_",
            ".jpg",
            storageDir
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        isLoadingLocation = true
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setMaxUpdates(1)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun vibrarTelefono() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(5000)
        }
    }

    fun guardarAccidente() {
        if (matricula.isEmpty() || nombreConductor.isEmpty() || cedulaConductor.isEmpty()) {
            Toast.makeText(context, "Por favor complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLocation == null) {
            Toast.makeText(context, "Por favor obtenga la ubicación GPS", Toast.LENGTH_SHORT).show()
            return
        }

        val accident = Accident(
            tipoAccidente = tipoAccidente,
            fecha = fechaSiniestro,
            matricula = matricula,
            nombreConductor = nombreConductor,
            cedulaConductor = cedulaConductor,
            observaciones = observaciones,
            fotos = fotos,
            latitud = currentLocation!!.latitude,
            longitud = currentLocation!!.longitude
        )

        AccidentManager.addAccident(accident)
        vibrarTelefono()

        Toast.makeText(context, "Accidente registrado exitosamente", Toast.LENGTH_LONG).show()
        onFinish()
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Accidente") },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Tipo de accidente
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tipo de Accidente *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = tipoAccidente,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("Choque", "Colisión", "Atropello").forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo) },
                                    onClick = {
                                        tipoAccidente = tipo
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fecha (solo mostrar, usa fecha actual)
            OutlinedTextField(
                value = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(fechaSiniestro),
                onValueChange = {},
                label = { Text("Fecha del Siniestro") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.DateRange, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Matrícula
            OutlinedTextField(
                value = matricula,
                onValueChange = { matricula = it.uppercase() },
                label = { Text("Matrícula del Vehículo *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Build, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre conductor
            OutlinedTextField(
                value = nombreConductor,
                onValueChange = { nombreConductor = it },
                label = { Text("Nombre del Conductor *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cédula
            OutlinedTextField(
                value = cedulaConductor,
                onValueChange = { cedulaConductor = it },
                label = { Text("Cédula del Conductor *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.AccountCircle, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Observaciones
            OutlinedTextField(
                value = observaciones,
                onValueChange = { observaciones = it },
                label = { Text("Observaciones") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fotografías
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Fotografías (${fotos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                if (permissions.allPermissionsGranted) {
                                    val newUri = createImageFile()
                                    imageUri = newUri
                                    takePictureLauncher.launch(newUri)
                                } else {
                                    permissions.launchMultiplePermissionRequest()
                                }
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tomar Foto")
                        }
                    }

                    if (fotos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fotos) { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto del accidente",
                                    modifier = Modifier
                                        .size(120.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ubicación GPS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentLocation != null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ubicación GPS *",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                if (permissions.allPermissionsGranted) {
                                    requestLocation()
                                } else {
                                    permissions.launchMultiplePermissionRequest()
                                }
                            },
                            enabled = !isLoadingLocation
                        ) {
                            Icon(Icons.Default.Place, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isLoadingLocation) "Obteniendo..." else "Obtener")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingLocation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Obteniendo ubicación GPS...")
                        }
                    } else {
                        currentLocation?.let { location ->
                            Text("📍 Latitud: ${location.latitude}")
                            Text("📍 Longitud: ${location.longitude}")
                            Text("🎯 Precisión: ${location.accuracy} m")
                        } ?: run {
                            Text(
                                "⚠️ Ubicación no obtenida",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Guardar
            Button(
                onClick = { guardarAccidente() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "GUARDAR ACCIDENTE",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}