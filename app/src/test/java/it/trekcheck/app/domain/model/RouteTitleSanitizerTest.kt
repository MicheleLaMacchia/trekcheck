package it.trekcheck.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTitleSanitizerTest {
    @Test
    fun blankAndControlOnlyTitlesAreRejected() {
        assertNull(RouteTitleSanitizer.sanitize(null))
        assertNull(RouteTitleSanitizer.sanitize("   \n\t "))
    }

    @Test
    fun collapsesWhitespaceAndTrims() {
        assertEquals("Monte Pizzoc", RouteTitleSanitizer.sanitize("  Monte \n  Pizzoc\t"))
    }

    @Test
    fun truncatesTo160CharsWithoutSplittingSurrogatePairs() {
        val long = "a".repeat(159) + "\uD83D\uDE00" + "tail"
        val result = RouteTitleSanitizer.sanitize(long)!!
        assertEquals(159, result.length)
        assertTrue(result.all { it == 'a' })
        assertEquals(160, RouteTitleSanitizer.sanitize("b".repeat(300))!!.length)
    }
}
