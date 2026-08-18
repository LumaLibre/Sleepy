package dev.lumas.sleepy.model

import java.util.UUID
import kotlin.math.IEEErem
import kotlin.math.abs

class PlayerActivity(stored: PlaytimeEntry) {
    val uuid: UUID = stored.uuid

    @Volatile
    var name: String = stored.name

    @Volatile
    var playtimeSeconds: Long = stored.playtimeSeconds
        private set

    @Volatile
    var totalAfkSeconds: Long = stored.afkSeconds
        private set

    @Volatile
    var points: Long = stored.points
        private set

    @Volatile
    var currentAfkSeconds: Long = 0
        private set

    @Volatile
    var cause: AfkCause = AfkCause.NONE
        private set

    @Volatile
    var teleportPending: Boolean = false

    @Volatile
    private var lastActivityNanos: Long = System.nanoTime()

    @Volatile
    private var manual: Boolean = false

    @Volatile
    private var wasInRegion: Boolean = false

    private var movementInputActive: Boolean = false
    private var movementAnchor: MovementAnchor? = null
    private var cameraAnchor: CameraAnchor? = null

    private val executedPreTeleportActions = mutableSetOf<Long>()

    val isAfk: Boolean
        get() = cause != AfkCause.NONE

    @Synchronized
    fun tick(inRegion: Boolean, exempt: Boolean, markAfterSeconds: Long): TickResult {
        val previous = cause
        val previousAfkSeconds = currentAfkSeconds
        val previouslyInRegion = wasInRegion
        wasInRegion = inRegion
        playtimeSeconds++

        when {
            exempt -> {
                manual = false
                currentAfkSeconds = 0
                lastActivityNanos = System.nanoTime()
                cause = AfkCause.NONE
                executedPreTeleportActions.clear()
            }

            manual -> {
                currentAfkSeconds++
                cause = AfkCause.MANUAL
            }

            else -> {
                currentAfkSeconds = maxOf(0, (System.nanoTime() - lastActivityNanos) / 1_000_000_000L)
                cause = if (currentAfkSeconds >= markAfterSeconds) AfkCause.INACTIVITY else AfkCause.NONE
            }
        }

        if (cause != AfkCause.NONE) totalAfkSeconds++
        return TickResult(
            previousCause = previous,
            cause = cause,
            previousAfkSeconds = previousAfkSeconds,
            afkSeconds = currentAfkSeconds,
            wasInRegion = previouslyInRegion,
            inRegion = inRegion,
        )
    }

    @Synchronized
    fun recordActivity(): Boolean {
        if (teleportPending) return false
        val returned = cause != AfkCause.NONE || manual
        manual = false
        currentAfkSeconds = 0
        cause = AfkCause.NONE
        lastActivityNanos = System.nanoTime()
        executedPreTeleportActions.clear()
        return returned
    }

    @Synchronized
    fun updateMovementInput(active: Boolean, world: UUID, x: Double, y: Double, z: Double) {
        movementInputActive = active
        when {
            !active -> movementAnchor = null
            movementAnchor?.world != world -> movementAnchor = MovementAnchor(world, x, y, z)
        }
    }

    @Synchronized
    fun recordMovement(
        world: UUID,
        x: Double,
        y: Double,
        z: Double,
        minimumDistance: Double,
    ): Boolean {
        if (!movementInputActive) return false

        val current = MovementAnchor(world, x, y, z)
        val previous = movementAnchor
        if (previous == null || previous.world != world) {
            movementAnchor = current
            return false
        }
        if (previous.distanceSquared(current) < minimumDistance * minimumDistance) return false

        movementAnchor = current
        return recordActivity()
    }

    @Synchronized
    fun resetCamera(yaw: Float, pitch: Float) {
        cameraAnchor = CameraAnchor(yaw, pitch)
    }

    @Synchronized
    fun recordCamera(yaw: Float, pitch: Float, toleranceDegrees: Double): Boolean {
        val current = CameraAnchor(yaw, pitch)
        val previous = cameraAnchor
        if (previous == null) {
            cameraAnchor = current
            return false
        }

        val moved = angularDistance(previous.yaw, current.yaw) >= toleranceDegrees ||
            abs(previous.pitch - current.pitch) >= toleranceDegrees
        if (!moved) return false

        cameraAnchor = current
        return recordActivity()
    }

    @Synchronized
    fun toggleManual(): Boolean {
        if (manual) {
            recordActivity()
            return false
        }
        markManual()
        return true
    }

    @Synchronized
    fun markManual(): Boolean {
        if (manual) return false
        manual = true
        currentAfkSeconds = maxOf(1, currentAfkSeconds)
        cause = AfkCause.MANUAL
        return true
    }

    @Synchronized
    fun addPoints(amount: Long): Long {
        require(amount > 0) { "Point amount must be positive" }
        points = if (Long.MAX_VALUE - points < amount) Long.MAX_VALUE else points + amount
        return points
    }

    @Synchronized
    fun withdrawPoints(amount: Long): Boolean {
        require(amount > 0) { "Point amount must be positive" }
        if (points < amount) return false
        points -= amount
        return true
    }

    fun snapshot(): PlaytimeEntry = PlaytimeEntry(uuid, name, playtimeSeconds, totalAfkSeconds, points)

    @Synchronized
    fun claimPreTeleportActions(beforeSeconds: Long): Boolean =
        executedPreTeleportActions.add(beforeSeconds)

    data class TickResult(
        val previousCause: AfkCause,
        val cause: AfkCause,
        val previousAfkSeconds: Long,
        val afkSeconds: Long,
        val wasInRegion: Boolean,
        val inRegion: Boolean,
    ) {
        val becameAfk: Boolean get() = previousCause == AfkCause.NONE && cause != AfkCause.NONE
        val enteredRegion: Boolean get() = !wasInRegion && inRegion
        val returned: Boolean get() = previousCause != AfkCause.NONE && cause == AfkCause.NONE
    }

    private data class MovementAnchor(
        val world: UUID,
        val x: Double,
        val y: Double,
        val z: Double,
    ) {
        fun distanceSquared(other: MovementAnchor): Double {
            val xDifference = other.x - x
            val yDifference = other.y - y
            val zDifference = other.z - z
            return xDifference * xDifference + yDifference * yDifference + zDifference * zDifference
        }
    }

    private data class CameraAnchor(
        val yaw: Float,
        val pitch: Float,
    )

    private fun angularDistance(first: Float, second: Float): Double =
        abs((second - first).toDouble().IEEErem(360.0))
}
