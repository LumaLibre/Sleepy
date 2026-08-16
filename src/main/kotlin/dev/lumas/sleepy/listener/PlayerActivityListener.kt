package dev.lumas.sleepy.listener

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.sleepy.Sleepy
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInputEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent

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
        Sleepy.activity.recordMovementInput(event.player, event.input)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        Sleepy.activity.recordMovement(event.player, event.to)
    }
}
