package com.nightwave.player.updates

import java.net.URL

internal object PrivateGithubUpdatePolicy {
    private const val API = "https://api.github.com/repos/Nahian2002/NightWave"
    const val FEED = "$API/contents/updates/stable.json?ref=main"

    fun validToken(value: String): Boolean = value.matches(Regex("github_pat_[A-Za-z0-9_]{20,240}"))

    fun asset(id: Long): String {
        require(id > 0)
        return "$API/releases/assets/$id"
    }

    fun canSendCredential(url: URL, exactEndpoint: String): Boolean =
        url.toExternalForm() == exactEndpoint &&
            (exactEndpoint == FEED || exactEndpoint.matches(Regex(Regex.escape(API) + "/releases/assets/[1-9][0-9]*")))

    fun isAssetRedirect(url: URL): Boolean =
        url.protocol == "https" && url.port == -1 && url.userInfo == null && url.ref == null &&
            url.host in setOf("release-assets.githubusercontent.com", "objects.githubusercontent.com")

    fun needsAccess(status: Int): Boolean = status == 401 || status == 403 || status == 404
}
