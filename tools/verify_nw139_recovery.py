"""NW139 exact caller fences. Source/host checks do not execute a live account restore."""
from pathlib import Path
import argparse

ROOT=Path(__file__).resolve().parents[1]
BASE='app/src/main/java/com/nightwave/player/v2/'
FILES={
 'policy':BASE+'cloud/CloudRecoverySessionPolicy.kt',
 'store':BASE+'cloud/CloudBackupState.kt',
 'worker':BASE+'cloud/NightWaveCloudWorker.kt',
 'activity':BASE+'ui/NightWaveStudioActivity.kt',
 'hub':BASE+'ui/NightWaveAccountHub.kt',
 'profile':BASE+'data/GuestProfileStore.kt',
 'archive':BASE+'data/LibraryArchiveManager.kt',
 'client':BASE+'cloud/NightWaveDriveClient.kt',
}
REQUIRED={
 'policy':['expected.accessRevision > 0L && expected.accessRevision == current.accessRevision',
  'current.accessState == CloudAccessState.CHECKED && current.lastAccessCheckMs > 0L',
  'CloudBackupConsentPolicy.same(expected.consent, current.consentSnapshot())',
  '!busy && matches(expected, current)', 'CloudOperationPhase.TRANSFERRING_BACKUP',
  'Checking or uploading your recovery copy'],
 'client':['val remote = try { download(latest.id) } catch (error: DriveHttpFailure)', 'if (error.statusCode == 404) null else throw error'],
 'store':['expected.accessRevision != previous.accessRevision', 'expected.consentSnapshot() != previous.consentSnapshot()', 'withRecoverySession(expected: CloudRecoverySession, commit: () -> T)',
  'if (!CloudRecoverySessionPolicy.matches(expected, readLocked())) return@synchronized null',
  '(expectedAccessRevision != null && expectedAccessRevision != current.accessRevision)',
  'private val guard = Any()', 'fun successIfCurrent(', 'fun statusIfCurrent('],
 'worker':['expectedAccessRevision = accessRevision', 'beforeUpload.accessRevision != accessRevision',
  'val failureRevision = store.recordAccessIfCurrent(', 'failureRevision)',
  'store.successIfCurrent(consent, System.currentTimeMillis(), outcome.created,',
  'throw cancel', 'backUpIfChangedVerified(payload)'],
 'activity':['CloudAuthorizationReceiptRetainedState : ViewModel()',
  'cloudAuthorizationRetained.start', 'cloudStore.connect(account, expected = verifiedState)', 'expectedAccessRevision = verifiedState.accessRevision', 'val verificationState = cloudRequestStart',
  'The permission request expired. Start the Drive action again.',
  'verificationState.accessRevision != current.accessRevision',
  'expectedAccessRevision = verificationState.accessRevision',
  'CloudRecoverySession(operationConsent, accessRevision)',
  'expectedSession = recoverySession', 'CloudRecoverySessionPolicy.mayApply(',
  'CloudRecoverySessionPolicy.matches(', 'cloudStore.withRecoverySession(expectedSession)',
  'archiveManager.mergeIfUnchanged(plan)', 'report.expectedLocalBackup, preview.archive, history',
  'profiles.saveIfUnchanged(expectedProfile, restored.applyTo(expectedProfile))',
  'lyricsStore.restoreCloudMissing(backup.lyrics)',
  'expectedAccessRevision = accessRevision', 'failureAccessRevision = failureRevision',
  'cloudStore.statusIfCurrent(operationConsent, message, failureAccessRevision)',
  'val cancelled = stage != CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK',
  'val receipt = if (cancelled) current.accessRevision',
  'CloudOperationPhase.AUTHORIZING', 'CloudOperationPhase.AWAITING_PERMISSION',
  'CloudOperationPhase.CHECKING_ACCOUNT', 'CloudOperationPhase.PREPARING_BACKUP',
  'CloudOperationPhase.TRANSFERRING_BACKUP', 'CloudOperationPhase.OPENING_BACKUP',
  'CloudOperationPhase.MERGING', 'CloudOperationPhase.RESTORING',
  'CloudOperationPhase.APPLYING_OPTIONS', 'phase = cloudPhase'],
 'hub':['strings.cloudBusy(phase)', 'nw139-primary-recovery',
  'R.string.nw141_library_metadata_backup_audio_files_stay_on_this_a80a5',
  'if (showBackupOptions)', 'openExternalFlow(onExportLocal)', 'openExternalFlow(onPreviewRestore)',
  'openExternalFlow(onCompare)', 'confirmDisconnect = true', 'confirmChangeAccount = true',
  'onHistory', 'onLyrics', 'onPreferences', 'onWifiOnly',
  'showConnectionHelp || CloudAccessPolicy.needsHelp', 'liveRegion = LiveRegionMode.Polite'],
 'profile':['private val guard = Any()', 'fun saveIfUnchanged(',
  'if (loadLocked() != expected) return@synchronized false', 'saveLocked(updated)',
  'existingId != clean.localId'],
 'archive':['LegacyLibraryTransaction.exclusive', 'current.contentEquals(plan.expectedLocalBackup)',
  'current.contentEquals(expectedLocalBackup)', 'previous_library', 'restoreWithConfirmation(archive, restoredHistory)'],
}

def verify(s):
 for name,markers in REQUIRED.items():
  for marker in markers: assert marker in s[name],(name,marker)
 assert s['store'].count('(expectedAccessRevision != null && expectedAccessRevision != current.accessRevision)') >= 3
 assert s['activity'].count('cloudStore.withRecoverySession(expectedSession)') == 3
 a=s['activity'];section=a[a.index('private fun showCloudSnapshotPicker('):a.index('private fun disconnectCloud()')]
 assert 'expectedSession: CloudBackupConsent' not in section
 assert 'cloudStore.status(' not in section
 for forbidden in ['getSharedPreferences', 'NightWaveDriveClient(', 'accessToken', 'startService']:
  assert forbidden not in s['policy'], forbidden
 assert 'Your music. Your library.' not in s['hub']

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true');args=p.parse_args()
 source={k:(ROOT/v).read_text(encoding='utf-8') for k,v in FILES.items()};verify(source)
 if args.self_test:
  for name,markers in REQUIRED.items():
   for marker in markers:
    broken=dict(source);broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
    try:verify(broken)
    except (AssertionError,ValueError):continue
    raise AssertionError('Accepted broken caller: '+name+': '+marker)
 print('PASS NW139 exact consent/access receipt, guarded local commit/profile CAS, scoped worker/interactive receipts, compact explicit account flows; live/physical NOT_RUN')
