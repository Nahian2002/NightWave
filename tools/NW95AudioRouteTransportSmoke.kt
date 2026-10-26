import com.nightwave.player.playback.AudioRouteTransportPolicy as P

private fun expect(key: P.Key, selected: Boolean, loaded: Boolean, offline: Boolean,
                   wants: Boolean, result: P.Action, label: String) {
    check(P.decide(key, selected, loaded, offline, wants) == result) { label }
}
fun main() {
    var n = 0
    fun checkCase(key: P.Key, selected: Boolean, loaded: Boolean, offline: Boolean,
                  wants: Boolean, result: P.Action, label: String) {
        expect(key, selected, loaded, offline, wants, result, label); n++
    }
    checkCase(P.Key.TOGGLE, true, false, false, true, P.Action.PAUSE_PENDING, "pause unresolved stream")
    checkCase(P.Key.TOGGLE, true, false, false, false, P.Action.RESUME_SAVED, "resume paused resolver")
    checkCase(P.Key.PLAY, true, false, false, true, P.Action.RESUME_SAVED, "idempotent explicit play")
    checkCase(P.Key.PLAY, true, false, false, false, P.Action.RESUME_SAVED, "restart from queue")
    checkCase(P.Key.PAUSE, true, false, false, true, P.Action.PAUSE_PENDING, "explicit pause unresolved")
    checkCase(P.Key.PAUSE, true, false, false, false, P.Action.DELEGATE, "already paused")
    checkCase(P.Key.TOGGLE, true, true, false, true, P.Action.DELEGATE, "loaded play pause")
    checkCase(P.Key.PAUSE, true, true, false, true, P.Action.DELEGATE, "loaded pause")
    checkCase(P.Key.PLAY, true, true, false, false, P.Action.DELEGATE, "loaded play")
    checkCase(P.Key.TOGGLE, true, false, true, false, P.Action.DELEGATE, "offline route")
    checkCase(P.Key.TOGGLE, false, false, false, true, P.Action.DELEGATE, "empty queue")
    checkCase(P.Key.PAUSE, false, false, false, true, P.Action.DELEGATE, "no saved song")
    println("NW95_AUDIO_ROUTE_TRANSPORT_SMOKE_PASS scenarios=$n")
}
