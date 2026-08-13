package dev.lumas.sleepy.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PlayerActivityTest {
    @Test
    fun `entering region is informational and does not force afk`() {
        val activity = PlayerActivity(PlaytimeEntry(UUID.randomUUID(), "Luma", 10, 3))

        val regionTick = activity.tick(true, false, 300)
        assertTrue(regionTick.enteredRegion)
        assertEquals(AfkCause.NONE, regionTick.cause)
        assertFalse(activity.isAfk)
    }

    @Test
    fun `activity clears afk inside region and inactivity can mark it again`() {
        val activity = PlayerActivity(PlaytimeEntry(UUID.randomUUID(), "Luma", 10, 3))

        assertTrue(activity.toggleManual())
        assertEquals(AfkCause.MANUAL, activity.tick(true, false, 300).cause)
        assertTrue(activity.recordActivity())
        assertFalse(activity.isAfk)

        val inactiveAgain = activity.tick(true, false, 0)
        assertEquals(AfkCause.INACTIVITY, inactiveAgain.cause)
        assertTrue(activity.isAfk)
    }

    @Test
    fun `pre-teleport action claims reset after activity`() {
        val activity = PlayerActivity(PlaytimeEntry(UUID.randomUUID(), "Luma", 0, 0))

        assertTrue(activity.claimPreTeleportActions(10))
        assertFalse(activity.claimPreTeleportActions(10))
        activity.recordActivity()
        assertTrue(activity.claimPreTeleportActions(10))
    }

    @Test
    fun `points are awarded and included in snapshots`() {
        val activity = PlayerActivity(PlaytimeEntry(UUID.randomUUID(), "Luma", 0, 0, 4))

        assertEquals(7, activity.addPoints(3))
        assertEquals(7, activity.snapshot().points)
    }

    @Test
    fun `point rewards use total online playtime intervals`() {
        val reward = PointReward(everySeconds = 300, amount = 2)

        assertFalse(reward.isDue(299))
        assertTrue(reward.isDue(300))
        assertTrue(reward.isDue(600))
    }

    @Test
    fun `currency withdrawal is atomic and cannot overspend`() {
        val activity = PlayerActivity(PlaytimeEntry(UUID.randomUUID(), "Luma", 0, 0, 10))

        assertTrue(activity.withdrawPoints(7))
        assertEquals(3, activity.points)
        assertFalse(activity.withdrawPoints(4))
        assertEquals(3, activity.points)
    }
}
