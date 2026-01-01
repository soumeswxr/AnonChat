package com.soumeswar.anonchat.domain

import com.soumeswar.anonchat.data.Chat
import com.soumeswar.anonchat.network.OnionClient
import com.soumeswar.anonchat.network.OnionServer
import com.soumeswar.anonchat.data.Message
import com.soumeswar.anonchat.data.Peer
import java.net.Socket
import java.security.KeyPair
import java.util.concurrent.ConcurrentHashMap

class ChatRepository(
    val onionClient : OnionClient
) {
    private val chats = ConcurrentHashMap<String, Chat>();
    fun startServer(
        port : Int,
        identity : KeyPair,
        onUpdate : (Chat) -> Unit
    ) : OnionServer {
        val server = OnionServer(port, identity) { raw, socket ->
            handleIncoming(raw, socket, onUpdate)
        }
        server.start()
        return server
    }
    private fun handleIncoming(raw : String, socket : Socket, onUpdate: (Chat) -> Unit) {
        val from = socket.inetAddress.hostName

        val msg = Message(
            from = from,
            body = raw,
            outgoing = false
        )

        val peer = Peer(onionAddress = from)
        val updated = chats[from]
            ?.copy(message = chats[from]!!.message + msg)
            ?: Chat(peer, listOf(msg))
        chats[from] = updated
        onUpdate(updated)
    }

    fun sendMessage(
        peer : Peer,
        text : String,
        onUpdate: (Chat) -> Unit
    )
    {
        val socket = onionClient.connect(peer.onionAddress)
        val writer = socket.getOutputStream().bufferedWriter()

        writer.write(text + "\n")
        writer.flush()

        val msg = Message(
            from = "me",
            body = text,
            outgoing = true
        )

        val updated = chats[peer.onionAddress]
            ?.copy(message = chats[peer.onionAddress]!!.message + msg)
            ?: Chat(peer, listOf(msg))


        chats[peer.onionAddress] = updated
        onUpdate(updated)
    }

    fun getChat(peer : Peer) : Chat? =
        chats[peer.onionAddress]
}