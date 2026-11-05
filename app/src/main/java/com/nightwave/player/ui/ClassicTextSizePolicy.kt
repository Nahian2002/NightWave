package com.nightwave.player.ui

/** SP bounds remain SP bounds at every system font scale. Never shrink below the readable minimum. */
internal object ClassicTextSizePolicy {
    fun choose(minSp: Int, maxSp: Int, fits: (Int) -> Boolean): Int {
        require(minSp > 0 && maxSp in minSp..72)
        return (maxSp downTo minSp).firstOrNull(fits) ?: minSp
    }
}
