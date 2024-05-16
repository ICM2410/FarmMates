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
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
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
import mobile.mates.farmmates.models.User
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
    ActivityCompat.OnRequestPermissionsResultCallback, SensorEventListener {

    /******************************GoogleMaps***************************/
    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder
    private lateinit var polyline: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private var locations = myRoute.sites(mutableListOf())


    /******************************Brujula***************************/
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false
    /******************************************************************/


    /******************************Permisos***************************/
    private var permissionDenied = false
    /*******************************************************************/


    /******************************Sensores***************************/
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener

    /*******************************************************************/

    // Variables para filtro pasa bajos y control de frecuencia
    private val alpha = 0.25f
    private var smoothedAzimuth = 0f
    private var lastUpdateTime = 0L
    private val updateInterval = 500
    private var isCompassClicked = false

    /******************************Firebase***************************/
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val usersSelector = "users/"
    private var currentUser: User? = null

    /*******************************************************************/

    private lateinit var binding: FragmentMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        database = Firebase.database
        getCurrentUser()

        //Geocoder initialization
        geocoder = Geocoder(baseContext)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        lightEventListener = createLightSensorListener()
        /*******************************************************************/

        /******************************Light Sensor***************************/
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
        /*******************************************************************/


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.goBackToMenu.setOnClickListener { goToMainMenu() }
        binding.reportButton.setOnClickListener { goToReports() }
        binding.chatButton.setOnClickListener { goToChat() }
        binding.reportButton.setOnClickListener { goToReports() }

        // Add OnClickListener to compassImage to set map orientation to north
        binding.compassImage.setOnClickListener {
            isCompassClicked = !isCompassClicked
            setMapToNorth()
        }
    }

    private fun getCurrentUser() {
        val ref = database.getReference(usersSelector + auth.currentUser!!.uid)
        ref.get().addOnSuccessListener {
            currentUser = it.getValue(User::class.java)
        }
    }

    private fun setMapToNorth() {
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(mMap.cameraPosition.target) // keep the current position
                    .zoom(mMap.cameraPosition.zoom) // keep the current zoom level
                    .bearing(0f) // set bearing to north
                    .tilt(mMap.cameraPosition.tilt) // keep the current tilt
                    .build()
            )
        )
        binding.compassImage.rotation = 0f
    }

    private fun lowPassFilter(input: Float, output: Float): Float {
        return output + alpha * (input - output)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isCompassClicked) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone()
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone()
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val rotationMatrix = FloatArray(9)
            val success =
                SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = (azimuth + 360) % 360

                // Aplicar filtro pasa bajos
                smoothedAzimuth = lowPassFilter(azimuth, smoothedAzimuth)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdateTime > updateInterval) {
                    lastUpdateTime = currentTime

                    if (!this::mMap.isInitialized) return
                    // Actualizar la orientación del mapa de Google
                    mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(mMap.cameraPosition.target)
                                .zoom(mMap.cameraPosition.zoom)
                                .bearing(smoothedAzimuth)
                                .tilt(mMap.cameraPosition.tilt)
                                .build()
                        )
                    )

                    // Actualizar la rotación de la imagen de la brújula
                    binding.compassImage.rotation = -smoothedAzimuth
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No se necesita implementar esto para la brújula.
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

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.oldmap))

        // Obtain the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            lastKnownLocation = location
            if (location != null) {
                // Start location updates
                startLocationUpdates()
            }
        }

        mMap.setOnMapLongClickListener {
            findAddress(it)?.let { it1 -> addMarker(it.latitude, it.longitude, it1) }
            try {
                Toast.makeText(
                    this, "Distancia:" + lastKnownLocation!!.distanceTo(convertLatLngToLocation(it))
                        .roundToInt().toString() + " metros", Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("LongClickMap", "Error generating the route: $e")
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

    private fun callRequestRoute(origen: LatLng, destino: LatLng) {
        // Start a coroutine in the main thread
        CoroutineScope(Dispatchers.Main).launch {
            requestRoute(origen, destino)
        }
    }

    private fun convertLatLngToLocation(latLng: LatLng): Location {
        // New location object
        val location = Location("provider_name")

        // New location object with the coordinates of the LatLng object
        location.latitude = latLng.latitude
        location.longitude = latLng.longitude

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
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val newLocation = locationResult.lastLocation
            if (newLocation != null && lastKnownLocation != null) {
                val distance = lastKnownLocation!!.distanceTo(newLocation)

                if (distance > 30) {
                    // IF the distance is greater than 30 meters, update the last known location
                    //TODO("Send the new location to the server")
                    lastKnownLocation = newLocation
                    currentUser?.lat = newLocation.latitude
                    currentUser?.long = newLocation.longitude
                    updateUser()
                }
            }
        }
    }

    private fun updateUser() {
        val ref = database.getReference(usersSelector + auth.currentUser!!.uid)
        ref.get().addOnSuccessListener {
            if (it.getValue(User::class.java) != currentUser)
                ref.setValue(currentUser)
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

        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.marcador)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 80, 80, false)
        val iconDescriptor = BitmapDescriptorFactory.fromBitmap(scaledBitmap)


        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(iconDescriptor)
        )

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
            lightEventListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(lightEventListener)
        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
    }

    private fun drawPolylineOnMap(ruta: String) {
        mMap.clear()
        addAllZones()
        drawAgriculturalObjects()

        val decodedPath: List<LatLng> = PolyUtil.decode(ruta)
        val polylineOptions = PolylineOptions()
            .addAll(decodedPath)
            .color(ContextCompat.getColor(this, R.color.purple_200))
            .width(10f)

        mMap.addPolyline(polylineOptions)
    }

    private suspend fun requestRoute(origin: LatLng, destination: LatLng) {

        val client = OkHttpClient()
        val gson = Gson()

        // Body of the POST request
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

        val requestBodyJson = gson.toJson(requestBodyData)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        val baseUrl = "https://routes.googleapis.com/directions/v2:computeRoutes"
        val url = "$baseUrl?key=AIzaSyAoixCSGBozzBjpV_njSFf3eTRTnFaW9kg"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
            .build()

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

    private fun addAllZones() {
        // Get an instance of Firestore
        val db = FirebaseFirestore.getInstance()

        // Get all documents in the 'zones' collection
        db.collection("zones").get().addOnSuccessListener { documents ->
            if (documents != null && !documents.isEmpty) {
                for (document in documents) {
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
                                binding.tempVal.text =
                                    "Temperatura actual: ${weatherResponse.current.temperature2m} °C"
                                binding.ambVal.text =
                                    "Humedad: ${weatherResponse.current.relativeHumidity2m} %"
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

