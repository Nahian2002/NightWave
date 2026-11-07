package com.nightwave.player.updates

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Optional read-only GitHub credential, encrypted and excluded from Android backups. */
internal object PrivateUpdateCredential {
    private const val ALIAS = "nightwave_private_update_access"
    private const val IV_BYTES = 12
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun file(context: Context) =
        AtomicFile(File(context.noBackupFilesDir, "nightwave-private-update.bin"))

    fun configured(context: Context): Boolean = file(context).baseFile.exists()

    @Synchronized
    fun read(context: Context): String? {
        if (!configured(context)) return null
        return try {
            val encrypted = file(context).readFully()
            require(encrypted.size in (IV_BYTES + 16)..300)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, encrypted, 0, IV_BYTES))
            cipher.doFinal(encrypted, IV_BYTES, encrypted.size - IV_BYTES).toString(Charsets.UTF_8)
                .also { require(PrivateGithubUpdatePolicy.validToken(it)) }
        } catch (_: Exception) {
            throw UpdateCheckException(UpdateCheckStatus.PRIVATE_ACCESS)
        }
    }

    @Synchronized
    fun save(context: Context, token: String) {
        require(PrivateGithubUpdatePolicy.validToken(token))
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        require(cipher.iv.size == IV_BYTES)
        val encrypted = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val destination = file(context)
        val output = destination.startWrite()
        try {
            output.write(encrypted)
            destination.finishWrite(output)
        } catch (error: Exception) {
            destination.failWrite(output)
            throw error
        }
    }

    @Synchronized
    fun remove(context: Context) {
        file(context).delete()
        check(!configured(context))
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
    }
}
