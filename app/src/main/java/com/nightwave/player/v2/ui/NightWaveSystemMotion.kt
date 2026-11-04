package com.nightwave.player.v2.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Read-only observation; no settings permission, polling, stored preference or service command. */
@Composable
internal fun rememberNightWaveReducedMotion(userReduced: Boolean, followSystem: Boolean): Boolean {
    if (userReduced || !followSystem) return userReduced
    val resolver = LocalContext.current.applicationContext.contentResolver
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var scale by remember(resolver, lifecycle) { mutableStateOf<Float?>(null) }
    DisposableEffect(resolver, lifecycle) {
        lateinit var observation: NightWaveMotionObservation
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { observation.refresh() }
        }
        observation = NightWaveMotionObservation(
            read = { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) },
            subscribe = { resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer) },
            unsubscribe = { resolver.unregisterContentObserver(observer) },
            publish = { scale = it }
        )
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> observation.start()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> observation.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(listener)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) observation.start()
        onDispose {
            lifecycle.removeObserver(listener)
            observation.stop()
        }
    }
    return NightWaveThemeMotionPolicy.reduceMotion(userReduced, followSystem, scale)
}
