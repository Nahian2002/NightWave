import com.nightwave.player.playback.PlaybackResolutionPolicy as P
import com.nightwave.player.widget.WidgetPlaybackIntentPolicy as W

fun main() {
    var n = 0
    fun expect(actual: Any?, expected: Any?, label: String) {
        check(actual == expected) { "$label: expected=$expected actual=$actual" }; n++
    }
    expect(W.command(true, true, true), W.Command.PAUSE, "buffering pending play")
    expect(W.command(true, false, true), W.Command.PLAY, "paused media")
    expect(W.command(false, true, true), W.Command.NONE, "lookup without media")
    expect(W.command(false, false, true), W.Command.NONE, "dead service session")
    expect(W.command(true, true, false), W.Command.NONE, "unavailable transport")
    expect(W.command(true, false, false), W.Command.NONE, "unavailable play")
    expect(W.showsPauseIcon(true, true), true, "buffering pause icon")
    expect(W.showsPauseIcon(true, false), false, "paused play icon")
    expect(W.showsPauseIcon(false, true), false, "no-media icon")
    expect(P.shouldResumePendingExplicitPlay(true,true,false,true,false),true,"explicit resume")
    expect(P.shouldResumePendingExplicitPlay(true,true,false,true,true),false,"already wants play")
    expect(P.shouldResumePendingExplicitPlay(false,true,false,true,false),false,"queue only")
    expect(P.shouldResumePendingExplicitPlay(true,false,false,true,false),false,"wrong selection")
    expect(P.shouldResumePendingExplicitPlay(true,true,true,true,false),false,"loaded")
    expect(P.shouldResumePendingExplicitPlay(true,true,false,false,false),false,"not resolving")
    expect(P.acceptsResult(4,4,"a","a",false),true,"valid extraction")
    expect(P.acceptsResult(3,4,"a","a",false),false,"stale generation")
    expect(P.acceptsResult(4,4,"a","b",false),false,"stale selected ID")
    for (loaded in listOf(false,true)) for (intent in listOf(false,true)) {
        expect(W.showsPauseIcon(loaded,intent),
            W.command(loaded,intent,true)==W.Command.PAUSE,
            "widget button matches icon")
    }
    println("NW98_BACKGROUND_INTENT_SMOKE_PASS scenarios=$n")
}
