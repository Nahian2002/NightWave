package com.nightwave.player.v2.ui

import android.app.AlertDialog
import android.app.Dialog
import android.widget.EditText
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.nightwave.player.v2.lyrics.PrivateLyricsStore
import com.nightwave.player.v2.lyrics.BoundLyricsStore
import com.nightwave.player.v2.lyrics.LyricsTrackBindingPolicy
import com.nightwave.player.v2.lyrics.PrivateLyricsOffsetStore
import com.nightwave.player.v2.lyrics.LyricsSyncPolicy
import com.nightwave.player.v2.lyrics.TimedLyrics
import com.nightwave.player.v2.lyrics.TimedLyricsCodec
import com.nightwave.player.v2.lyrics.LyricsEditorDraft
import com.nightwave.player.v2.lyrics.LyricsEditorPolicy
import com.nightwave.player.v2.lyrics.LyricsExportPolicy
import com.nightwave.player.v2.lyrics.PreparedLyricsExport
import com.nightwave.player.v2.lyrics.LyricsImportReviewPolicy
import com.nightwave.player.v2.lyrics.LyricsImportReview
import com.nightwave.player.v2.insights.ListeningInsights
import com.nightwave.player.v2.insights.ListeningInsightsEngine
import com.nightwave.player.v2.insights.ListeningMemoryStore
import com.nightwave.player.v2.insights.ListeningMemorySummary
import com.nightwave.player.v2.cloud.CloudAccountPresentation
import com.nightwave.player.v2.cloud.CloudAuthorizationRecoveryPolicy
import com.nightwave.player.v2.cloud.CloudAuthorizationDiagnosticPolicy
import com.nightwave.player.v2.cloud.CloudAuthorizationFailurePolicy
import com.nightwave.player.v2.cloud.CloudAuthorizationFailureStore
import com.nightwave.player.v2.cloud.CloudBackupStatus
import com.nightwave.player.v2.cloud.CloudAccountSessionPolicy
import com.nightwave.player.v2.cloud.CloudRecoverySession
import com.nightwave.player.v2.cloud.CloudRecoverySessionPolicy
import com.nightwave.player.v2.cloud.CloudOperationPhase
import com.nightwave.player.v2.cloud.CloudRecoveryUiPolicy
import com.nightwave.player.v2.cloud.CloudAccessState
import com.nightwave.player.v2.cloud.CloudAccessPolicy
import com.nightwave.player.v2.cloud.CloudFailureCausePolicy
import com.nightwave.player.v2.cloud.CloudBackupConsent
import com.nightwave.player.v2.cloud.CloudBackupConsentPolicy
import com.nightwave.player.v2.cloud.consentSnapshot
import com.nightwave.player.v2.cloud.DriveHttpFailure
import com.nightwave.player.v2.cloud.DriveHttpFailurePolicy
import com.nightwave.player.v2.cloud.CloudBackupState
import com.nightwave.player.v2.cloud.CloudBackupCodec
import com.nightwave.player.v2.cloud.CloudBackupPreview
import com.nightwave.player.v2.cloud.CloudPlaybackPreferences
import com.nightwave.player.v2.cloud.CloudRecoveryPolicy
import com.nightwave.player.v2.cloud.CloudRecoveryReport
import com.nightwave.player.v2.data.CloudRestoreApplyResult
import com.nightwave.player.v2.cloud.NightWaveDriveClient
import com.nightwave.player.offline.OfflineSong
import com.nightwave.player.offline.OfflineTrackSearch
import com.nightwave.player.v2.cloud.DriveSnapshot
import com.nightwave.player.v2.cloud.CloudLibraryMerger
import com.nightwave.player.v2.cloud.CloudMergePlan
import com.nightwave.player.v2.data.CloudMergeApplyResult
import com.nightwave.player.v2.cloud.NightWaveCloudAuthorization
import com.nightwave.player.v2.cloud.NightWaveCloudSchedule
import com.nightwave.player.v2.data.LibraryArchiveManager
import com.nightwave.player.diagnostics.NightWaveCrashRecorder
import com.nightwave.player.diagnostics.NightWaveExitHistory
import com.nightwave.player.offline.NightWaveOfflineNavigator
import com.nightwave.player.offline.NightWaveMainNavigationRequest
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import com.nightwave.player.playback.SleepTimerStatusStore
import com.nightwave.player.playback.OfflinePlaybackSpeedStore
import com.nightwave.player.playback.OfflinePlaybackSpeedPolicy
import com.nightwave.player.playback.EqualizerStatusStore
import com.nightwave.player.playback.ListeningLevelStore
import com.nightwave.player.playback.ListeningLevelPolicy
import com.nightwave.player.playback.CrossfadePolicy
import com.nightwave.player.playback.CrossfadeSettingsStore
import com.nightwave.player.playback.OfflineNormalizationStore
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import com.nightwave.player.v2.ui.NightWaveMatteButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.nightwave.player.v2.ui.NightWaveMatteIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.offline.FreeAudioCatalogActivity
import com.nightwave.player.offline.CatalogDownloadsActivity
import com.nightwave.player.offline.SearchTrackSaveQueue
import com.nightwave.player.offline.SearchTrackOfflineIndex
import com.nightwave.player.offline.OfflineLibraryActivity
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.offline.OfflineSessionStore
import com.nightwave.player.playback.PlaybackService
import com.nightwave.player.library.LegacyLibraryTransaction
import com.nightwave.player.core.VideoLink
import com.nightwave.player.core.SearchInput
import com.nightwave.player.core.SearchInputResolver
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.net.YouTubeGateway
import com.nightwave.player.net.VideoSearchResults
import com.nightwave.player.net.SearchMode
import com.nightwave.player.net.LocalMusicCorpusState
import com.nightwave.player.net.LocalMusicQueryRunner
import com.nightwave.player.net.LocalMusicSearchResult
import com.nightwave.player.net.SavedTrackSearch
import com.nightwave.player.net.LocalSearchSuggestions
import com.nightwave.player.net.PublicMusicResultEvidence
import com.nightwave.player.net.PublicMusicSearchRunner
import com.nightwave.player.net.PublicSearchPhase
import com.nightwave.player.net.PublicSearchDeadlineException
import kotlinx.coroutines.TimeoutCancellationException
import com.nightwave.player.net.SearchResultPresentationPolicy
import com.nightwave.player.net.MusicIdentity
import com.nightwave.player.net.SavedCreatorBrowser
import com.nightwave.player.v2.library.LibraryCollectionPresentationPolicy
import com.nightwave.player.v2.library.LibraryBrowsePolicy
import com.nightwave.player.v2.library.LibraryQuickPlayPolicy
import com.nightwave.player.v2.library.LibraryPlaylistPreview
import com.nightwave.player.v2.library.PlaylistPinStore
import com.nightwave.player.v2.library.PlaylistReorderGesturePolicy
import com.nightwave.player.v2.library.LibraryBulkSelectionPolicy
import com.nightwave.player.v2.library.PlaylistPinPolicy
import com.nightwave.player.v2.library.PlaylistFolderStore
import com.nightwave.player.v2.library.PlaylistFolderSnapshot
import com.nightwave.player.v2.library.SmartLibraryPolicy
import com.nightwave.player.v2.library.SmartLibraryCollection
import com.nightwave.player.ui.MainActivity
import com.nightwave.player.ui.YouTubeSearchBrowser
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.data.ProfileDataStoreMirror
import com.nightwave.player.v2.data.room.NightWaveDatabase
import com.nightwave.player.v2.data.room.RoomShadowMigrator
import com.nightwave.player.v2.data.room.VerifiedRoomReader
import com.nightwave.player.v2.playback.StudioPlaybackBridge
import com.nightwave.player.v2.playback.StudioPlayerState
import com.nightwave.player.v2.playback.SpotlightResumePolicy
import com.nightwave.player.v2.playback.SpotlightAction
import com.nightwave.player.v2.recommendation.PersonalMixEngine
import com.nightwave.player.v2.recommendation.LocalDailyMixEngine
import com.nightwave.player.v2.recommendation.NightWaveHomeCurationPolicy
import com.nightwave.player.v2.recommendation.PersonalMix
import com.nightwave.player.v2.social.PlaylistShareFormatter
import com.nightwave.player.v2.social.PlaylistShareParser
import com.nightwave.player.v2.social.SharedPlaylist
import com.nightwave.player.v2.social.PlaylistDocumentPolicy
import com.nightwave.player.v2.social.PreparedPlaylistDocument
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.NightWaveTheme
import com.nightwave.player.v2.model.ProfileSettings

/** Native result rows are an enhancement; the official browser remains a fallback. */
internal data class StudioSearchSnapshot(
    val query: String = "",
    val mode: SearchMode = SearchMode.SONGS,
    val loading: Boolean = false,
    val videos: List<VideoPreview> = emptyList(),
    val provider: String = "",
    val error: String = "",
    val hint: String = "",
    val phase: PublicSearchPhase = PublicSearchPhase.DIRECT,
    val fromCache: Boolean = false,
    val details: String = "",
    val prefillToken: String = ""
)

/** One short-lived in-memory LRC preview; the file is never cached to disk. */
internal data class PendingLyricsReview(
    val bytes: ByteArray,
    val report: LyricsImportReview,
    val title: String,
    val previousFingerprint: String?
)

/** An unsaved LRC editing session, scoped to one private offline song. */
internal data class LyricsEditorSession(
    val requestId: Int,
    val songId: String,
    val songTitle: String,
    val expectedFingerprint: String?,
    val baseline: LyricsEditorDraft,
    val draft: LyricsEditorDraft,
    val undo: List<LyricsEditorDraft> = emptyList(),
    val saving: Boolean = false
)

/** An in-memory, user-approved file export awaiting Android document picker. */
internal data class PendingLyricsExport(
    val requestId: Int,
    val songId: String,
    val prepared: PreparedLyricsExport
)

/** One user-visible playlist Undo, guarded by an exact post-edit snapshot. */
internal data class PlaylistUndoState(
    val playlist: String,
    val before: List<VideoPreview>,
    val after: List<VideoPreview>,
    val message: String
)

/** Retain only the pre-picker receipt across rotation, never tokens or Drive IDs. */
internal class CloudAuthorizationReceiptRetainedState : ViewModel() {
    var start: CloudBackupStatus? = null
    override fun onCleared() { start = null }
}

private sealed interface CloudActionResult {
    data class Backup(val created: Boolean) : CloudActionResult
    data class Snapshots(val items: List<DriveSnapshot>) : CloudActionResult
}

/**
 * Default NightWave 2.0 application surface. The original UI remains an
 * explicit, reversible fallback in Profile and keeps the same local library.
 */
/** Retains unsaved drafts across rotation; no disk or cloud persistence. */
internal class LyricsEditorRetainedState : ViewModel() {
    @Volatile var epoch: Int = 0
    var session by mutableStateOf<LyricsEditorSession?>(null)
    var pendingExport: PendingLyricsExport? = null
}

/** One bounded private import survives rotation, never process death or cloud backup. */
internal class LyricsImportRetainedState : ViewModel() {
    @Volatile var epoch = 0
    var pendingId: String? = null
    var reviewId: String? = null
    var review: PendingLyricsReview? = null
    override fun onCleared() {
        epoch++
        pendingId = null; reviewId = null; review = null
    }
}

/** One user-approved playlist text snapshot survives SAF rotation, not process death. */
internal class PlaylistDocumentRetainedState : ViewModel() {
    var pendingExport: PreparedPlaylistDocument? = null
}

