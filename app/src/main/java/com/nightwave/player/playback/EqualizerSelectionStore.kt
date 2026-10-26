package com.nightwave.player.playback

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable USER INTENT only, never an "effect active" receipt. On a new
 * Android session PlaybackService must revalidate the actual supported bands.
 *
 * This is a separate, additive preference namespace: existing queue, offline
 * audio, OAuth, and historical EQ diagnostic receipts are never rewritten.
 */
internal class EqualizerSelectionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "nightwave_eq_selection_v1", Context.MODE_PRIVATE
    )

    fun read(): EqualizerSelection? = runCatching {
        val serialized = prefs.getString("choice", null) ?: return@runCatching null
        require(serialized.length in 1..3_000) { "Unexpected saved EQ size" }
        val raw = JSONObject(serialized)
        require(raw.getInt("version") == 1)
        val preset = raw.getInt("preset")
        val centers = raw.getJSONArray("centers")
        val levels = raw.getJSONArray("levels")
        require(centers.length() <= 32 && levels.length() <= 32)
        val selection = EqualizerSelection(
            preset,
            List(centers.length()) { centers.getInt(it) },
            List(levels.length()) { levels.getInt(it) }
        )
        require(EqualizerSelectionPolicy.valid(selection)) { "Unsafe saved EQ curve" }
        selection
    }.getOrNull()

    fun save(selection: EqualizerSelection): Boolean {
        if (!EqualizerSelectionPolicy.valid(selection)) return false
        return runCatching {
            val raw = JSONObject()
                .put("version", 1)
                .put("preset", selection.preset)
                .put("centers", JSONArray(selection.centersHz))
                .put("levels", JSONArray(selection.levelsMb))
                .toString()
            require(raw.length <= 3_000)
            prefs.edit().putString("choice", raw).commit()
        }.getOrDefault(false)
    }
}
