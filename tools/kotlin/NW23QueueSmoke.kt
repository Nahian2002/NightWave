package com.nightwave.player.offline

/** Runs with the actual GitHub production policy files; no Android runtime. */
object NW23QueueSmoke {
    @JvmStatic fun main(args: Array<String>) {
        var checks = 0
        fun confirm(ok: Boolean) {
            check(ok) { "NW23 queue identity invariant failed at " + (checks + 1) }
            checks++
        }
        fun blocked(block: () -> Unit) {
            confirm(runCatching(block).isFailure)
        }
        val source = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Song.ogg"
        val source2 = "https://upload.wikimedia.org/wikipedia/commons/a/ab/Other.ogg"
        val original = "0".repeat(31)
        val key = CatalogWorkIdentity.uniqueName(source, original)
        confirm(key == CatalogWorkIdentity.uniqueName(source, original))
        confirm(key.startsWith("nw23-"))
        confirm(key.length == 41)
        confirm(key.drop(5).all { it in '0'..'9' || it in 'a'..'f' })
        confirm("Song" !in key && "wikimedia" !in key)
        confirm(key != CatalogWorkIdentity.uniqueName(source2, original))
        confirm(key != CatalogWorkIdentity.uniqueName(source, "1".repeat(31)))
        blocked {
            CatalogWorkIdentity.uniqueName("https://upload.wikimedia.org.attacker.invalid/f.ogg", original)
        }
        blocked {
            CatalogWorkIdentity.uniqueName("http://upload.wikimedia.org/wikipedia/commons/a.ogg", original)
        }
        blocked { CatalogWorkIdentity.uniqueName(source, "bad hash") }
        confirm(FreeAudioCatalogPolicy.permittedLicense("CC0 1.0"))
        confirm(!FreeAudioCatalogPolicy.permittedLicense("CC BY-NC 4.0"))
        println("PASS: NW23 " + checks + " production Kotlin queue identity assertions")
    }
}
