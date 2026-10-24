package android.content

/**
 * TEST-ONLY, in-memory Android API substitute.
 * This file lives under tools/ and is never included in the Android APK.
 * Only the Context/SharedPreferences subset needed for the production
 * CloudBackupState is implemented; no real user preferences are accessed.
 */
open class Context {
    companion object { const val MODE_PRIVATE = 0 }
    val applicationContext: Context get() = this
    private val stores = mutableMapOf<String, SharedPreferences>()

    fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        stores.getOrPut(name) { InMemoryPrefs() }
}

interface SharedPreferences {
    fun getString(key: String, fallback: String?): String?
    fun getBoolean(key: String, fallback: Boolean): Boolean
    fun getLong(key: String, fallback: Long): Long
    fun edit(): Editor

    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun putLong(key: String, value: Long): Editor
        fun remove(key: String): Editor
        fun commit(): Boolean
    }
}

private class InMemoryPrefs : SharedPreferences {
    private val data = mutableMapOf<String, Any>()
    override fun getString(key: String, fallback: String?): String? =
        data[key] as? String ?: fallback
    override fun getBoolean(key: String, fallback: Boolean): Boolean =
        data[key] as? Boolean ?: fallback
    override fun getLong(key: String, fallback: Long): Long =
        data[key] as? Long ?: fallback

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            apply { changes[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            apply { changes[key] = value }
        override fun remove(key: String): SharedPreferences.Editor =
            apply { changes[key] = null }
        override fun commit(): Boolean {
            for ((key, value) in changes) {
                if (value == null) data.remove(key) else data[key] = value
            }
            return true
        }
    }
}
