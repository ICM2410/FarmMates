package mobile.mates.farmmates.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import mobile.mates.farmmates.R

class Notifications : Service() {

    private val reportsCollection = "reports"
    private val channelID = "report_notification"
    private val channelName = "Report Notifications"

    companion object {
        var isRunning = false
        var currentUserId: String? = null
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.e("Notifications", "Received null intent in onStartCommand")
            return START_NOT_STICKY
        }

        if (!isRunning) {
            isRunning = true
            currentUserId = intent.getStringExtra("currentUserId") ?: currentUserId
            createNotificationChannel()

            val notification: Notification = NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.loupe)
                .setContentTitle("Background services")
                .setContentText("Awaiting new reports...")
                .build()

            // Iniciar servicio en primer plano con tipo especificado
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, notification)
            }
            setupFirestoreListener()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val descriptionText = getString(R.string.channel_description)

                val notificationManager = getSystemService(NotificationManager::class.java)
                val existingChannel =
                    notificationManager.getNotificationChannel(channelID)

                if (existingChannel == null) {
                    val channel = NotificationChannel(
                        channelID,
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
    }

    private fun setupFirestoreListener() {
        val firestore = Firebase.firestore
        firestore.collection(reportsCollection)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Notifications", "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val report = dc.document.data
                            val createdBy = report["createdBy"] as String
                            if (createdBy == currentUserId)
                                return@addSnapshotListener

                            val description = report["description"] as String
                            sendNotification(createdBy, description)
                        }
                        else -> {}
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(createdBy: String, description: String) {
        val notificationId = createdBy.hashCode() // Usamos el hash del nombre para generar un ID único.

        val intent = Intent(this, Map::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelID)
            .setSmallIcon(R.drawable.loupe)
            .setContentTitle("Nuevo Reporte")
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
