package com.recovery.companion.ui

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import java.io.ByteArrayInputStream

/**
 * The app's own in-app browser — the ONLY browsing surface where content
 * can actually be inspected on-device (plan §2.3, §13).
 *
 * Layer 1: any URL whose host matches the blocklist is stopped before it
 * loads, and the user sees an explanation instead of the page.
 * Layer 2: image/media intercepts are filtered and blocked media is replaced
 * with a clean mindful placeholder SVG.
 */
@Composable
fun SafeWebView(initialUrl: String = "https://www.google.com") {
    var blockedHost by remember { mutableStateOf<String?>(null) }

    val placeholderSvg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200">
            <rect width="300" height="200" fill="#2d3748" rx="8"/>
            <text x="50%" y="45%" text-anchor="middle" fill="#e2e8f0" font-family="sans-serif" font-size="14" font-weight="bold">
                محتوى محجوب لحمايتك
            </text>
            <text x="50%" y="65%" text-anchor="middle" fill="#a0aec0" font-family="sans-serif" font-size="11">
                تذكر هدفك واستمر
            </text>
        </svg>
    """.trimIndent()

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
                                true // stop navigation
                            } else {
                                blockedHost = null
                                false
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            val host = request.url.host
                            if (host != null && Blocklist.isBlocked(host)) {
                                // Block explicit host resource
                                return WebResourceResponse(
                                    "image/svg+xml",
                                    "UTF-8",
                                    ByteArrayInputStream(placeholderSvg.toByteArray(Charsets.UTF_8))
                                )
                            }

                            // Intercept adult image/video URLs
                            val path = request.url.path?.lowercase() ?: ""
                            val isMedia = path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                                    path.endsWith(".png") || path.endsWith(".webp") ||
                                    path.endsWith(".gif") || path.endsWith(".mp4")

                            if (isMedia && (path.contains("nsfw") || path.contains("porn") || path.contains("xxx"))) {
                                return WebResourceResponse(
                                    "image/svg+xml",
                                    "UTF-8",
                                    ByteArrayInputStream(placeholderSvg.toByteArray(Charsets.UTF_8))
                                )
                            }

                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    loadUrl(initialUrl)
                }
            }
        )
    }
}
