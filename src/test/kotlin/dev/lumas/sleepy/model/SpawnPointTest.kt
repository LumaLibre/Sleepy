package dev.lumas.sleepy.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SpawnPointTest {
    @Test
    fun `parses coordinates without a pose`() {
        val point = SpawnPoint.parse("minecraft:overworld,0,65,0")
        assertEquals(SpawnPose.STAND, point.pose)
        assertEquals(WorldPosition("minecraft:overworld", 0.0, 65.0, 0.0), point.position)
    }

    @Test
    fun `parses a pose appended to coordinates`() {
        val point = SpawnPoint.parse("minecraft:overworld,0.5,64.5,0.5,sit")
        assertEquals(SpawnPose.SIT, point.pose)
        assertEquals(WorldPosition("minecraft:overworld", 0.5, 64.5, 0.5), point.position)
    }

    @Test
    fun `parses a pose appended to coordinates with yaw and pitch`() {
        val point = SpawnPoint.parse("minecraft:overworld,0.5,64.5,0.5,90,0,lay_back")
        assertEquals(SpawnPose.LAY_BACK, point.pose)
        assertEquals(WorldPosition("minecraft:overworld", 0.5, 64.5, 0.5, 90f, 0f), point.position)
    }

    @Test
    fun `accepts dashes in pose names`() {
        assertEquals(SpawnPose.LAY_BACK, SpawnPoint.parse("world,0,65,0,lay-back").pose)
    }

    @Test
    fun `rejects an unknown pose`() {
        assertThrows(IllegalArgumentException::class.java) {
            SpawnPoint.parse("minecraft:overworld,0,65,0,recline")
        }
    }

    @Test
    fun `rejects malformed coordinates`() {
        assertThrows(IllegalArgumentException::class.java) {
            SpawnPoint.parse("minecraft:overworld,0,65,sit")
        }
    }
}
