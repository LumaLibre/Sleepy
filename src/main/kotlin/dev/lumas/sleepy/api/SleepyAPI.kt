package dev.lumas.sleepy.api

import dev.lumas.sleepy.model.PlayerActivity
import org.bukkit.entity.Player
import java.util.UUID

object SleepyAPI {
    @Volatile
    private var activityLookup: ((UUID) -> PlayerActivity?)? = null

    internal fun bind(lookup: ((UUID) -> PlayerActivity?)?) {
        activityLookup = lookup
    }

    /**
     * Returns whether the tracked streak is at least [ticks] long, independently of the AFK marker.
     * Returns false if Sleepy is disabled or the player is not loaded/tracked.
     * Zero matches any tracked player. Negative durations throw [IllegalArgumentException].
     */
    @JvmStatic
    fun hasBeenAfkForTicks(player: Player, ticks: Long): Boolean = hasBeenAfkForTicks(player.uniqueId, ticks)

    /** UUID overload of [hasBeenAfkFor]; only the current online session is queried. */
    @JvmStatic
    fun hasBeenAfkForTicks(uuid: UUID, ticks: Long): Boolean {
        require(ticks >= 0) { "AFK duration must not be negative" }
        val activity = activityLookup?.invoke(uuid) ?: return false
        val seconds = ticks / 20 + if (ticks % 20 == 0L) 0 else 1
        return activity.currentAfkSeconds >= seconds
    }

    @JvmStatic
    fun hasBeenAfkFor(player: Player, seconds: Long): Boolean = hasBeenAfkFor(player.uniqueId, seconds)

    @JvmStatic
    fun hasBeenAfkFor(uuid: UUID, seconds: Long): Boolean {
        require(seconds >= 0) { "AFK duration must not be negative" }
        val activity = activityLookup?.invoke(uuid) ?: return false
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
        val seconds = activityLookup?.invoke(uuid)?.currentAfkSeconds ?: return 0
        return if (seconds > Long.MAX_VALUE / 20) Long.MAX_VALUE else seconds * 20
    }

    /** Returns Sleepy's configured AFK marker, or false if untracked/disabled. */
    @JvmStatic
    fun isAfk(player: Player): Boolean = isAfk(player.uniqueId)

    /** UUID overload of [isAfk]; unlike duration queries, this respects the configured AFK marker. */
    @JvmStatic
    fun isAfk(uuid: UUID): Boolean = activityLookup?.invoke(uuid)?.isAfk ?: false

    @JvmStatic
    fun getPlayerActivity(player: Player): PlayerActivity? {
        return activityLookup?.invoke(player.uniqueId)
    }
}
