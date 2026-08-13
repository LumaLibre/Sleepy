package dev.lumas.sleepy.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DurationsTest {
    @Test
    fun `formats useful units`() {
        assertEquals("0s", Durations.format(0))
        assertEquals("1m 1s", Durations.format(61))
        assertEquals("1d 2h 3m 4s", Durations.format(93_784))
        assertEquals(Durations.Parts(1, 2, 3, 4), Durations.parts(93_784))
    }
}
