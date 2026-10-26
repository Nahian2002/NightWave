package com.nightwave.player.playback

/** Standalone NW105 Kotlin test adapter only: the actual offline engine only
 * references the Repeat enum. The PUBLIC PlaybackQueue is not tested here.
 * This file stays outside app/src and is never compiled into the APK. */
class PlaybackQueue { enum class Repeat { OFF, ALL, ONE } }
