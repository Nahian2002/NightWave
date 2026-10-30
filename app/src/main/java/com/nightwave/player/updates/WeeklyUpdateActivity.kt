package com.nightwave.player.updates

import com.nightwave.player.R
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.ui.NightWaveMaterialTheme
import com.nightwave.player.v2.data.GuestProfileStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

internal data class UpdateScreen(val loading: Boolean = true, val update: ApprovedUpdate? = null,
    val progress: Int? = null, val file: File? = null, val failed: Boolean = false)

internal class WeeklyUpdateModel(app: Application) : AndroidViewModel(app) {
    val state = MutableStateFlow(UpdateScreen())
    init { check() }
    fun check() {
        if (state.value.progress != null) return
        state.value = UpdateScreen()
        viewModelScope.launch {
            try { state.value = UpdateScreen(loading = false, update = WeeklyUpdates.latest(getApplication())) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.value = UpdateScreen(loading = false, failed = true) }
        }
    }
    fun download() {
        val update = state.value.update ?: return
        if (state.value.progress != null) return
        state.value = state.value.copy(progress = 0, failed = false)
        viewModelScope.launch {
            try {
                val file = withTimeout(10 * 60 * 1000L) { WeeklyUpdates.download(getApplication(), update) { p -> state.value = state.value.copy(progress = p) } }
                state.value = state.value.copy(file = file, progress = null)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.value = state.value.copy(progress = null, failed = true) }
        }
    }
}

/** Only a user-approved installer handoff can leave this screen. No automatic install. */
class WeeklyUpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[WeeklyUpdateModel::class.java]
        val theme = GuestProfileStore(this).load().theme
        setContent {
            val state by model.state.collectAsState()
            NightWaveMaterialTheme(theme) {
                AlertDialog(onDismissRequest = { finish() },
                    title = { Text(if (state.update != null) getString(R.string.weekly_update_available) else getString(R.string.weekly_updates_title)) },
                    text = { Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            state.loading -> { CircularProgressIndicator(); Text(getString(R.string.weekly_checking)) }
                            state.update != null -> {
                                Text("NightWave " + state.update!!.name)
                                Text(state.update!!.notes)
                                Text(getString(R.string.weekly_weekly_notice))
                            }
                            !state.failed -> Text(getString(R.string.weekly_latest))
                        }
                        state.progress?.let { Text(getString(R.string.weekly_downloading, it)); LinearProgressIndicator(progress = { it / 100f }) }
                        if (state.file != null) Text(getString(R.string.weekly_ready))
                        if (state.failed) Text(getString(R.string.weekly_failed))
                    } },
                    confirmButton = {
                        when {
                            state.file != null -> TextButton(onClick = {
                                try {
                                    if (!packageManager.canRequestPackageInstalls()) {
                                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                    } else {
                                        val uri = FileProvider.getUriForFile(this, "$packageName.updates", requireNotNull(state.file))
                                        startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                    }
                                } catch (_: Exception) { model.state.value = state.copy(failed = true) }
                            }) { Text(getString(R.string.weekly_install)) }
                            state.update != null && state.progress == null -> TextButton(onClick = model::download) { Text(getString(R.string.weekly_update_now)) }
                            state.failed -> TextButton(onClick = model::check) { Text(getString(R.string.weekly_retry)) }
                        }
                    }, dismissButton = { TextButton(onClick = { finish() }) { Text(getString(R.string.weekly_later)) } })
            }
        }
    }
}
