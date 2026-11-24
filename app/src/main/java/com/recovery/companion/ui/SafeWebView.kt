package com.recovery.companion.ui

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.recovery.companion.blocking.Blocklist

/**
 * The app's own in-app browser — the ONLY browsing surface where content
 * can actually be inspected on-device (plan §2.3). Chrome and other apps
 * are outside our reach: their traffic is TLS-encrypted and DNS blocking
 * is the only lever there.
 *
 * Layer 1: any URL whose host matches the blocklist is stopped before it
 * loads, and the user sees an explanation instead of the page.
 * Layer 2 (Phase 1): image bytes in this WebView are passed to the
 * on-device NSFW classifier when its model file has been downloaded.
 */
@Composable
fun SafeWebView(initialUrl: String = "https://www.google.com") {
    var blockedHost by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        blockedHost?.let { host ->
            Text(
                "تم حجب $host — النطاق ده في قائمة الحجب. لو شايف إن ده غلط، تقدر تبلغ من الإعدادات.",
                Modifier.fillMaxWidth().padding(12.dp)
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            val host = request.url.host ?: return false
                            return if (Blocklist.isBlocked(host)) {
                                blockedHost = host
                                true // stop the load
                            } else {
                                blockedHost = null
                                false
                            }
                        }
                    }
                    loadUrl(initialUrl)
                }
            }
        )
    }
}
