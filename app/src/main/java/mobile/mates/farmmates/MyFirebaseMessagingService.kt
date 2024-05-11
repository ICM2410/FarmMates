package mobile.mates.farmmates

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.io.IOException
import kotlin.collections.Map
import okhttp3.Request
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import android.app.Service



class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Aquí manejas el mensaje recibido. Puedes obtener el cuerpo del mensaje, título y otros datos.
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Verifica si el mensaje contiene una carga útil de datos.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
            handleNow(remoteMessage.data)
        }

        // Verifica si el mensaje contiene una notificación:
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            // Aquí puedes personalizar cómo manejas la notificación, por ejemplo mostrando tu propia notificación.
            sendNotification(it.title ?: "Title", it.body ?: "Body")
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
        // Aquí puedes enviar el token a tu servidor si es necesario.
        sendRegistrationToServer(token)
    }

    private fun handleNow(data: Map<String, String>) {
        // Manejar la carga útil de datos recibida en segundo plano.
        Log.d("FCM", "Handling now.")
    }
    private val CHANNEL_ID = "my_channel_id"
    private var NOTIFICATIONID = 100
    private val channelName = "user_available"
    @SuppressLint("MissingPermission")

    private fun sendNotification(title: String, messageBody: String) {
// Crear un canal de notificación (necesario para Android Oreo y versiones posteriores)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val descriptionText = getString(R.string.channel_description)

                val notificationManager = getSystemService(NotificationManager::class.java)
                val existingChannel =
                    notificationManager.getNotificationChannel(CHANNEL_ID)

                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        channelName,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    channel.description = descriptionText
                    notificationManager.createNotificationChannel(channel)
                }
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error creating notification channel: ${e.message}")
        }

        Log.d("FCM", "Sending notification.")
        try{
        // Crear la notificación
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.tractor2)
            .setContentTitle("Notificacion")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATIONID, notificationBuilder.build())
        }
        NOTIFICATIONID++
        }   catch (e: Exception) {
            Log.e("FCM", "Error creating notification: ${e.message}")
        }
    }

    private fun sendRegistrationToServer(token: String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, "{\"token\":\"$token\"}")
        val request = Request.Builder()
            .url("https://tuapi.com/api/tokens")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                println("Token enviado con éxito al servidor.")
            }
        })
    }
}
