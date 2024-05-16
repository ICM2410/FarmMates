package mobile.mates.farmmates

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import com.javeriana.taller2_movil.models.RoutesResponse
import com.javeriana.taller2_movil.models.myRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobile.mates.farmmates.utils.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import mobile.mates.farmmates.utils.PermissionUtils.isPermissionGranted
import mobile.mates.farmmates.databinding.FragmentMapBinding
import mobile.mates.farmmates.models.LatLngGR
import mobile.mates.farmmates.models.OriginOrDestination
import mobile.mates.farmmates.models.googleRoutesRequest
import mobile.mates.farmmates.utils.PermissionUtils
import mobile.mates.farmmates.utils.RestUtils.getWeatherInfo
import mobile.mates.farmmates.utils.RestUtils.processWeatherResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Date
import kotlin.math.roundToInt
import kotlin.collections.Map as KotlinMap


class Map : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var mMap: GoogleMap
    private var permissionDenied = false
    private lateinit var polyline: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private var locations = myRoute.sites(mutableListOf())

    private lateinit var binding: FragmentMapBinding
    private lateinit var geocoder: Geocoder
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)


        if (temperatureSensor == null) {
            // El dispositivo no tiene un sensor de temperatura
            //binding.tempVal.text = "No hay sensor de temperatura"

        } else {
            val temperatureListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val temperatureValue = event?.values?.get(0)
                    // Procesar el valor de la temperatura aquí
                    //tempVal.text = temperatureValue.toString() + " °C"
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Manejar cambios en la precisión del sensor si es necesario
                }
            }

            sensorManager.registerListener(
                temperatureListener,
                temperatureSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (humiditySensor == null) {
            // El dispositivo no tiene un sensor de humedad relativa
            //binding.ambVal.text = "No hay sensor de humedad relativa"
        } else {
            val humidityListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val humidityValue = event?.values?.get(0)
                    // Procesar el valor de la humedad relativa aquí
                    //binding.ambVal.text = humidityValue.toString() + " %"
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // Manejar cambios en la precisión del sensor si es necesario
                }
            }

            sensorManager.registerListener(
                humidityListener,
                humiditySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager = getSystemService(
            SENSOR_SERVICE
        ) as SensorManager

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!

        lightEventListener = createLightSensorListener()

        //onCreate
        geocoder = Geocoder(baseContext)

        val file = File(filesDir, "location_data.json")

        if (file.exists()) {
            val isDeleted = file.delete()  // Elimina el archivo
            if (isDeleted) {
                // Si el archivo se eliminó correctamente, puedes crear uno nuevo (si es necesario)
                val newFile = File(filesDir, "location_data.json")
                newFile.createNewFile()  // Crea un archivo nuevo
            }
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.goBackToMenu.setOnClickListener { goToMainMenu() }
        binding.reportButton.setOnClickListener { goToReports() }
        binding.chatButton.setOnClickListener { goToChat() }

    }

    private fun goToReports() {
        startActivity(Intent(baseContext, ReportActivity::class.java))
    }

    private fun goToMainMenu() {
        startActivity(Intent(baseContext, MainActivity::class.java))
    }

    private fun goToChat() {
        startActivity(Intent(baseContext, ChatListActivity::class.java))
    }

    private fun createLightSensorListener(): SensorEventListener {
        val ret: SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (this@Map::mMap.isInitialized) {
                    if (event != null) {
                        if (event.values[0] < 1000) {
                            mMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    baseContext, R.raw.darkmap
                                )
                            )
                        } else {
                            mMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    baseContext, R.raw.oldmap
                                )
                            )
                        }
                    }
                }
            }

            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            }
        }
        return ret
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setPadding(0, 200, 20, 200)
        enableMyLocation()

        // Add a marker in Sydney and move the camera
        val bogota = LatLng(4.6, -74.06)
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setMapStyle(
            MapStyleOptions
                .loadRawResourceStyle(this, R.raw.oldmap)
        )

        // Obtener la última ubicación conocida del usuario
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            lastKnownLocation = location
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)

                // Iniciar la escucha de cambios de ubicación
                startLocationUpdates()
            }
        }

        mMap.setOnMapLongClickListener {
            findAddress(it)?.let { it1 -> addMarker(it.latitude, it.longitude, it1) }

            try {
                Toast.makeText(
                    this,
                    "Distancia:" + lastKnownLocation!!.distanceTo(convertLatLngToLocation(it))
                        .roundToInt().toString() + " metros",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("punto10", "Error al calcular la distancia: $e")
            }





            lifecycleScope.launch {
                callRequestRoute(
                    LatLng(
                        lastKnownLocation!!.latitude,
                        lastKnownLocation!!.longitude
                    ), it
                )

                delay(5000)


                drawPolylineOnMap(polyline)

            }


        }
        addAllZones()
        drawAgriculturalObjects()

        val thread = Thread { updateWeather() }
        thread.start()
    }

    fun callRequestRoute(origen: LatLng, destino: LatLng) {
        // Lanzar una corrutina para llamar a la función suspendida
        var response = ""
        CoroutineScope(Dispatchers.Main).launch {
            requestRoute(origen, destino)
        }
    }

    private fun convertLatLngToLocation(latLng: LatLng): Location {
        // Crea un nuevo objeto Location
        val location = Location("provider_name")

        // Establece la latitud y longitud a partir del objeto LatLng
        location.latitude = latLng.latitude
        location.longitude = latLng.longitude

        // Devuelve el objeto Location
        return location
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            createLocationRequest(),
            locationCallback,
            null
        )
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000 // Intervalo de actualización
        locationRequest.fastestInterval = 5000 // Intervalo más rápido
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val newLocation = locationResult.lastLocation
            if (newLocation != null && lastKnownLocation != null) {
                val distance = lastKnownLocation!!.distanceTo(newLocation)

                if (distance > 30) {
                    // Si la distancia es mayor a 30 metros, guarda la nueva ubicación
                    saveLocation(newLocation)
                    lastKnownLocation = newLocation
                }
            }
        }
    }

    private fun saveLocation(location: Location) {
        val route = myRoute.site(
            date = Date().toString(),
            latitud = location.latitude,
            longitud = location.longitude,
        )


        Log.i("punto10", route.toString())
        var gson = Gson()
        locations.list.add(route)
        try {
            val file = File(filesDir, "location_data.json")
            file.createNewFile()
            val fileWriter = FileWriter(file, false)
            fileWriter.write(gson.toJson(locations, myRoute.sites::class.java))
            fileWriter.close()
            Log.i("LocationActivity", "Ubicación guardada: $locations")

        } catch (e: Exception) {
            Log.e("punto10", "Error al guardar la ubicación: $e")
        }


    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {

        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            return
        }

        // 2. If if a permission rationale dialog should be shown
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            PermissionUtils.RationaleDialog.newInstance(
                LOCATION_PERMISSION_REQUEST_CODE, true
            ).show(supportFragmentManager, "dialog")
            return
        }

        // 3. Otherwise, request permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onMyLocationButtonClick(): Boolean {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onMyLocationClick(location: Location) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private fun addMarker(lat: Double, long: Double, title: String) {
        val location = LatLng(lat, long)

        // Convertir el recurso de imagen a un Bitmap
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.marcador)

        // Escalar el Bitmap al tamaño deseado
        // Ajusta los valores de width y height según tus necesidades
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 80, 80, false)

        // Convertir el Bitmap escalado a un BitmapDescriptor
        val iconDescriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap)

        // Añadir el marcador con el ícono escalado
        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(iconDescriptor)
        )

        // Mover la cámara al marcador
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
    }

    private fun findAddress(location: LatLng): String? {
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 2)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses.get(0)
            return addr.getAddressLine(0)
        }
        return null
    }

    private fun findLocation(address: String): LatLng? {
        val addresses = geocoder.getFromLocationName(address, 2)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses.get(0)
            return LatLng(
                addr.latitude, addr.longitude
            )
        }
        return null
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            lightEventListener, lightSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(lightEventListener)
    }

    private fun drawPolylineOnMap(ruta: String) {
        mMap.clear()
        addAllZones()
        drawAgriculturalObjects()

        Log.i("route", ruta)
        val decodedPath: List<LatLng> = PolyUtil.decode(ruta)
        val polylineOptions = PolylineOptions()
            .addAll(decodedPath)
            .color(ContextCompat.getColor(this, R.color.purple_200))
            .width(10f)

        mMap.addPolyline(polylineOptions)
    }

    suspend fun requestRoute(origin: LatLng, destination: LatLng) {
        // Crear un cliente HTTP
        val client = OkHttpClient()

        var gson = Gson()

        // Crear el cuerpo de la solicitud utilizando tus clases de datos
        val requestBodyData = googleRoutesRequest(
            OriginOrDestination(
                mobile.mates.farmmates.models.Location(
                    LatLngGR(
                        origin.latitude,
                        origin.longitude
                    )
                )
            ),
            OriginOrDestination(
                mobile.mates.farmmates.models.Location(
                    LatLngGR(
                        destination.latitude,
                        destination.longitude
                    )
                )
            ),
            "DRIVE",
            "TRAFFIC_AWARE"
        )

        // Convertir el cuerpo de la solicitud a JSON utilizando kotlinx.serialization
        val requestBodyJson = gson.toJson(requestBodyData)
        // Crear un RequestBody utilizando el JSON y el tipo MIME adecuado con la extensión toRequestBody
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        // Construir la URL para la solicitud
        val baseUrl = "https://routes.googleapis.com/directions/v2:computeRoutes"
        val url = "$baseUrl?key=AIzaSyAoixCSGBozzBjpV_njSFf3eTRTnFaW9kg"

        Log.e("route", url)
        // Construir la solicitud HTTP POST
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
            .build()

        // Realizar la solicitud HTTP de forma asíncrona utilizando corrutinas
        withContext(Dispatchers.IO) {
            polyline = try {
                val response: Response = client.newCall(request).execute()
                val body = response.body!!.string()
                Log.e("route", body)

                val polylineR = gson.fromJson(body, RoutesResponse::class.java)
                Log.e("route", polylineR.routes[0].polyline.encodedPolyline)

                polylineR.routes[0].polyline.encodedPolyline
            } catch (e: IOException) {
                "error"
            }
        }
    }

    private fun obtainSites(): MutableList<myRoute.site> {
        Log.i("punto10", "obtainSites")
        val file = File(filesDir, "location_data.json")


        if (file.exists()) {
            val json = file.readText()
            Log.i("punto10", "Obteniendo datos: $json")

            try {
                val gson = Gson()
                val sitesData = gson.fromJson(json, myRoute.sites::class.java)

                Log.w("punto10", "Datos decodificados: $sitesData")

                return sitesData.list

            } catch (e: Exception) {
                Log.e("punto10", "Error al decodificar los datos: $e")
            }

        }
        return mutableListOf()

    }

    private fun addAllZones() {
        // Obtener la instancia de Firestore
        val db = FirebaseFirestore.getInstance()

        // Leer todos los documentos de la colección 'zones'
        db.collection("zones").get().addOnSuccessListener { documents ->
            if (documents != null && !documents.isEmpty) {
                for (document in documents) {
                    // Extraer los puntos de cada documento y crear un polígono
                    val points =
                        document.get("points") as? List<KotlinMap<String, Double>> ?: listOf()
                    val polygonOptions = PolygonOptions()

                    for (point in points) {
                        val lat = point["latitude"] ?: 0.0
                        val lng = point["longitude"] ?: 0.0
                        polygonOptions.add(LatLng(lat, lng))
                    }

                    polygonOptions.strokeColor(Color.GRAY)
                    polygonOptions.strokeWidth(2f)
                    polygonOptions.fillColor(ContextCompat.getColor(this, R.color.translucent_blue))
                    val polygon = mMap.addPolygon(polygonOptions)

                    // Extraer datos del marcador si están disponibles y añadir al mapa
                    val markerData = document.get("marker") as? KotlinMap<String, Double>
                    if (markerData != null) {
                        val markerLat = markerData["latitude"] ?: 0.0
                        val markerLng = markerData["longitude"] ?: 0.0
                        // Cargar el drawable en un Bitmap
                        val originalBitmap =
                            BitmapFactory.decodeResource(baseContext.resources, R.drawable.zona)

                        // Redimensionar el Bitmap
                        val resizedBitmap =
                            Bitmap.createScaledBitmap(originalBitmap, 150, 150, false)

                        // Convertir el Bitmap redimensionado en un BitmapDescriptor
                        val icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)
                        mMap.addMarker(
                            MarkerOptions()
                                //Agregar un marcador personalizado
                                .icon(icon)
                                .position(LatLng(markerLat, markerLng))
                                .title(document.getString("name"))  // Usando el nombre de la zona como título del marcador
                        )
                    }
                }

                // Mover la cámara al primer marcador (opcional)
                documents.documents.firstOrNull()?.let {
                    val firstMarker = it.get("marker") as? KotlinMap<String, Double>
                    if (firstMarker != null) {
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLng(
                                LatLng(
                                    firstMarker["latitude"] ?: 0.0, firstMarker["longitude"] ?: 0.0
                                )
                            )
                        )
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(10f))  // Ajustar el zoom según la necesidad
                    }
                }
            } else {
                Log.d("Firestore", "No documents found in 'zones' collection")
            }
        }.addOnFailureListener { exception ->
            Log.d("Firestore", "Error getting documents: ", exception)
        }
    }


    private fun drawAgriculturalObjects() {
        val db = FirebaseFirestore.getInstance()
        val iconMap = mapOf(
            "Tanque de agua" to R.drawable.tanqueagua,
            "Cosechadora" to R.drawable.cosechadora,
            "Remolque" to R.drawable.remolque,
            "camion" to R.drawable.camion,
            "Tractor" to R.drawable.tractor2
        )

        db.collection("agriculturalObjects").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val type = document.getString("type") ?: "Unknown"
                    val lat = document.getDouble("latitude") ?: 0.0
                    val lng = document.getDouble("longitude") ?: 0.0
                    val position = LatLng(lat, lng)

                    // Cargar el drawable en un Bitmap
                    val originalBitmap = BitmapFactory.decodeResource(
                        baseContext.resources,
                        iconMap[type] ?: R.drawable.marcador
                    )

                    // Redimensionar el Bitmap
                    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)

                    // Convertir el Bitmap redimensionado en un BitmapDescriptor
                    val icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)


                    val markerOptions = MarkerOptions().position(position).title(type).icon(icon)
                    mMap.addMarker(markerOptions)
                }
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: $exception")
            }
    }

    @SuppressLint("SetTextI18n")
    private fun updateWeather() {
        Log.i("Weather", "Actualizando el clima")
        while (true) {
            try {
                if (lastKnownLocation != null) {
                    getWeatherInfo(
                        lastKnownLocation!!.latitude,
                        lastKnownLocation!!.longitude
                    ) { responseData ->
                        val weatherResponse = processWeatherResponse(responseData)

                        // Acceder a datos específicos
                        Log.i("Weather", "$weatherResponse")
                        if (weatherResponse != null) {
                            runOnUiThread {
                                binding.tempVal.text = "${weatherResponse.current.temperature2m} °C"
                                binding.ambVal.text =
                                    "${weatherResponse.current.relativeHumidity2m} %"
                            }
                        }


                    }
                }
                Thread.sleep(10000)
            } catch (e: Exception) {
                Log.e("Weather", "Error al obtener el clima: $e")
            }


        }


    }

}

