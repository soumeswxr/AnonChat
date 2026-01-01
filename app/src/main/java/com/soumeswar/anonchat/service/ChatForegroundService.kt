package com.soumeswar.anonchat.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.soumeswar.anonchat.R
import com.soumeswar.anonchat.data.Chat
import com.soumeswar.anonchat.domain.ChatRepository
import com.soumeswar.anonchat.network.OnionClient
import com.soumeswar.anonchat.network.OnionServer
import com.soumeswar.anonchat.tor.TorManager
import com.soumeswar.anonchat.tor.TorStatusReceiver
import org.torproject.jni.TorService.ACTION_STATUS
import com.soumeswar.anonchat.crypto.IdentityManager
import java.security.KeyPair

class ChatForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "CHANNEL_1"
        const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()

    private lateinit var torManager: TorManager
    private lateinit var chatRepository: ChatRepository

    private var onionServer : OnionServer? = null

    private val chatListeners = mutableListOf<(Chat) -> Unit>()

    inner class LocalBinder : Binder() {
        fun getService() : ChatForegroundService = this@ChatForegroundService
    }
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        startForeground(NOTIFICATION_ID, buildNotification("Starting Tor Service..."))

        torManager = TorManager(this)
        torManager.startTor()

        ContextCompat.registerReceiver(
            this,
            TorStatusReceiver {
                onTorReady()
            },
            android.content.IntentFilter(ACTION_STATUS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private lateinit var identity : KeyPair

    private fun onTorReady() {
        updateNotification("Tor Service is initialized successfully! Starting Chat Service...")

        val onionClient = OnionClient("127.0.0.1",9050)
        chatRepository = ChatRepository(onionClient)

        identity = IdentityManager.loadOrCreate(this)

        onionServer = chatRepository.startServer(
            port = 7777,
            identity = identity
        ) { chat ->
            notifyChatUpdate(chat)
        }
        updateNotification("AnonChat is running...")
    }

    fun addChatListener(listener : (Chat) -> Unit) {
        chatListeners += listener
    }

    fun removeChatListener(listener : (Chat) -> Unit) {
        chatListeners -= listener
    }

    private fun notifyChatUpdate(chat : Chat) {
        chatListeners.forEach { it(chat) }
    }

    override fun onDestroy() {
        onionServer?.stop()
        torManager.stopTor()
        super.onDestroy()
    }

    private fun buildNotification(text : String) : Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AnonChat")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text : String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AnonChat Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}