// This component intentionally uses the pinned Media3 unstable API.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class NightWaveStudioActivity : ComponentActivity() {
    private lateinit var profiles: GuestProfileStore
    private lateinit var library: MusicLibrary
    private lateinit var folderStore: PlaylistFolderStore
    private lateinit var pinStore: PlaylistPinStore
    private var pinnedPlaylistNames by mutableStateOf<List<String>>(emptyList())
    private var folderSnapshot by mutableStateOf(PlaylistFolderSnapshot())
    private lateinit var playback: StudioPlaybackBridge
    private val playlistDocumentRetained by lazy {
        ViewModelProvider(this)[PlaylistDocumentRetainedState::class.java]
    }
    private lateinit var sleepTimerStatus: SleepTimerStatusStore
    private var sleepRemainingMs by mutableStateOf(0L)
    private lateinit var offlineSpeedStore: OfflinePlaybackSpeedStore
    private var offlineSpeedPercent by mutableStateOf(OfflinePlaybackSpeedPolicy.NORMAL_PERCENT)
    private val eqStatusStore by lazy { EqualizerStatusStore(this) }
    private lateinit var listeningLevelStore: ListeningLevelStore
    private var listeningLevelPercent by mutableStateOf(ListeningLevelPolicy.DEFAULT_PERCENT)
    private lateinit var crossfadeSettings: CrossfadeSettingsStore
    private var crossfadeSeconds by mutableStateOf(CrossfadePolicy.DEFAULT_SECONDS)
    private lateinit var normalizationStore: OfflineNormalizationStore
    private var normalizationEnabled by mutableStateOf(false)
    private lateinit var archiveManager: LibraryArchiveManager
    private lateinit var lyricsStore: PrivateLyricsStore
    private lateinit var lyricsOffsetStore: PrivateLyricsOffsetStore
    private lateinit var boundLyrics: BoundLyricsStore
    private var currentLyricsOffsetMs by mutableStateOf(0)
    // Main-thread UI changes invalidate earlier queued disk writes.
    @Volatile private var lyricsOffsetWriteVersion = 0
    private lateinit var listeningMemoryStore: ListeningMemoryStore
    private var memorySummary by mutableStateOf(
        ListeningMemorySummary(false, 0L, 0, 0, emptyList())
    )
    private var memoryReadGeneration = 0
    private val lyricsImportRetained by lazy {
        ViewModelProvider(this)[LyricsImportRetainedState::class.java]
    }
    private var pendingLyricsId: String?
        get() = lyricsImportRetained.pendingId
        set(value) { lyricsImportRetained.pendingId = value }
    private var lyricsImportEpoch: Int
        get() = lyricsImportRetained.epoch
        set(value) { lyricsImportRetained.epoch = value }
    private var lyricsReviewDialog: AlertDialog? = null
    private var lyricsRevision by mutableStateOf(0)
    private var loadedLyrics by mutableStateOf<Pair<String, TimedLyrics>?>(null)
    private val lyricsEditorRetained by lazy {
        ViewModelProvider(this)[LyricsEditorRetainedState::class.java]
    }
    private var lyricsEditorEpoch: Int
        get() = lyricsEditorRetained.epoch
        set(value) { lyricsEditorRetained.epoch = value }
    private var lyricsEditorSession: LyricsEditorSession?
        get() = lyricsEditorRetained.session
        set(value) { lyricsEditorRetained.session = value }
    private var pendingLyricsExport: PendingLyricsExport?
        get() = lyricsEditorRetained.pendingExport
        set(value) { lyricsEditorRetained.pendingExport = value }
    private lateinit var cloudStore: CloudBackupState
    private val cloudFailureStore by lazy { CloudAuthorizationFailureStore(this) }
    private var cloudInfo by mutableStateOf(CloudBackupStatus())
    private var cloudStatusSubscription: AutoCloseable? = null
    private var cloudBusy by mutableStateOf(false)
    private var cloudPhase by mutableStateOf(CloudOperationPhase.IDLE)
    private var cloudPendingAction = ""
    private val cloudAuthorizationRetained by lazy {
        ViewModelProvider(this)[CloudAuthorizationReceiptRetainedState::class.java]
    }
    private var cloudRequestStart: CloudBackupStatus?
        get() = cloudAuthorizationRetained.start
        set(value) { cloudAuthorizationRetained.start = value }
    private var cloudAwaitingResolution = false
    private var cloudRequestGeneration = 0
    private val cloudActionSavedKey = "nw09_cloud_pending_action"
    private val cloudResolutionSavedKey = "nw09_cloud_awaiting_resolution"
    private lateinit var roomShadow: RoomShadowMigrator
    private lateinit var verifiedReader: VerifiedRoomReader
    private var roomPreviewRequested = false
    private var roomPreviewActive by mutableStateOf(false)
    private var roomReadStatus by mutableStateOf("Legacy library reads are active")
    private var roomPreviewPlaylists by mutableStateOf<Map<String, List<VideoPreview>>>(emptyMap())
    private lateinit var profileShadow: ProfileDataStoreMirror
    private var shadowStatus by mutableStateOf("Legacy storage active · Room/DataStore migration pending")
    private val nativeGateway = YouTubeGateway()
    private var nativeSearchState by mutableStateOf(StudioSearchSnapshot())
    private var nativeSearchJob: Job? = null
    private var nativeSearchGeneration = 0
    private var offlineResumeReadJob: Job? = null
    private var offlineResumeReadGeneration = 0
    private var recentRefreshJob: Job? = null
    private var recentRefreshGeneration = 0
    private var searchNavigationToken by mutableStateOf(0)
    private var mainNavigationRequest by mutableStateOf<NightWaveMainNavigationRequest?>(null)
    private var appliedMainNavigation: String? = null
    private var shadowRunning = false
    private var shadowRequested = false
    private var youtubeDialog: Dialog? = null
    private val cloudResolution = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // This callback can arrive on a recreated Activity after Google's
        // external permission sheet closes.
        if (!cloudAwaitingResolution ||
            CloudAuthorizationRecoveryPolicy.safeAction(cloudPendingAction) == null) {
            if (::cloudStore.isInitialized) {
                cloudStore.status("Google authorization returned without a pending request")
                refreshCloud()
            }
            return@registerForActivityResult
        }
        cloudAwaitingResolution = false
        if (result.resultCode != RESULT_OK || result.data == null) {
            // Google may return an ApiException even when the Activity was
            // cancelled. Inspect only its numeric status, never result extras.
            val code = result.data?.let { data ->
                runCatching {
                    Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data)
                }.exceptionOrNull()?.let {
                    (it as? com.google.android.gms.common.api.ApiException)?.statusCode
                }
            }
            cloudBusy = false
            cloudPendingAction = ""
            if (code == null && result.resultCode != RESULT_OK) {
                // A real user dismissal isn't necessarily an OAuth failure.
                cloudFailureStore.clear()
                val prior = cloudRequestStart ?: cloudStore.read()
                cloudStore.statusIfCurrent(prior.consentSnapshot(),
                    "Google permission was not completed; local music unchanged", prior.accessRevision)
                refreshCloud()
                showMessage("Google permission not completed. Your library is safe.")
            } else {
                cloudAuthorizationFailed(
                    CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER, code)
            }
        } else {
            runCatching {
                Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data!!)
            }.onSuccess { finishCloudAuthorization(it) }
                .onFailure { error ->
                    cloudBusy = false
                    cloudPendingAction = ""
                    val code = CloudFailureCausePolicy.find(error,
                        com.google.android.gms.common.api.ApiException::class.java)?.statusCode
                    cloudAuthorizationFailed(
                        CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER, code)
                }
        }
    }

    private val importLyricsFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val id = pendingLyricsId
        pendingLyricsId = null
        if (uri == null || id == null) return@registerForActivityResult
        val selectedMedia = LyricsTrackBindingPolicy.mediaId(id) ?: return@registerForActivityResult
        val requestEpoch = ++lyricsImportEpoch
        lifecycleScope.launch {
            val ready = kotlinx.coroutines.withTimeoutOrNull(3_000L) {
                while (!playback.state.connected) delay(80L)
                true
            } == true
            if (!ready || requestEpoch != lyricsImportEpoch || playback.state.videoUrl != selectedMedia) {
                showMessage("Player or selected song changed. Choose lyrics again for the current track.")
                return@launch
            }
            val duration = playback.state.durationMs
            val selectedTitle = playback.state.title
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val title = boundLyrics.title(id, selectedTitle)
                    val displayName = runCatching {
                        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
                            null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    }.getOrNull().orEmpty().take(100).ifBlank { "Selected LRC" }
                    val data = requireNotNull(contentResolver.openInputStream(uri)) {
                        "Could not open the selected LRC file"
                    }.use { stream ->
                        val result = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(result.size() + count <= TimedLyricsCodec.MAX_BYTES) {
                                "Lyrics exceed the 64 KB import limit"
                            }
                            result.write(buffer, 0, count)
                        }
                        result.toByteArray()
                    }
                    val report = LyricsImportReviewPolicy.inspect(
                        data, displayName, title, duration)
                    PendingLyricsReview(data, report, title,
                        boundLyrics.fingerprint(id))
                }
            }
            if (requestEpoch != lyricsImportEpoch ||
                playback.state.videoUrl != selectedMedia) return@launch
            outcome.onSuccess { selection ->
                lyricsImportRetained.reviewId = id
                lyricsImportRetained.review = selection
                showLyricsImportReview(id, requestEpoch, selection)
            }.onFailure { error ->
                // Never expose the provider path, song content, or exception
                // stack trace in diagnostics or a notification.
                showMessage(LyricsImportReviewPolicy.failureHint(error))
            }
        }
    }

    /** The final Replace/Import tap is the only path that writes a picked LRC. */
    private fun showLyricsImportReview(
        id: String, requestEpoch: Int, selection: PendingLyricsReview
    ) {
        if (playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(id) ||
            requestEpoch != lyricsImportEpoch || isFinishing || isDestroyed) return
        val report = selection.report
        val replacing = selection.previousFingerprint != null
        val warnings = report.warnings.take(5).joinToString("\n") { "• $it" }
        val samples = report.lyrics.cues.take(2).joinToString("\n") {
            val minutes = it.timeMs / 60_000L
            val seconds = (it.timeMs / 1_000L) % 60
            "[%02d:%02d] %s".format(java.util.Locale.ROOT, minutes,
                seconds, it.text.take(64))
        }
        val summary = "Selected song: " + selection.title.take(85) +
            "\nBinding: " + LyricsTrackBindingPolicy.sourceLabel(id) +
            "\nSelected file: " + report.sourceLabel +
            "\nTimed lyric lines: " + report.cueCount +
            "\nLast timestamp: " + (report.lastCueMs / 1_000L) + "s" +
            "\n" + (if (replacing) "Existing LRC will be replaced only if you confirm."
                else "No existing LRC will be overwritten.") +
            "\n\nReview notes:\n" + warnings +
            "\n\nPreview:\n" + samples +
            "\n\nFilename/title matching is advisory. Confirm the song yourself."
        if (lyricsReviewDialog != null) return
        lyricsReviewDialog = AlertDialog.Builder(this)
            .setTitle(if (replacing) "Review lyric replacement" else "Review imported lyrics")
            .setMessage(summary)
            .setNegativeButton(if (replacing) "Keep existing lyrics" else "Cancel") { _, _ ->
                lyricsImportRetained.review = null; lyricsImportRetained.reviewId = null
            }
            .setOnCancelListener {
                lyricsImportRetained.review = null; lyricsImportRetained.reviewId = null
            }
            .setOnDismissListener { lyricsReviewDialog = null }
            .setPositiveButton(if (replacing) "Replace LRC" else "Import LRC") { _, _ ->
                if (requestEpoch != lyricsImportEpoch ||
                    playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(id)) {
                    showMessage("Song changed; lyrics were not imported.")
                    return@setPositiveButton
                }
                lyricsImportRetained.review = null; lyricsImportRetained.reviewId = null
                lifecycleScope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        runCatching {
                            check(requestEpoch == lyricsImportEpoch &&
                                playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(id)) {
                                "The selected song changed before saving"
                            }
                            boundLyrics.saveReviewed(
                                id, selection.bytes, selection.previousFingerprint) {
                                    !isDestroyed && requestEpoch == lyricsImportEpoch &&
                                        LyricsTrackBindingPolicy.matches(id, playback.state.videoUrl)
                                }
                        }
                    }
                    if (requestEpoch != lyricsImportEpoch ||
                        playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(id)) return@launch
                    saved.onSuccess { accepted ->
                        if (accepted) {
                            lyricsRevision++
                            showMessage("Your lyrics are attached to this selected source.")
                        } else {
                            showMessage("Existing lyrics changed during preview. " +
                                "Select the file again; no lyrics were replaced.")
                        }
                    }.onFailure {
                        showMessage("Could not save the reviewed LRC. " +
                            "Previously saved lyrics were kept.")
                    }
                }
            }.show()
    }

    /** Android asks the user where to save a separate .lrc document. */
    private val exportLyricsFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = pendingLyricsExport
        pendingLyricsExport = null
        if (uri == null) return@registerForActivityResult
        val editor = lyricsEditorSession
        if (pending == null || editor == null ||
            pending.requestId != lyricsEditorEpoch ||
            editor.requestId != pending.requestId ||
            editor.songId != pending.songId ||
            playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(pending.songId) ||
            isFinishing || isDestroyed) {
            showMessage("Lyrics export expired or song changed. No data was written.")
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Document pickers may return after a song change. Check
                    // again on the writing path, not only on callback entry.
                    val activeEditor = lyricsEditorSession
                    check(pending.requestId == lyricsEditorEpoch &&
                        activeEditor?.requestId == pending.requestId &&
                        activeEditor.songId == pending.songId &&
                        playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(pending.songId)) {
                        "Lyrics export session changed before opening destination"
                    }
                    val bytes = pending.prepared.bytes
                    require(bytes.size in 1..TimedLyricsCodec.MAX_BYTES)
                    requireNotNull(contentResolver.openOutputStream(uri, "w")) {
                        "Destination was unavailable"
                    }.use { out -> out.write(bytes); out.flush() }
                }
            }
            if (isFinishing || isDestroyed) return@launch
            result.onSuccess {
                showMessage(if (pending.prepared.unsavedDraft)
                    "LRC draft exported. Your NightWave edits are still unsaved."
                    else "Synchronized LRC exported to your chosen folder.")
            }.onFailure {
                showMessage("LRC export could not finish. The selected document " +
                    "may be incomplete; your saved NightWave lyrics are unchanged.")
            }
        }
    }

    private val exportFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val data = archiveManager.export()
            requireNotNull(contentResolver.openOutputStream(uri)) { "Can't open destination" }
                .use { it.write(data) }
        }.onSuccess { showMessage("Guest backup exported successfully.") }
            .onFailure { showMessage("Export failed: " + (it.message ?: "unknown error")) }
    }
    private val importFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            requireNotNull(contentResolver.openInputStream(uri)).use { input ->
                val limit = com.nightwave.player.v2.data.LibraryBackupCodec.MAX_BYTES
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(out.size() + count <= limit) { "Backup exceeds the size limit" }
                    out.write(buffer, 0, count)
                }
                archiveManager.preview(out.toByteArray())
            }
        }.onSuccess { archive ->
            AlertDialog.Builder(this)
                .setTitle("Replace your local library?")
                .setMessage(
                    "Backup contains " + archive.favorites.size + " liked songs and " +
                    archive.playlists.size + " playlists. This will replace your current favorites " +
                    "and playlists. Your local listening history stays on this phone. " +
                    "Export your existing library first if you want to keep it."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Replace library") { _, _ ->
                    if (archiveManager.restoreWithConfirmation(archive)) {
                        profile = profiles.load()
                        refresh()
                        showMessage("Guest library restored.")
                    } else showMessage("Import could not complete. Your previous library is retained.")
                }.show()
        }.onFailure { showMessage("Invalid backup: " + (it.message ?: "cannot read file")) }
    }
    /** SAF picker is always user-initiated; an absent/cancelled destination is a no-op. */
    private val exportPlaylistTextFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val pending = playlistDocumentRetained.pendingExport
        playlistDocumentRetained.pendingExport = null
        if (uri == null) return@registerForActivityResult
        if (pending == null) {
            showMessage("Playlist export expired after the picker. No file was written.")
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    require(pending.bytes.isNotEmpty() &&
                        pending.bytes.size <= PlaylistDocumentPolicy.MAX_BYTES)
                    requireNotNull(contentResolver.openOutputStream(uri, "w")) {
                        "Selected file cannot be opened"
                    }.use { output ->
                        output.write(pending.bytes)
                        output.flush()
                    }
                }
            }
            if (isFinishing || isDestroyed) return@launch
            outcome.onSuccess {
                showMessage("Playlist links exported to your chosen .txt file.")
            }.onFailure {
                showMessage("Could not finish exporting. The selected file may be incomplete; " +
                    "your NightWave playlists are unchanged.")
            }
        }
    }

    /** Imported untrusted text is strictly bounded and reviewed before any library write. */
    private val importPlaylistTextFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(contentResolver.openInputStream(uri)) {
                        "Selected text could not be opened"
                    }.use { input -> PlaylistDocumentPolicy.read(input) }
                }
            }
            if (isFinishing || isDestroyed) return@launch
            result.onSuccess { parsed ->
                if (parsed == null) showMessage(
                    "Invalid or incomplete NightWave playlist text. Library unchanged.")
                else showSharedPlaylistPreview(parsed)
            }.onFailure {
                showMessage("Unable to read that playlist text file. Library unchanged.")
            }
        }
    }

    private var profile by mutableStateOf(ProfileSettings(localId = "pending"))
    private var favorites by mutableStateOf<List<VideoPreview>>(emptyList())
    private var recentlyPlayed by mutableStateOf<List<VideoPreview>>(emptyList())
    /** Invalidates cached playlist recommendations after edits without renaming playlists. */
    private var libraryRevision by mutableStateOf(0)
    private var playlistUndo by mutableStateOf<PlaylistUndoState?>(null)
    private var offlineResumeTitle by mutableStateOf<String?>(null)
    private var offlineResumePositionMs by mutableStateOf(0L)
    private var names by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appliedMainNavigation = savedInstanceState?.getString("nw128-applied-main")
        mainNavigationRequest = NightWaveOfflineNavigator.request(intent, appliedMainNavigation)
        profiles = GuestProfileStore(this)
        library = MusicLibrary(this)
        folderStore = PlaylistFolderStore(this)
        pinStore = PlaylistPinStore(this)
        pinnedPlaylistNames = pinStore.snapshot(library.playlistNames())
        folderSnapshot = folderStore.snapshot()
        playback = StudioPlaybackBridge(this)
        sleepTimerStatus = SleepTimerStatusStore(this)
        offlineSpeedStore = OfflinePlaybackSpeedStore(this)
        offlineSpeedPercent = offlineSpeedStore.read()
        listeningLevelStore = ListeningLevelStore(this)
        listeningLevelPercent = listeningLevelStore.read()
        crossfadeSettings = CrossfadeSettingsStore(this)
        crossfadeSeconds = crossfadeSettings.read()
        normalizationStore = OfflineNormalizationStore(this)
        normalizationEnabled = normalizationStore.enabled()
        archiveManager = LibraryArchiveManager(this)
        lyricsStore = PrivateLyricsStore(this)
        lyricsOffsetStore = PrivateLyricsOffsetStore(this)
        boundLyrics = BoundLyricsStore(this, lyricsStore, lyricsOffsetStore)
        listeningMemoryStore = ListeningMemoryStore(this)
        refreshListeningMemory()
        cloudStore = CloudBackupState(this)
        refreshCloud()
        val restoredCloudAction = savedInstanceState?.getString(cloudActionSavedKey)
        val restoredAwaiting = savedInstanceState?.getBoolean(cloudResolutionSavedKey) == true
        if (CloudAuthorizationRecoveryPolicy.shouldRestore(
                restoredCloudAction, restoredAwaiting)) {
            cloudPendingAction = restoredCloudAction.orEmpty()
            cloudAwaitingResolution = true
            cloudBusy = true
            cloudPhase = CloudOperationPhase.AWAITING_PERMISSION
        }
        // Room/DataStore are read-only previews at this phase. A failure
        // opening the optional shadow must not take down the classic library
        // or the premium player on app launch.
        try {
            val database = NightWaveDatabase.get(this)
            roomShadow = RoomShadowMigrator(this, database)
            verifiedReader = VerifiedRoomReader(this, database)
            profileShadow = ProfileDataStoreMirror(this)
        } catch (error: Exception) {
            shadowStatus = "Optional storage preview unavailable; original library retained."
            roomReadStatus = "Original library reads remain active."
        }
        profile = profiles.load()
        refresh()
        setContent {
            // Display only. The media service remains the single timer owner,
            // including when this Activity is no longer visible.
            LaunchedEffect(Unit) {
                while (true) {
                    sleepRemainingMs = sleepTimerStatus.remainingMs()
                    delay(5_000L)
                }
            }
            LaunchedEffect(playback.state.videoUrl) {
                // A public track change only affects listening history. Do not
                // reset the Room preview or hash large offline audio on the UI.
                if (playback.state.videoUrl.isNotBlank()) refreshRecentForTrackChange()
            }
            LaunchedEffect(playback.state.videoUrl, playback.state.connected, lyricsRevision) {
                lyricsOffsetWriteVersion++
                val selected = playback.state.videoUrl
                if (playback.state.connected && selected.isNotBlank()) {
                    val awaiting = lyricsImportRetained.reviewId ?: pendingLyricsId
                    if (awaiting != null && !LyricsTrackBindingPolicy.matches(awaiting, selected)) {
                        lyricsImportEpoch++
                        pendingLyricsId = null
                        lyricsImportRetained.reviewId = null; lyricsImportRetained.review = null
                        lyricsReviewDialog?.dismiss()
                    } else if (lyricsImportRetained.reviewId != null && lyricsImportRetained.review != null) {
                        showLyricsImportReview(requireNotNull(lyricsImportRetained.reviewId),
                            lyricsImportEpoch, requireNotNull(lyricsImportRetained.review))
                    }
                }
                lyricsEditorSession?.let { session ->
                    // Media3 can briefly report a blank track while a
                    // recreated Activity reconnects. Keep the retained draft
                    // until an actual, different track becomes known.
                    val current = playback.state.videoUrl
                    if (current.isNotBlank() &&
                        current != LyricsTrackBindingPolicy.mediaId(session.songId)) {
                        val unsaved = LyricsEditorPolicy.changed(
                            session.baseline, session.draft)
                        lyricsEditorEpoch++
                        lyricsEditorSession = null
                        pendingLyricsExport = null
                        if (unsaved) showMessage(
                            "Song changed; unsaved lyric edits were discarded. " +
                                "Previously saved lyrics are unchanged.")
                    }
                }
                loadedLyrics = null
                currentLyricsOffsetMs = 0
                val current = playback.state.videoUrl
                val lyricId = LyricsTrackBindingPolicy.storageId(current)
                if (lyricId != null) {
                    val id = lyricId
                    val result = withContext(Dispatchers.IO) {
                        val lines = runCatching { boundLyrics.read(id) }.getOrNull()
                        val offset = if (lines != null)
                            boundLyrics.readOffset(id) else 0
                        lines to offset
                    }
                    if (playback.state.videoUrl == current && result.first != null) {
                        loadedLyrics = id to requireNotNull(result.first)
                        currentLyricsOffsetMs = result.second
                    }
                }
            }
            NightWaveMaterialTheme(
                choice = profile.theme,
                reduceMotion = profile.motion == MotionPreference.REDUCED
            ) {
                StudioRoot(
                    profile = profile, favorites = favorites, recent = recentlyPlayed,
                    libraryRevision = libraryRevision,
                    playlistNames = names,
                    folderSnapshot = folderSnapshot,
                    getPlaylist = { name ->
                        if (roomPreviewActive) roomPreviewPlaylists[name] ?: emptyList()
                        else library.playlist(name)
                    },
                    playerState = playback.state, playerActions = playback,
                    sleepRemainingMs = sleepRemainingMs,
                    onSleepTimer = { minutes ->
                        runCatching {
                            startService(Intent(this, PlaybackService::class.java).apply {
                                action = if (minutes == null)
                                    PlaybackService.ACTION_SLEEP_TIMER_CANCEL
                                else PlaybackService.ACTION_SLEEP_TIMER_SET
                                if (minutes != null)
                                    putExtra(PlaybackService.EXTRA_SLEEP_MINUTES, minutes)
                            })
                        }.onSuccess {
                            lifecycleScope.launch {
                                delay(300L)
                                sleepRemainingMs = sleepTimerStatus.remainingMs()
                            }
                        }.onFailure {
                            showMessage("Sleep timer couldn't be updated. Try again.")
                        }
                    },
                    listeningLevelPercent = listeningLevelPercent,
                    crossfadeSeconds = crossfadeSeconds,
                    normalizationEnabled = normalizationEnabled,
                    onNormalizationChange = { enabled ->
                        runCatching {
                            startService(Intent(this, PlaybackService::class.java).apply {
                                action = PlaybackService.ACTION_NORMALIZATION_SET
                                putExtra(PlaybackService.EXTRA_NORMALIZATION_ENABLED, enabled)
                            })
                        }.onSuccess {
                            lifecycleScope.launch {
                                delay(300L)
                                normalizationEnabled = normalizationStore.enabled()
                            }
                        }.onFailure {
                            showMessage("Offline volume balancing could not be changed.")
                        }
                    },
                    onCrossfade = { seconds ->
                        if (CrossfadePolicy.accepts(seconds)) {
                            runCatching {
                                startService(Intent(this, PlaybackService::class.java).apply {
                                    action = PlaybackService.ACTION_CROSSFADE_SET
                                    putExtra(PlaybackService.EXTRA_CROSSFADE_SECONDS, seconds)
                                })
                            }.onSuccess {
                                lifecycleScope.launch {
                                    delay(300L)
                                    crossfadeSeconds = crossfadeSettings.read()
                                }
                            }.onFailure {
                                showMessage("Crossfade could not be updated.")
                            }
                        }
                    },
                    onListeningLevel = { percent ->
                        if (ListeningLevelPolicy.accepts(percent)) {
                            runCatching {
                                startService(Intent(this, PlaybackService::class.java).apply {
                                    action = PlaybackService.ACTION_LISTENING_LEVEL_SET
                                    putExtra(PlaybackService.EXTRA_LISTENING_LEVEL_PERCENT, percent)
                                })
                            }.onSuccess {
                                lifecycleScope.launch {
                                    delay(300L)
                                    listeningLevelPercent = listeningLevelStore.read()
                                }
                            }.onFailure {
                                showMessage("Listening level could not be changed.")
                            }
                        }
                    },
                    offlineSpeedPercent = offlineSpeedPercent,
                    onOfflineSpeed = { percent ->
                        if (OfflinePlaybackSpeedPolicy.accepts(percent)) {
                            runCatching {
                                startService(Intent(this, PlaybackService::class.java).apply {
                                    action = PlaybackService.ACTION_OFFLINE_SPEED_SET
                                    putExtra(PlaybackService.EXTRA_OFFLINE_SPEED_PERCENT, percent)
                                })
                            }.onSuccess {
                                // The service commits the new setting before it is shown.
                                lifecycleScope.launch {
                                    delay(300L)
                                    offlineSpeedPercent = offlineSpeedStore.read()
                                }
                            }.onFailure {
                                showMessage("Could not change offline playback speed.")
                            }
                        }
                    },
                    memory = memorySummary,
                    onMemoryEnabledChange = { setListeningMemory(it) },
                    onMemoryErase = { confirmEraseListeningMemory() },
                    onMemoryRefresh = { refreshListeningMemory() },
                    currentLyrics = loadedLyrics?.takeIf {
                        playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(it.first)
                    }?.second,
                    lyricsOffsetMs = currentLyricsOffsetMs,
                    onLyricsOffsetChange = { desiredOffset ->
                        val current = playback.state.videoUrl
                        val id = LyricsTrackBindingPolicy.storageId(current)
                        if (id != null && loadedLyrics?.first == id &&
                            LyricsSyncPolicy.validOffset(desiredOffset)) {
                            val version = ++lyricsOffsetWriteVersion
                            // Immediate response to repeated taps; only the
                            // latest scheduled adjustment may commit to disk.
                            currentLyricsOffsetMs = desiredOffset
                            lifecycleScope.launch {
                                val saved = withContext(Dispatchers.IO) {
                                    synchronized(lyricsOffsetStore) {
                                        if (version != lyricsOffsetWriteVersion) true
                                        else runCatching {
                                            boundLyrics.saveOffset(id, desiredOffset) {
                                                version == lyricsOffsetWriteVersion && !isDestroyed &&
                                                    LyricsTrackBindingPolicy.matches(id, playback.state.videoUrl)
                                            }
                                        }.getOrDefault(false)
                                    }
                                }
                                if (version == lyricsOffsetWriteVersion &&
                                    playback.state.videoUrl == current &&
                                    loadedLyrics?.first == id && !saved) {
                                    currentLyricsOffsetMs = boundLyrics.readOffset(id)
                                    showMessage("Lyric timing could not be saved.")
                                }
                            }
                        }
                    },
                    onEditLyrics = { openLyricsEditor() },
                    onImportLyrics = {
                        val id = LyricsTrackBindingPolicy.storageId(playback.state.videoUrl)
                        if (id != null) {
                            pendingLyricsId = id
                            importLyricsFile.launch(arrayOf("text/*", "application/octet-stream"))
                        }
                    },
                    onRemoveLyrics = {
                        val id = LyricsTrackBindingPolicy.storageId(playback.state.videoUrl)
                        if (id != null) lifecycleScope.launch {
                            val done = withContext(Dispatchers.IO) {
                                runCatching { boundLyrics.delete(id) {
                                    !isDestroyed && LyricsTrackBindingPolicy.matches(id, playback.state.videoUrl)
                                } }.getOrDefault(false)
                            }
                            if (done) {
                                lyricsRevision++
                                showMessage("Private lyrics removed from this device.")
                            } else showMessage("Lyrics could not be removed.")
                        }
                    },
                    offlineResumeTitle = offlineResumeTitle,
                    offlineResumePositionMs = offlineResumePositionMs,
                    onResumeOffline = {
                        runCatching {
                            startService(Intent(this, PlaybackService::class.java)
                                .setAction(PlaybackService.ACTION_RESUME_OFFLINE))
                        }.onFailure {
                            showMessage("Offline music could not resume. Open the Offline library.")
                        }
                    },
                    isFavorite = { url -> library.isFavorite(url) },
                    onToggleFavorite = { video -> library.toggleFavorite(video); refresh() },
                    onSongOptions = { video -> showSongOptions(video) },
                    onPlaylistSongOptions = { name, video ->
                        showPlaylistSongOptions(name, video)
                    },
                    onRemoveRecent = { video -> confirmRemoveRecent(video) },
                    onClearRecent = { confirmClearRecent() },
                    onSharePlaylist = { name, tracks -> sharePlaylist(name, tracks) },
                    onExportPlaylist = { name, tracks -> exportPlaylistText(name, tracks) },
                    onBulkRemove = { name, selected ->
                        val before = library.playlist(name)
                        val urls = selected.map { it.url }.distinct()
                        if (urls.isEmpty() || urls.size > 20 ||
                            !urls.all { target -> before.any { it.url == target } }) {
                            showMessage("Selection is no longer current. Refresh the playlist.")
                        } else AlertDialog.Builder(this)
                            .setTitle("Remove ${urls.size} songs from playlist?")
                            .setMessage("Only the “${name.take(50)}” playlist changes. " +
                                "Liked songs, listening history and offline audio are retained.")
                            .setNegativeButton("Keep", null)
                            .setPositiveButton("Remove") { _, _ ->
                                val updated = library.bulkRemoveFromPlaylist(
                                    name, urls, before.map { it.url })
                                if (updated.accepted && updated.changed) {
                                    playlistUndo = PlaylistUndoState(name, before,
                                        updated.updated.songs(name),
                                        "Removed ${urls.size} songs from ${name.take(40)}")
                                    refresh()
                                    showMessage("Removed from playlist · Undo available in Library.")
                                } else showMessage("Playlist changed or couldn't be saved. Try again.")
                            }.show()
                    },
                    onBulkCopy = { fromName, selected ->
                        val source = library.playlist(fromName)
                        val urls = selected.map { it.url }.distinct()
                        val freshSource = fromName in library.playlistNames() &&
                            urls.isNotEmpty() && urls.size == selected.size &&
                            urls.size <= LibraryBulkSelectionPolicy.MAX_SELECTED &&
                            selected.all { track -> source.any { it == track } }
                        val names = library.playlistNames().filterNot { it == fromName }
                        if (!freshSource) showMessage("Source playlist changed. Reopen selection.")
                        else if (names.isEmpty()) showMessage("Create another playlist before copying.")
                        else AlertDialog.Builder(this).setTitle("Copy selected songs to…")
                            .setItems(names.toTypedArray()) { _, chosen ->
                                val destination = StudioUiStateSafetyPolicy.itemAt(names, chosen)
                                if (destination == null) {
                                    showMessage("Playlist selection changed. Open Copy again.")
                                    return@setItems
                                }
                                val latestSource = library.playlist(fromName)
                                if (fromName !in library.playlistNames() ||
                                    latestSource != source) {
                                    // Metadata and order matter too: stale source records
                                    // must never overwrite newer library information.
                                    showMessage("Source playlist changed. Nothing was copied.")
                                } else {
                                    val before = library.playlist(destination)
                                    val updated = library.bulkAddToPlaylist(
                                        destination, selected, before.map { it.url })
                                    if (updated.accepted && updated.changed) {
                                        playlistUndo = PlaylistUndoState(destination, before,
                                            updated.updated.songs(destination),
                                            "Copied songs to ${destination.take(40)}")
                                        refresh()
                                        showMessage("Copied to playlist · Undo available in Library.")
                                    } else {
                                        showMessage("Nothing copied; target changed, is full, or already has these songs.")
                                    }
                                }
                            }.setNegativeButton("Cancel", null).show()
                    },
                    onMovePlaylistSong = { name, url, delta, expected ->
                        val now = library.playlist(name)
                        val current = now.map { it.url }
                        val index = current.indexOf(url)
                        val destination = PlaylistReorderGesturePolicy.target(
                            index, delta, current.size)
                        if (current != expected || destination == null) {
                            showMessage("Playlist changed or track is already at the edge.")
                        } else {
                            val ordered = current.toMutableList()
                            val moved = ordered.removeAt(index)
                            ordered.add(destination, moved)
                            val updated = library.reorderPlaylist(name, expected, ordered)
                            if (updated.accepted && updated.changed) {
                                playlistUndo = PlaylistUndoState(name, now,
                                    updated.updated.songs(name), "Playlist order updated")
                                refresh()
                            } else showMessage("Couldn't save new track order. Try again.")
                        }
                    },
                    playlistUndo = playlistUndo,
                    onUndoPlaylistEdit = {
                        playlistUndo?.let { pending ->
                            val restored = library.restorePlaylistIfUnchanged(
                                pending.playlist, pending.after, pending.before)
                            if (restored.accepted && restored.changed) {
                                showMessage("Playlist change undone.")
                                refresh()
                            } else showMessage("Undo is unavailable: playlist changed again.")
                        }
                        playlistUndo = null
                    },
                    onDismissPlaylistUndo = { playlistUndo = null },
                    onBulkShareLinks = { songs, copyOnly ->
                        val safe = songs.take(LibraryBulkSelectionPolicy.MAX_SELECTED)
                            .mapNotNull { VideoLink.normalize(it.url) }.distinct()
                        if (safe.isEmpty()) showMessage("No public song links selected.")
                        else {
                            val links = safe.joinToString("\n")
                            if (copyOnly) {
                                runCatching {
                                    val clipboard = getSystemService(
                                        android.content.Context.CLIPBOARD_SERVICE)
                                        as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                                        "NightWave selected song links", links))
                                }.onSuccess { showMessage("${safe.size} song links copied.") }
                                    .onFailure { showMessage("Couldn't copy song links.") }
                            } else runCatching {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, links)
                                }
                                startActivity(Intent.createChooser(share, "Share selected songs"))
                            }.onFailure { showMessage("Sharing is unavailable on this device.") }
                        }
                    },
                    onImportSharedPlaylist = { choosePlaylistImport() },
                    onSaveCurrentToPlaylist = { saveCurrentToPlaylist() },
                    onSaveCurrentOffline = { saveCurrentOffline() },
                    storageStatus = shadowStatus,
                    onCheckStorage = { refreshShadows() },
                    roomReadStatus = roomReadStatus,
                    roomPreviewActive = roomPreviewActive,
                    onEnableRoomPreview = { enableVerifiedRoomPreview() },
                    onUseLegacyReads = { disableRoomPreview() },
                    onExportGuest = { exportFile.launch("nightwave-guest-backup.json") },
                    onImportGuest = { importFile.launch(arrayOf("application/json", "application/octet-stream")) },
                    onRecoverLastChange = { confirmLastWriteRecovery() },
                    cloud = cloudInfo,
                    cloudBusy = cloudBusy, cloudPhase = cloudPhase,
                    onCloudConnect = { requestCloudAction("connect") },
                    onCloudBackup = { requestCloudAction("backup") },
                    onCloudPreviewRestore = { requestCloudAction("preview") },
                    onCloudCompare = { requestCloudAction("merge") },
                    onCloudDisconnect = { disconnectCloud() },
                    onCloudHistory = { enabled ->
                        if (cloudStore.setHistory(enabled)) refreshCloud()
                    },
                    onCloudLyrics = { enabled ->
                        if (cloudStore.setLyrics(enabled)) refreshCloud()
                    },
                    onCloudPreferences = { enabled ->
                        if (cloudStore.setPreferences(enabled)) refreshCloud()
                    },
                    onCloudWifi = { enabled ->
                        if (cloudStore.setWifiOnly(enabled)) {
                            refreshCloud()
                            runCatching { NightWaveCloudSchedule.update(this) }
                                .onFailure { cloudStore.status("Could not update backup schedule") }
                        }
                    },
                    onSaveProfile = { updated ->
                        if (profiles.save(updated)) {
                            profile = profiles.load()
                            refreshShadows()
                        }
                    },
                    searchState = nativeSearchState,
                    onSearch = { query, mode -> nativeSearch(query, mode) },
                    onPrimarySearch = { query, mode -> primarySearch(query, mode) },
                    onPrepareSearch = { query, mode -> prepareSearch(query, mode) },
                    onWebSearch = { query -> openOfficialSearch(query) },
                    onCancelSearch = { cancelNativeSearch() },
                    onPlay = { video, collection -> playSearchedOfflineFirst(video, collection) },
                    onLibraryQuickPlay = { collection -> playback.playLibraryCollection(collection) },
                    onLegacyPlayer = { manageLibrary() },
                    onManageFolders = { managePlaylistFolders() },
                    onMovePlaylist = { name -> choosePlaylistFolder(name) },
                    pinnedPlaylistNames = pinnedPlaylistNames,
                    onTogglePinPlaylist = { name ->
                        val result = pinStore.toggle(name, library.playlistNames())
                        if (result == null) showMessage("Pin change was not saved.")
                        else {
                            pinnedPlaylistNames = result
                            showMessage(if (name in result) "Playlist pinned." else "Playlist unpinned.")
                        }
                    },
                    onCreatePlaylist = {
                        promptPlaylistName { name ->
                            if (library.createPlaylist(name)) refresh()
                            else showMessage("That playlist name is unavailable.")
                        }
                    },
                    onOfflineLibrary = { startActivity(Intent(this, OfflineLibraryActivity::class.java)) },
                    onOfflineSearch = { search ->
                        startActivity(Intent(this, OfflineLibraryActivity::class.java)
                            .putExtra(OfflineLibraryActivity.EXTRA_SEARCH_QUERY, search))
                    },
                    onShareDiagnostics = { showCrashSummaryDialog() },
                    onOpenClassic = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    searchNavigationToken = searchNavigationToken,
                    mainNavigationRequest = mainNavigationRequest,
                    onMainNavigationApplied = { id ->
                        appliedMainNavigation = id
                        if (mainNavigationRequest?.id == id) mainNavigationRequest = null
                    }
                )
                lyricsEditorSession?.let { session ->
                    if (playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(session.songId)) {
                        NightWaveLyricsEditorDialog(
                            songId = session.songId,
                            songTitle = session.songTitle,
                            draft = session.draft,
                            dirty = LyricsEditorPolicy.changed(session.baseline, session.draft),
                            canUndo = session.undo.isNotEmpty(),
                            saving = session.saving,
                            playbackMs = playback.state.positionMs,
                            offsetMs = currentLyricsOffsetMs,
                            reduceMotion = profile.motion == MotionPreference.REDUCED,
                            onApply = { onLyricsEditorDraft(it) },
                            onUndo = { undoLyricsEditorDraft() },
                            onSave = { saveLyricsEditorDraft() },
                            onExport = { exportLyricsEditorDraft() },
                            onDiscard = { discardLyricsEditorDraft() }
                        )
                    }
                }
            }
        }
        playback.connect()
        // Rotation must not repeat external share playback or web searches.
        if (savedInstanceState == null) handleIncomingShare(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        super.startActivity(NightWaveOfflineNavigator.reuseOfflineIntent(intent), options)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainNavigationRequest = NightWaveOfflineNavigator.request(intent, appliedMainNavigation)
        handleIncomingShare(intent)
    }

    /**
     * The premium Activity owns ACTION_SEND now that it is the launcher.
     * Accept bounded plain text only; never treat arbitrary external links as
     * media, and don't replay an old share during a configuration change.
     */
    private fun handleIncomingShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val raw = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (raw.isBlank()) return
        // An incoming playlist never auto-plays and never mutates the library.
        // The explicit preview and confirmation are mandatory.
        if (raw.trimStart().startsWith(PlaylistShareParser.PREFIX)) {
            val incoming = PlaylistShareParser.parse(raw)
            if (incoming == null) showMessage("Invalid or incomplete NightWave playlist share.")
            else showSharedPlaylistPreview(incoming)
            return
        }
        val shared = raw.trim().take(160)
        if (shared.isBlank()) return
        searchNavigationToken++
        val watchUrl = VideoLink.normalize(shared)
        if (watchUrl != null) {
            playback.play(VideoPreview("Shared track", "YouTube", watchUrl, "", 0L))
        } else {
            prepareSearch(shared, SearchMode.SONGS)
        }
    }

    /**
     * Official YouTube discovery is the primary route. Public scrapers are
     * optional quick results and must not block the primary search action.
     */
    private fun primarySearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        when (SearchInputResolver.resolve(clean)) {
            SearchInput.Blank -> return
            is SearchInput.VideoUrl, SearchInput.BadLink -> nativeSearch(clean, mode)
            is SearchInput.Keywords -> {
                prepareSearch(clean, mode)
                openOfficialSearch(clean)
            }
        }
    }

    /** Home shortcuts prefill without starting an unreliable provider call. */
    private fun prepareSearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        if (clean.isBlank()) return
        nativeSearchJob?.cancel()
        nativeSearchGeneration++
        nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
            prefillToken = java.util.UUID.randomUUID().toString())
    }

    private fun openOfficialSearch(query: String) {
        val clean = query.trim().take(160)
        if (clean.isBlank()) return
        youtubeDialog?.dismiss()
        youtubeDialog = YouTubeSearchBrowser(this).show(clean) { url ->
            playback.play(VideoPreview("Loading track…", "YouTube", url, "", 0L))
        }
    }

    private fun refreshCloud() {
        if (::cloudStore.isInitialized) cloudInfo = cloudStore.read()
    }

    /**
     * Only record a numeric public Google status and the lifecycle stage.
     * No email, token, Intent contents or exception message is retained.
     */
    private fun cloudAuthorizationFailed(
        stage: CloudAuthorizationFailurePolicy.Stage, code: Int?
    ) {
        val message = CloudAuthorizationFailurePolicy.shortMessage(code, stage)
        val current = cloudRequestStart ?: cloudStore.read()
        val cancelled = stage != CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK && code in setOf(16, 12501)
        val receipt = if (cancelled) current.accessRevision else cloudStore.recordAccessIfCurrent(
            current.consentSnapshot(), CloudAccessPolicy.afterFailure(current.accessState, stage, code),
            expectedAccessRevision = current.accessRevision)
        if (receipt == null || !cloudStore.statusIfCurrent(current.consentSnapshot(), message, receipt)) {
            refreshCloud()
            showMessage("A newer Drive access check is available. Start the action again.")
            return
        }
        cloudFailureStore.record(stage, code)
        refreshCloud()
        showMessage(message)
    }

    private fun requestCloudAction(action: String) {
        if (cloudBusy) return
        if (CloudAuthorizationRecoveryPolicy.safeAction(action) == null) {
            showMessage("Unknown Google Drive action.")
            return
        }
        val googleAvailability = GoogleApiAvailability.getInstance()
        val playServicesCode = googleAvailability.isGooglePlayServicesAvailable(this)
        if (playServicesCode != ConnectionResult.SUCCESS) {
            val message = "Google Play services unavailable (status $playServicesCode)"
            cloudStore.status(message)
            refreshCloud()
            if (googleAvailability.isUserResolvableError(playServicesCode)) {
                // Native Google UI, not an untrusted browser or login capture.
                runCatching {
                    googleAvailability.getErrorDialog(this, playServicesCode, 4501)?.show()
                }
            }
            showMessage("$message. Resolve the Google Play services prompt and retry.")
            return
        }
        if (action != "connect" && cloudStore.read().account.isBlank()) {
            showMessage("Connect Google Drive first.")
            return
        }
        cloudRequestStart = cloudStore.read()
        cloudPendingAction = action
        cloudPhase = CloudOperationPhase.AUTHORIZING
        cloudBusy = true
        cloudAwaitingResolution = false
        val attempt = ++cloudRequestGeneration
        // A Play-services Task may never complete. Only the Task itself has
        // a timeout; never time out an account picker while the user decides.
        lifecycleScope.launch {
            delay(CloudAuthorizationRecoveryPolicy.timeoutMs())
            if (CloudAuthorizationRecoveryPolicy.requestIsCurrent(
                    attempt, cloudRequestGeneration, cloudBusy, cloudAwaitingResolution)) {
                cloudBusy = false
                cloudPendingAction = ""
                cloudAuthorizationFailed(CloudAuthorizationFailurePolicy.Stage.REQUEST, null)
            }
        }
        val name = if (action == "connect") null else cloudStore.read().account
        // Google Play services can fail synchronously before returning a Task.
        // Always restore the UI and explain the failure without exposing tokens.
        val authorization = try {
            Identity.getAuthorizationClient(this)
                .authorize(NightWaveCloudAuthorization.request(name))
        } catch (_: Exception) {
            cloudBusy = false
            cloudPendingAction = ""
            cloudAuthorizationFailed(CloudAuthorizationFailurePolicy.Stage.REQUEST, null)
            return
        }
        authorization.addOnSuccessListener { response ->
                if (!CloudAuthorizationRecoveryPolicy.requestIsCurrent(
                        attempt, cloudRequestGeneration, cloudBusy, cloudAwaitingResolution)
                    || isFinishing || isDestroyed) return@addOnSuccessListener
                if (response.hasResolution()) {
                    val pending = response.pendingIntent
                    if (pending == null) {
                        cloudBusy = false
                        cloudPendingAction = ""
                        cloudAuthorizationFailed(CloudAuthorizationFailurePolicy.Stage.REQUEST, null)
                    } else {
                        cloudAwaitingResolution = true
                        cloudPhase = CloudOperationPhase.AWAITING_PERMISSION
                        runCatching {
                            cloudResolution.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                        }.onFailure {
                            cloudAwaitingResolution = false
                            cloudBusy = false
                            cloudPendingAction = ""
                            cloudAuthorizationFailed(CloudAuthorizationFailurePolicy.Stage.REQUEST, null)
                        }
                    }
                } else finishCloudAuthorization(response)
            }
            .addOnFailureListener { error ->
                if (!CloudAuthorizationRecoveryPolicy.requestIsCurrent(
                        attempt, cloudRequestGeneration, cloudBusy, cloudAwaitingResolution)
                    || isFinishing || isDestroyed) return@addOnFailureListener
                cloudBusy = false
                cloudPendingAction = ""
                // Numeric Play-services status is safe diagnostic evidence.
                // Do not log OAuth tokens, email addresses or exception text.
                val statusCode =
                    CloudFailureCausePolicy.find(error,
                        com.google.android.gms.common.api.ApiException::class.java)?.statusCode
                cloudAuthorizationFailed(
                    CloudAuthorizationFailurePolicy.Stage.REQUEST, statusCode)
            }
    }

    private fun finishCloudAuthorization(result: AuthorizationResult) {
        cloudAwaitingResolution = false
        val action = cloudPendingAction
        cloudPendingAction = ""
        if (CloudAuthorizationRecoveryPolicy.safeAction(action) == null) {
            cloudBusy = false
            cloudStore.status("Google permission returned without a valid pending operation")
            refreshCloud()
            showMessage("Google permission expired. Please start the action again.")
            return
        }
        val token = runCatching { NightWaveCloudAuthorization.requireToken(result) }.getOrNull()
        if (token == null) {
            cloudBusy = false
            cloudAuthorizationFailed(
                CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER, null)
            return
        }
        val legacyAccount = NightWaveCloudAuthorization.authorizedAccount(result)
        // The deprecated GoogleSignInAccount field is not guaranteed after a
        // Google permission chooser. Resolve the email from Drive itself.
        // Invalidate only the pre-picker timeout; verification has HTTP limits.
        val verificationGeneration = ++cloudRequestGeneration
        val verificationState = cloudRequestStart
        if (verificationState == null) {
            cloudBusy = false
            refreshCloud()
            showMessage("The permission request expired. Start the Drive action again.")
            return
        }
        cloudPhase = CloudOperationPhase.CHECKING_ACCOUNT
        lifecycleScope.launch {
            val verified = withContext(Dispatchers.IO) {
                runCatching { NightWaveDriveClient(token).authorizedEmailAddress() }
            }
            if (isFinishing || isDestroyed || !cloudBusy ||
                cloudRequestGeneration != verificationGeneration) return@launch
            val account = verified.getOrNull()
            if (account == null ||
                (legacyAccount != null && !account.equals(legacyAccount, ignoreCase = true))) {
                cloudBusy = false
                val code = (verified.exceptionOrNull() as? DriveHttpFailure)?.statusCode
                val message = if (code != null) DriveHttpFailurePolicy.message(code)
                    else "Google permitted access, but Drive account verification failed. Retry later."
                val failureRevision = cloudStore.recordAccessIfCurrent(verificationState.consentSnapshot(),
                    CloudAccessPolicy.afterFailure(verificationState.accessState,
                        CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, code),
                    expectedAccessRevision = verificationState.accessRevision)
                if (failureRevision == null) {
                    refreshCloud()
                    showMessage("A newer account check is available. Start the action again.")
                    return@launch
                }
                cloudStore.statusIfCurrent(verificationState.consentSnapshot(), message, failureRevision)
                cloudFailureStore.record(CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, code)
                refreshCloud()
                showMessage(message)
                return@launch
            }
            val current = cloudStore.read()
            if (!CloudBackupConsentPolicy.same(verificationState.consentSnapshot(), current.consentSnapshot()) ||
                verificationState.accessRevision != current.accessRevision) {
                cloudBusy = false
                refreshCloud()
                showMessage("Drive access or backup choices changed. Start the action again.")
                return@launch
            }
            finishVerifiedCloudAuthorization(action, token, account, verificationState)
        }
    }

    private fun finishVerifiedCloudAuthorization(action: String, token: String, account: String,
        verifiedState: CloudBackupStatus) {
        if (action == "connect") {
            cloudBusy = false
            if (cloudStore.connect(account, expected = verifiedState)) {
                cloudFailureStore.clear()
                runCatching { NightWaveCloudSchedule.stop(this) }
                refreshCloud()
                showMessage("Drive connected. Choose Back up now or Preview restore.")
            } else showMessage("Google account could not be saved.")
            return
        }
        val expectedAccount = cloudStore.read().account
        if (expectedAccount.isBlank() || !expectedAccount.equals(account, ignoreCase = true)) {
            cloudBusy = false
            cloudStore.status("Google account mismatch; reconnect before using backup")
            refreshCloud()
            return
        }
        val operationConsent = cloudStore.read().consentSnapshot()
        val accessRevision = cloudStore.recordAccessIfCurrent(operationConsent, CloudAccessState.CHECKED,
            expectedAccessRevision = verifiedState.accessRevision)
        if (accessRevision == null) {
            cloudBusy = false
            refreshCloud()
            showMessage("Drive access could not be confirmed locally. Try again.")
            return
        }
        val recoverySession = CloudRecoverySession(operationConsent, accessRevision)
        cloudPhase = if (action == "backup") CloudOperationPhase.PREPARING_BACKUP
            else CloudOperationPhase.LISTING_BACKUPS
        cloudFailureStore.clear()
        refreshCloud()
        lifecycleScope.launch {
            val resultTask = withContext(Dispatchers.IO) {
                runCatching {
                    val drive = NightWaveDriveClient(token)
                    when (action) {
                        "backup" -> {
                            val local = archiveManager.export()
                            val settings = cloudStore.read()
                            val history = if (settings.includeHistory) library.recent() else emptyList()
                            val extraPreferences = if (settings.includePreferences)
                                CloudPlaybackPreferences.from(profiles.load()) else null
                            val extraLyrics = if (settings.includeLyrics)
                                lyricsStore.exportCloud() else emptyList()
                            val bytes = CloudBackupCodec.encode(local, history, settings.includeHistory,
                                extraPreferences, extraLyrics, settings.includeLyrics)
                            val latestSettings = cloudStore.read()
                            check(CloudBackupConsentPolicy.same(operationConsent,
                                latestSettings.consentSnapshot())) {
                                "Google account or backup privacy settings changed during upload"
                            }
                            check(CloudRecoverySessionPolicy.matches(recoverySession, cloudStore.read()))
                            withContext(Dispatchers.Main) { cloudPhase = CloudOperationPhase.TRANSFERRING_BACKUP }
                            val outcome = drive.backUpIfChangedVerified(bytes)
                            CloudActionResult.Backup(outcome.created)
                        }
                        "preview", "merge" -> {
                            val options = drive.listSnapshots().takeIf { it.isNotEmpty() }
                                ?: error("No NightWave cloud backup found")
                            CloudActionResult.Snapshots(options)
                        }
                        else -> error("Invalid cloud action")
                    }
                }
            }
            cloudBusy = false
            resultTask.onSuccess { outcome ->
                when (outcome) {
                    is CloudActionResult.Backup -> {
                        if (!CloudBackupConsentPolicy.same(operationConsent,
                                cloudStore.read().consentSnapshot())) {
                            showMessage("Backup transfer finished, but your settings changed. " +
                                "Run Back up now again to verify the current choices.")
                            return@onSuccess
                        }
                        if (cloudStore.successIfCurrent(
                                operationConsent, System.currentTimeMillis(), outcome.created,
                                expectedAccessRevision = accessRevision)) {
                            runCatching {
                                NightWaveCloudSchedule.update(this@NightWaveStudioActivity)
                            }.onFailure {
                                cloudStore.status("Upload verified; backup schedule needs attention")
                            }
                            refreshCloud()
                            showMessage(if (outcome.created) "Cloud backup uploaded and verified."
                                else "Cloud backup already current; nothing uploaded.")
                        } else {
                            refreshCloud()
                            showMessage(if (CloudAccountSessionPolicy.same(operationConsent,
                                    cloudStore.read().consentSnapshot()))
                                "Drive copy verified; local status was not updated. Back up again to confirm."
                                else "Backup finished, but your privacy choices or Google account changed. Review again.")
                        }
                    }
                    is CloudActionResult.Snapshots -> {
                        // A late Drive response must not display snapshots from
                        // an old account or privacy revision after reconnection.
                        if (!CloudAccountSessionPolicy.same(
                                operationConsent, cloudStore.read().consentSnapshot()) ||
                            !CloudRecoverySessionPolicy.matches(recoverySession, cloudStore.read())) {
                            refreshCloud()
                            showMessage("Google account session changed. Preview again.")
                        } else {
                            showCloudSnapshotPicker(outcome.items, token,
                                forMerge = action == "merge",
                                expectedSession = recoverySession)
                        }
                    }
                }
            }.onFailure { error ->
                // A failed old request must never overwrite the newly connected
                // account's backup status. Tokens, account IDs and exception
                // messages are excluded from user-visible diagnostics.
                val message = when {
                    error is DriveHttpFailure ->
                        DriveHttpFailurePolicy.message(error.statusCode)
                    error.message == "No NightWave cloud backup found" ->
                        "No cloud backups found in this Google account"
                    else -> "Drive operation unsuccessful; local library unchanged"
                }
                var failureAccessRevision = accessRevision
                if (error is DriveHttpFailure) {
                    val current = cloudStore.read()
                    val failureRevision = cloudStore.recordAccessIfCurrent(operationConsent, CloudAccessPolicy.afterFailure(
                        current.accessState, CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, error.statusCode),
                        expectedAccessRevision = accessRevision)
                    if (failureRevision == null) {
                        refreshCloud()
                        showMessage("A newer Drive access check is available. Start the action again.")
                        return@onFailure
                    }
                    failureAccessRevision = failureRevision
                    if (DriveHttpFailurePolicy.needsAccountAttention(error.statusCode))
                        cloudFailureStore.record(CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK, error.statusCode)
                }
                if (cloudStore.statusIfCurrent(operationConsent, message, failureAccessRevision)) {
                    refreshCloud()
                    showMessage(message)
                } else {
                    refreshCloud()
                    showMessage("Google account or backup choices changed. Start the action again.")
                }
            }
        }
    }

    private fun showCloudSnapshotPicker(
        snapshots: List<DriveSnapshot>, token: String,
        forMerge: Boolean, expectedSession: CloudRecoverySession
    ) {
        val options = snapshots.take(50)
        val labels = options.mapIndexed { index, snapshot ->
            val timestamp = snapshot.name.removePrefix("nightwave-cloud-v2-")
                .substringBefore("-").toLongOrNull()
            val whenSaved = timestamp?.let {
                java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
            } ?: "Unknown date"
            val size = snapshot.sizeBytes?.let {
                " · " + (it / 1024L).coerceAtLeast(1L) + " KB"
            }.orEmpty()
            "${index + 1}. $whenSaved$size"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(if (forMerge) "Compare backups (${options.size} of ${snapshots.size})"
                else "Choose a cloud backup (${options.size} of ${snapshots.size})")
            .setItems(labels) { _, which ->
                if (!CloudRecoverySessionPolicy.mayApply(
                        expectedSession, cloudStore.read(), cloudBusy)) {
                    showMessage("Google account session changed. Reconnect and preview again.")
                    return@setItems
                }
                val chosen = StudioUiStateSafetyPolicy.itemAt(options, which)
                if (chosen == null) {
                    showMessage("Drive snapshot list changed. Preview again.")
                    return@setItems
                }
                cloudPhase = CloudOperationPhase.OPENING_BACKUP
                cloudBusy = true
                lifecycleScope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            check(CloudRecoverySessionPolicy.matches(
                                expectedSession, cloudStore.read())) {
                                "Google account session changed during cloud preview"
                            }
                            val remote = CloudBackupCodec.decode(
                                NightWaveDriveClient(token).download(chosen.id))
                            val localBytes = archiveManager.export()
                            val plan = if (forMerge) CloudLibraryMerger.preview(
                                localBytes, remote.archive) else null
                            val comparison = if (forMerge) null
                                else CloudRecoveryPolicy.inspect(localBytes, remote)
                            Triple(remote, plan, comparison)
                        }
                    }
                    cloudBusy = false
                    outcome.onSuccess { (remote, plan, comparison) ->
                        if (!CloudRecoverySessionPolicy.matches(
                                expectedSession, cloudStore.read())) {
                            showMessage("Google account session changed; preview again.")
                        } else if (plan != null) showCloudMergePreview(plan, expectedSession)
                        else if (comparison != null) {
                            showCloudRestorePreview(remote, comparison, expectedSession)
                        }
                    }
                        .onFailure {
                            cloudStore.statusIfCurrent(expectedSession.consent,
                                "Selected backup unavailable or failed validation", expectedSession.accessRevision)
                            refreshCloud()
                            showMessage("Selected backup could not be opened; local data unchanged.")
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Preview changes before confirmation. Applying checks account and local
     * checkpoint; neither a late playlist edit nor a different Google account
     * may be overwritten by the merge.
     */
    private fun showCloudMergePreview(
        plan: CloudMergePlan, expectedSession: CloudRecoverySession
    ) {
        val conflicts = if (plan.conflictingPlaylistNames.isEmpty()) ""
            else "\n\nPlaylist name conflicts: " +
                plan.conflictingPlaylistNames.take(5).joinToString(", ") +
                ". Separate (Drive) copies will be added."
        val message = "Cloud comparison: ${plan.addedFavoriteCount} additional liked tracks, " +
            "${plan.addedPlaylistCount} additional playlists, " +
            "${plan.newTrackReferences} imported playlist entries." +
            conflicts + "\n\nLocal profile, playlists and listening history are preserved. " +
            "This operation does not upload or delete cloud files."
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (plan.hasChanges) "Review library merge" else "Already up to date")
            .setMessage(message)
            .setNegativeButton("Close", null)
        if (plan.hasChanges) {
            dialog.setPositiveButton("Merge into this device") { _, _ ->
                if (!CloudRecoverySessionPolicy.mayApply(
                        expectedSession, cloudStore.read(), cloudBusy)) {
                    showMessage("Google account session changed or busy; compare again.")
                    return@setPositiveButton
                }
                cloudPhase = CloudOperationPhase.MERGING
                cloudBusy = true
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            cloudStore.withRecoverySession(expectedSession) {
                                archiveManager.mergeIfUnchanged(plan)
                            }
                        }.getOrDefault(CloudMergeApplyResult.FAILED)
                    }
                    cloudBusy = false
                    when (result) {
                        null -> showMessage("Drive access changed. Compare again before merging.")
                        CloudMergeApplyResult.APPLIED -> {
                            profile = profiles.load()
                            refresh()
                            showMessage("Library merged. Cloud backups are unchanged.")
                        }
                        CloudMergeApplyResult.STALE_PREVIEW ->
                            showMessage("Your library changed. Compare again before merging.")
                        CloudMergeApplyResult.NO_CHANGES ->
                            showMessage("Library already contains those tracks.")
                        CloudMergeApplyResult.FAILED ->
                            showMessage("Merge could not finish. Review your library before retrying.")
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showCloudRestorePreview(
        preview: CloudBackupPreview, report: CloudRecoveryReport,
        expectedSession: CloudRecoverySession
    ) {
        val message = buildString {
            append("IN GOOGLE DRIVE\n")
            append("${report.cloudLiked} liked songs · ${report.cloudPlaylistCount} playlists")
            append(" · ${report.cloudPlaylistTracks} playlist entries\n\n")
            append("CURRENTLY ON THIS PHONE\n")
            append("${report.localLiked} liked songs · ${report.localPlaylistCount} playlists\n\n")
            append("IF YOU RESTORE THIS BACKUP\n")
            append("${report.localLikedNotInCloud} currently liked songs would be removed.\n")
            append("${report.localPlaylistsNotInCloud} local playlists would be removed.\n")
            append("${report.localPlaylistsWithChangedContent} playlists would be replaced or renamed.\n")
            if (report.profileWouldChange) append("The local profile appearance/name would change.\n")
            if (report.optionalHistoryCount > 0) {
                append("\nOptional: ${report.optionalHistoryCount} recent listening entries can be restored.")
            } else append("\nCurrent listening history remains unless separately chosen.")
            if (preview.preferences != null) {
                append("\n\nOptional playback preferences are available (not restored automatically).")
            }
            if (preview.includesLyrics) {
                append("\nOptional imported LRC entries: ${preview.lyrics.size} (audio not included).")
            } else {
                append("\nImported LRC lyrics are not included in this cloud backup.")
            }
            append("\nOffline audio files are never included.")
            append("\nA recovery checkpoint is kept before replacement. Export locally if you need both versions.")
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Review cloud restore")
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore library") { _, _ ->
                restoreCloudAfterConfirmation(preview, report, false, expectedSession)
            }
        if (preview.includesHistory && preview.history.isNotEmpty()) {
            dialog.setNeutralButton("Restore + history") { _, _ ->
                restoreCloudAfterConfirmation(preview, report, true, expectedSession)
            }
        }
        dialog.show()
    }

    private fun restoreCloudAfterConfirmation(
        preview: CloudBackupPreview, report: CloudRecoveryReport,
        withHistory: Boolean, expectedSession: CloudRecoverySession
    ) {
        if (!CloudRecoverySessionPolicy.mayApply(
                expectedSession, cloudStore.read(), cloudBusy)) {
            showMessage("Google account session changed or backup is busy; review again.")
            return
        }
        cloudPhase = CloudOperationPhase.RESTORING
        cloudBusy = true
        val history = if (withHistory) preview.history else null
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    cloudStore.withRecoverySession(expectedSession) {
                        archiveManager.restoreCloudIfUnchanged(
                            report.expectedLocalBackup, preview.archive, history)
                    }
                }.getOrDefault(CloudRestoreApplyResult.FAILED)
            }
            cloudBusy = false
            when (outcome) {
                null -> showMessage("Drive access changed. Review the backup again before restoring.")
                CloudRestoreApplyResult.APPLIED -> {
                    profile = profiles.load()
                    refresh()
                    showMessage("Cloud library restored. Automatic backup remains unchanged.")
                    if (preview.preferences != null || preview.lyrics.isNotEmpty()) {
                        showCloudOptionalRestorePrompt(preview, expectedSession)
                    }
                }
                CloudRestoreApplyResult.STALE_PREVIEW ->
                    showMessage("Your library changed. Review this backup again before restoring.")
                CloudRestoreApplyResult.FAILED ->
                    showMessage("Restore could not finish. Your previous library is preserved.")
            }
        }
    }

    private fun showCloudOptionalRestorePrompt(
        backup: CloudBackupPreview, expectedSession: CloudRecoverySession
    ) {
        val expectedProfile = profiles.load()
        val options = buildString {
            append("This cloud backup also contains ")
            append(if (backup.preferences == null) "no preferences"
                else "selected playback preferences")
            append(" and ${backup.lyrics.size} imported LRC entries.\n\n")
            append("Applying preferences changes only motion, audio quality, crossfade, " +
                "lock-screen artwork and browsing screen-awake. Account, local identity, " +
                "and privacy choices are preserved.\n\n")
            append("Imported lyrics are added ONLY when a matching offline song ID exists " +
                "and has no lyrics. Audio files and unmatched lyrics are not restored.")
        }
        AlertDialog.Builder(this)
            .setTitle("Restore optional backup data?")
            .setMessage(options)
            .setNegativeButton("Keep local options", null)
            .setPositiveButton("Apply optional data") { _, _ ->
                if (!CloudRecoverySessionPolicy.mayApply(
                        expectedSession, cloudStore.read(), cloudBusy)) {
                    showMessage("Google account session changed or busy. Open backup again.")
                    return@setPositiveButton
                }
                cloudPhase = CloudOperationPhase.APPLYING_OPTIONS
                cloudBusy = true
                lifecycleScope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            checkNotNull(cloudStore.withRecoverySession(expectedSession) {
                                val changedPreferences = backup.preferences?.let { restored ->
                                    check(profiles.saveIfUnchanged(expectedProfile, restored.applyTo(expectedProfile))) {
                                        "Playback preferences could not be saved"
                                    }
                                    true
                                } ?: false
                                val restoredLyrics = if (backup.includesLyrics)
                                    lyricsStore.restoreCloudMissing(backup.lyrics) else 0
                                Pair(restoredLyrics, changedPreferences)
                            })
                        }
                    }
                    cloudBusy = false
                    outcome.onSuccess { (addedLyrics, changedPreferences) ->
                        profile = profiles.load()
                        lyricsRevision++
                        refresh()
                        showMessage("Optional restore: ${addedLyrics} LRC entries added" +
                            if (changedPreferences) "; playback preferences applied."
                            else "; phone preferences unchanged.")
                    }.onFailure {
                        showMessage("Optional data could not fully restore. " +
                            "Existing lyrics were not overwritten; review local settings.")
                    }
                }
            }
            .show()
    }

    private fun disconnectCloud() {
        if (cloudBusy) {
            showMessage("Wait for the current backup operation to finish.")
            return
        }
        val saved = cloudStore.disconnect()
        runCatching { NightWaveCloudSchedule.stop(this) }
        refreshCloud()
        showMessage(if (saved) "Disconnected locally. Existing cloud snapshots were not deleted."
            else "Automatic backup stopped, but disconnection could not be saved. Please retry.")
    }

    private fun cancelNativeSearch() {
        nativeSearchGeneration++
        nativeSearchJob?.cancel()
        nativeSearchJob = null
        if (nativeSearchState.loading) nativeSearchState = nativeSearchState.copy(loading = false,
            error = "Search cancelled. Retry when you are ready.")
    }

    private fun nativeSearch(query: String, mode: SearchMode) {
        val clean = query.trim().take(160)
        when (val input = SearchInputResolver.resolve(clean)) {
            SearchInput.Blank -> return
            is SearchInput.VideoUrl -> {
                nativeSearchJob?.cancel()
                nativeSearchGeneration++
                nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                    hint = "Opening this public video in NightWave…")
                playback.play(VideoPreview("Loading track…", "YouTube", input.url, "", 0L))
                return
            }
            SearchInput.BadLink -> {
                nativeSearchJob?.cancel()
                nativeSearchGeneration++
                nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                    hint = "This link is not a supported public YouTube video. Check the URL.")
                return
            }
            is SearchInput.Keywords -> Unit
        }
        val request = ++nativeSearchGeneration
        nativeSearchJob?.cancel()
        // A retry for the same query should not erase successful public
        // results while the network is temporarily unavailable.
        val previous = nativeSearchState.takeIf {
            it.query.equals(clean, ignoreCase = true) && it.mode == mode
        }
        val recent = nativeGateway.cachedSearch(clean, mode)
        val carriedVideos = recent?.result?.videos ?: previous?.videos.orEmpty()
        val carriedProvider = recent?.result?.note ?: previous?.provider.orEmpty()
        // An explicit repeat/Refresh/Retry with visible results must contact the provider.
        val forceRefresh = previous?.videos?.isNotEmpty() == true
        nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode, loading = true,
            videos = carriedVideos, provider = carriedProvider,
            fromCache = recent != null || previous?.fromCache == true)
        nativeSearchJob = lifecycleScope.launch {
            var searchDetails = ""
            val result: Result<VideoSearchResults> = try {
                Result.success(PublicMusicSearchRunner.run { searchRequest ->
                    try {
                        nativeGateway.search(clean, mode, forceRefresh = forceRefresh) { phase ->
                            lifecycleScope.launch {
                                if (request == nativeSearchGeneration && nativeSearchState.loading &&
                                    !isFinishing && !isDestroyed) nativeSearchState = nativeSearchState.copy(phase = phase)
                            }
                        }
                    } finally { searchDetails = searchRequest.details() }
                })
            } catch (timeout: TimeoutCancellationException) {
                Result.failure(PublicSearchDeadlineException())
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                Result.failure(error)
            }
            if (request != nativeSearchGeneration || isFinishing || isDestroyed) return@launch
            result.fold(
                onSuccess = { response ->
                    nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                        videos = response.videos, provider = response.note,
                        fromCache = response.fromCache, details = searchDetails,
                        hint = SearchResultPresentationPolicy.emptyHint(response.videos))
                },
                onFailure = { failure ->
                    nativeSearchState = StudioSearchSnapshot(query = clean, mode = mode,
                        videos = carriedVideos, provider = carriedProvider,
                        fromCache = recent != null || previous?.fromCache == true, details = searchDetails,
                        error = if (failure is PublicSearchDeadlineException)
                            "Search took too long. Retry or open YouTube search."
                        else "Online providers could not be reached. Your local music is still available.")
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (::cloudStore.isInitialized) {
            cloudStatusSubscription?.close()
            cloudStatusSubscription = cloudStore.observe { cloudInfo = it }
        }
        if (::playback.isInitialized) playback.resumeUiUpdates()
    }

    override fun onStop() {
        cloudStatusSubscription?.close()
        cloudStatusSubscription = null
        offlineResumeReadJob?.cancel()
        offlineResumeReadGeneration++
        recentRefreshJob?.cancel()
        recentRefreshGeneration++
        nativeSearchJob?.cancel()
        nativeSearchGeneration++
        if (nativeSearchState.loading) {
            nativeSearchState = nativeSearchState.copy(loading = false,
                error = "Search was paused. Tap Find music to retry.")
        }
        if (::playback.isInitialized) playback.pauseUiUpdates()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nw128-applied-main", appliedMainNavigation)
        if (cloudBusy && CloudAuthorizationRecoveryPolicy.shouldRestore(
                cloudPendingAction, cloudAwaitingResolution)) {
            // Restore only the action, never the Google token or account.
            outState.putString(cloudActionSavedKey, cloudPendingAction)
            outState.putBoolean(cloudResolutionSavedKey, true)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        lyricsReviewDialog?.dismiss()
        lyricsReviewDialog = null
        cloudStatusSubscription?.close()
        cloudStatusSubscription = null
        cloudRequestGeneration++
        nativeSearchJob?.cancel()
        nativeSearchGeneration++
        youtubeDialog?.dismiss()
        youtubeDialog = null
        if (::playback.isInitialized) playback.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::library.isInitialized) refresh()
        if (::folderStore.isInitialized) folderSnapshot = folderStore.snapshot()
        if (::pinStore.isInitialized && ::library.isInitialized) {
            // Classic/library edits can rename playlists while this activity is paused.
            // Resynchronize optional pin metadata without writing library content.
            pinnedPlaylistNames = pinStore.snapshot(library.playlistNames())
        }
        if (::offlineSpeedStore.isInitialized) offlineSpeedPercent = offlineSpeedStore.read()
        if (::listeningLevelStore.isInitialized) listeningLevelPercent = listeningLevelStore.read()
        if (::crossfadeSettings.isInitialized) crossfadeSeconds = crossfadeSettings.read()
        if (::normalizationStore.isInitialized) normalizationEnabled = normalizationStore.enabled()
        refreshListeningMemory()
        if (::roomShadow.isInitialized && ::profileShadow.isInitialized &&
            ::verifiedReader.isInitialized) refreshShadows()
    }

    /** Reading small private aggregates never changes player or cloud state. */
    private fun refreshListeningMemory() {
        if (!::listeningMemoryStore.isInitialized) return
        val generation = ++memoryReadGeneration
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching { listeningMemoryStore.snapshot() }.getOrNull()
            }
            if (generation == memoryReadGeneration && snapshot != null) {
                memorySummary = snapshot
            }
        }
    }

    private fun setListeningMemory(enabled: Boolean) {
        if (!::listeningMemoryStore.isInitialized) return
        val result = listeningMemoryStore.setEnabled(enabled)
        if (!result) {
            showMessage("Could not change listening privacy settings.")
        }
        refreshListeningMemory()
    }

    private fun confirmEraseListeningMemory() {
        AlertDialog.Builder(this)
            .setTitle("Erase listening statistics?")
            .setMessage("Remove the local play-session statistics and switch tracking off. " +
                "This cannot be undone. Your songs, playlists and listening history stay untouched.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Erase & turn off") { _, _ ->
                if (listeningMemoryStore.eraseAndDisable()) {
                    refreshListeningMemory()
                    showMessage("Listening statistics erased and tracking disabled.")
                } else showMessage("Could not erase listening statistics.")
            }.show()
    }

    private fun saveCurrentToPlaylist() {
        val now = playback.state
        if (now.offline) {
            showMessage("Imported audio stays in your Offline library.")
            return
        }
        if (now.hasTrack) saveSongToPlaylist(VideoPreview(
            now.title.ifBlank { "YouTube video" }, now.artist, now.videoUrl,
            now.thumbnail, now.durationMs / 1000L
        ))
    }

    private fun saveSongToPlaylist(video: VideoPreview) {
        val names = library.playlistNames()
        val choices = names + "＋ Create playlist"
        AlertDialog.Builder(this).setTitle("Save to playlist")
            .setItems(choices.toTypedArray()) { _, option ->
                if (option !in choices.indices) {
                    showMessage("Playlist selection changed. Open Save again.")
                    return@setItems
                }
                if (option < names.size) {
                    val chosen = StudioUiStateSafetyPolicy.itemAt(names, option)
                    if (chosen == null || chosen !in library.playlistNames()) {
                        showMessage("Playlist changed. Open Save to playlist again.")
                    } else if (library.addToPlaylist(chosen, video)) refresh()
                    else showMessage("Already saved or the playlist is full.")
                } else promptPlaylistName { created ->
                    if (library.createPlaylist(created)) {
                        library.addToPlaylist(created, video)
                        refresh()
                    } else showMessage("Choose a different playlist name.")
                }
            }.setNegativeButton("Cancel", null).show()
    }

    /**
     * A verified private copy takes priority over online search playback.
     * Pass the full locally available sequence to PlaybackService so Next
     * advances while offline; unsaved results remain ordinary online tracks.
     * Never replace or rewrite the user's existing YouTube playlist.
     */
    private fun playSearchedOfflineFirst(
        video: VideoPreview, collection: List<VideoPreview>
    ) {
        val selection = SearchTrackOfflineIndex.offlineSelection(
            this, video.url, collection.map(VideoPreview::url))
        if (selection != null) {
            val started = runCatching {
                startService(Intent(this, PlaybackService::class.java)
                    .setAction(PlaybackService.ACTION_PLAY_OFFLINE)
                    .putExtra(PlaybackService.EXTRA_OFFLINE_ID, selection.selectedId)
                    .putStringArrayListExtra(PlaybackService.EXTRA_OFFLINE_IDS, selection.queueIds))
            }.isSuccess
            if (started) return
            showMessage("Saved track could not start. Trying online playback.")
        }
        playback.play(video, collection)
    }

    private fun playSavedFromSearch(video: VideoPreview): Boolean {
        val saved = SearchTrackOfflineIndex.readySong(this, video.url) ?: return false
        return runCatching {
            startService(Intent(this, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_PLAY_OFFLINE)
                .putExtra(PlaybackService.EXTRA_OFFLINE_ID, saved.id))
        }.isSuccess
    }

    private fun openOfflineSaving(video: VideoPreview) {
        val previous = SearchTrackOfflineIndex.readySong(this, video.url)
        if (previous != null) {
            AlertDialog.Builder(this)
                .setTitle("Ready offline")
                .setMessage("This recording is already verified in your private Offline Library.")
                .setPositiveButton("Play offline") { _, _ ->
                    if (!playSavedFromSearch(video))
                        showMessage("Could not open saved audio. Try Offline Library.")
                }
                .setNeutralButton("Open library") { _, _ ->
                    startActivity(Intent(this, OfflineLibraryActivity::class.java)
                        .putExtra(OfflineLibraryActivity.EXTRA_SEARCH_QUERY, previous.title))
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Save this track offline?")
            .setMessage("NightWave can attempt to save an accessible, audio-only copy " +
                "into its private library. This works only when you have permission to " +
                "store this recording and the source permits offline access. " +
                "Restricted or unavailable tracks cannot be saved; no DRM is bypassed. " +
                "Only a fully verified audio file is marked Ready Offline.")
            .setPositiveButton("I have permission - Save") { _, _ ->
                runCatching { SearchTrackSaveQueue.enqueue(this, video) }
                    .onSuccess { operation ->
                        operation.result.addListener({
                            if (isFinishing || isDestroyed) return@addListener
                            runCatching { operation.result.get() }
                                .onSuccess {
                                    startActivity(Intent(this, CatalogDownloadsActivity::class.java))
                                }
                                .onFailure {
                                    showMessage("Could not queue offline save. Try again.")
                                }
                        }, androidx.core.content.ContextCompat.getMainExecutor(this))
                        showMessage("Submitting audio-only save to background downloads...")
                    }
                    .onFailure {
                        showMessage("Cannot save this searched track: " +
                            (it.message?.take(85) ?: "Unsupported source"))
                    }
            }
            .setNeutralButton("Other audio sources") { _, _ ->
                showOfflineSources(video)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOfflineSources(video: VideoPreview) {
        AlertDialog.Builder(this).setTitle("Choose an offline source")
            .setItems(arrayOf("Import owned audio · Copy files from this device",
                "Browse supported catalog · Eligible licensed audio only",
                "Direct audio link · Authorized HTTPS files only",
                "Open Offline Library · Check saved copies")) { _, choice ->
                when (choice) {
                    0 -> startActivity(Intent(this, OfflineLibraryActivity::class.java)
                        .putExtra(OfflineLibraryActivity.EXTRA_IMPORT_OWNED, true))
                    1 -> startActivity(Intent(this, FreeAudioCatalogActivity::class.java)
                        .putExtra(FreeAudioCatalogActivity.EXTRA_QUERY, video.title.take(100))
                        .putExtra(FreeAudioCatalogActivity.EXTRA_ARTIST, video.author.take(100)))
                    2 -> startActivity(Intent(this, com.nightwave.player.offline.PermittedAudioDownloadActivity::class.java))
                    3 -> startActivity(Intent(this, OfflineLibraryActivity::class.java))
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun saveCurrentOffline() {
        val current = playback.state
        if (!current.hasTrack) return
        if (current.offline) {
            startActivity(Intent(this, OfflineLibraryActivity::class.java)
                .putExtra(OfflineLibraryActivity.EXTRA_SEARCH_QUERY, current.title.take(100)))
        } else {
            openOfflineSaving(VideoPreview(
                current.title.ifBlank { "Song" }, current.artist,
                current.videoUrl, current.thumbnail, current.durationMs / 1000L
            ))
        }
    }

    /** User-intent queue action; never silently mixes online and offline sessions. */
    private fun requestPublicQueueAction(video: VideoPreview, playNext: Boolean) {
        val current = playback.state
        if (current.offline) {
            showMessage("Online and offline queues are separate. Switch to online music first.")
            return
        }
        if (VideoLink.normalize(video.url) == null) {
            showMessage("This song has no supported public queue link.")
            return
        }
        if (playNext && current.videoUrl == video.url) {
            showMessage("This song is already playing.")
            return
        }
        if (!playNext && current.queue.any { it.url == video.url }) {
            showMessage("This song is already in your queue.")
            return
        }
        if (!playNext && current.queue.size >= 100) {
            showMessage("Your queue is full (100 songs).")
            return
        }
        if (playNext) playback.playNext(video) else playback.enqueue(video)
    }

    private fun showSongOptions(video: VideoPreview) {
        val likeText = if (library.isFavorite(video.url)) "Unlike song" else "Like song"
        // A damaged offline index must not crash the ordinary song menu.
        // The offline library retains its own explicit repair/recovery UI.
        val offlineReady = runCatching {
            SearchTrackOfflineIndex.readySong(this, video.url) != null
        }.getOrDefault(false)
        AlertDialog.Builder(this).setTitle(video.title)
            .setItems(arrayOf(likeText, "Save to playlist", "Play next",
                "Add to end of queue", "Play now",
                if (offlineReady) "Play saved offline" else "Save offline",
                "Share song link", "Copy song link")) { _, selected ->
                when (selected) {
                    0 -> { library.toggleFavorite(video); refresh() }
                    1 -> saveSongToPlaylist(video)
                    2 -> requestPublicQueueAction(video, playNext = true)
                    3 -> requestPublicQueueAction(video, playNext = false)
                    4 -> playSearchedOfflineFirst(video, listOf(video))
                    5 -> if (offlineReady) {
                        if (!playSavedFromSearch(video)) openOfflineSaving(video)
                    } else openOfflineSaving(video)
                    7 -> {
                        val publicUrl = VideoLink.normalize(video.url)
                        if (publicUrl == null) {
                            showMessage("This track has no shareable public link.")
                        } else {
                            runCatching {
                                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                                    "NightWave song", publicUrl))
                            }.onSuccess {
                                showMessage("Song link copied.")
                            }.onFailure { showMessage("Could not copy this song link.") }
                        }
                    }
                    6 -> {
                        val publicUrl = VideoLink.normalize(video.url)
                        if (publicUrl == null) {
                            showMessage("This track has no shareable public link.")
                        } else {
                            runCatching {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, publicUrl)
                                }
                                startActivity(Intent.createChooser(send, "Share song"))
                            }.onFailure { showMessage("Sharing is unavailable on this device.") }
                        }
                    }
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun showPlaylistSongOptions(name: String, song: VideoPreview) {
        if (name !in library.playlistNames() ||
            library.playlist(name).none { it.url == song.url }) {
            showSongOptions(song)
            return
        }
        AlertDialog.Builder(this).setTitle(song.title.take(110))
            .setItems(arrayOf("Play this playlist", "Play next",
                "Add to end of queue", "More song actions", "Remove from this playlist")) {
                    _, selected ->
                when (selected) {
                    0 -> playSearchedOfflineFirst(song, library.playlist(name))
                    1 -> requestPublicQueueAction(song, playNext = true)
                    2 -> requestPublicQueueAction(song, playNext = false)
                    3 -> showSongOptions(song)
                    4 -> AlertDialog.Builder(this)
                        .setTitle("Remove from ${name.take(55)}?")
                        .setMessage("Remove this entry from the playlist only? " +
                            "Liked songs, listening history and offline audio remain saved.")
                        .setNegativeButton("Keep", null)
                        .setPositiveButton("Remove") { _, _ ->
                            val before = library.playlist(name)
                            val updated = library.bulkRemoveFromPlaylist(
                                name, listOf(song.url), before.map { it.url })
                            if (updated.accepted && updated.changed) {
                                playlistUndo = PlaylistUndoState(name, before,
                                    updated.updated.songs(name),
                                    "Removed from ${name.take(40)}")
                                refresh()
                                showMessage("Removed from playlist · Undo in Library.")
                            } else showMessage("Playlist changed or couldn't be saved.")
                        }.show()
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun promptPlaylistName(initial: String = "", onSave: (String) -> Unit) {
        val name = EditText(this).apply {
            setSingleLine(true)
            hint = "Playlist name"
            setText(initial)
        }
        AlertDialog.Builder(this).setTitle(if (initial.isBlank()) "New playlist" else "Rename playlist")
            .setView(name)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> onSave(name.text.toString().trim()) }
            .show()
    }

    /**
     * Private app-only crash evidence is available directly from premium UI.
     * Show a preview first and require an explicit confirmation before opening
     * Android's share chooser. No telemetry is uploaded automatically.
     */
    private fun showCrashSummaryDialog() {
        // Share exactly the text displayed in the confirmation dialog.
        // Keep the newest exception evidence, plus the OS exit history at end.
        val report = NightWaveCrashRecorder.report(this).take(5000) + "\n\n" +
            NightWaveExitHistory.report(this)
        val contextText = "Includes app-only exception types/stack frames and recent " +
            "Android process-exit reasons. No song titles, URLs, file paths, " +
            "full system traces, account details or device identifiers are collected."
        AlertDialog.Builder(this)
            .setTitle("NightWave crash diagnostics")
            .setMessage(contextText + "\n\n" + report)
            .setNegativeButton("Close", null)
            .setPositiveButton("Share summary") { _, _ ->
                try {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                    }, "Share NightWave crash diagnostics"))
                } catch (error: Exception) {
                    showMessage("No sharing app was available. The report remains on this device.")
                }
            }.show()
    }

    /** Explicit sharesheet only; never shares imported audio files or private data. */
    /** File export uses the existing 30-link public-only format, never audio bytes. */
    private fun exportPlaylistText(name: String, tracks: List<VideoPreview>) {
        val prepared = PlaylistDocumentPolicy.prepare(name, tracks)
        if (prepared == null) {
            showMessage("No eligible public links to export from this playlist.")
            return
        }
        playlistDocumentRetained.pendingExport = prepared
        runCatching { exportPlaylistTextFile.launch(prepared.fileName) }
            .onFailure {
                playlistDocumentRetained.pendingExport = null
                showMessage("Could not open the document picker. Your playlists are unchanged.")
            }
    }

    private fun sharePlaylist(name: String, tracks: List<VideoPreview>) {
        val text = PlaylistShareFormatter.format(name, tracks)
        if (text == null) {
            showMessage("No shareable public links in this playlist.")
            return
        }
        runCatching {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }, "Share NightWave playlist"))
        }.onFailure { showMessage("No sharing app was available.") }
    }


    /** Clipboard is only accessed if the user explicitly pastes into this field. */
    private fun choosePlaylistImport() {
        AlertDialog.Builder(this)
            .setTitle("Import NightWave playlist")
            .setMessage(
                "Choose a NightWave .txt playlist or paste shared text. " +
                "Only public song links are imported after preview and confirmation; " +
                "no local audio or existing playlists are overwritten."
            )
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Paste text") { _, _ -> promptSharedPlaylistPaste() }
            .setPositiveButton("Choose .txt") { _, _ ->
                runCatching { importPlaylistTextFile.launch(arrayOf("text/plain")) }
                    .onFailure { showMessage("The document picker is unavailable.") }
            }.show()
    }

    private fun promptSharedPlaylistPaste() {
        val entry = EditText(this).apply {
            hint = "Paste the NightWave playlist share text"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
            maxLines = 7
        }
        AlertDialog.Builder(this)
            .setTitle("Import a shared playlist")
            .setMessage("Paste text shared from NightWave. Review before saving.")
            .setView(entry)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Review") { _, _ ->
                val parsed = PlaylistShareParser.parse(entry.text.toString())
                if (parsed == null) {
                    showMessage("Incomplete or unsupported NightWave playlist share.")
                } else showSharedPlaylistPreview(parsed)
            }.show()
    }

    /** Imported text is untrusted. No file, stream or account data is restored. */
    private fun showSharedPlaylistPreview(incoming: SharedPlaylist) {
        val preview = incoming.tracks.take(3).joinToString("\n") { "• " + it.title }
        val more = if (incoming.tracks.size > 3) "\n…" else ""
        AlertDialog.Builder(this)
            .setTitle("Import playlist?")
            .setMessage(
                incoming.name + "\n\n" + incoming.tracks.size +
                    " public song links:\n" + preview + more +
                    "\n\nCreate a new playlist without replacing existing songs, " +
                    "favorites or listening history. Duplicate names receive a (Shared) suffix. " +
                    "Track titles and creators are supplied by the sender and not verified; " +
                    "playback still depends on the public source."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Import") { _, _ ->
                val beforeNames = library.playlistNames()
                val outcome = library.importSharedPlaylist(incoming.name, incoming.tracks)
                when {
                    !outcome.accepted ->
                        showMessage("Import failed; library unchanged. Check collection limits.")
                    !outcome.changed -> showMessage("This playlist is already saved.")
                    else -> {
                        val savedName = StudioUiStateSafetyPolicy.createdPlaylistName(
                            beforeNames, outcome.updated.playlists.map { it.first })
                        refresh()
                        showMessage(if (savedName != null) "Imported playlist: $savedName"
                            else "Playlist imported. Check Library for the new collection.")
                    }
                }
            }.show()
    }

    /** Show only user-imported offline lyrics. Read baseline before editing. */
    private fun openLyricsEditor() {
        val chosen = playback.state.videoUrl
        val id = LyricsTrackBindingPolicy.storageId(chosen) ?: return
        if (loadedLyrics?.first != id) {
            showMessage("Import your own LRC for this selected source before editing.")
            return
        }
        val requestId = ++lyricsEditorEpoch
        val expectedRevision = lyricsRevision
        val selectedTitle = playback.state.title
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching {
                    val title = boundLyrics.title(id, selectedTitle)
                    val imported = requireNotNull(boundLyrics.read(id)) {
                        "No imported LRC is available for this song"
                    }
                    Triple(title, LyricsEditorPolicy.begin(imported),
                        boundLyrics.fingerprint(id))
                }
            }
            if (lyricsEditorEpoch != requestId ||
                lyricsRevision != expectedRevision ||
                playback.state.videoUrl != chosen || isFinishing || isDestroyed) return@launch
            snapshot.onSuccess { (title, original, fingerprint) ->
                lyricsEditorSession = LyricsEditorSession(
                    requestId = requestId, songId = id, songTitle = title,
                    expectedFingerprint = fingerprint,
                    baseline = original, draft = original
                )
            }.onFailure {
                showMessage("Lyrics editor could not open. Imported LRC remains unchanged.")
            }
        }
    }

    private fun onLyricsEditorDraft(edited: LyricsEditorDraft) {
        val editor = lyricsEditorSession ?: return
        if (editor.saving || lyricsEditorEpoch != editor.requestId ||
            playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(editor.songId) ||
            !LyricsEditorPolicy.changed(editor.draft, edited)) return
        val undo = (editor.undo + editor.draft).takeLast(LyricsEditorPolicy.MAX_UNDO)
        lyricsEditorSession = editor.copy(draft = edited, undo = undo)
    }

    private fun undoLyricsEditorDraft() {
        val editor = lyricsEditorSession ?: return
        if (editor.saving || lyricsEditorEpoch != editor.requestId ||
            playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(editor.songId) ||
            editor.undo.isEmpty()) return
        lyricsEditorSession = editor.copy(
            draft = editor.undo.last(), undo = editor.undo.dropLast(1))
    }

    private fun discardLyricsEditorDraft() {
        val editor = lyricsEditorSession ?: return
        if (editor.saving) return
        lyricsEditorEpoch++
        lyricsEditorSession = null
    }

    /** Export the current validated draft; exporting never saves it to NightWave. */
    private fun exportLyricsEditorDraft() {
        val editor = lyricsEditorSession ?: return
        if (editor.saving || lyricsEditorEpoch != editor.requestId ||
            playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(editor.songId)) return
        val prepared = runCatching {
            LyricsExportPolicy.prepare(editor.songTitle, editor.baseline, editor.draft)
        }.getOrElse {
            showMessage("Draft export rejected: correct invalid lines or timestamps.")
            return
        }
        pendingLyricsExport = PendingLyricsExport(editor.requestId, editor.songId, prepared)
        runCatching {
            exportLyricsFile.launch(prepared.suggestedName)
        }.onFailure {
            pendingLyricsExport = null
            showMessage("Could not open the document picker. Your lyrics are unchanged.")
        }
    }

    /** Actual private LRC write only happens after the explicit Save tap. */
    private fun saveLyricsEditorDraft() {
        val editor = lyricsEditorSession ?: return
        if (editor.saving || lyricsEditorEpoch != editor.requestId ||
            playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(editor.songId) ||
            !LyricsEditorPolicy.changed(editor.baseline, editor.draft)) return
        val output = runCatching { LyricsEditorPolicy.serialize(editor.draft) }
            .getOrElse {
                showMessage("Draft contains invalid lyrics or exceeds the LRC limits.")
                return
            }
        lyricsEditorSession = editor.copy(saving = true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    check(editor.requestId == lyricsEditorEpoch &&
                        playback.state.videoUrl == LyricsTrackBindingPolicy.mediaId(editor.songId)) {
                        "Song changed before LRC could be saved"
                    }
                    boundLyrics.saveReviewed(
                        editor.songId, output, editor.expectedFingerprint) {
                            !isDestroyed && editor.requestId == lyricsEditorEpoch &&
                                LyricsTrackBindingPolicy.matches(editor.songId, playback.state.videoUrl)
                        }
                }
            }
            if (editor.requestId != lyricsEditorEpoch ||
                playback.state.videoUrl != LyricsTrackBindingPolicy.mediaId(editor.songId)) return@launch
            result.onSuccess { accepted ->
                if (accepted) {
                    lyricsEditorEpoch++
                    lyricsEditorSession = null
                    lyricsRevision++
                    showMessage("Edited lyrics saved to this selected source.")
                } else {
                    lyricsEditorSession = editor.copy(saving = false)
                    showMessage("Saved lyrics changed since the editor opened. " +
                        "Draft preserved; reload before saving.")
                }
            }.onFailure {
                lyricsEditorSession = editor.copy(saving = false)
                showMessage("Lyrics could not be saved. Your original LRC is intact.")
            }
        }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /** Folder changes are metadata-only; no playlist or song contents are rewritten. */
    private fun promptFolderName(initial: String = "", save: (String) -> Unit) {
        val input = EditText(this).apply {
            setSingleLine(true)
            hint = "Folder name"
            setText(initial)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(if (initial.isBlank()) "Create folder" else "Rename folder")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ -> save(input.text.toString().trim()) }
            .show()
    }

    private fun refreshFolders() {
        folderSnapshot = folderStore.snapshot()
        libraryRevision++
    }

    private fun choosePlaylistFolder(name: String) {
        val current = library.playlistNames()
        if (name !in current) {
            showMessage("That playlist is no longer available.")
            return
        }
        val snapshot = folderStore.snapshot()
        val choices = listOf("No folder") + snapshot.folders
        AlertDialog.Builder(this).setTitle("Move “$name” to folder")
            .setSingleChoiceItems(choices.toTypedArray(),
                snapshot.folderOf(name)?.let { choices.indexOf(it) } ?: 0) { dialog, chosen ->
                if (chosen !in choices.indices) {
                    showMessage("Folder selection changed. Open folders again.")
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }
                val folder = if (chosen == 0) null
                    else StudioUiStateSafetyPolicy.itemAt(choices, chosen)
                if (folderStore.assign(name, folder, current)) {
                    refreshFolders()
                    showMessage(if (folder == null) "Moved out of folder." else "Added to $folder.")
                } else showMessage("Folder change wasn't saved.")
                dialog.dismiss()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun managePlaylistFolders() {
        val snapshot = folderStore.snapshot()
        val choices = listOf("＋ New folder", "Move a playlist") + snapshot.folders
        AlertDialog.Builder(this).setTitle("Playlist folders")
            .setItems(choices.toTypedArray()) { _, index ->
                when (index) {
                    0 -> promptFolderName { newName ->
                        if (folderStore.create(newName)) {
                            refreshFolders()
                            showMessage("Folder created.")
                        } else showMessage("Invalid or duplicate folder name.")
                    }
                    1 -> {
                        val playlists = library.playlistNames()
                        if (playlists.isEmpty()) showMessage("Create a playlist first.")
                        else AlertDialog.Builder(this).setTitle("Choose playlist")
                            .setItems(playlists.toTypedArray()) { _, selected ->
                                val chosen = StudioUiStateSafetyPolicy.itemAt(playlists, selected)
                                if (chosen != null) choosePlaylistFolder(chosen)
                                else showMessage("Playlist list changed. Open folders again.")
                            }.setNegativeButton("Cancel", null).show()
                    }
                    else -> {
                        val name = StudioUiStateSafetyPolicy.itemAt(choices, index)
                            ?: return@setItems
                        AlertDialog.Builder(this).setTitle(name)
                            .setItems(arrayOf("Rename folder", "Delete folder")) { _, action ->
                                if (action == 0) promptFolderName(name) { updated ->
                                    if (folderStore.rename(name, updated)) refreshFolders()
                                    else showMessage("That folder name is unavailable.")
                                } else AlertDialog.Builder(this)
                                    .setTitle("Delete folder “$name”?")
                                    .setMessage("Playlists and songs remain unchanged. Only folder grouping is removed.")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Delete folder") { _, _ ->
                                        if (folderStore.delete(name)) refreshFolders()
                                        else showMessage("Folder couldn't be deleted.")
                                    }.show()
                            }.setNegativeButton("Cancel", null).show()
                    }
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun manageLibrary() {
        val names = library.playlistNames()
        val choices = arrayOf("＋ Create playlist", *names.toTypedArray())
        AlertDialog.Builder(this).setTitle("Manage your playlists")
            .setItems(choices) { _, selected ->
                if (selected == 0) {
                    promptPlaylistName { name ->
                        if (library.createPlaylist(name)) refresh()
                        else showMessage("That playlist name is unavailable.")
                    }
                } else {
                    val name = names[selected - 1]
                    AlertDialog.Builder(this).setTitle(name)
                        .setItems(arrayOf("Rename", "Delete")) { _, action ->
                            if (action == 0) promptPlaylistName(name) { updated ->
                                if (library.renamePlaylist(name, updated)) {
                                    folderStore.playlistRenamed(name, updated)
                                    pinStore.playlistRenamed(name, updated)
                                    pinnedPlaylistNames = pinStore.snapshot(library.playlistNames())
                                    refresh()
                                } else showMessage("That playlist name is unavailable.")
                            } else AlertDialog.Builder(this)
                                .setTitle("Delete " + name + "?")
                                .setMessage("Songs remain in your favorites and listening history.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Delete") { _, _ ->
                                    if (library.deletePlaylist(name)) {
                                        folderStore.playlistDeleted(name)
                                        pinStore.playlistDeleted(name)
                                        pinnedPlaylistNames = pinStore.snapshot(library.playlistNames())
                                        refresh()
                                    } else showMessage("Playlist could not be deleted.")
                                }.show()
                        }.show()
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun confirmRemoveRecent(song: VideoPreview) {
        AlertDialog.Builder(this)
            .setTitle("Remove from Recently Played?")
            .setMessage(song.title.take(90) + "\n\nOnly the listening-history entry is removed. " +
                "Liked songs, playlists and saved offline audio stay untouched.")
            .setNegativeButton("Keep", null)
            .setPositiveButton("Remove") { _, _ ->
                if (library.removeRecent(song.url)) {
                    refresh()
                    showMessage("Removed from history. Undo last edit is available in Profile.")
                } else showMessage("History unchanged; try again.")
            }.show()
    }

    private fun confirmClearRecent() {
        val count = library.recent().size
        if (count == 0) {
            showMessage("Recently Played is already empty.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Clear Recently Played?")
            .setMessage("Remove $count history entries? Liked songs, playlists and downloaded " +
                "audio will not be deleted. Undo the last library edit from Profile.")
            .setNegativeButton("Keep", null)
            .setPositiveButton("Clear history") { _, _ ->
                if (library.clearRecent()) {
                    refresh()
                    showMessage("History cleared. Your saved music is unchanged.")
                } else showMessage("History could not be cleared. Your music is unchanged.")
            }.show()
    }

    private fun confirmLastWriteRecovery() {
        AlertDialog.Builder(this)
            .setTitle("Undo the last library edit?")
            .setMessage(
                "This restores the state immediately before the most recent saved library change. " +
                "It might be a recent listening-history update, favorite, or playlist edit. " +
                "It will only run if none of the saved library keys have changed since that write. " +
                "For long-term recovery, export a backup first."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Undo last edit") { _, _ ->
                val restored = LegacyLibraryTransaction.recoverLastWriteIfUnchanged(this)
                if (restored) {
                    disableRoomPreview()
                    refresh()
                    showMessage("Last library edit undone.")
                } else {
                    showMessage("Recovery not available or library has changed. Nothing replaced.")
                }
            }.show()
    }

    private fun refresh() {
        // A legacy edit invalidates any previously displayed Room snapshot.
        roomPreviewActive = false
        roomPreviewPlaylists = emptyMap()
        favorites = library.favorites()
        recentlyPlayed = library.recent()
        names = library.playlistNames()
        folderSnapshot = folderStore.snapshot()
        libraryRevision++
        refreshOfflineResume()
        // Do not enqueue a complete Room rewrite for every player/queue update.
        // onResume and the explicit "Recheck" action handle shadow verification.
    }

    /**
     * Heavy offline-file validation can hash an entire recording. Run it on
     * Dispatchers.IO and refuse stale results after another refresh or Stop.
     * Never mutate the offline index just to render a resume shortcut.
     */
    private fun refreshOfflineResume() {
        val token = ++offlineResumeReadGeneration
        offlineResumeReadJob?.cancel()
        offlineResumeReadJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val checkpoint = OfflineSessionStore(applicationContext).read()
                    val lib = OfflineAudioLibrary(applicationContext)
                    val song = checkpoint?.selectedId?.let(lib::find)
                        ?.takeIf { lib.fileFor(it) != null }
                    song?.title to if (song != null)
                        (checkpoint?.positionMs ?: 0L).coerceAtLeast(0L) else 0L
                }.getOrDefault(null to 0L)
            }
            if (!LibraryUiRefreshPolicy.accepts(
                    token, offlineResumeReadGeneration, isFinishing, isDestroyed))
                return@launch
            offlineResumeTitle = result.first
            offlineResumePositionMs = result.second
        }
    }

    /** Only the real listening-history index changes when service plays a new song. */
    private fun refreshRecentForTrackChange() {
        val token = ++recentRefreshGeneration
        recentRefreshJob?.cancel()
        recentRefreshJob = lifecycleScope.launch {
            val updated = withContext(Dispatchers.IO) {
                runCatching { library.recent() }.getOrNull()
            } ?: return@launch
            if (!LibraryUiRefreshPolicy.accepts(
                    token, recentRefreshGeneration, isFinishing, isDestroyed))
                return@launch
            if (recentlyPlayed != updated) {
                recentlyPlayed = updated
                libraryRevision++
            }
        }
    }

    /**
     * A secondary, read-only-vs-legacy sync. On any failure the old library
     * remains unchanged and the UI can still play music.
     */
    private fun enableVerifiedRoomPreview() {
        roomPreviewRequested = true
        roomReadStatus = "Validating every track before reading Room…"
        refreshShadows()
    }

    private fun disableRoomPreview() {
        roomPreviewRequested = false
        roomPreviewActive = false
        roomPreviewPlaylists = emptyMap()
        favorites = library.favorites()
        recentlyPlayed = library.recent()
        names = library.playlistNames()
        roomReadStatus = "Legacy library reads are active"
    }

    private fun refreshShadows() {
        if (!::roomShadow.isInitialized || !::profileShadow.isInitialized ||
            !::verifiedReader.isInitialized) {
            roomPreviewRequested = false
            roomReadStatus = "Storage preview unavailable; NightWave is using your original library."
            return
        }
        if (shadowRunning) {
            shadowRequested = true
            return
        }
        shadowRunning = true
        lifecycleScope.launch {
            try {
                do {
                    shadowRequested = false
                    shadowStatus = "Checking local storage copy…"
                    try {
                        val libraryReport = roomShadow.reconcile()
                        profileShadow.reconcile()
                        val check = verifiedReader.inspect()
                        shadowStatus = "Room/DataStore copy · " +
                            libraryReport.favorites + " likes · " +
                            libraryReport.playlists + " playlists · " +
                            libraryReport.songs + " unique songs"
                        roomReadStatus = if (check.canUseRoom)
                            "Exact Room parity verified against the current legacy library"
                        else "Legacy fallback · " + check.parity.blocker.name
                        if (roomPreviewRequested && check.canUseRoom) {
                            val snapshot = check.snapshot!!
                            // Compose previews can read the validated Room snapshot.
                            roomPreviewPlaylists = snapshot.playlists.toMap()
                            favorites = snapshot.favorites
                            recentlyPlayed = snapshot.recent
                            names = snapshot.playlistNames()
                            roomPreviewActive = true
                            roomReadStatus = "Room read preview active for this session"
                        } else if (roomPreviewActive || roomPreviewRequested) {
                            roomPreviewActive = false
                            roomPreviewPlaylists = emptyMap()
                            favorites = library.favorites()
                            recentlyPlayed = library.recent()
                            names = library.playlistNames()
                        }
                    } catch (error: kotlinx.coroutines.CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        roomPreviewActive = false
                        roomPreviewPlaylists = emptyMap()
                        favorites = library.favorites()
                        recentlyPlayed = library.recent()
                        names = library.playlistNames()
                        roomReadStatus = "Legacy fallback · migration or validation failed"
                        shadowStatus = "Storage preview needs attention: " +
                            (error.message ?: "Could not migrate legacy data").take(150) +
                            ". Original library remains in use."
                    }
                } while (shadowRequested)
            } finally {
                shadowRunning = false
            }
        }
    }
}

private const val LIKED_COLLECTION = "nightwave://local-liked"
private const val RECENT_COLLECTION = "nightwave://local-recent"
private enum class StudioTab(val caption: String) {
    HOME("Home"), SEARCH("Search"), LIBRARY("Library"), PROFILE("You")
}

@Composable
private fun StudioRoot(
    profile: ProfileSettings,
    favorites: List<VideoPreview>,
    recent: List<VideoPreview>,
    libraryRevision: Int,
    playlistNames: List<String>,
    folderSnapshot: PlaylistFolderSnapshot,
    pinnedPlaylistNames: List<String>,
    onTogglePinPlaylist: (String) -> Unit,
    getPlaylist: (String) -> List<VideoPreview>,
    onSaveProfile: (ProfileSettings) -> Unit,
    onExportGuest: () -> Unit,
    onImportGuest: () -> Unit,
    onRecoverLastChange: () -> Unit,
    cloud: CloudBackupStatus,
    cloudBusy: Boolean,
    cloudPhase: CloudOperationPhase = CloudOperationPhase.IDLE,
    onCloudConnect: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudPreviewRestore: () -> Unit,
    onCloudCompare: () -> Unit,
    onCloudDisconnect: () -> Unit,
    onCloudHistory: (Boolean) -> Unit,
    onCloudLyrics: (Boolean) -> Unit,
    onCloudPreferences: (Boolean) -> Unit,
    onCloudWifi: (Boolean) -> Unit,
    storageStatus: String,
    onCheckStorage: () -> Unit,
    roomReadStatus: String,
    roomPreviewActive: Boolean,
    onEnableRoomPreview: () -> Unit,
    onUseLegacyReads: () -> Unit,
    playerState: StudioPlayerState,
    playerActions: StudioPlaybackBridge,
    sleepRemainingMs: Long,
    onSleepTimer: (Int?) -> Unit,
    offlineSpeedPercent: Int,
    onOfflineSpeed: (Int) -> Unit,
    listeningLevelPercent: Int,
    onListeningLevel: (Int) -> Unit,
    crossfadeSeconds: Int,
    onCrossfade: (Int) -> Unit,
    normalizationEnabled: Boolean,
    onNormalizationChange: (Boolean) -> Unit,
    memory: ListeningMemorySummary,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onMemoryErase: () -> Unit,
    onMemoryRefresh: () -> Unit,
    currentLyrics: TimedLyrics?,
    lyricsOffsetMs: Int,
    onLyricsOffsetChange: (Int) -> Unit,
    onImportLyrics: () -> Unit,
    onEditLyrics: () -> Unit,
    onRemoveLyrics: () -> Unit,
    offlineResumeTitle: String?,
    offlineResumePositionMs: Long,
    onResumeOffline: () -> Unit,
    isFavorite: (String) -> Boolean,
    onToggleFavorite: (VideoPreview) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onRemoveRecent: (VideoPreview) -> Unit,
    onClearRecent: () -> Unit,
    onPlaylistSongOptions: (String, VideoPreview) -> Unit,
    onSharePlaylist: (String, List<VideoPreview>) -> Unit,
    onExportPlaylist: (String, List<VideoPreview>) -> Unit,
    onBulkShareLinks: (List<VideoPreview>, Boolean) -> Unit,
    onBulkRemove: (String, List<VideoPreview>) -> Unit,
    onBulkCopy: (String, List<VideoPreview>) -> Unit,
    onMovePlaylistSong: (String, String, Int, List<String>) -> Unit,
    playlistUndo: PlaylistUndoState?,
    onUndoPlaylistEdit: () -> Unit,
    onDismissPlaylistUndo: () -> Unit,
    onImportSharedPlaylist: () -> Unit,
    onSaveCurrentToPlaylist: () -> Unit,
    onSaveCurrentOffline: () -> Unit,
    onSearch: (String, SearchMode) -> Unit,
    onPrimarySearch: (String, SearchMode) -> Unit,
    onPrepareSearch: (String, SearchMode) -> Unit,
    onWebSearch: (String) -> Unit,
    onCancelSearch: () -> Unit,
    searchState: StudioSearchSnapshot,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onLibraryQuickPlay: (List<VideoPreview>) -> Unit,
    onLegacyPlayer: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onManageFolders: () -> Unit,
    onMovePlaylist: (String) -> Unit,
    onOfflineLibrary: () -> Unit,
    onOfflineSearch: (String) -> Unit,
    onShareDiagnostics: () -> Unit,
    onOpenClassic: () -> Unit,
    searchNavigationToken: Int,
    mainNavigationRequest: NightWaveMainNavigationRequest?,
    onMainNavigationApplied: (String) -> Unit
) {
    // The pager is the single source of truth for swipes, tabs and Back.
    // Pager state survives rotation and process recreation through Compose Saver.
    val studioContext = androidx.compose.ui.platform.LocalContext.current
    // Explicit search history stays on device, outside music and cloud backup.
    val searchHistoryStore = remember(studioContext) { SearchHistoryStore(studioContext) }
    var searchHistory by remember { mutableStateOf(searchHistoryStore.read()) }
    var removedSearchSnapshot by remember {
        mutableStateOf<Pair<List<String>, List<String>>?>(null)
    }
    val pagerState = rememberPagerState(initialPage = StudioTab.HOME.ordinal) {
        StudioTab.entries.size
    }
    val shelfGestures = remember { NightWaveShelfGestureState() }
    NightWavePagerRecovery(pagerState, shelfGestures.active)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val tab = StudioTab.entries[pagerState.settledPage]
    LaunchedEffect(tab, playerState.positionMs / 15000L) {
        if (tab == StudioTab.PROFILE || tab == StudioTab.HOME) onMemoryRefresh()
    }
    LaunchedEffect(tab) {
        // An active search/library text editor must not leave the soft keyboard
        // covering another page after a user swipe or bottom-tab selection.
        focus.clearFocus()
        keyboard?.hide()
    }
    val smartCollections = remember(libraryRevision, playlistNames, favorites, recent, roomPreviewActive) {
        SmartLibraryPolicy.generate(favorites, recent,
            playlistNames.take(60).map(getPlaylist))
    }
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    val resolvedPlaylist = StudioUiStateSafetyPolicy.resolveCollection(
        selectedPlaylist, playlistNames, smartCollections.map { it.id },
        LIKED_COLLECTION, RECENT_COLLECTION)
    LaunchedEffect(selectedPlaylist, resolvedPlaylist) {
        // Deleted/renamed playlists and regenerated Smart collections
        // cannot survive as ghost detail screens after Activity recreation.
        if (selectedPlaylist != resolvedPlaylist) selectedPlaylist = resolvedPlaylist
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showSoundStudio by rememberSaveable { mutableStateOf(false) }
    var showAccountHub by rememberSaveable { mutableStateOf(false) }
    var compactHidden by rememberSaveable { mutableStateOf(false) }
    var tabNavigationJob by remember { mutableStateOf<Job?>(null) }
    fun navigateToTab(destination: StudioTab) {
        if (!NightWavePagerRecoveryPolicy.needsNavigation(pagerState.currentPage,
                pagerState.currentPageOffsetFraction, pagerState.isScrollInProgress, destination.ordinal) &&
            tabNavigationJob?.isActive != true) return
        if (destination != StudioTab.LIBRARY) selectedPlaylist = null
        // Most recent tap wins. Avoid multiple long animations and expensive
        // intermediate-page compositions on distant-tab jumps.
        tabNavigationJob?.cancel()
        tabNavigationJob = scope.launch {
            if (NightWaveMotionPolicy.animateTabChange(
                    pagerState.currentPage, destination.ordinal,
                    StudioTab.entries.size, profile.motion == MotionPreference.REDUCED)) {
                pagerState.animateScrollToPage(destination.ordinal,
                    animationSpec = tween(durationMillis = NightWaveMotionPolicy.NAV_MS))
            } else {
                pagerState.scrollToPage(destination.ordinal)
            }
        }
    }
    LaunchedEffect(playerState.videoUrl) { compactHidden = false }
    LaunchedEffect(searchNavigationToken) {
        // Never replay shared media on rotation or slide through other pages.
        if (searchNavigationToken > 0) pagerState.scrollToPage(StudioTab.SEARCH.ordinal)
    }
    LaunchedEffect(mainNavigationRequest?.id) {
        val request = mainNavigationRequest ?: return@LaunchedEffect
        tabNavigationJob?.cancel()
        showQueue = false
        showSoundStudio = false
        showAccountHub = false
        if (request.tab != StudioTab.LIBRARY.ordinal) selectedPlaylist = null
        pagerState.scrollToPage(request.tab)
        expanded = request.openPlayer
        onMainNavigationApplied(request.id)
    }
    BackHandler(enabled = showQueue || expanded ||
        (tab == StudioTab.LIBRARY && resolvedPlaylist != null) || tab != StudioTab.HOME) {
        when {
            showQueue -> showQueue = false
            expanded -> expanded = false
            tab == StudioTab.LIBRARY && resolvedPlaylist != null -> selectedPlaylist = null
            tab != StudioTab.HOME -> navigateToTab(StudioTab.HOME)
        }
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NightWaveCompactTopBar { navigateToTab(StudioTab.PROFILE) }
        },
        bottomBar = {
            Column {
                if (!playerState.error.isNullOrBlank() &&
                    (playerState.hasTrack || !playerState.connected)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(playerState.error.orEmpty(),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = { playerActions.retry() }) {
                                Text(if (!playerState.connected) "Reconnect" else "Retry")
                            }
                        }
                    }
                }
                if (playerState.hasTrack && compactHidden) {
                    // Collapsed UI keeps playback alive and offers one-tap restore.
                    Surface(onClick = { compactHidden = false },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(48.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.PlayArrow,
                                contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Now playing · Tap to show",
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else if (playerState.hasTrack) {
                    StudioMiniPlayer(playerState, playerActions,
                        onExpand = { expanded = true },
                        onHide = { compactHidden = true })
                }
                NightWavePremiumNavigation(
                    selectedIndex = tab.ordinal,
                    reducedMotion = profile.motion == MotionPreference.REDUCED,
                    onReselect = { navigateToTab(tab) },
                    onNavigate = { target ->
                        navigateToTab(StudioTab.entries[
                            NightWaveChromePolicy.selectedTab(target)])
                    }
                )
            }
        }
    ) { safe ->
        // HorizontalPager owns page swipes; child LazyColumns own vertical scroll.
        // Full player / Up Next overlay gestures never move the background tabs.
        CompositionLocalProvider(LocalNightWaveShelfGestures provides shelfGestures) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(safe).testTag("nightwave-tab-pager"),
            key = { StudioTab.entries[it].name },
            beyondViewportPageCount = 1,
            userScrollEnabled = !expanded && !showQueue && !shelfGestures.active,
            pageSpacing = 0.dp
        ) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when (StudioTab.entries[page]) {
                    StudioTab.HOME -> StudioHome(
                        modifier = Modifier, profile = profile, recent = recent,
                        favorites = favorites,
                        memory = memory,
                        playlistTracks = remember(libraryRevision, playlistNames, roomPreviewActive) {
                            playlistNames.take(25).flatMap(getPlaylist).distinctBy { it.url }.take(350)
                        },
                        playingUrl = playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        onSearch = { query ->
                            onPrepareSearch(query, SearchMode.SONGS)
                            navigateToTab(StudioTab.SEARCH)
                        },
                        onResumeSpotlight = { song, collection ->
                            when (SpotlightResumePolicy.choose(
                                tappedUrl = song.url, currentUrl = playerState.videoUrl,
                                currentQueueIndex = playerState.queueIndex, offline = playerState.offline,
                                playing = playerState.playing, buffering = playerState.buffering,
                                hasError = playerState.error != null
                            )) {
                                SpotlightAction.KEEP_CURRENT -> Unit
                                SpotlightAction.RESUME_CURRENT -> playerActions.resumeCurrent()
                                SpotlightAction.RETRY_CURRENT -> playerActions.retry()
                                SpotlightAction.START_SELECTED -> onPlay(song, collection)
                            }
                        },
                        onPlay = onPlay, onSongOptions = onSongOptions,
                        onRemoveRecent = onRemoveRecent, onClearRecent = onClearRecent,
                        openRecentHistory = {
                            selectedPlaylist = RECENT_COLLECTION
                            navigateToTab(StudioTab.LIBRARY)
                        },
                        openLibrary = { navigateToTab(StudioTab.LIBRARY) },
                        openLikedSongs = {
                            selectedPlaylist = LIKED_COLLECTION
                            navigateToTab(StudioTab.LIBRARY)
                        },
                        openOffline = onOfflineLibrary,
                        openSearch = { navigateToTab(StudioTab.SEARCH) }
                    )
                    StudioTab.SEARCH -> {
                        val (localCorpus, retryLocalCorpus) = rememberNightWaveLocalCorpus(
                            studioContext, tab == StudioTab.SEARCH, libraryRevision)
                        StudioSearch(
                        Modifier, searchState, onPrimarySearch, onSearch, onWebSearch,
                        onPlay, onSongOptions,
                        playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        onCancelSearch = onCancelSearch,
                        corpus = localCorpus,
                        onRetryCorpus = retryLocalCorpus,
                        onOfflineSearch = onOfflineSearch,
                        searchHistory = searchHistory,
                        canUndoSearchRemoval = removedSearchSnapshot != null,
                        onRecordSearch = { entered ->
                            searchHistory = searchHistoryStore.record(entered)
                            removedSearchSnapshot = null
                        },
                        onRemoveSearch = { entered ->
                            val before = searchHistoryStore.read()
                            val after = searchHistoryStore.remove(entered)
                            searchHistory = after
                            removedSearchSnapshot = if (after != before) before to after else null
                        },
                        onUndoSearchRemoval = {
                            removedSearchSnapshot?.let { (before, after) ->
                                searchHistory = searchHistoryStore.restoreIfUnchanged(after, before)
                            }
                            removedSearchSnapshot = null
                        },
                        onClearSearch = {
                            if (searchHistory.isNotEmpty()) {
                                AlertDialog.Builder(studioContext)
                                    .setTitle("Clear recent searches?")
                                    .setMessage("Only searches on this device are removed. " +
                                        "Your songs, playlists and downloads stay untouched.")
                                    .setNegativeButton("Keep", null)
                                    .setPositiveButton("Clear searches") { _, _ ->
                                        searchHistory = searchHistoryStore.clear()
                                        removedSearchSnapshot = null
                                    }.show()
                            }
                        }
                    )
                    }
                    StudioTab.LIBRARY -> StudioLibrary(
                        modifier = Modifier, favorites = favorites, recent = recent,
                        names = playlistNames,
                        libraryRevision = libraryRevision, roomPreviewActive = roomPreviewActive,
                        smartCollections = smartCollections, folderSnapshot = folderSnapshot,
                        pinnedPlaylistNames = pinnedPlaylistNames,
                        onTogglePinPlaylist = onTogglePinPlaylist,
                        playingUrl = playerState.videoUrl.takeIf { playerState.playing }.orEmpty(),
                        selectedPlaylist = resolvedPlaylist,
                        onSelectPlaylist = { selectedPlaylist = it },
                        getPlaylist = getPlaylist, onPlay = onPlay,
                        onQuickPlay = onLibraryQuickPlay,
                        onSongOptions = onSongOptions,
                        onPlaylistSongOptions = onPlaylistSongOptions,
                        onRemoveRecent = onRemoveRecent, onClearRecent = onClearRecent,
                        onSharePlaylist = onSharePlaylist,
                        onExportPlaylist = onExportPlaylist,
                        onBulkShareLinks = onBulkShareLinks,
                        onBulkRemove = onBulkRemove,
                        onBulkCopy = onBulkCopy,
                        onMovePlaylistSong = onMovePlaylistSong,
                        playlistUndo = playlistUndo,
                        onUndoPlaylistEdit = onUndoPlaylistEdit,
                        onDismissPlaylistUndo = onDismissPlaylistUndo,
                        onImportSharedPlaylist = onImportSharedPlaylist,
                        onManage = onLegacyPlayer,
                        onCreatePlaylist = onCreatePlaylist,
                        onManageFolders = onManageFolders,
                        onMovePlaylist = onMovePlaylist,
                        onOffline = onOfflineLibrary,
                        offlineResumeTitle = offlineResumeTitle,
                        offlineResumePositionMs = offlineResumePositionMs,
                        onResumeOffline = onResumeOffline,
                        onSearch = { navigateToTab(StudioTab.SEARCH) }
                    )
                    StudioTab.PROFILE -> StudioProfile(
                        modifier = Modifier, profile = profile, save = onSaveProfile,
                        onExport = onExportGuest, onImport = onImportGuest,
                        onRecoverLastChange = onRecoverLastChange,
                        cloud = cloud, cloudBusy = cloudBusy, cloudPhase = cloudPhase,
                        onCloudConnect = onCloudConnect,
                        onCloudBackup = onCloudBackup,
                        onCloudPreviewRestore = onCloudPreviewRestore,
                        onCloudCompare = onCloudCompare,
                        onCloudDisconnect = onCloudDisconnect,
                        onCloudHistory = onCloudHistory,
                        onCloudLyrics = onCloudLyrics,
                        onCloudPreferences = onCloudPreferences,
                        onCloudWifi = onCloudWifi,
                        storageStatus = storageStatus, onCheckStorage = onCheckStorage,
                        roomReadStatus = roomReadStatus,
                        roomPreviewActive = roomPreviewActive,
                        onEnableRoomPreview = onEnableRoomPreview,
                        onUseLegacyReads = onUseLegacyReads,
                        insights = remember(recent, favorites, playlistNames) {
                            ListeningInsightsEngine.calculate(recent, favorites, playlistNames)
                        },
                        memory = memory,
                        onMemoryEnabledChange = onMemoryEnabledChange,
                        onMemoryErase = onMemoryErase,
                        onMemoryRefresh = onMemoryRefresh,
                        playbackState = playerState,
                        onRecoverPlayback = { playerActions.retry() },
                        onShareDiagnostics = onShareDiagnostics,
                        onOpenClassic = onOpenClassic,
                        onEqualizer = { showSoundStudio = true },
                        onOpenAccount = { showAccountHub = true },
                        onOffline = onOfflineLibrary,
                        onManageHistory = {
                            selectedPlaylist = RECENT_COLLECTION
                            navigateToTab(StudioTab.LIBRARY)
                        }
                    )
                }
            }
        }
        }
    }
    StudioNowPlayingOverlay(
        expanded = expanded && playerState.hasTrack,
        state = playerState, actions = playerActions,
        reduceMotion = profile.motion == MotionPreference.REDUCED,
        liked = playerState.hasTrack && !playerState.offline &&
            isFavorite(playerState.videoUrl),
        onLike = {
            if (playerState.hasTrack && !playerState.offline) onToggleFavorite(VideoPreview(
                playerState.title, playerState.artist, playerState.videoUrl,
                playerState.thumbnail, playerState.durationMs / 1000L
            ))
        },
        onOpenQueue = { showQueue = true },
        onSaveToPlaylist = onSaveCurrentToPlaylist,
        onSaveOffline = onSaveCurrentOffline,
        sleepRemainingMs = sleepRemainingMs,
        onSleepTimer = onSleepTimer,
        offlineSpeedPercent = offlineSpeedPercent,
        onOfflineSpeed = onOfflineSpeed,
        listeningLevelPercent = listeningLevelPercent,
        onListeningLevel = onListeningLevel,
        crossfadeSeconds = crossfadeSeconds,
        onCrossfade = onCrossfade,
        normalizationEnabled = normalizationEnabled,
        onNormalizationChange = onNormalizationChange,
        lyrics = currentLyrics,
        lyricsOffsetMs = lyricsOffsetMs,
        onLyricsOffsetChange = onLyricsOffsetChange,
        onImportLyrics = onImportLyrics,
        onEditLyrics = onEditLyrics,
        onRemoveLyrics = onRemoveLyrics,
        onDismiss = { expanded = false }
    )
    if (showSoundStudio) NightWaveSoundStudioDialog(
        onDismiss = { showSoundStudio = false }
    )
    if (showAccountHub) NightWaveAccountHubDialog(
        profile = profile, cloud = cloud, busy = cloudBusy, phase = cloudPhase,
        onDismiss = { showAccountHub = false },
        onConnect = onCloudConnect,
        onBackUp = onCloudBackup,
        onPreviewRestore = onCloudPreviewRestore,
        onCompare = onCloudCompare,
        onDisconnect = onCloudDisconnect,
        onExportLocal = onExportGuest,
        onHistory = onCloudHistory,
        onLyrics = onCloudLyrics,
        onPreferences = onCloudPreferences,
        onWifiOnly = onCloudWifi
    )
    if (showQueue) StudioQueueSheet(
        state = playerState, onDismiss = { showQueue = false },
        onSelect = { index -> playerActions.jumpTo(index); showQueue = false },
        onRemoveUpcoming = { index, url -> playerActions.removeUpcoming(index, url) },
        onUndoRemoval = { token -> playerActions.undoUpcoming(token) },
        onMoveUpcoming = { from, to, expected -> playerActions.moveUpcoming(from, to, expected) },
        onClearUpcoming = { expected -> playerActions.clearUpcoming(expected) },
        reduceMotion = profile.motion == MotionPreference.REDUCED
    )
    }
}

