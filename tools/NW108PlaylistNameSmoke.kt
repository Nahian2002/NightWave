import com.nightwave.player.v2.data.PlaylistNameIdentity as P
import java.util.Locale

fun main() {
    var checks=0
    fun eq(actual: Any?, expected: Any?, label: String) {
        check(actual==expected) { "$label: expected $expected got $actual" };checks++
    }
    val prev=Locale.getDefault()
    try {
        for(lang in listOf("en-US","tr-TR","az-AZ","lt-LT","bn-BD")) {
            Locale.setDefault(Locale.forLanguageTag(lang))
            eq(P.same("I","i"),true,"I-i $lang")
            eq(P.same("İ","i"),true,"İ-i $lang")
            eq(P.same("I","ı"),true,"I-ı $lang")
            eq(P.allDistinct(listOf("I","i")),false,"duplicate $lang")
            eq(P.contains(listOf("Mix","Workout"),"mIX"),true,"contained $lang")
        }
        eq(P.allDistinct((1..50).map{"Playlist $it"}),true,"limit distinct")
        eq(P.allDistinct((1..50).map{if(it==50)"pLAYLIST 1" else "Playlist $it"}),false,"limit dup")
        eq(P.allDistinct(listOf("İ","i")),false,"dotted collision")
        eq(P.allDistinct(emptyList()),true,"empty")
        eq(P.same("My Mix","Different"),false,"different")
    } finally {Locale.setDefault(prev)}
    println("NW108_PLAYLIST_LOCALE_SMOKE_PASS checks=$checks")
}
