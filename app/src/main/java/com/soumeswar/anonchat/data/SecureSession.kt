package com.soumeswar.anonchat.data

import java.security.PublicKey

data class SecureSession(
    val peerIdentityKey : PublicKey,
    val sharedKey : ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecureSession

        if (peerIdentityKey != other.peerIdentityKey) return false
        if (!sharedKey.contentEquals(other.sharedKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = peerIdentityKey.hashCode()
        result = 31 * result + sharedKey.contentHashCode()
        return result
    }
}
