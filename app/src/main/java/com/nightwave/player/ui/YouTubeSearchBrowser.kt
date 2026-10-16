package com.nightwave.player.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.nightwave.player.R
import com.nightwave.player.core.WebSearchRoute

/**
 * Official YouTube search inside the app, rather than depending on public
 * third-party search API instances. Tapping a public video returns its URL to
 * NightWave's existing foreground media player.
 *
 * The site itself is rendered by Android WebView: no HTML scraping, no
 * Javascript extraction of private state, and no account credentials stored
 * by NightWave.
 */
class YouTubeSearchBrowser(private val context: Context) {
    fun show(query: String, onVideoSelected: (String) -> Unit) {
        val pageUrl = WebSearchRoute.searchUrl(query)
        val dialog = Dialog(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 11, 21))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(7))
        }
        fun control(title: String, action: () -> Unit): TextView {
            return TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(Color.rgb(222, 219, 255))
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
        }
        val status = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.rgb(169, 174, 203))
            text = "Opening YouTube search…"
            setPadding(dp(17), dp(9), dp(17), dp(12))
        }
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.rgb(189, 175, 255))
            layoutParams = LinearLayout.LayoutParams(-1, dp(3))
        }
        val browser = WebView(context).apply {
            setBackgroundColor(Color.rgb(8, 11, 21))
            settings.javaScriptEnabled = true // required for YouTube's own search UI
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            isVerticalScrollBarEnabled = false
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser, false)

        var closed = false
        var delivered = false
        fun select(raw: String): Boolean {
            val video = WebSearchRoute.selectedVideo(raw) ?: return false
            if (delivered || closed) return true
            delivered = true
            dialog.dismiss()
            onVideoSelected(video)
            return true
        }

        header.addView(control("←  CLOSE") { dialog.dismiss() })
        header.addView(TextView(context).apply {
            text = "Search YouTube"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -1, 1f))
        header.addView(control("▶ PLAY") {
            val url = browser.url.orEmpty()
            if (!select(url)) status.text = "Open a YouTube video first, then tap ▶ PLAY."
        })
        header.addView(control("↗ BROWSER") {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl)))
            } catch (_: Exception) {
                status.text = "No browser available. Select a video here instead."
            }
        })
        root.addView(header)
        root.addView(progress)
        root.addView(status)
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(context).apply {
            text = "Tap a video to listen. If it opens here, tap ▶ PLAY. No sign-in needed."
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(168, 176, 203))
            setPadding(dp(7), dp(12), dp(7), dp(15))
        })

        browser.webViewClient = object : WebViewClient() {
            private fun handle(raw: String): Boolean {
                if (select(raw)) return true
                if (!WebSearchRoute.isAllowedWebsiteNavigation(raw)) {
                    status.text = "This page isn't part of YouTube. Use the browser button for external sites."
                    return true
                }
                return false
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handle(request.url.toString())

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                if (url != null && select(url)) return
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                if (closed || delivered || url == null || !WebSearchRoute.isAllowedWebsiteNavigation(url)) return
                status.text = "Showing YouTube results  ·  Tap a video to listen in NightWave"
                // YouTube sometimes uses same-page JS navigation. Capture actual
                // user taps on video anchors before its SPA router processes them.
                // No Javascript interface is exposed to the page.
                view.evaluateJavascript(
                    """
                    (function(){
                        if (window.__nwVideoClickInstalled) return;
                        window.__nwVideoClickInstalled = true;
                        document.addEventListener('click', function(event) {
                            var el = event.target;
                            var link = el && el.closest ? el.closest('a[href]') : null;
                            if (!link) return;
                            try {
                                var uri = new URL(link.href, location.href);
                                var host = uri.hostname.toLowerCase();
                                var validHost = ['www.youtube.com','youtube.com','m.youtube.com','music.youtube.com','youtu.be'].indexOf(host) >= 0;
                                var isVideo = /^\/watch$|^\/shorts\/|^\/live\//.test(uri.pathname);
                                if (validHost && isVideo) {
                                    event.preventDefault();
                                    event.stopImmediatePropagation();
                                    location.href = 'nightwave://play?url=' + encodeURIComponent(uri.href);
                                }
                            } catch(e) {}
                        }, true);
                    })();
                    """.trimIndent(), null
                )
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame && !delivered) {
                    progress.visibility = View.GONE
                    status.text = "YouTube couldn't load inside the app. Try ↗ BROWSER, then share the video to NightWave."
                }
            }
        }
        browser.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress < 100 && !closed && !delivered) progress.visibility = View.VISIBLE
                else progress.visibility = View.GONE
            }
        }

        dialog.setContentView(root)
        dialog.setOnDismissListener {
            closed = true
            browser.stopLoading()
            browser.loadUrl("about:blank")
            (browser.parent as? ViewGroup)?.removeView(browser)
            browser.destroy()
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_root)
        dialog.window?.setLayout(-1, -1)
        browser.loadUrl(pageUrl)
    }

    private fun dp(v: Int): Int = (context.resources.displayMetrics.density * v + 0.5f).toInt()
}
