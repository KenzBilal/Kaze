package com.kaze.util

import android.net.Uri

/**
 * Parses incoming deep link URIs and extracts the IMDB ID.
 *
 * Supported formats:
 *  - https://imdb.com/title/tt1234567/
 *  - https://www.imdb.com/title/tt1234567/?ref=...
 *  - https://kenzbilal.github.io/Kaze/m/tt1234567
 */
object DeepLinkHandler {

    private val IMDB_ID_REGEX = Regex("tt\\d{7,8}")

    /**
     * Returns the IMDB ID from a URI, or null if the URI is not a supported deep link.
     */
    fun extractImdbId(uri: Uri?): String? {
        uri ?: return null
        val uriString = uri.toString()

        // Match any ttXXXXXXX pattern anywhere in the URL
        return IMDB_ID_REGEX.find(uriString)?.value
    }

    /**
     * Returns true if this URI is a WatchLater or IMDB deep link we should handle.
     */
    fun isWatchLaterDeepLink(uri: Uri?): Boolean {
        uri ?: return false
        val host = uri.host ?: return false
        return host.contains("imdb.com") || host.contains("kenzbilal.github.io")
    }
}
