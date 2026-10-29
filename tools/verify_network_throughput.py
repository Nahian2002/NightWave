"""NETWORK01 actual caller guard. Does not certify Internet bandwidth or phone buffering."""
from pathlib import Path
import argparse

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/nightwave/player'
FILES=('playback/PlaybackService.kt','playback/PlaybackMediaNetwork.kt','net/MediaTransferIo.kt',
       'offline/PermittedAudioDownloader.kt','offline/OfflineAudioLibrary.kt')

def verify(src):
    player,network,io,download,library=(src[n] for n in FILES)
    for marker in ('OkHttpDataSource.Factory(PlaybackMediaNetwork.client)',
                   'DefaultDataSource.Factory(this, networkSource)', 'setLoadControl(DefaultLoadControl.Builder()',
                   'PlaybackMediaNetwork.TARGET_BYTES', 'setPrioritizeTimeOverSizeThresholds(false)'):
        assert marker in player,marker
    assert 'media3-datasource-okhttp:1.6.1' in (ROOT/'app/build.gradle.kts').read_text()
    assert 'val client: OkHttpClient by lazy' in network
    assert 'MAX_BUFFER_MS = 120_000' in network and 'TARGET_BYTES = 16 * 1024 * 1024' in network
    for forbidden in ('CookieJar(','.cache(', 'bindProcessToNetwork', 'Authorization', '.callTimeout('):
        assert forbidden not in network,forbidden
    assert io.index('val result = write(buffered)') < io.index('buffered.flush()') < io.index('return result')
    assert 'BufferedOutputStream(output, BUFFER_BYTES)' in io
    assert 'BUFFER_BYTES = 256 * 1024' in io
    assert 'companion object {' in download and download.index('private val client =') < download.index('fun cancel()')
    for marker in ('resolved.all(::isPublicAddress)', '.followRedirects(false)', '.followSslRedirects(false)',
                   'currentCall?.cancel()', 'AudioDownloadNetworkPolicy.isFullStartResponse(',
                   'SearchTrackSavePolicy.safeAudioEndpoint(', 'inspectAudioOnly(pending)',
                   'OfflineSaveStoragePolicy.canContinue(', 'pending.delete()',
                   'MediaTransferIo.bufferedWrite(output) { buffered ->', 'buffered.write(buffer, 0, n)'):
        assert marker in download,marker
    for text in (download,library):
        assert text.index('MediaTransferIo.bufferedWrite(output)') < text.index('output.fd.sync()')
    assert 'MediaTransferIo.bufferedWrite(output) { buffered ->' in library
    # NW136 introduces Range only through its independently guarded checkpoint
    # engine; the original full-start response policy remains the fallback.
    assert 'AudioRangeTransfer(File(home, "audio_transfer_checkpoints"), metrics' in download
    for forbidden in ('dispatcher.cancelAll', 'Thread.sleep(', 'requestNetwork(', 'setRate('):
        assert forbidden not in download and forbidden not in network,forbidden

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--self-test',action='store_true'); args=p.parse_args()
    src={n:(BASE/n).read_text(encoding='utf-8') for n in FILES};verify(src)
    if args.self_test:
        for name,marker in ((FILES[0],'OkHttpDataSource.Factory(PlaybackMediaNetwork.client)'),
                            (FILES[0],'DefaultDataSource.Factory(this, networkSource)'),
                            (FILES[0],'setPrioritizeTimeOverSizeThresholds(false)'),
                            (FILES[2],'buffered.flush()'),
                            (FILES[3],'resolved.all(::isPublicAddress)'),
                            (FILES[3],'currentCall?.cancel()'),
                            (FILES[3],'inspectAudioOnly(pending)'),
                            (FILES[4],'MediaTransferIo.bufferedWrite(output) { buffered ->')):
            broken=dict(src);broken[name]=broken[name].replace(marker,'BROKEN_FIXTURE')
            try:verify(broken)
            except (AssertionError,ValueError):continue
            raise AssertionError('Accepted broken network caller: '+marker)
    print('PASS pooled HTTP2-capable playback, bounded read-ahead, flush-before-sync I/O and unchanged source/cancel/privacy guards; phone throughput NOT_RUN')
