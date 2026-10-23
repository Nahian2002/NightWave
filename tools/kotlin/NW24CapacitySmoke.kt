package com.nightwave.player.offline

/** Standalone direct compilation of the exact NW24 production capacity policy. */
object NW24CapacitySmoke {
    @JvmStatic fun main(args: Array<String>) {
        var passed = 0
        fun checkPolicy(value: Boolean) {
            check(value) { "NW24 capacity invariant failed at " + (passed + 1) }
            passed++
        }
        val policy = OfflineLibraryCapacity
        val mb = 1_048_576L
        val max = policy.MAX_LIBRARY_BYTES
        checkPolicy(policy.MAX_ITEMS == 5000)
        checkPolicy(max == 32L * 1024L * 1024L * 1024L)
        checkPolicy(policy.acceptsNew(0, 0, mb))
        checkPolicy(policy.acceptsNew(400, 3L * 1024L * 1024L * 1024L, mb))
        checkPolicy(policy.acceptsNew(4999, 0, mb))
        checkPolicy(!policy.acceptsNew(5000, 0, mb))
        checkPolicy(!policy.acceptsNew(-1, 0, mb))
        checkPolicy(!policy.acceptsNew(1, -1, mb))
        checkPolicy(policy.acceptsNew(1, max - mb, mb))
        checkPolicy(!policy.acceptsNew(1, max - mb, mb + 1))
        checkPolicy(!policy.acceptsNew(1, max, mb))
        checkPolicy(!policy.acceptsNew(1, Long.MAX_VALUE, Long.MAX_VALUE))
        checkPolicy(!policy.acceptsNew(1, 0, 0))
        checkPolicy(policy.canStage(40 * mb, 20 * mb))
        checkPolicy(!policy.canStage(30 * mb, 20 * mb))
        checkPolicy(!policy.canStage(-1, mb))
        checkPolicy(policy.safeIndex("[]"))
        checkPolicy(!policy.safeIndex("a".repeat(policy.MAX_INDEX_CHARS + 1)))
        checkPolicy(policy.pageCount(0) == 1)
        checkPolicy(policy.pageCount(60) == 1)
        checkPolicy(policy.pageCount(61) == 2)
        checkPolicy(policy.pageCount(5000) == 84)
        checkPolicy(policy.safePage(-1, 5000) == 0)
        checkPolicy(policy.safePage(999, 5000) == 83)
        println("PASS: NW24 " + passed + " production Kotlin library capacity assertions")
        println("LIMIT: Android storage, migration and UI not compiled")
    }
}