@Composable
internal fun StudioHome(
    modifier: Modifier, profile: ProfileSettings, recent: List<VideoPreview>,
    favorites: List<VideoPreview>, playlistTracks: List<VideoPreview>,
    memory: ListeningMemorySummary,
    playingUrl: String, onSearch: (String) -> Unit,
    onResumeSpotlight: (VideoPreview, List<VideoPreview>) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onRemoveRecent: (VideoPreview) -> Unit,
    onClearRecent: () -> Unit, openRecentHistory: () -> Unit,
    openLibrary: () -> Unit,
    openLikedSongs: () -> Unit, openOffline: () -> Unit,
    openSearch: () -> Unit
) {
    val dayNumber = java.time.LocalDate.now().toEpochDay()
    val recentShelfState = rememberLazyListState()
    val nextShelfState = rememberLazyListState()
    val moodShelfState = rememberLazyListState()
    // Extend the existing personal mixes, rather than replacing them.
    // All inputs are already device-local saved/recent public video metadata.
    val homePlan = remember(favorites, recent, playlistTracks, dayNumber, memory) {
        val daily = LocalDailyMixEngine.make(favorites, recent, playlistTracks,
            dayNumber, memoryTracks = memory.recommendationTracks,
            memoryEnabled = memory.enabled)
        val existing = PersonalMixEngine.make(favorites, recent, playlistTracks,
            dayNumber, memoryTracks = memory.recommendationTracks,
            memoryEnabled = memory.enabled)
            // Daily Mix 1–3 supersede the earlier generic daily rotation card.
            .filterNot { it.key == "daily" }
        NightWaveHomeCurationPolicy.curate(favorites, recent, playlistTracks,
            daily, existing, dayNumber)
    }
    val mixes = homePlan.mixes
    // The rail displays at most 100 unique recent songs; never create a huge
    // Home tap-to-play queue just because saved history grew to 5k entries.
    // The complete history remains untouched in Room/preferences/library.
    val recentWindow = remember(recent) {
        NightWaveHomeCurationPolicy.recentWindow(recent.filter { VideoLink.normalize(it.url) == it.url })
    }
    val recentShelf = recentWindow.tracks
    val entry = remember(favorites, recentShelf, playlistTracks) {
        NightWaveHomePresentationPolicy.entry(favorites, recentShelf, playlistTracks)
    }
    val mixPresentations = remember(mixes, favorites, recent, playlistTracks, memory.enabled) {
        mixes.associate { it.key to NightWaveHomePresentationPolicy.mix(it, favorites, recent, playlistTracks, memory.enabled) }
    }
    val openPrimary = {
        when (entry.primary?.destination) {
            NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIKED_SONGS -> openLikedSongs()
            NightWaveHomeLayoutPolicy.EmptyRecentDestination.LIBRARY -> openLibrary()
            NightWaveHomeLayoutPolicy.EmptyRecentDestination.SEARCH -> openSearch()
            null -> Unit
        }
    }
    var chosenMixKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showAllMixTracks by rememberSaveable { mutableStateOf(false) }
    var showAllRecent by rememberSaveable { mutableStateOf(false) }
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }
    val recentBrowseActions = NightWaveHomeLayoutPolicy.recentBrowseActions(
        recentShelf.size, recentWindow.hasMore, showAllRecent)
    val chosenMix = mixes.firstOrNull { it.key == chosenMixKey }
    val palettes = listOf(
        listOf(Color(0xFF423163), Color(0xFF242B57)),
        listOf(Color(0xFF164F58), Color(0xFF19334D)),
        listOf(Color(0xFF744259), Color(0xFF3E2B56)),
        listOf(Color(0xFF4D577C), Color(0xFF273B51)),
        listOf(Color(0xFF705031), Color(0xFF4F334B)),
        listOf(Color(0xFF3B5867), Color(0xFF344066))
    )
    val nw83PageGutter = NightWavePremiumDesignTokens.pageGutterDp(
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()).dp
    LazyColumn(
        modifier = modifier.testTag("nw131-home-list").fillMaxWidth().widthIn(max =
            NightWavePremiumDesignTokens.MAX_CONTENT_WIDTH_DP.dp).fillMaxHeight(),
        // Scaffold already reserves the mini-player and navigation height;
        // only add breathing room, not an artificial 150dp blank spacer.
        contentPadding = PaddingValues(start = nw83PageGutter, end = nw83PageGutter,
            top = NightWavePremiumDesignTokens.PAGE_TOP_DP.dp,
            bottom = NightWavePremiumDesignTokens.PAGE_BOTTOM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(NightWavePremiumDesignTokens.ITEM_SPACING_DP.dp)
    ) {
        item(key = "nightwave-premium-home-header") {
            NightWaveHomeHero(
                greeting = NightWaveChromePolicy.greeting(java.time.LocalTime.now().hour),
                displayName = profile.displayName,
                lastTrack = recentShelf.firstOrNull(),
                onContinue = recentShelf.firstOrNull()?.let { last ->
                    { onResumeSpotlight(last, recentShelf) }
                },
                emptyTitle = entry.primary?.title,
                emptyDetail = entry.primary?.detail,
                emptyActionLabel = entry.primary?.actionLabel,
                onEmptyAction = if (entry.primary != null) openPrimary else null
            )
        }
        item(key = "nw47-home-quick-access") {
            Row(Modifier.fillMaxWidth().nightWaveShelfBoundary().horizontalScroll(rememberScrollState())
                    .testTag("nw47-home-quick-access"),
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                NightWaveQuickActionPill("Offline",
                    Icons.Outlined.LibraryMusic, "nw47-home-offline", openOffline)
                if (entry.showLikedShortcut) NightWaveQuickActionPill("Liked songs",
                    Icons.Outlined.FavoriteBorder, "nw47-home-liked", openLikedSongs)
                if (entry.showSearchShortcut) NightWaveQuickActionPill("Find music",
                    Icons.Outlined.Search, "nw47-home-search", openSearch)
            }
        }
        // Empty history is handled by the single purposeful hero above.
        if (recentShelf.isNotEmpty()) {
            item(key = "recent-heading") {
                NightWaveSectionActions(heading = {
                    SectionHeading("Recently played",
                        (if (recentWindow.hasMore)
                            "${recentShelf.size}+ tracks"
                        else "${recentShelf.size} tracks"))
                }, action = {
                    if (recentBrowseActions.showExpansion) {
                        TextButton(onClick = { showAllRecent = !showAllRecent },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("nw117-recent-expand")) {
                            Text(recentBrowseActions.expansionLabel.orEmpty())
                        }
                    }
                })
            }
            if (entry.kind == NightWaveHomePresentationPolicy.Kind.HISTORY_ONLY) item(key = "nw131-history-save-hint") {
                Text("Like or add to a playlist from track options.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item(key = "nw47-recent-shelf") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().nightWaveShelfBoundary().testTag("nw47-recent-shelf"),
                    state = recentShelfState,
                    flingBehavior = rememberSnapFlingBehavior(recentShelfState, SnapPosition.Start),
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    items(if (showAllRecent) recentShelf.take(100) else recentShelf.take(6),
                        key = { "recent:" + it.url }) { song ->
                        NightWaveRecentShelfTile(
                            song = song, isPlaying = playingUrl == song.url,
                            onPlay = { onPlay(song, recentShelf) },
                            onOptions = { onSongOptions(song) },
                            onRemove = { onRemoveRecent(song) }
                        )
                    }
                }
            }
            if (recentBrowseActions.showFullHistory) {
                item(key = "nw117-full-recent-history") {
                    OutlinedButton(
                        onClick = openRecentHistory,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("nw117-open-full-recent-history")
                    ) {
                        Text("Browse full history")
                    }
                }
            }
            if (showAllRecent) item(key = "clear-listening-history") {
                OutlinedButton(onClick = { confirmClearHistory = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("nw86-clear-recent-request")) {
                    Text("Clear listening history")
                }
                if (confirmClearHistory) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { confirmClearHistory = false },
                        title = { Text("Clear Recently Played?") },
                        text = {
                            Text("This removes history entries only. Liked songs, " +
                                "playlists and downloaded music stay saved.")
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                confirmClearHistory = false
                                onClearRecent()
                            }, modifier = Modifier.testTag("nw86-clear-recent-confirm")) {
                                Text("Clear history")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { confirmClearHistory = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
        // Music from the actual saved library, excluding the visible recent
        // shelf: no fabricated suggestions or duplicate "Continue" cards.
        if (homePlan.nextListens.isNotEmpty()) {
            item(key = "nw84-next-listens-heading") {
                SectionHeading("For your next listen",
                    "Liked and playlist tracks beyond your recent rotation")
            }
            item(key = "nw84-next-listens-shelf") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth()
                        .nightWaveShelfBoundary().testTag("nw84-next-listens-shelf"),
                    state = nextShelfState,
                    flingBehavior = rememberSnapFlingBehavior(nextShelfState, SnapPosition.Start),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(homePlan.nextListens, key = { "next:" + it.url }) { song ->
                        NightWaveNextListenTile(
                            song = song,
                            provenance = NightWaveHomeCurationPolicy.sourceLabel(
                                song, favorites),
                            isPlaying = playingUrl == song.url,
                            onPlay = { onPlay(song, homePlan.nextListens) },
                            onOptions = { onSongOptions(song) }
                        )
                    }
                }
            }
        }
        // Lead with actual music before accounting-style counters.
        if (mixes.isNotEmpty()) {
            item {
                SectionHeading("Your mixes", "Choose a mix to preview")
            }
            // Keep every existing mix and preview action. One full-width feature leads
            // into measured paired cells; narrow/large text stacks the same reading order.
            mixes.firstOrNull()?.let { mix ->
                item(key = "matte-featured-mix") {
                    NightWaveMixCard(mix = mix, selected = chosenMixKey == mix.key,
                        modifier = Modifier.fillMaxWidth(), featured = true, emphasized = true,
                        presentation = mixPresentations[mix.key], onClick = {
                            chosenMixKey = if (chosenMixKey == mix.key) null else mix.key
                            showAllMixTracks = false
                        })
                }
            }
            mixes.drop(1).chunked(2).forEachIndexed { row, pair ->
                item(key = "personal-mix-$row") {
                    val firstCell: @Composable () -> Unit = {
                        val mix = pair.first()
                        NightWaveMixCard(mix = mix, selected = chosenMixKey == mix.key,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(), presentation = mixPresentations[mix.key],
                            featured = NightWaveThemeExpressionPolicy.featuredMixCell(pair.size),
                            onClick = {
                                chosenMixKey = if (chosenMixKey == mix.key) null else mix.key
                                showAllMixTracks = false
                            })
                    }
                    if (pair.size == 2) NightWaveBentoPair(first = firstCell, second = {
                        val mix = pair[1]
                        NightWaveMixCard(mix = mix, selected = chosenMixKey == mix.key,
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(), presentation = mixPresentations[mix.key],
                            featured = NightWaveThemeExpressionPolicy.featuredMixCell(pair.size),
                            onClick = {
                                chosenMixKey = if (chosenMixKey == mix.key) null else mix.key
                                showAllMixTracks = false
                            })
                    }) else firstCell()
                }
            }
            if (chosenMix != null) {
                item(key = "mix-explanation") {
                    NightWaveSectionActions(heading = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(mixPresentations[chosenMix.key]?.title ?: chosenMix.title,
                                style = MaterialTheme.typography.titleSmall)
                            Text(mixPresentations[chosenMix.key]?.reason.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }, action = {
                        OutlinedButton(onClick = {
                            chosenMix.tracks.firstOrNull()?.let { onPlay(it, chosenMix.tracks) }
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("nw131-play-mix")) { Text("Play mix") }
                    })
                }
                items(chosenMix.tracks.take(if (showAllMixTracks) 12 else 3),
                    key = { "mix:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        onOptions = { onSongOptions(song) },
                        onClick = { onPlay(song, chosenMix.tracks) })
                }
                if (chosenMix.tracks.size > 3) {
                    item(key = "mix-more-tracks") {
                        OutlinedButton(onClick = { showAllMixTracks = !showAllMixTracks },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAllMixTracks) "Show fewer tracks"
                                else "See all ${chosenMix.tracks.size} tracks")
                        }
                    }
                }
            }
        }
        item { SectionHeading("Search by mood", "Choose a mood to explore") }
        // One horizontally scrollable shelf instead of three oversized rows.
        // Uses existing real mood shortcuts and unchanged search actions.
        item(key = "discovery-shelf") {
            LazyRow(Modifier.fillMaxWidth().nightWaveShelfBoundary().testTag("content-mood-shelf"), state = moodShelfState,
                flingBehavior = rememberSnapFlingBehavior(moodShelfState, SnapPosition.Start),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(DiscoveryShortcuts.picks, key = { _, shortcut -> shortcut.key }) { index, shortcut ->
                    MoodTile(shortcut.title, shortcut.subtitle,
                        palettes[index % palettes.size], Modifier.width(178.dp)) {
                        onSearch(shortcut.query)
                    }
                }
            }
        }

    }
}

@Composable
private fun MoodTile(title: String, subtitle: String, gradient: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    NightWaveMoodArt(title, subtitle, gradient, modifier, onClick)
}

@Composable
internal fun StudioSearch(
    modifier: Modifier,
    state: StudioSearchSnapshot,
    onPrimarySearch: (String, SearchMode) -> Unit,
    onNativeSearch: (String, SearchMode) -> Unit,
    onWebSearch: (String) -> Unit,
    onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    playingUrl: String,
    corpus: LocalMusicCorpusState,
    onRetryCorpus: () -> Unit,
    onOfflineSearch: (String) -> Unit,
    searchHistory: List<String>,
    canUndoSearchRemoval: Boolean,
    onRecordSearch: (String) -> Unit,
    onRemoveSearch: (String) -> Unit,
    onUndoSearchRemoval: () -> Unit,
    onClearSearch: () -> Unit,
    onCancelSearch: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(SearchMode.SONGS) }
    var selectedCreator by rememberSaveable { mutableStateOf<String?>(null) }
    var showAllCreators by rememberSaveable { mutableStateOf(false) }
    var consumedPrefill by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(state.prefillToken) {
        // Only explicit shortcuts/shared text may replace a draft. Provider responses
        // and pager recreation must never resurrect a query the listener cleared.
        if (SearchDraftPolicy.shouldApply(state.prefillToken, consumedPrefill)) {
            query = state.query.take(160)
            mode = state.mode
            consumedPrefill = state.prefillToken
            selectedCreator = null
        }
    }
    var queryFocused by remember { mutableStateOf(false) }
    // Search on the keyboard stays inside NightWave. YouTube stays an
    // explicit alternative when public provider results are unavailable.
    val submit = {
        val entered = query.trim()
        if (entered.isNotBlank()) {
            onRecordSearch(entered)
            onNativeSearch(entered, mode)
        }
    }
    val index = corpus.index
    val localSearch by produceState<LocalMusicSearchResult?>(null, query, index) {
        value = null
        if (query.isNotBlank() && index != null) {
            value = LocalMusicQueryRunner.run(index, query)
        }
    }
    // Synchronous tags gate the first composition of a new query/library,
    // even before the old producer has received cancellation.
    val acceptedLocal = localSearch?.takeIf { it.accepts(query, index) }
    val savedMatches = acceptedLocal?.saved.orEmpty()
    val offlineMatches = acceptedLocal?.offline.orEmpty()
    val localSuggestions = acceptedLocal?.suggestions.orEmpty()
    val offlineSuggestions = offlineMatches.take(3)
    val savedTracks = index?.previews.orEmpty()
    val knownCreators = index?.creators.orEmpty().take(8)
    val creatorGroups = index?.creatorGroups.orEmpty()
    val localPending = !corpus.failed && (index == null || (query.isNotBlank() && acceptedLocal == null))
    var savedVisible by remember(query, index) { mutableIntStateOf(24) }
    var offlineVisible by remember(query, index) { mutableIntStateOf(60) }
    val selectedCreatorGroup = remember(selectedCreator, creatorGroups) {
        creatorGroups.firstOrNull { it.name == selectedCreator }
    }
    // Never present results from an older query as results for new typed text.
    val responseMatchesQuery = SearchResultPresentationPolicy.matches(
        query, mode, state.query, state.mode)
    val searching = responseMatchesQuery && state.loading
    val publicGroups = remember(query, mode, responseMatchesQuery, state.videos) {
        if (responseMatchesQuery) SearchResultPresentationPolicy.groupMusic(
            query, mode, state.videos)
        else SearchResultPresentationPolicy.Groups(emptyList(), emptyList())
    }
    val publicEvidence = remember(state.videos, mode) {
        PublicMusicResultEvidence.forResults(state.videos, mode)
    }
    // All public-result groups share one user-intent playback command.
    val playSinglePublic = { selected: VideoPreview ->
        onPlay(selected, listOf(selected))
    }
    val nw83PageGutter = NightWavePremiumDesignTokens.pageGutterDp(
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()).dp
    var showSearchDetails by remember(state.query, state.mode, state.details) { mutableStateOf(false) }
    if (showSearchDetails && responseMatchesQuery && state.details.isNotBlank()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSearchDetails = false },
            title = { Text("Search details") },
            text = { Text(state.details) },
            confirmButton = { TextButton(onClick = { showSearchDetails = false }) { Text("Done") } }
        )
    }
    LazyColumn(
        modifier.testTag("nw129-search-list").fillMaxWidth().widthIn(max =
            NightWavePremiumDesignTokens.MAX_CONTENT_WIDTH_DP.dp).fillMaxHeight(),
        contentPadding = PaddingValues(start = nw83PageGutter, end = nw83PageGutter,
            top = NightWavePremiumDesignTokens.PAGE_TOP_DP.dp,
            bottom = NightWavePremiumDesignTokens.PAGE_BOTTOM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(NightWavePremiumDesignTokens.ITEM_SPACING_DP.dp)
    ) {
        item {
            NightWaveDiscoveryHeader(searchQuery = query)
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = {
                    if (it != query && state.loading) onCancelSearch()
                    query = it.take(160)
                    selectedCreator = null
                },
                modifier = Modifier.fillMaxWidth().testTag("nw129-search-query")
                    .onFocusChanged { queryFocused = it.isFocused }
                    .semantics { contentDescription = "Search songs, artists or links" },
                shape = RoundedCornerShape(17.dp),
                singleLine = true, placeholder = {
                    if (!queryFocused) Text("Search songs, artists or links",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = {
                        if (state.loading) onCancelSearch()
                        query = ""
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submit() })
            )
        }
        if (localPending) item(key = "nw129-local-loading") {
            Text(if (index == null) "Updating your music…" else "Searching your music…",
                modifier = Modifier.testTag("nw129-local-loading"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (corpus.failed) item(key = "nw129-local-error") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Local music could not be read. Your music has not been changed; public search is still available.",
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetryCorpus, modifier = Modifier.heightIn(min = 48.dp)) { Text("Retry local music") }
            }
        }
        if (query.isNotBlank()) {
            item(key = "search-mode-selector") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = mode == SearchMode.SONGS,
                        onClick = {
                            if (mode != SearchMode.SONGS) {
                                mode = SearchMode.SONGS
                                onNativeSearch(query.trim(), SearchMode.SONGS)
                            }
                        }, label = { Text("Songs") })
                    FilterChip(selected = mode == SearchMode.VIDEOS,
                        onClick = {
                            if (mode != SearchMode.VIDEOS) {
                                mode = SearchMode.VIDEOS
                                onNativeSearch(query.trim(), SearchMode.VIDEOS)
                            }
                        }, label = { Text("Videos") })
                }
            }
        }
        if (query.isNotBlank() &&
            (localSuggestions.isNotEmpty() || offlineSuggestions.isNotEmpty())) {
            item(key = "local-search-suggestions") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Suggestions from your library",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().nightWaveShelfBoundary().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        offlineSuggestions.forEach { offline ->
                            FilterChip(
                                selected = false,
                                onClick = { onOfflineSearch(offline.title) },
                                label = {
                                    Text(offline.title + " · Offline",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                        localSuggestions.forEach { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    // Only prefill local text; never send a
                                    // saved library suggestion to a provider.
                                    query = suggestion.query
                                    selectedCreator = null
                                },
                                label = {
                                    Text(suggestion.label + if (
                                        suggestion.kind ==
                                            com.nightwave.player.net.LocalSearchSuggestion.Kind.CREATOR
                                    ) " · Creator" else " · Song",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            )
                        }
                    }
                }
            }
        }
        if (selectedCreatorGroup != null && query.isBlank()) {
            item(key = "selected-saved-creator") {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = {
                        selectedCreator = null
                        showAllCreators = false
                    }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Back to discovery")
                    }
                    SectionHeading(selectedCreatorGroup.name,
                        "${selectedCreatorGroup.trackCount} songs from your music")
                    Text("Creator names come from liked songs, playlists and history; they are not verified artist profiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (selectedCreatorGroup.hasMore) {
                        Text("Showing the first 100 local references for this creator. " +
                            "Search your library to find more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        mode = SearchMode.SONGS
                        onRecordSearch(selectedCreatorGroup.name)
                        onPrimarySearch(selectedCreatorGroup.name, SearchMode.SONGS)
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Search this creator on YouTube")
                    }
                }
            }
            items(selectedCreatorGroup.tracks, key = { "creator-track:" + it.url }) { song ->
                MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    surface = NightWaveTrackSurface.SEARCH,
                    sourceLabel = index?.origin(song.url)?.label(),
                    onOptions = { onSongOptions(song) },
                    onClick = { onPlay(song, listOf(song)) })
            }
        } else {
        if (query.isNotBlank()) {
            item {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = submit,
                        enabled = !searching,
                        modifier = Modifier.weight(1.45f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null,
                            modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (searching) "Finding music…" else "Find music",
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            onRecordSearch(query.trim())
                            onPrimarySearch(query.trim(), mode)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("YouTube", maxLines = 1) }
                }
            }
        }
        if (query.isBlank() && !searching) {
            if (savedTracks.isNotEmpty()) {
                item {
                    SectionHeading("From your music",
                        "Liked, playlists and recent plays")
                }
                items(savedTracks.take(3), key = { "search-starter:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    surface = NightWaveTrackSurface.SEARCH,
                    sourceLabel = index?.origin(song.url)?.label(),
                        onOptions = { onSongOptions(song) },
                        onClick = { onPlay(song, listOf(song)) })
                }
            } else {
                item {
                    Text("Your liked songs, playlists and recent music will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (searchHistory.isNotEmpty()) {
                item(key = "search-history-heading") {
                    Row(Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = onClearSearch,
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Clear all")
                        }
                    }
                }
                items(searchHistory, key = { "search-history:" + it.lowercase(java.util.Locale.ROOT) }) { previous ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = {
                                query = previous
                                mode = SearchMode.SONGS
                                selectedCreator = null
                                onRecordSearch(previous)
                                onNativeSearch(previous, SearchMode.SONGS)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Search, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Text(previous, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        IconButton(onClick = { onRemoveSearch(previous) },
                            modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.Close,
                                contentDescription = "Remove " + previous + " from search history")
                        }
                    }
                }
            }
            if (canUndoSearchRemoval) {
                item(key = "search-history-undo") {
                    TextButton(onClick = onUndoSearchRemoval,
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Undo last search-history removal")
                    }
                }
            }
            item(key = "search-offline-quick-access") {
                NightWaveSectionActions(heading = { NightWaveMicroLabel("Explore") }, action = {
                    TextButton(onClick = { onOfflineSearch("") }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp)); Text("Offline songs")
                    }
                })
                val moodFilters = rememberLazyListState()
                LazyRow(modifier = Modifier.nightWaveShelfBoundary(), state = moodFilters,
                    flingBehavior = rememberSnapFlingBehavior(moodFilters, SnapPosition.Start),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DiscoveryShortcuts.picks.take(4), key = { it.key }) { shortcut ->
                        FilterChip(selected = false,
                            onClick = {
                                mode = SearchMode.SONGS
                                query = shortcut.query
                                onRecordSearch(shortcut.query)
                                onPrimarySearch(shortcut.query, SearchMode.SONGS)
                            }, label = { Text(shortcut.title, maxLines = 1) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            if (knownCreators.isNotEmpty()) {
                item(key = "local-creators") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Creators", "From your music")
                        Row(Modifier.fillMaxWidth().nightWaveShelfBoundary().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            knownCreators.forEach { creator ->
                                FilterChip(selected = false,
                                    onClick = {
                                        // Only update the local search filter.
                                        // Online search remains a separate tap.
                                        mode = SearchMode.SONGS
                                        selectedCreator = creator.name
                                    },
                                    label = {
                                        Text(creator.name + " · " + creator.trackCount,
                                            maxLines = 1)
                                    })
                            }
                        }
                    }
                }
            }
            if (creatorGroups.isNotEmpty()) {
                item(key = "all-saved-creators-toggle") {
                    OutlinedButton(
                        onClick = { showAllCreators = !showAllCreators },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (showAllCreators) "Hide creators"
                            else "All creators · " + creatorGroups.size)
                    }
                }
                if (showAllCreators) {
                    item(key = "all-saved-creators-list") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            creatorGroups.chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { creator ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                selectedCreator = creator.name
                                                showAllCreators = false
                                                mode = SearchMode.SONGS
                                            },
                                            label = {
                                                Text(creator.name + " · " + creator.trackCount,
                                                    maxLines = 1)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        if (savedMatches.isNotEmpty()) {
            item {
                SectionHeading("From your music",
                    savedMatches.size.toString() + " matches in your music")
            }
            items(savedMatches.take(savedVisible), key = { "saved:" + it.url }) { song ->
                MusicTrackTile(song, isPlaying = playingUrl == song.url,
                    surface = NightWaveTrackSurface.SEARCH,
                    sourceLabel = index?.origin(song.url)?.label(),
                    onOptions = { onSongOptions(song) },
                    onClick = { onPlay(song, listOf(song)) })
            }
        }
        if (savedMatches.size > savedVisible) item(key = "nw129-more-saved") {
            TextButton(onClick = { savedVisible += 24 }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Show more music matches")
            }
        }
        if (offlineMatches.isNotEmpty()) {
            item(key = "offline-search-header") {
                SectionHeading("On-device entries",
                    "${offlineMatches.size} matches · Open On device to check file availability")
            }
            items(offlineMatches.take(offlineVisible), key = { "offline-search:" + it.id }) { song ->
                OutlinedButton(
                    onClick = { onOfflineSearch(song.title) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Text("♫  " + song.title, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Offline", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (offlineMatches.size > offlineVisible) item(key = "nw129-more-offline") {
            TextButton(onClick = { offlineVisible += 60 }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Show more on-device matches")
            }
        }
        if (searching) {
            item(key = "public-search-loading") {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(state.phase.label,
                            style = MaterialTheme.typography.titleSmall)
                        Text(SearchConsumerCopyPolicy.loadingHint(
                            savedMatches.isNotEmpty() || offlineMatches.isNotEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onCancelSearch, modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("nw130-cancel-search")) { Text("Cancel search") }
                        TextButton(onClick = { onCancelSearch(); onWebSearch(query.trim()) },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Open YouTube search") }
                    }
                }
            }
        }
        if (responseMatchesQuery && state.videos.isNotEmpty()) {
            item(key = "nw54-public-results-heading") {
                SectionHeading(
                    SearchConsumerCopyPolicy.resultsTitle(
                        if (mode == SearchMode.VIDEOS) SearchConsumerCopyPolicy.Mode.VIDEOS
                        else SearchConsumerCopyPolicy.Mode.SONGS),
                    SearchConsumerCopyPolicy.resultCount(state.videos.size))
                if (searching || state.fromCache) Text(
                    if (searching) "Recent results · Updating…" else "Recent results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!searching) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { onNativeSearch(query.trim(), mode) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("search02-refresh")) { Text("Refresh results") }
                    if (state.details.isNotBlank()) TextButton(onClick = { showSearchDetails = true },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Search details") }
                }
            }
            if (state.error.isNotBlank()) {
                item {
                    Column {
                        Text(state.error + " Showing previous results.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { onNativeSearch(query.trim(), mode) },
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Retry search") }
                    }
                }
            }
            if (publicGroups.bestMatches.isNotEmpty()) {
                item(key = "public-best-heading") {
                    SectionHeading("Best matches",
                        SearchConsumerCopyPolicy.bestMatchesSubtitle())
                }
                items(publicGroups.bestMatches, key = { "public:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        surface = NightWaveTrackSurface.SEARCH,
                    sourceLabel = index?.origin(song.url)?.label(),
                        publicEvidence = publicEvidence[song.url],
                        rawVideoCaption = mode == SearchMode.VIDEOS,
                        onOptions = { onSongOptions(song) },
                        onClick = { playSinglePublic(song) })
                }
            }
            if (publicGroups.otherVersions.isNotEmpty()) {
                item(key = "nw82-alternate-recordings-heading") {
                    SectionHeading("Other versions",
                        "Version labels come from video titles · each source stays selectable")
                }
                items(publicGroups.otherVersions, key = { "alternate:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        surface = NightWaveTrackSurface.SEARCH,
                    sourceLabel = index?.origin(song.url)?.label(),
                        publicEvidence = publicEvidence[song.url],
                        onOptions = { onSongOptions(song) },
                        onClick = { playSinglePublic(song) })
                }
            }
            if (publicGroups.moreResults.isNotEmpty()) {
                if (publicGroups.bestMatches.isNotEmpty() ||
                    publicGroups.otherVersions.isNotEmpty()) {
                    item(key = "public-more-heading") {
                        SectionHeading("More results",
                            SearchConsumerCopyPolicy.moreResultsSubtitle())
                    }
                }
                items(publicGroups.moreResults, key = { "public:" + it.url }) { song ->
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        surface = NightWaveTrackSurface.SEARCH,
                    sourceLabel = index?.origin(song.url)?.label(),
                        publicEvidence = publicEvidence[song.url],
                        rawVideoCaption = mode == SearchMode.VIDEOS,
                        onOptions = { onSongOptions(song) },
                        onClick = { playSinglePublic(song) })
                }
            }
        } else if (!searching && responseMatchesQuery && state.error.isNotBlank()) {
            item {
                Column(Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(SearchConsumerCopyPolicy.unavailableTitle(),
                        style = MaterialTheme.typography.titleSmall)
                    Text(state.error, style = MaterialTheme.typography.bodyMedium)
                    if (state.details.isNotBlank()) TextButton(onClick = { showSearchDetails = true },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Search details") }
                    Text(SearchConsumerCopyPolicy.fallbackHint(
                        savedMatches.isNotEmpty() || offlineMatches.isNotEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = { onNativeSearch(query.trim(), mode) },
                            enabled = query.isNotBlank()) {
                            Text("Retry search")
                        }
                        OutlinedButton(onClick = { onWebSearch(query.trim()) },
                            enabled = query.isNotBlank()) {
                            Text("YouTube")
                        }
                    }
                }
            }
        } else if (!searching && responseMatchesQuery && state.hint.isNotBlank()) {
            item {
                Text(state.hint, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (query.isNotBlank() && savedMatches.isEmpty() &&
            offlineMatches.isEmpty() && !searching && !localPending && !corpus.failed) {
            item {
                Text("No saved matches yet. Search public music, or try a different title or artist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (query.isNotBlank()) {
        }
        } // Keep creator-specific local browsing separate from public results.
    }
}

@Composable
internal fun StudioLibrary(
    modifier: Modifier, favorites: List<VideoPreview>, recent: List<VideoPreview>,
    names: List<String>,
    libraryRevision: Int, roomPreviewActive: Boolean,
    smartCollections: List<SmartLibraryCollection>,
    folderSnapshot: PlaylistFolderSnapshot,
    pinnedPlaylistNames: List<String>,
    onTogglePinPlaylist: (String) -> Unit,
    playingUrl: String,
    selectedPlaylist: String?, onSelectPlaylist: (String?) -> Unit,
    getPlaylist: (String) -> List<VideoPreview>, onPlay: (VideoPreview, List<VideoPreview>) -> Unit,
    onQuickPlay: (List<VideoPreview>) -> Unit,
    onManage: () -> Unit, onSearch: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onManageFolders: () -> Unit, onMovePlaylist: (String) -> Unit,
    onOffline: () -> Unit,
    offlineResumeTitle: String?, offlineResumePositionMs: Long,
    onResumeOffline: () -> Unit,
    onSongOptions: (VideoPreview) -> Unit,
    onRemoveRecent: (VideoPreview) -> Unit, onClearRecent: () -> Unit,
    onPlaylistSongOptions: (String, VideoPreview) -> Unit,
    onSharePlaylist: (String, List<VideoPreview>) -> Unit,
    onExportPlaylist: (String, List<VideoPreview>) -> Unit,
    onBulkShareLinks: (List<VideoPreview>, Boolean) -> Unit,
    onBulkRemove: (String, List<VideoPreview>) -> Unit,
    onBulkCopy: (String, List<VideoPreview>) -> Unit,
    onMovePlaylistSong: (String, String, Int, List<String>) -> Unit,
    playlistUndo: PlaylistUndoState?,
    onUndoPlaylistEdit: () -> Unit,
    onDismissPlaylistUndo: () -> Unit,
    onImportSharedPlaylist: () -> Unit
) {
    // The library's name/order and song lists are read-only. Cache snapshots
    // by a real library/preview revision, not the per-750ms playback UI ticker.
    val playlistPreviews = remember(names, libraryRevision, roomPreviewActive) {
        names.map { name -> LibraryPlaylistPreview(name, getPlaylist(name)) }
    }
    val overview = remember(favorites, recent, playlistPreviews) {
        LibraryCollectionPresentationPolicy.overview(favorites, recent, playlistPreviews)
    }
    val automaticDetails = remember(smartCollections, favorites, recent, playlistPreviews) {
        LibraryCollectionPresentationPolicy.automaticDetails(smartCollections, favorites, recent, playlistPreviews)
    }
    var automaticCollectionsOpen by rememberSaveable { mutableStateOf(true) }
    var playlistFilter by rememberSaveable { mutableStateOf("") }
    var playlistSort by rememberSaveable { mutableStateOf(LibraryBrowsePolicy.SORT_SAVED) }
    var showFilledOnly by rememberSaveable { mutableStateOf(false) }
    var confirmClearHistory by rememberSaveable(selectedPlaylist) {
        mutableStateOf(false)
    }
    var playlistToolsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    val validFolder = selectedFolder?.takeIf { folder ->
        folder == "nightwave://folder/unfiled" || folder in folderSnapshot.folders
    }
    val visiblePlaylists = remember(playlistPreviews, playlistFilter,
        playlistSort, showFilledOnly, validFolder, folderSnapshot, pinnedPlaylistNames) {
        val filtered = LibraryBrowsePolicy.playlists(playlistPreviews, playlistFilter,
            playlistSort, showFilledOnly, matchContainedSongs = true).filter { preview ->
            when (validFolder) {
                null -> true
                "nightwave://folder/unfiled" -> folderSnapshot.folderOf(preview.name) == null
                else -> folderSnapshot.folderOf(preview.name) == validFolder
            }
        }
        PlaylistPinPolicy.pinnedFirst(filtered, pinnedPlaylistNames) { it.name }
    }
    val selectedSmart = smartCollections.firstOrNull { it.id == selectedPlaylist }
    val likedTracks = remember(favorites) { LibraryCollectionPresentationPolicy.displaySongs(favorites) }
    val historyTracks = remember(recent) { LibraryCollectionPresentationPolicy.displaySongs(recent) }
    val tracks = when {
        selectedPlaylist == null || selectedPlaylist == LIKED_COLLECTION -> likedTracks
        // Library must show the entire real history (unlike Home's 100-slot
        // rail), but use unique IDs for stable LazyColumn keys after restores.
        selectedPlaylist == RECENT_COLLECTION ->
            historyTracks
        selectedSmart != null -> selectedSmart.songs
        else -> playlistPreviews.firstOrNull { it.name == selectedPlaylist }?.songs.orEmpty()
    }
    var localFilter by rememberSaveable(selectedPlaylist) { mutableStateOf("") }
    var sortMode by rememberSaveable(selectedPlaylist) {
        mutableStateOf(LibraryBrowsePolicy.SORT_SAVED)
    }
    val orderedTracks = remember(tracks, localFilter, sortMode) {
        if (selectedPlaylist == null) tracks
        else LibraryBrowsePolicy.songs(tracks, localFilter, sortMode)
    }
    val visibleTracks = if (selectedPlaylist == null) tracks.take(5) else orderedTracks
    val canSelectPlaylistTracks = selectedPlaylist != null && selectedPlaylist in names &&
        selectedSmart == null
    var selectingTracks by remember(selectedPlaylist) { mutableStateOf(false) }
    var reorderingTracks by remember(selectedPlaylist) { mutableStateOf(false) }
    var selectedTrackUrls by remember(selectedPlaylist) { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(selectedPlaylist, tracks) {
        // Drop stale checkboxes after another device surface edits this playlist.
        selectedTrackUrls = LibraryBulkSelectionPolicy.sanitize(
            selectedTrackUrls, tracks)
    }
    // A selection remains valid across sorting and filtering. Bulk edits
    // show the full explicitly selected set, including temporarily hidden
    // items, instead of silently dropping them from the submitted edit.
    val selectedTracks = LibraryBulkSelectionPolicy.selected(tracks, selectedTrackUrls)
    val hiddenSelectedCount = LibraryBulkSelectionPolicy.hiddenSelectionCount(
        selectedTrackUrls, orderedTracks)
    val quickPlayableTracks = remember(orderedTracks) {
        LibraryQuickPlayPolicy.plan(orderedTracks, shuffle = false, seed = 0L).songs
    }
    // Collection navigation must not inherit a deep scroll offset from another list.
    // The active collection still restores its position after recreation.
    val libraryListState = rememberSaveable(selectedPlaylist,
        saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    val nw83PageGutter = NightWavePremiumDesignTokens.pageGutterDp(
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()).dp
    LazyColumn(modifier.testTag("nw132-library-list").fillMaxWidth().widthIn(max =
        NightWavePremiumDesignTokens.MAX_CONTENT_WIDTH_DP.dp).fillMaxHeight(),
        state = libraryListState,
        contentPadding = PaddingValues(start = nw83PageGutter, end = nw83PageGutter,
            top = NightWavePremiumDesignTokens.PAGE_TOP_DP.dp,
            bottom = NightWavePremiumDesignTokens.PAGE_BOTTOM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(NightWavePremiumDesignTokens.ITEM_SPACING_DP.dp)) {
        if (playlistUndo != null) {
            item(key = "playlist-undo-banner") {
                Surface(shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(playlistUndo.message,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = onUndoPlaylistEdit,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Undo") }
                        IconButton(onClick = onDismissPlaylistUndo,
                            modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Dismiss playlist Undo")
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPlaylist != null) IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Library")
                }
                Column(Modifier.weight(1f)) {
                    if (selectedPlaylist == null) {
                        NightWaveLibraryOverviewHeader(overview.summary)
                    } else NightWaveCollectionHeader(
                        name = when {
                            selectedPlaylist == LIKED_COLLECTION -> "Liked Songs"
                            selectedPlaylist == RECENT_COLLECTION -> "Recently Played"
                            selectedSmart != null -> selectedSmart.title
                            else -> selectedPlaylist ?: "Your library"
                        },
                        description = when {
                            selectedPlaylist == null -> "Liked songs, playlists and listening history"
                            selectedPlaylist == RECENT_COLLECTION ->
                                "${overview.historySongs} songs · ${overview.historyEntries} history entries · History only"
                            selectedSmart != null -> automaticDetails[selectedSmart.id].orEmpty()
                            selectedPlaylist == LIKED_COLLECTION -> "${overview.likedCount} liked songs · Chosen by you"
                            else -> "${tracks.size} songs · Your playlist"
                        }
                    )
                }
            }
        }
        if (selectedPlaylist == RECENT_COLLECTION && tracks.isNotEmpty()) {
            item(key = "library-clear-recent") {
                OutlinedButton(onClick = { confirmClearHistory = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("nw86-library-clear-recent-request")) {
                    Text("Clear listening history")
                }
                if (confirmClearHistory) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { confirmClearHistory = false },
                        title = { Text("Clear Recently Played?") },
                        text = {
                            Text("Only your listening history is cleared. " +
                                "Liked songs, playlists and offline audio stay saved.")
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                confirmClearHistory = false
                                onClearRecent()
                            }, modifier = Modifier.testTag("nw86-library-clear-recent-confirm")) {
                                Text("Clear history")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = { confirmClearHistory = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
        if (selectedPlaylist != null && tracks.isNotEmpty()) {
            item(key = "library-quick-play-actions") {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedButton(
                            onClick = {
                                val plan = LibraryQuickPlayPolicy.plan(
                                    orderedTracks, shuffle = false, seed = 0L)
                                plan.first?.let { onQuickPlay(plan.songs) }
                            },
                            enabled = quickPlayableTracks.isNotEmpty(),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Play list", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                val plan = LibraryQuickPlayPolicy.plan(
                                    orderedTracks, shuffle = true, seed = System.nanoTime())
                                plan.first?.let { onQuickPlay(plan.songs) }
                            },
                            enabled = quickPlayableTracks.isNotEmpty(),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Shuffle play", maxLines = 1)
                        }
                    }
                    if (orderedTracks.size > LibraryQuickPlayPolicy.MAX_QUEUE) {
                        Text("Playback queues up to 100 visible tracks. Your saved collection stays intact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (canSelectPlaylistTracks) item(key = "nw132-playlist-tools") {
            val name = requireNotNull(selectedPlaylist)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NightWaveLibraryOptions("Playlist options", "nw132-playlist-options", buildList {
                    if (tracks.isNotEmpty()) {
                        add("Share playlist" to { onSharePlaylist(name, tracks) })
                        add("Export playlist .txt" to { onExportPlaylist(name, tracks) })
                    }
                    add((if (name in pinnedPlaylistNames) "Unpin playlist" else "Pin playlist to top") to { onTogglePinPlaylist(name) })
                    add("Change folder" to { onMovePlaylist(name) })
                    add("Rename or delete playlists" to onManage)
                })
                Text((if (name in pinnedPlaylistNames) "Pinned · " else "") +
                    (folderSnapshot.folderOf(name) ?: "Unfiled"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selectedPlaylist == null) {
            item(key = "nw127-library-create") {
                NightWaveActionGroup {
                    OutlinedButton(onClick = onCreatePlaylist,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("nw47-library-new-playlist")) {
                        Text("New playlist")
                    }
                    if (!overview.showSaveHint) TextButton(onClick = onSearch, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Find music")
                    }
                    NightWaveLibraryOptions("Library options", "nw132-library-options", listOf(
                        "Import playlist (.txt or pasted links)" to onImportSharedPlaylist,
                        "Organize playlist folders" to onManageFolders,
                        "Manage playlists" to onManage
                    ))
                }
            }
            // Counts live once in the overview header; collection rows remain real navigation.
            if (offlineResumeTitle != null) {
                item {
                    Card(onClick = onResumeOffline, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("CONTINUE LISTENING · OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text(offlineResumeTitle, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Resume on this device" +
                                (if (offlineResumePositionMs >= 60_000)
                                    " · " + (offlineResumePositionMs / 60_000) + " min" else "") +
                                "  ›", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                MusicCollectionTile("Offline music",
                    "Your imported audio · listen without Wi-Fi",
                    cover = null, onClick = onOffline)
            }
            if (overview.likedCount > 0) item {
                MusicCollectionTile("Liked songs",
                    overview.likedCount.toString() + " liked songs · Chosen by you",
                    cover = likedTracks.firstOrNull()) {
                    onSelectPlaylist(LIKED_COLLECTION)
                }
            }
            if (overview.historyEntries > 0) item(key = "library-recent-collection") {
                MusicCollectionTile("Recently played",
                    NightWaveContentPresentationPolicy.historySummary(overview.historySongs, overview.historyEntries),
                    cover = historyTracks.firstOrNull()) {
                    onSelectPlaylist(RECENT_COLLECTION)
                }
            }
            if (overview.showSaveHint) item(key = "nw132-save-hint") {
                NightWaveLibraryStartCard(hasHistory = overview.historyEntries > 0, onFindMusic = onSearch)
            }
            if (smartCollections.isNotEmpty()) {
                item(key = "smart-collections-section") {
                    NightWaveSectionActions(heading = {
                        SectionHeading("Automatic collections", "Grouped from likes, playlists and history")
                    }, action = {
                        TextButton(onClick = { automaticCollectionsOpen = !automaticCollectionsOpen },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("nw132-automatic-toggle")) {
                            Text(if (automaticCollectionsOpen) "Hide collections" else "Show ${smartCollections.size}")
                        }
                    })
                }
                if (automaticCollectionsOpen) items(smartCollections, key = { it.id }) { collection ->
                    MusicCollectionTile(collection.title,
                        NightWaveContentPresentationPolicy.automaticSummary(automaticDetails[collection.id].orEmpty()),
                        cover = collection.songs.firstOrNull(),
                        collectionKind = collection.id.removePrefix(SmartLibraryPolicy.PREFIX).substringBefore('/')) {
                        onSelectPlaylist(collection.id)
                    }
                }
            }
            if (names.isNotEmpty()) {
                item(key = "playlist-section") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeading("Your playlists",
                            visiblePlaylists.size.toString() + " of " + names.size + " collections")
                        if (folderSnapshot.folders.isNotEmpty()) {
                            Row(Modifier.nightWaveShelfBoundary().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                FilterChip(selected = validFolder == null,
                                    onClick = { selectedFolder = null },
                                    label = { Text("All folders") })
                                FilterChip(
                                    selected = validFolder == "nightwave://folder/unfiled",
                                    onClick = { selectedFolder = "nightwave://folder/unfiled" },
                                    label = { Text("Unfiled") })
                                folderSnapshot.folders.forEach { folder ->
                                    FilterChip(selected = validFolder == folder,
                                        onClick = { selectedFolder = folder },
                                        label = { Text(folder, maxLines = 1) })
                                }
                            }
                        }
                        OutlinedButton(onClick = { playlistToolsOpen = !playlistToolsOpen },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Text(if (playlistToolsOpen) "Hide sorting & filters"
                                else "Sort & filter playlists")
                        }
                    }
                }
                if (playlistToolsOpen) {
                    item(key = "playlist-filter-input") {
                        OutlinedTextField(
                            value = playlistFilter,
                            onValueChange = { playlistFilter = it.take(80) },
                            modifier = Modifier.fillMaxWidth().testTag("nw132-playlist-filter"),
                            shape = RoundedCornerShape(15.dp),
                            singleLine = true,
                            placeholder = { Text("Search playlists or their songs") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (playlistFilter.isNotBlank()) {
                                    IconButton(onClick = { playlistFilter = "" }) {
                                        Icon(Icons.Outlined.Close,
                                            contentDescription = "Clear playlist search")
                                    }
                                }
                            }
                        )
                    }
                    item(key = "playlist-sorting") {
                        Row(Modifier.nightWaveShelfBoundary().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            FilterChip(
                                selected = playlistSort == LibraryBrowsePolicy.SORT_SAVED,
                                onClick = { playlistSort = LibraryBrowsePolicy.SORT_SAVED },
                                label = { Text("Saved order") })
                            FilterChip(
                                selected = playlistSort == LibraryBrowsePolicy.SORT_NAME,
                                onClick = { playlistSort = LibraryBrowsePolicy.SORT_NAME },
                                label = { Text("A–Z") })
                            FilterChip(
                                selected = playlistSort == LibraryBrowsePolicy.SORT_NAME_DESC,
                                onClick = { playlistSort = LibraryBrowsePolicy.SORT_NAME_DESC },
                                label = { Text("Z–A") })
                            FilterChip(
                                selected = playlistSort == LibraryBrowsePolicy.SORT_MOST_TRACKS,
                                onClick = { playlistSort = LibraryBrowsePolicy.SORT_MOST_TRACKS },
                                label = { Text("Most songs") })
                            FilterChip(
                                selected = showFilledOnly,
                                onClick = { showFilledOnly = !showFilledOnly },
                                label = { Text("Hide empty") })
                        }
                    }
                }
                if (visiblePlaylists.isEmpty()) {
                    item(key = "no-matching-playlists") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No playlists match these filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = {
                                playlistFilter = ""
                                playlistSort = LibraryBrowsePolicy.SORT_SAVED
                                showFilledOnly = false
                                selectedFolder = null
                            }) { Text("Show all playlists") }
                        }
                    }
                } else {
                    items(visiblePlaylists, key = { "playlist:" + it.name }) { collection ->
                        val isPinned = collection.name in pinnedPlaylistNames
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.weight(1f)) {
                                MusicCollectionTile(collection.name,
                                    (if (isPinned) "Pinned · " else "") +
                                        collection.size.toString() + " tracks · " +
                                        (folderSnapshot.folderOf(collection.name) ?: "Unfiled"),
                                    cover = collection.cover) {
                                    onSelectPlaylist(collection.name)
                                }
                            }
                            IconButton(
                                onClick = { onTogglePinPlaylist(collection.name) },
                                modifier = Modifier.size(48.dp).semantics {
                                    contentDescription = if (isPinned)
                                        "Unpin playlist " + collection.name
                                    else "Pin playlist " + collection.name
                                }
                            ) {
                                Text(if (isPinned) "★" else "☆",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isPinned) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        if (selectedPlaylist != null || overview.showRecentlyLiked) item {
            SectionHeading(LibraryCollectionPresentationPolicy.songSection(
                overview = selectedPlaylist == null, history = selectedPlaylist == RECENT_COLLECTION,
                automatic = selectedSmart != null, liked = selectedPlaylist == LIKED_COLLECTION),
                if (selectedPlaylist == null) "Your latest five liked songs" else null)
        }
        if (selectedPlaylist != null && tracks.isNotEmpty()) {
            if (canSelectPlaylistTracks) {
                item(key = "playlist-bulk-mode") {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                selectingTracks = !selectingTracks
                                reorderingTracks = false
                                selectedTrackUrls = emptySet()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(if (selectingTracks) "Cancel selection" else "Select songs")
                        }
                        if (!selectingTracks) {
                            OutlinedButton(
                                onClick = {
                                    reorderingTracks = !reorderingTracks
                                    sortMode = LibraryBrowsePolicy.SORT_SAVED
                                    localFilter = ""
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Text(if (reorderingTracks) "Done ordering" else "Reorder")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    selectedTrackUrls = LibraryBulkSelectionPolicy.toggleVisible(
                                        selectedTrackUrls, orderedTracks, tracks)
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    .testTag("nw86-select-visible")) {
                                Text(LibraryBulkSelectionPolicy.visibleActionLabel(
                                    selectedTrackUrls, orderedTracks))
                            }
                        }
                    }
                }
                if (selectingTracks) {
                    item(key = "playlist-bulk-actions") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${selectedTracks.size} / " +
                                    LibraryBulkSelectionPolicy.MAX_SELECTED +
                                    " selected" +
                                    (if (hiddenSelectedCount > 0)
                                        " · $hiddenSelectedCount hidden by filter" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    selectedTrackUrls = emptySet()
                                }, enabled = selectedTrackUrls.isNotEmpty(),
                                    modifier = Modifier.testTag("nw86-clear-selection")) {
                                    Text("Clear")
                                }
                            }
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        selectedPlaylist?.let { onBulkCopy(it, selectedTracks) }
                                    },
                                    enabled = selectedTracks.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("Copy to…", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedPlaylist?.let { onBulkRemove(it, selectedTracks) }
                                    },
                                    enabled = selectedTracks.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("Remove", maxLines = 1)
                                }
                            }
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onBulkShareLinks(selectedTracks, true) },
                                    enabled = selectedTracks.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("Copy links")
                                }
                                OutlinedButton(
                                    onClick = { onBulkShareLinks(selectedTracks, false) },
                                    enabled = selectedTracks.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("Share links")
                                }
                            }
                        }
                    }
                }
            }
            if (!reorderingTracks) {
            item {
                OutlinedTextField(
                    value = localFilter, onValueChange = { localFilter = it.take(80) },
                    modifier = Modifier.fillMaxWidth().testTag("nw132-track-filter"),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    placeholder = { Text("Filter this collection") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (localFilter.isNotBlank()) IconButton(onClick = { localFilter = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear library filter")
                        }
                    }
                )
            }
            item {
                Row(Modifier.nightWaveShelfBoundary().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FilterChip(
                        selected = sortMode == LibraryBrowsePolicy.SORT_SAVED,
                        onClick = { sortMode = LibraryBrowsePolicy.SORT_SAVED },
                        label = { Text(if (selectedPlaylist == RECENT_COLLECTION)
                            "Newest first" else "Saved order") })
                    if (selectedPlaylist == RECENT_COLLECTION) {
                        FilterChip(
                            selected = sortMode == LibraryBrowsePolicy.SORT_OLDEST,
                            onClick = { sortMode = LibraryBrowsePolicy.SORT_OLDEST },
                            label = { Text("Oldest first") })
                    }
                    FilterChip(
                        selected = sortMode == LibraryBrowsePolicy.SORT_TITLE,
                        onClick = { sortMode = LibraryBrowsePolicy.SORT_TITLE },
                        label = { Text("Title A–Z") })
                    FilterChip(
                        selected = sortMode == LibraryBrowsePolicy.SORT_TITLE_DESC,
                        onClick = { sortMode = LibraryBrowsePolicy.SORT_TITLE_DESC },
                        label = { Text("Title Z–A") })
                    FilterChip(
                        selected = sortMode == LibraryBrowsePolicy.SORT_CREATOR,
                        onClick = { sortMode = LibraryBrowsePolicy.SORT_CREATOR },
                        label = { Text("Creator") })
                }
            }
            } else item(key = "playlist-order-help") {
                Text("Drag the ⠿ handle to move multiple rows, or tap ↑ and ↓. " +
                    "Only the saved playlist order changes; audio playback continues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (visibleTracks.isEmpty() && selectedPlaylist != null) item {
            EmptyMusicCard(
                if (selectedPlaylist == RECENT_COLLECTION && tracks.isEmpty())
                    "No recent plays" else if (tracks.isEmpty()) "Nothing here yet"
                    else "No matching tracks",
                if (selectedPlaylist == RECENT_COLLECTION && tracks.isEmpty())
                    "Songs you play appear here. Liked and downloaded music stays saved."
                else if (tracks.isEmpty()) "Save a song to build your collection."
                else "Try another title or artist.",
                if (tracks.isEmpty()) "Find music" else "Clear filter") {
                if (tracks.isEmpty()) onSearch() else localFilter = ""
            }
        } else items(visibleTracks, key = { it.url }) { song ->
            var rowHeightPx by remember(song.url) { mutableStateOf(0) }
            var dragOffsetPx by remember(song.url) { mutableStateOf(0f) }
            Row(Modifier.fillMaxWidth().onSizeChanged { rowHeightPx = it.height },
                verticalAlignment = Alignment.CenterVertically) {
                if (reorderingTracks && canSelectPlaylistTracks) {
                    Box(Modifier.size(48.dp)
                        .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta -> dragOffsetPx += delta },
                            onDragStarted = { dragOffsetPx = 0f },
                            onDragStopped = {
                                val steps = if (rowHeightPx > 0)
                                    PlaylistReorderGesturePolicy.offsetToSteps(
                                        dragOffsetPx, rowHeightPx) else 0
                                dragOffsetPx = 0f
                                if (steps != 0) selectedPlaylist?.let { name ->
                                    onMovePlaylistSong(name, song.url, steps, tracks.map { it.url })
                                }
                            }
                        ).semantics {
                            contentDescription = "Drag " + song.title + " to reorder"
                        },
                        contentAlignment = Alignment.Center) {
                        Text("⠿", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (selectingTracks && canSelectPlaylistTracks) {
                    androidx.compose.material3.Checkbox(
                        checked = song.url in selectedTrackUrls,
                        onCheckedChange = {
                            selectedTrackUrls = LibraryBulkSelectionPolicy.toggle(
                                selectedTrackUrls, song.url, tracks)
                        },
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = "Select " + song.title
                        })
                }
                Box(Modifier.weight(1f)) {
                    MusicTrackTile(song, isPlaying = playingUrl == song.url,
                        surface = NightWaveTrackSurface.LIBRARY,
                        onOptions = {
                            if (selectingTracks && canSelectPlaylistTracks) {
                                selectedTrackUrls = LibraryBulkSelectionPolicy.toggle(
                                    selectedTrackUrls, song.url, tracks)
                            } else if (reorderingTracks && canSelectPlaylistTracks) {
                                Unit
                            } else if (selectedPlaylist != null &&
                                selectedPlaylist in names && selectedSmart == null &&
                                selectedPlaylist != LIKED_COLLECTION) {
                                onPlaylistSongOptions(selectedPlaylist, song)
                            } else onSongOptions(song)
                        },
                        onRemove = if (selectedPlaylist == RECENT_COLLECTION)
                            ({ onRemoveRecent(song) }) else null,
                        onClick = {
                            if (selectingTracks && canSelectPlaylistTracks) {
                                selectedTrackUrls = LibraryBulkSelectionPolicy.toggle(
                                    selectedTrackUrls, song.url, tracks)
                            } else if (reorderingTracks && canSelectPlaylistTracks) {
                                Unit
                            } else onPlay(song,
                                if (selectedPlaylist == null) tracks else orderedTracks)
                        })
                }
                if (reorderingTracks && canSelectPlaylistTracks) {
                    val index = tracks.indexOfFirst { it.url == song.url }
                    Column {
                        IconButton(
                            onClick = {
                                selectedPlaylist?.let { name ->
                                    onMovePlaylistSong(name, song.url, -1, tracks.map { it.url })
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(48.dp).semantics {
                                contentDescription = "Move " + song.title + " up"
                            }) {
                            Text("↑", style = MaterialTheme.typography.titleLarge,
                                color = if (index > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f))
                        }
                        IconButton(
                            onClick = {
                                selectedPlaylist?.let { name ->
                                    onMovePlaylistSong(name, song.url, 1, tracks.map { it.url })
                                }
                            },
                            enabled = index >= 0 && index < tracks.lastIndex,
                            modifier = Modifier.size(48.dp).semantics {
                                contentDescription = "Move " + song.title + " down"
                            }) {
                            Text("↓", style = MaterialTheme.typography.titleLarge,
                                color = if (index >= 0 && index < tracks.lastIndex)
                                    MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioProfile(
    modifier: Modifier, profile: ProfileSettings, save: (ProfileSettings) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onRecoverLastChange: () -> Unit,
    cloud: CloudBackupStatus,
    cloudBusy: Boolean,
    cloudPhase: CloudOperationPhase = CloudOperationPhase.IDLE,
    onCloudConnect: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudPreviewRestore: () -> Unit,
    onCloudCompare: () -> Unit,
    onCloudDisconnect: () -> Unit,
    onCloudHistory: (Boolean) -> Unit,
    onCloudLyrics: (Boolean) -> Unit,
    onCloudPreferences: (Boolean) -> Unit,
    onCloudWifi: (Boolean) -> Unit,
    insights: ListeningInsights,
    memory: ListeningMemorySummary,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onMemoryErase: () -> Unit,
    onMemoryRefresh: () -> Unit,
    playbackState: StudioPlayerState,
    onRecoverPlayback: () -> Unit,
    storageStatus: String, onCheckStorage: () -> Unit,
    roomReadStatus: String, roomPreviewActive: Boolean,
    onEnableRoomPreview: () -> Unit, onUseLegacyReads: () -> Unit,
    onShareDiagnostics: () -> Unit, onOpenClassic: () -> Unit,
    onEqualizer: () -> Unit,
    onOpenAccount: () -> Unit,
    onManageHistory: () -> Unit,
    onOffline: () -> Unit
) {
    var name by rememberSaveable(profile.localId) { mutableStateOf(profile.displayName) }
    var showBuildDetails by rememberSaveable { mutableStateOf(false) }
    if (showBuildDetails) NightWaveBuildDetailsDialog { showBuildDetails = false }
    var editing by rememberSaveable { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    // Keep the everyday You screen legible; full insights, local analytics
    // controls and developer-like playback health are one tap away.
    var showSnapshot by rememberSaveable { mutableStateOf(false) }
    var showListeningMemory by rememberSaveable { mutableStateOf(false) }
    var showPlaybackHealth by rememberSaveable { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val nw83PageGutter = NightWavePremiumDesignTokens.pageGutterDp(
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()).dp
    LazyColumn(
        modifier = modifier.widthIn(max =
            NightWavePremiumDesignTokens.MAX_CONTENT_WIDTH_DP.dp).fillMaxHeight(),
        contentPadding = PaddingValues(start = nw83PageGutter, end = nw83PageGutter,
            top = NightWavePremiumDesignTokens.PAGE_TOP_DP.dp,
            bottom = NightWavePremiumDesignTokens.PAGE_BOTTOM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(NightWavePremiumDesignTokens.ITEM_SPACING_DP.dp)
    ) {
        item {
            NightWaveSectionActions(heading = {
                Text("You", style = MaterialTheme.typography.headlineSmall)
            }, action = {
                Surface(shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                    Text(CloudAccessPolicy.compactBadge(cloud.account.isNotBlank(), cloud.accessState,
                            CloudAccountPresentation.badge(CloudAccountPresentation.stage(cloud))),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            })
        }

        item {
            Card(
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(when (profile.avatarStyle) {
                                "moon" -> "☾"
                                "spark" -> "✦"
                                "prism" -> "◇"
                                "vinyl" -> "◎"
                                "wave" -> "〰"
                                else -> "◉"
                            }, style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Local profile · Your music stays on this device",
                                style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                        TextButton(onClick = {
                            name = profile.displayName
                            editing = !editing
                        }) {
                            Text(if (editing) "Close" else "Edit")
                        }
                    }
                    if (editing) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedTextField(
                            value = name, onValueChange = { name = it.take(40) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("Display name") }
                        )
                        Button(onClick = {
                            val updated = name.trim()
                            if (updated.isNotEmpty()) {
                                save(profile.copy(displayName = updated))
                                editing = false
                            }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = name.trim().isNotEmpty() &&
                                name.trim() != profile.displayName) {
                            Text("Save profile")
                        }
                    }
                }
            }
        }

        item(key = "nw47-you-quick-access") {
            Column(Modifier.fillMaxWidth().testTag("nw47-you-quick-access")) {
                NightWaveActionGroup {
                NightWaveQuickActionPill("Account & backup",
                    Icons.Outlined.Cloud, "nw47-you-account", onOpenAccount)
                NightWaveQuickActionPill("Offline music",
                    Icons.Outlined.LibraryMusic, "nw128-you-offline", onOffline)
                }
            }
        }

        item(key = "nw86-you-drive-status") {
            // Keep Google connection transparent but not a second oversized
            // account dashboard. The quick action above and Manage backups
            // below both retain every existing authorization/recovery control.
            StudioProfileGroup("Google Drive · optional") {
                StudioProfileRow(
                    "Backup connection",
                    if (cloudBusy) CloudRecoveryUiPolicy.busyLabel(cloudPhase)
                    else if (cloud.account.isNotBlank() && cloud.accessState != CloudAccessState.CHECKED)
                        CloudAccessPolicy.label(true, cloud.accessState, cloud.lastAccessCheckMs)
                    else CloudAccountPresentation.title(CloudAccountPresentation.stage(cloud)),
                    Icons.Outlined.Cloud, onOpenAccount,
                    trailing = {
                        Text(if (cloudBusy) "Busy" else "Manage",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                )
                if (cloud.lastResult.isNotBlank() &&
                    cloud.lastResult != "Not connected" && cloud.account.isBlank()) {
                    Text(cloud.lastResult,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                }
            }
        }

        item {
            StudioProfileGroup("Appearance & playback") {
                StudioProfileRow(
                    "Sound Studio", "Equalizer · presets and manual frequency bands",
                    Icons.Outlined.GraphicEq, onEqualizer,
                    modifier = Modifier.testTag("nw47-you-sound")
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StudioProfileRow(
                    "Theme", profile.theme.name.lowercase().replaceFirstChar { it.titlecase() },
                    Icons.Outlined.AutoAwesome, { showAppearance = !showAppearance },
                    trailing = { Text(if (showAppearance) "Hide" else "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary) }
                )
                if (showAppearance) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Colour theme", style = MaterialTheme.typography.labelSmall,
                            color = muted)
                        for (options in NightWaveTheme.entries.chunked(2)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEach { option ->
                                    FilterChip(
                                        selected = profile.theme == option,
                                        onClick = { save(profile.copy(theme = option)) },
                                        label = { Text(option.name.lowercase()
                                            .replaceFirstChar { it.titlecase() }) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Text("Profile icon", style = MaterialTheme.typography.labelSmall,
                            color = muted)
                        Row(Modifier.fillMaxWidth().nightWaveShelfBoundary().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileSettings.allowedAvatars.sorted().forEach { style ->
                                FilterChip(
                                    selected = profile.avatarStyle == style,
                                    onClick = { save(profile.copy(avatarStyle = style)) },
                                    label = { Text(style.replaceFirstChar { it.titlecase() }) }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StudioProfileRow(
                    "Reduced motion", "Minimize screen transitions",
                    Icons.Outlined.Settings,
                    { save(profile.copy(motion =
                        if (profile.motion == MotionPreference.REDUCED)
                            MotionPreference.SYSTEM else MotionPreference.REDUCED)) },
                    trailing = {
                        Switch(checked = profile.motion == MotionPreference.REDUCED,
                            onCheckedChange = {
                                save(profile.copy(motion =
                                    if (it) MotionPreference.REDUCED else MotionPreference.SYSTEM))
                            })
                    }
                )
            }
        }

        item {
            StudioProfileGroup("Your listening") {
                StudioProfileRow("Recent listening",
                    "Artists and songs in your recent history",
                    Icons.Outlined.LibraryMusic, { showSnapshot = !showSnapshot },
                    trailing = {
                        Text(if (showSnapshot) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    })
                if (showSnapshot) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        ListeningInsightsPanel(insights)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StudioProfileRow("Manage Recently Played",
                    "Remove one entry or clear history · Saved music stays",
                    Icons.Outlined.LibraryMusic, onManageHistory,
                    modifier = Modifier.testTag("nw47-you-history"))
            }
        }

        item {
            StudioProfileGroup("Listening memory · optional") {
                StudioProfileRow("Listening statistics",
                    if (memory.enabled) "On · Stored privately on this device"
                    else "Off · Opt in only if you choose",
                    Icons.Outlined.AutoAwesome,
                    { showListeningMemory = !showListeningMemory },
                    trailing = {
                        Text(if (showListeningMemory) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    })
                if (showListeningMemory) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListeningMemoryPanel(
                        memory, onMemoryEnabledChange, onMemoryErase, onMemoryRefresh
                    )
                }
            }
        }

        item {
            StudioProfileGroup("Library & backup") {
                StudioProfileRow("Manage backups", "Export, restore and undo edits",
                    Icons.Outlined.LibraryMusic, { showBackup = !showBackup },
                    trailing = { Text(if (showBackup) "Hide" else "Open",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary) })
                if (showBackup) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StudioProfileRow("Export music library", "Profile, favorites and playlists",
                        Icons.Outlined.LibraryMusic, onExport)
                    StudioProfileRow("Restore from backup", "Replaces collections after confirmation",
                        Icons.Outlined.LibraryMusic, onImport)
                    StudioProfileRow("Undo most recent library edit", "Restore last protected change",
                        Icons.AutoMirrored.Outlined.ArrowBack, onRecoverLastChange)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("GOOGLE DRIVE BACKUP", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp, top = 6.dp))
                    Text(if (cloud.account.isBlank()) "Not connected"
                        else "Connected: " +
                            CloudAccountPresentation.maskedAccount(cloud.account),
                        modifier = Modifier.padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodySmall)
                    Text(cloud.lastResult + if (cloud.lastSuccessMs > 0L)
                        " · Last verified: " + java.text.DateFormat.getDateTimeInstance()
                            .format(java.util.Date(cloud.lastSuccessMs)) else "",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                    StudioProfileRow("Open account & backup", "Connection, backup status and privacy",
                        Icons.Outlined.Settings, onOpenAccount)
                    if (cloud.account.isNotBlank()) {
                        StudioProfileRow("Back up now", "Verified, versioned Google Drive snapshot",
                            Icons.Outlined.LibraryMusic, onCloudBackup)
                        StudioProfileRow("Preview cloud restore", "Browse recovery points and review what changes",
                            Icons.Outlined.LibraryMusic, onCloudPreviewRestore)
                        StudioProfileRow("Compare & merge devices",
                            "Add saved music without replacing local playlists",
                            Icons.Outlined.LibraryMusic, onCloudCompare)
                        StudioProfileRow("Back up listening history",
                            "Off by default; old snapshots keep their previous contents",
                            Icons.Outlined.Settings, { onCloudHistory(!cloud.includeHistory) },
                            trailing = {
                                Switch(checked = cloud.includeHistory,
                                    onCheckedChange = onCloudHistory)
                            })
                        StudioProfileRow("Back up imported LRC lyrics",
                            "Off by default · max 20 entries · audio not included",
                            Icons.Outlined.Settings, { onCloudLyrics(!cloud.includeLyrics) },
                            trailing = {
                                Switch(checked = cloud.includeLyrics,
                                    onCheckedChange = onCloudLyrics)
                            })
                        StudioProfileRow("Back up playback preferences",
                            "Off by default · excludes account and privacy choices",
                            Icons.Outlined.Settings,
                            { onCloudPreferences(!cloud.includePreferences) },
                            trailing = {
                                Switch(checked = cloud.includePreferences,
                                    onCheckedChange = onCloudPreferences)
                            })
                        StudioProfileRow("Wi-Fi-only automatic backup",
                            "Daily scheduling after first successful backup",
                            Icons.Outlined.Settings, { onCloudWifi(!cloud.wifiOnly) },
                            trailing = {
                                Switch(checked = cloud.wifiOnly,
                                    onCheckedChange = onCloudWifi)
                            })
                        StudioProfileRow("Disconnect or change Drive", "Review the effect on your local library",
                            Icons.AutoMirrored.Outlined.ArrowBack, onOpenAccount)
                    }
                    if (cloudBusy) Text(CloudRecoveryUiPolicy.busyLabel(cloudPhase),
                        modifier = Modifier.padding(horizontal = 14.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                    Text("Backups contain metadata, never offline audio or passwords. " +
                        "Google consent is optional. Manual compare is not live device sync. " +
                        "Automatic backup may run later than scheduled.",
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall, color = muted)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                StudioProfileRow("Advanced storage", "Check storage without changing your music",
                    Icons.Outlined.Settings, { showAdvanced = !showAdvanced },
                    trailing = { Text(if (showAdvanced) "Hide" else "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary) })
                if (showAdvanced) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(storageStatus, style = MaterialTheme.typography.bodySmall)
                        Text(roomReadStatus, style = MaterialTheme.typography.bodySmall,
                            color = muted)
                        Text("Room and DataStore are optional read-only mirrors. " +
                            "Your existing saved library stays in use.",
                            style = MaterialTheme.typography.bodySmall, color = muted)
                        OutlinedButton(onClick = onCheckStorage,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("RECHECK NEW STORAGE")
                        }
                        OutlinedButton(
                            onClick = if (roomPreviewActive) onUseLegacyReads
                                else onEnableRoomPreview,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (roomPreviewActive) "RETURN TO ORIGINAL LIBRARY"
                                else "TRY VERIFIED ROOM READS")
                        }
                        Text("Cloud recovery requires a Google-authorized, validated snapshot and " +
                            "always asks before replacing local collections.",
                            style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        }

        item {
            StudioProfileGroup("App health") {
                StudioProfileRow("Playback health",
                    if (!playbackState.error.isNullOrBlank() || !playbackState.connected)
                        "Action may be needed · Tap to inspect"
                    else "Current status and recovery controls",
                    Icons.Outlined.Settings,
                    { showPlaybackHealth = !showPlaybackHealth },
                    trailing = {
                        Text(if (showPlaybackHealth) "Hide" else "View",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    })
                if (showPlaybackHealth || !playbackState.connected ||
                    !playbackState.error.isNullOrBlank()) {
                    PlaybackHealthPanel(playbackState, onRecoverPlayback)
                }
                StudioProfileRow("Crash diagnostics", "Preview before sharing any report",
                    Icons.Outlined.Settings, onShareDiagnostics)
                StudioProfileRow("App details", "Version and build ID for support",
                    Icons.Outlined.Settings, { showBuildDetails = true })
            }
        }
        item {
            StudioProfileGroup("Interface") {
                StudioProfileRow("Open classic interface", "Temporary fallback · Shared library",
                    Icons.AutoMirrored.Outlined.ArrowBack, onOpenClassic)
            }
        }
        item {
            Text("NightWave · Your music stays on this device unless you choose to export it.",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}

@Composable
private fun StudioProfileGroup(
    title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    // Settings are an editorial list, not a stack of oversized cards.
    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 0.dp)) {
        Text(title,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = .21f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp))
        content()
    }
}

@Composable
private fun StudioProfileRow(
    title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit, modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .09f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = .90f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) trailing()
        else Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge)
    }
}
