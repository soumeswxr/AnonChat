package com.soumeswar.anonchat.crypto

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import androidx.core.content.edit

object IdentityManager {

    private const val ALGORITHM = "Ed25519"
    private const val PREFS = "identity"
    private const val PUB = "pub"
    private const val PRIV = "priv"

    fun loadOrCreate(context: Context): KeyPair {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val pub = prefs.getString(PUB, null)
        val priv = prefs.getString(PRIV, null)

        if (pub != null && priv != null) {
            return KeyPair(
                decodePublicKey(pub),
                decodePrivateKey(priv)
            )
        }

        val kp = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair()

        prefs.edit() {
            putString(PUB, encodePublicKey(kp.public))
                .putString(PRIV, encodePrivateKey(kp.private))
        }

        return kp
    }

    fun encodePublicKey(key: PublicKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun encodePrivateKey(key: PrivateKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun decodePublicKey(encoded: String): PublicKey {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return KeyFactory.getInstance(ALGORITHM)
            .generatePublic(X509EncodedKeySpec(bytes))
    }

    fun decodePrivateKey(encoded: String): PrivateKey {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return KeyFactory.getInstance(ALGORITHM)
            .generatePrivate(PKCS8EncodedKeySpec(bytes))
    }
}
