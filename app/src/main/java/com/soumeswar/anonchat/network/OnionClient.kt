package com.soumeswar.anonchat.network

import android.util.Base64
import com.soumeswar.anonchat.crypto.Handshake
import com.soumeswar.anonchat.crypto.IdentityManager
import com.soumeswar.anonchat.data.SecureSession
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.Signature

class OnionClient(
    private val SOCKS_HOST : String,
    private val SOCKS_PORT : Int = 9050
) {
    fun connect(onion : String, port : Int = 80) : Socket {
        val proxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(SOCKS_HOST, SOCKS_PORT)
        )
        val socket = Socket(proxy)
        socket.connect(
            InetSocketAddress(onion, port),
            60_000
        )
        performHandshake(socket)
        return socket
    }

    private fun performHandshake(socket : Socket) : SecureSession {
        val reader = socket.getInputStream().bufferedReader()
        val writer = socket.getOutputStream().bufferedWriter()

        val identity = IdentityManager.generateIdentity()

        val ephemeal = Handshake.generateEphemealKeypair()

        val ephemeal_public = Handshake.encodePublicKey(ephemeal.public)

        val signature = Base64.encodeToString(
            Signature.getInstance("Ed25519").apply {
                initSign(identity.private)
                update(ephemeal.public.encoded)
            }.sign(),
            Base64.NO_WRAP
        )

        writer.write(
            """
                HELLO
                IDENTITY:${IdentityManager.encodePublicKey(identity.public)}
                EPHEMEAL_KEY:${ephemeal_public}
                SIGNATURE:${signature}
            """.trimIndent() + "\n"
        )

        writer.flush()

        val ok = reader.readLine()
        if (ok != ChatProtocol.OK) error("Handshake Failed")

        val peerID = IdentityManager.decodePublicKey(reader.readLine().substringAfter(":"))
        val peerEphemeal = Handshake.decodePublicKey(reader.readLine().substringAfter(":"))

        val shared = Handshake.deriveSharedSecret(ephemeal.private, peerEphemeal)

        return SecureSession(peerID, shared)
    }
}