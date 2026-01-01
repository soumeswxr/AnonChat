package com.soumeswar.anonchat.domain

import android.util.Base64
import com.soumeswar.anonchat.crypto.CryptoUtils
import com.soumeswar.anonchat.data.Chat
import com.soumeswar.anonchat.network.OnionClient
import com.soumeswar.anonchat.network.OnionServer
import com.soumeswar.anonchat.data.Message
import com.soumeswar.anonchat.data.Peer
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap

class ChatRepository(
    val onionClient : OnionClient
) {
    private val chats = ConcurrentHashMap<String, Chat>()
    private val sessions = ConcurrentHashMap<String, ByteArray>()
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

    private fun ensureSession(peer : Peer) : ByteArray {
        return sessions[peer.onionAddress] ?: run {
            val socket = onionClient.connect(peer.onionAddress)
            val out = socket.outputStream.bufferedWriter()
            val input = socket.inputStream.bufferedReader()

            val public = Base64.encodeToString(
                identity.public.encoded,
                Base64.NO_WRAP
            )

            out.write("HELLO:$public\n")
            out.flush()

            val response = input.readLine()

            val serverPublic = Base64.decode(
                response.removePrefix("HELLO_ACK:"),
                Base64.NO_WRAP
            )

            val shared = CryptoUtils.deriveSharedSecret(
                identity.private,
                KeyFactory.getInstance("X25519")
                    .generatePublic(X509EncodedKeySpec(serverPublic))
            )
            sessions[peer.onionAddress] = shared
            shared
        }
    }

    private fun run(function: ChatRepository.() -> Unit): ByteArray {

        return TODO("Provide the return value")
    }

    fun sendMessage(
        peer : Peer,
        text : String,
        onUpdate: (Chat) -> Unit
    )
    {
        val sessionKey = ensure
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