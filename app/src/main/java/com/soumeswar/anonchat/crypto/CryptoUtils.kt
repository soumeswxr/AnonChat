package com.soumeswar.anonchat.crypto

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import java.security.SecureRandom
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    fun deriveSharedSecret(
        privateKey: PrivateKey,
        publicKey: PublicKey
    ) : ByteArray {
        val keyAgreement = KeyAgreement.getInstance("X25519")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        return keyAgreement.generateSecret()
    }

    fun encrypt(key : ByteArray, plaintext : ByteArray) : ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secureRandom = SecureRandom()
        val nonce = ByteArray(12).also {
            secureRandom.nextBytes(it)
        }
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            spec
        )
        val cipherText = cipher.doFinal(plaintext)
        return nonce + cipherText
    }

    fun decrypt(key : ByteArray, cipherData : ByteArray) : ByteArray {
        val nonce = cipherData.copyOfRange(0, 12)
        val data = cipherData.copyOfRange(12, cipherData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, nonce)
        )
        return cipher.doFinal(data)
    }
}