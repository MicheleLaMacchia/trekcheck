package it.trekcheck.app

import org.junit.Assert.assertEquals
import org.junit.Test

class WelcomeMessageTest {
    @Test
    fun appTitleIsTrekCheck() {
        assertEquals("TrekCheck", appTitle())
    }

    @Test
    fun welcomeMessageIsInItalian() {
        assertEquals("Pronto per la prossima escursione?", welcomeMessage())
    }
}
