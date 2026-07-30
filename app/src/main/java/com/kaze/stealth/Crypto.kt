package com.kaze.stealth

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_NONCE_LENGTH = 12

    fun encrypt(plaintext: String): String {
        val keySpec = SecretKeySpec(Config.AES_KEY, "AES")
        val nonce = ByteArray(GCM_NONCE_LENGTH)
        java.security.SecureRandom().nextBytes(nonce)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(nonce.size + ct.size)
        System.arraycopy(nonce, 0, combined, 0, nonce.size)
        System.arraycopy(ct, 0, combined, nonce.size, ct.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(b64: String): String {
        val raw = Base64.decode(b64, Base64.NO_WRAP)
        val nonce = raw.copyOfRange(0, GCM_NONCE_LENGTH)
        val ct = raw.copyOfRange(GCM_NONCE_LENGTH, raw.size)
        val keySpec = SecretKeySpec(Config.AES_KEY, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }
}
