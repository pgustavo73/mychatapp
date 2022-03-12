package com.example.mychatapp.firebase

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mychatapp.R
import com.example.mychatapp.activities.ChatActivity
import com.example.mychatapp.activities.SignInActivity
import com.example.mychatapp.models.User
import com.example.mychatapp.utilities.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MessagingService : FirebaseMessagingService() {

    @Override
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    @SuppressLint("ResourceAsColor")
    @Override
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val user = User()
        user.id = remoteMessage.data.get(Constants.KEY_USER).toString()
        user.name = remoteMessage.data.get(Constants.KEY_NAME).toString()
        user.token = remoteMessage.data.get(Constants.KEY_FCM_TOKEN).toString()

        val notificationId: Int = Random().nextInt()
        val channelId = "chat_message"

        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK  and Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(Constants.KEY_USER, user)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, channelId)
        builder.setSmallIcon(R.drawable.ic_notifications1)
        builder.color = (0x5F771E)
        builder.setContentTitle(user.name)
        builder.setContentText(remoteMessage.data.get(Constants.KEY_MESSAGE))
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(
            remoteMessage.data.get(Constants.KEY_MESSAGE)
        ))
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = "ChatMessage"
            val channelDescription = "This notification channel is used for chat message notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = channelDescription
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.notify(notificationId, builder.build())

    }

}