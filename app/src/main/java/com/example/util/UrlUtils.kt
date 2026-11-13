package com.example.util

import java.util.regex.Pattern

object UrlUtils {

    private val URL_PATTERN: Pattern = Pattern.compile(
        "(https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )

    private val DOMAIN_PATTERN: Pattern = Pattern.compile(
        "((?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(?:/[^\\s]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Extracts a clean HTTP or HTTPS URL from shared text (such as text shared from Google Chrome,
     * which may include webpage titles, annotations, or multiple tokens).
     */
    fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()

        // 1. Direct match for http:// or https:// URL in the text
        val urlMatcher = URL_PATTERN.matcher(trimmed)
        if (urlMatcher.find()) {
            return urlMatcher.group(1)
        }

        // 2. Domain pattern like "www.google.com" or "youtube.com/watch?v=..."
        val domainMatcher = DOMAIN_PATTERN.matcher(trimmed)
        if (domainMatcher.find()) {
            val domainPart = domainMatcher.group(1)
            return "https://$domainPart"
        }

        return null
    }

    /**
     * Formats a user-entered URL ensuring valid protocol.
     */
    fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }
}
