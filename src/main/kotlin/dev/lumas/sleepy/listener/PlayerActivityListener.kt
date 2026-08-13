package dev.lumas.sleepy.listener

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.config.SleepyConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.IEEErem
import kotlin.math.abs

@Register(Autowire.LISTENER)
class PlayerActivityListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        Sleepy.activity.join(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        Sleepy.activity.quit(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInput(event: PlayerInputEvent) {
        Sleepy.activity.recordActivity(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        val tolerance = SleepyConfig.instance.detection.cameraToleranceDegrees
        val cameraMoved = angularDistance(from.yaw, to.yaw) >= tolerance ||
            abs(from.pitch - to.pitch) >= tolerance
        if (cameraMoved) {
            Sleepy.activity.recordActivity(event.player)
        }
    }

    private fun angularDistance(first: Float, second: Float): Double =
        abs((second - first).toDouble().IEEErem(360.0))
}
