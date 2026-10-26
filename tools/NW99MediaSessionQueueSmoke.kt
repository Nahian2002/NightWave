import com.nightwave.player.playback.MediaSessionQueueControlPolicy as P

fun main() {
    var n = 0
    fun verify(actual: Boolean, expected: Boolean, label: String) {
        check(actual == expected) { "$label: wanted $expected got $actual" }; n++
    }
    verify(P.trustedController(true,false),true,"own widget")
    verify(P.trustedController(false,true),true,"media notification")
    verify(P.trustedController(true,true),true,"same app notification")
    verify(P.trustedController(false,false),false,"foreign controller")
    verify(P.knownAction("NEXT","NEXT","PREV"),true,"explicit next")
    verify(P.knownAction("PREV","NEXT","PREV"),true,"explicit previous")
    verify(P.knownAction("SEEK","NEXT","PREV"),false,"unknown action")
    verify(P.knownAction(null,"NEXT","PREV"),false,"null action")
    verify(P.knownAction("NEXT","","PREV"),false,"blank next")
    verify(P.knownAction("NEXT","NEXT","NEXT"),false,"indistinct names")
    verify(P.canNavigate(false,false,true),true,"selected public track")
    verify(P.canNavigate(true,true,false),true,"selected private offline track")
    verify(P.canNavigate(true,true,true),true,"offline selection wins")
    verify(P.canNavigate(false,true,true),true,"public selection wins")
    verify(P.canNavigate(false,false,false),false,"cold session")
    verify(P.canNavigate(true,false,false),false,"missing offline selection")
    verify(P.canNavigate(true,false,true),false,"cannot cross from public to offline")
    verify(P.canNavigate(false,true,false),false,"cannot cross from offline to public")
    println("NW99_MEDIA_SESSION_QUEUE_SMOKE_PASS scenarios=$n")
}
