package mobile.mates.farmmates.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

object RestUtils {
    fun addSampleAgriculturalObjects() {
        val db = FirebaseFirestore.getInstance()
        val objects = listOf(
            hashMapOf("type" to "Tractor", "latitude" to 7.210, "longitude" to -73.120, "zoneId" to "zone1"),
            hashMapOf("type" to "Camión", "latitude" to 7.211, "longitude" to -73.121, "zoneId" to "zone1"),
            hashMapOf("type" to "Cosechadora", "latitude" to 7.212, "longitude" to -73.122, "zoneId" to "zone2"),
            hashMapOf("type" to "Pulverizador", "latitude" to 7.213, "longitude" to -73.123, "zoneId" to "zone2"),
            hashMapOf("type" to "Sembradora", "latitude" to 7.214, "longitude" to -73.124, "zoneId" to "zone3"),
            hashMapOf("type" to "Tanque de agua", "latitude" to 7.215, "longitude" to -73.125, "zoneId" to "zone3"),
            hashMapOf("type" to "Remolque", "latitude" to 7.216, "longitude" to -73.126, "zoneId" to "zone4"),
            hashMapOf("type" to "Fumigadora", "latitude" to 7.217, "longitude" to -73.127, "zoneId" to "zone4"),
            hashMapOf("type" to "Recogedora de frutas", "latitude" to 7.218, "longitude" to -73.128, "zoneId" to "zone5"),
            hashMapOf("type" to "ATV", "latitude" to 7.219, "longitude" to -73.129, "zoneId" to "zone5")
        )

        objects.forEach { obj ->
            db.collection("agriculturalObjects").add(obj)
                .addOnSuccessListener { documentReference ->
                    println("DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    println("Error adding document: $e")
                }
        }
    }
    fun createMultipleZonesInFirestore() {
        // Obtener la instancia de Firestore
        val db = FirebaseFirestore.getInstance()

        // Lista de zonas a añadir
        val zones = listOf(
            hashMapOf(
                "name" to "Zona Norte Bucaramanga",
                "points" to listOf(
                    hashMapOf("latitude" to 7.225, "longitude" to -73.140),
                    hashMapOf("latitude" to 7.230, "longitude" to -73.135),
                    hashMapOf("latitude" to 7.235, "longitude" to -73.140),
                    hashMapOf("latitude" to 7.230, "longitude" to -73.145)
                ),
                "marker" to hashMapOf("latitude" to 7.230, "longitude" to -73.140)
            ),
            hashMapOf(
                "name" to "Zona Sur Bucaramanga",
                "points" to listOf(
                    hashMapOf("latitude" to 7.190, "longitude" to -73.160),
                    hashMapOf("latitude" to 7.195, "longitude" to -73.155),
                    hashMapOf("latitude" to 7.190, "longitude" to -73.150),
                    hashMapOf("latitude" to 7.185, "longitude" to -73.155)
                ),
                "marker" to hashMapOf("latitude" to 7.190, "longitude" to -73.155)
            ),
            hashMapOf(
                "name" to "Zona Este Bucaramanga",
                "points" to listOf(
                    hashMapOf("latitude" to 7.210, "longitude" to -73.120),
                    hashMapOf("latitude" to 7.215, "longitude" to -73.115),
                    hashMapOf("latitude" to 7.220, "longitude" to -73.120),
                    hashMapOf("latitude" to 7.215, "longitude" to -73.125)
                ),
                "marker" to hashMapOf("latitude" to 7.215, "longitude" to -73.120)
            ),
            hashMapOf(
                "name" to "Zona Oeste Bucaramanga",
                "points" to listOf(
                    hashMapOf("latitude" to 7.200, "longitude" to -73.170),
                    hashMapOf("latitude" to 7.205, "longitude" to -73.165),
                    hashMapOf("latitude" to 7.200, "longitude" to -73.160),
                    hashMapOf("latitude" to 7.195, "longitude" to -73.165)
                ),
                "marker" to hashMapOf("latitude" to 7.200, "longitude" to -73.165)
            )
        )

        // Añadir cada zona a Firestore
        zones.forEach { zone ->
            db.collection("zones").add(zone)
                .addOnSuccessListener {
                    Log.d("Firestore", "DocumentSnapshot successfully written with ID: ${it.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error writing document", e)
                }
        }
    }

    fun getWeatherInfo(latitude: Double, longitude: Double, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,wind_speed_10m,rain,precipitation,relative_humidity_2m"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // En caso de fallo en la solicitud, manejar adecuadamente
                callback(null)
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null)
                    throw IOException("Unexpected code $response")
                } else {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        // Procesar la respuesta
                        callback(responseData)
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }
    fun processWeatherResponse(jsonData: String?): WeatherData? {
        jsonData ?: return null
        val gson = Gson()
        return gson.fromJson(jsonData, WeatherData::class.java)
    }
    data class CurrentUnits(
        val time: String,
        val interval: String,
        @SerializedName("temperature_2m")
        val temperature2m: String,
        @SerializedName("wind_speed_10m")
        val windSpeed10m: String,
        val rain: String,
        val precipitation: String,
        @SerializedName("relative_humidity_2m")
        val relativeHumidity2m: String
    )

    data class CurrentData(
        val time: String,
        val interval: Int,
        @SerializedName("temperature_2m")
        val temperature2m: Double,
        @SerializedName("wind_speed_10m")
        val windSpeed10m: Double,
        val rain: Double,
        val precipitation: Double,
        @SerializedName("relative_humidity_2m")
        val relativeHumidity2m: Int
    )

    data class WeatherData(
        val latitude: Double,
        val longitude: Double,
        @SerializedName("generationtime_ms")
        val generationTimeMs: Double,
        @SerializedName("utc_offset_seconds")
        val utcOffsetSeconds: Int,
        val timezone: String,
        @SerializedName("timezone_abbreviation")
        val timezoneAbbreviation: String,
        val elevation: Double,
        @SerializedName("current_units")
        val currentUnits: CurrentUnits,
        val current: CurrentData
    )
}