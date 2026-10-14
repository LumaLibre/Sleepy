package dev.lumas.sleepy.api

import dev.lumas.sleepy.api.SleepyAPI.getAfkTicks
import dev.lumas.sleepy.api.SleepyAPI.hasBeenAfkFor
import dev.lumas.sleepy.api.SleepyAPI.hasBeenAfkForTicks
import dev.lumas.sleepy.api.SleepyAPI.isAfk
import dev.lumas.sleepy.model.PlayerActivity
import dev.lumas.sleepy.service.ActivityService
import org.bukkit.entity.Player
import java.util.*

object SleepyAPI {

    @Volatile
    private var activityService: ActivityService? = null

    internal fun bind(activityService: ActivityService?) {
        this.activityService = activityService
    }

    @JvmStatic
    fun wakeup(player: Player) {
        activityService?.recordActivity(player)
    }

    /**
     * Returns whether the tracked streak is at least [ticks] long, independently of the AFK marker.
     * Returns false if Sleepy is disabled or the player is not loaded/tracked.
     * Zero matches any tracked player. Negative durations throw [IllegalArgumentException].
     */
    @JvmStatic
    fun hasBeenAfkForTicks(player: Player, ticks: Long): Boolean = hasBeenAfkForTicks(player.uniqueId, ticks)

    /** UUID overload of [hasBeenAfkForTicks] */
    @JvmStatic
    fun hasBeenAfkForTicks(uuid: UUID, ticks: Long): Boolean {
        require(ticks >= 0) { "AFK duration must not be negative" }
        val activity = activityService?.activity(uuid) ?: return false
        val seconds = ticks / 20 + if (ticks % 20 == 0L) 0 else 1
        return activity.currentAfkSeconds >= seconds
    }

    /**
     * Returns whether the tracked streak is at least [seconds] long, independently of the AFK marker.
     * Returns false if Sleepy is disabled or the player is not loaded/tracked.
     * Zero matches any tracked player.
     */
    @JvmStatic
    fun hasBeenAfkFor(player: Player, seconds: Long): Boolean = hasBeenAfkFor(player.uniqueId, seconds)

    /** UUID overload of [hasBeenAfkFor] */
    @JvmStatic
    fun hasBeenAfkFor(uuid: UUID, seconds: Long): Boolean {
        require(seconds >= 0) { "AFK duration must not be negative" }
        val activity = activityService?.activity(uuid) ?: return false
        return activity.currentAfkSeconds >= seconds
    }

    /**
     * Returns the current streak in ticks (multiples of 20), or zero if untracked/disabled.
     * This includes inactivity before the configured AFK marker and follows manual-AFK accounting.
     */
    @JvmStatic
    fun getAfkTicks(player: Player): Long = getAfkTicks(player.uniqueId)

    /** UUID overload of [getAfkTicks]; only the current online session is queried. */
    @JvmStatic
    fun getAfkTicks(uuid: UUID): Long {
        val seconds = activityService?.activity(uuid)?.currentAfkSeconds ?: return 0
        return if (seconds > Long.MAX_VALUE / 20) Long.MAX_VALUE else seconds * 20
    }

    /** Returns Sleepy's configured AFK marker, or false if untracked/disabled. */
    @JvmStatic
    fun isAfk(player: Player): Boolean = isAfk(player.uniqueId)

    /** UUID overload of [isAfk]; unlike duration queries, this respects the configured AFK marker. */
    @JvmStatic
    fun isAfk(uuid: UUID): Boolean = activityService?.activity(uuid)?.isAfk ?: false

    @JvmStatic
    fun getPlayerActivity(player: Player): PlayerActivity? {
        return activityService?.activity(player.uniqueId)
    }
}
