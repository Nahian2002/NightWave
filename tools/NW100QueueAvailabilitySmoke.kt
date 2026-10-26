import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.QueueEntry
import com.nightwave.player.offline.OfflinePlaybackQueue

fun main() {
    var n = 0
    fun expect(actual: Any?, wanted: Any?, scenario: String) {
        check(actual == wanted) { "$scenario expected=$wanted actual=$actual" }
        n++
    }
    fun track(i: Int) = QueueEntry("https://www.youtube.com/watch?v=" +
        i.toString().padStart(11,'0'), "Song $i", "Artist", "")
    fun publicQueue() = PlaybackQueue(pick={0}).apply {
        replace((1..4).map(::track), 0)
    }
    fun offlineQueue() = OfflinePlaybackQueue(pick={0}).apply {
        select(listOf("a","b","c","d"), "a")
    }
    run {
        val q=PlaybackQueue()
        expect(q.hasNext(),false,"public cold no-op")
        expect(q.next(),null,"public cold navigation")
    }
    run {
        val q=publicQueue()
        expect(q.hasNext(),true,"public start Next")
        expect(q.next(),track(2),"public linear Next")
        q.jump(3)
        expect(q.hasNext(),false,"public Repeat Off end")
        expect(q.next(),null,"public end no-op")
        q.repeat=PlaybackQueue.Repeat.ONE
        expect(q.hasNext(),false,"manual skip ignores Repeat One")
        q.repeat=PlaybackQueue.Repeat.ALL
        expect(q.hasNext(),true,"Repeat All wrap")
        expect(q.next(),track(1),"public wrapped track")
    }
    run {
        val q=publicQueue()
        q.shuffle=true
        repeat(3){
            expect(q.hasNext(),true,"public unseen random available")
            expect(q.next()!=null,true,"public random Next")
        }
        expect(q.hasNext(),false,"public shuffle exhausted")
        expect(q.next(),null,"public no silent reroll")
        q.repeat=PlaybackQueue.Repeat.ALL
        expect(q.hasNext(),true,"public repeat all cycle")
        expect(q.next()!=null,true,"public new shuffle cycle")
    }
    run {
        val q=publicQueue()
        q.shuffle=true
        val song=q.next()
        q.previous()
        val before=q.shuffleRecoveryState()
        expect(q.hasNext(),true,"public forward retrace")
        expect(q.hasNext(),true,"public idempotent probe")
        expect(q.shuffleRecoveryState(),before,"public history not consumed")
        expect(q.next(),song,"public actual forward")
    }
    run {
        val q=publicQueue()
        q.shuffle=true
        q.insertNext(track(11))
        val priorities=q.priorityNextUrls()
        repeat(3){expect(q.hasNext(),true,"public manual priority")}
        expect(q.priorityNextUrls(),priorities,"priority not consumed")
        expect(q.next(),track(11),"priority still plays")
    }
    run {
        val q=offlineQueue()
        expect(q.hasNext(),true,"offline linear start")
        expect(q.next(false),"b","offline linear Next")
        q.jump(3)
        expect(q.hasNext(),false,"offline ended Repeat Off")
        expect(q.next(false),null,"offline end no-op")
        q.setRepeat(PlaybackQueue.Repeat.ONE)
        expect(q.hasNext(),false,"offline manual skips Repeat One")
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        expect(q.hasNext(),true,"offline repeat all wrap")
        expect(q.next(false),"a","offline wrapped track")
    }
    run {
        val q=offlineQueue()
        q.setShuffle(true)
        val heard=q.next(false)
        q.previous()
        repeat(3){expect(q.hasNext(),true,"offline forward still exists")}
        expect(q.next(false),heard,"offline forward preserved")
        repeat(2){q.next(false)}
        expect(q.hasNext(),false,"offline shuffle exhausted")
        expect(q.next(false),null,"offline exhausted no reroll")
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        expect(q.hasNext(),true,"offline shuffle new cycle")
    }
    run {
        val q=offlineQueue()
        q.setShuffle(true)
        q.next(false)
        q.previous()
        q.jump(3)
        expect(q.hasNext(),true,"offline branch has unseen c")
        expect(q.next(false),"c","offline branch picks c")
        expect(q.hasNext(),false,"offline branch exhausts")
    }
    println("NW100_QUEUE_NEXT_AVAILABILITY_SMOKE_PASS assertions=$n")
}
