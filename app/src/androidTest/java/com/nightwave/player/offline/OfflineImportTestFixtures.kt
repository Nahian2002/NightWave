package com.nightwave.player.offline

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** A valid original PCM WAV and an isolated context; no installed library/account is touched. */
internal object OfflineImportTestFixtures {
    fun wav(): ByteArray = ByteBuffer.allocate(1644).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(1636); put("WAVEfmt ".toByteArray()); putInt(16)
        putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
        put("data".toByteArray()); putInt(1600)
        repeat(800) { putShort(if (it % 2 == 0) 1000.toShort() else (-1000).toShort()) }
    }.array()
}

internal class OfflineImportTestContext(base: Context) : ContextWrapper(base) {
    private val suffix = "_nw135_" + UUID.randomUUID()
    private val folder = File(base.cacheDir, suffix).apply { check(mkdir()) }
    private val preferenceNames = mutableSetOf<String>()
    override fun getApplicationContext(): Context = this
    override fun getFilesDir(): File = File(folder,"files").apply { check(isDirectory || mkdir()) }
    override fun getCacheDir(): File = File(folder,"cache").apply { check(isDirectory || mkdir()) }
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        preferenceNames.add(name + suffix)
        return super.getSharedPreferences(name + suffix,mode)
    }
    fun close() {
        preferenceNames.forEach { baseContext.deleteSharedPreferences(it) }
        check(folder.parentFile == baseContext.cacheDir && folder.name == suffix)
        check(folder.deleteRecursively())
    }
}
