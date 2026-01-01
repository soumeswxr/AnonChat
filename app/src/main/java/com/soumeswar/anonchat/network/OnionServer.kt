package com.soumeswar.anonchat.network

import android.provider.ContactsContract
import android.util.Base64
import com.soumeswar.anonchat.crypto.Handshake
import com.soumeswar.anonchat.crypto.IdentityManager
import com.soumeswar.anonchat.crypto.MessageCrypto
import java.net.ServerSocket
import java.net.Socket
import java.security.Signature
import kotlin.concurrent.thread
import com.soumeswar.anonchat.service.ChatForegroundService
import java.security.KeyPair

class OnionServer(
    private val port : Int,
    private val identity: KeyPair,
    private val onMessage : (String, Socket) -> Unit
) {
    private var running = false

    fun start() {
        running = true
        thread(name = "OnionServer") {
            val server = ServerSocket(port)
            while (running) {
                val socket = server.accept()
                handleConnection(socket)
            }
        }
    }

private fun handleConnection(socket : Socket) {
        thread {
            val reader = socket.getInputStream().bufferedReader()
            val writer = socket.getOutputStream().bufferedWriter()

            if (reader.readLine() != ChatProtocol.HELLO) {
                socket.close(); return@thread
            }

            val peerID = IdentityManager.decodePublicKey(reader.readLine().substringAfter(":"))
            val peerEphemeal = Handshake.decodePublicKey(reader.readLine().substringAfter(":"))
            val signature = Base64.decode(reader.readLine().substringAfter(":"), Base64.NO_WRAP)

            val verifier = Signature.getInstance("Ed25519")
            verifier.initVerify(peerID)
            verifier.update(peerEphemeal.encoded)
            if (!verifier.verify(signature)) {
                socket.close(); return@thread
            }

            val ephemeal = Handshake.generateEphemealKeypair()
            val shared = Handshake.deriveSharedSecret(ephemeal.private, peerEphemeal)

            writer.write("${ChatProtocol.OK}\n")
            writer.write("IDENTITY:${IdentityManager.encodePublicKey(identity.public)}\n")
            writer.write("EPHEMEAL_KEY:${Handshake.encodePublicKey(ephemeal.public)}\n")

            writer.flush()

            while (true) {
                val line = reader.readLine() ?: break

                if (!line.startsWith(ChatProtocol.MSG)) continue

                val plain = MessageCrypto.decrypt(
                    line.removePrefix(ChatProtocol.MSG),
                    shared
                )
                onMessage(plain, socket)
            }
        }
    }
    fun stop() {
        running = false
    }
}