package it.trekcheck.app.domain.model

object RouteTitleSanitizer {
    const val MAX_TITLE_LENGTH: Int = 160

    /**
     * Collapses whitespace/control characters, trims and truncates to [MAX_TITLE_LENGTH]
     * without splitting a surrogate pair. Returns null when nothing usable remains.
     */
    fun sanitize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val collapsed = buildString(raw.length) {
            var lastWasSpace = true
            for (ch in raw) {
                if (ch.isISOControl() || ch.isWhitespace()) {
                    if (!lastWasSpace) {
                        append(' ')
                        lastWasSpace = true
                    }
                } else {
                    append(ch)
                    lastWasSpace = false
                }
            }
        }.trim()
        if (collapsed.isEmpty()) return null
        return if (collapsed.length <= MAX_TITLE_LENGTH) {
            collapsed
        } else {
            var end = MAX_TITLE_LENGTH
            if (Character.isHighSurrogate(collapsed[end - 1])) {
                end -= 1
            }
            collapsed.substring(0, end).trimEnd().ifEmpty { null }
        }
    }
}
