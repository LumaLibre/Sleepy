package dev.lumas.sleepy.service

import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.config.SleepyConfig
import dev.lumas.sleepy.model.AfkRegion
import dev.lumas.sleepy.model.PlayerActivity
import dev.lumas.sleepy.util.Messages
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.concurrent.ThreadLocalRandom

class RegionService {
    fun contains(location: Location): Boolean =
        SleepyConfig.instance.teleport.regions.any { it.contains(location) }

    fun hasAvailableRegion(): Boolean =
        SleepyConfig.instance.teleport.regions.any { resolveWorld(it.world) != null }

    fun teleport(player: Player, activity: PlayerActivity) {
        val available = SleepyConfig.instance.teleport.regions.mapNotNull { region ->
            resolveWorld(region.world)?.let { world -> region to world }
        }
        if (available.isEmpty() || activity.teleportPending) return

        val (selected, world) = available[ThreadLocalRandom.current().nextInt(available.size)]
        val spawn = selected.selectSpawn()
        activity.teleportPending = true

        val vehicle = player.vehicle
        if (vehicle != null) {
            if (!player.leaveVehicle()) {
                vehicle.removePassenger(player)
            }
            player.scheduler.execute(
                Sleepy.instance,
                { executeTeleport(player, activity, selected, world, spawn) },
                { activity.teleportPending = false },
                1L
            )
            return
        }

        executeTeleport(player, activity, selected, world, spawn)
    }

    private fun executeTeleport(player: Player, activity: PlayerActivity, selected: AfkRegion, world: World, spawn: AfkRegion.Spawn) {
        val schedulingLocation = spawn.position.toLocation(world)
        Bukkit.getRegionScheduler().execute(Sleepy.instance, schedulingLocation) {
            val destination = selected.resolveSpawn(world, spawn)
            if (destination == null) {
                activity.teleportPending = false
                LOGGER.warning("AFK region '${selected.name}' has no safe random spawn location")
                return@execute
            }

            player.teleportAsync(destination).whenComplete { success, error ->
                activity.teleportPending = false
                when {
                    error != null -> LOGGER.warning(
                        "Unable to teleport ${player.name}: ${error.message}",
                        error,
                    )

                    success == true -> player.scheduler.execute(
                        Sleepy.instance,
                        { Messages.send(player, "sleepy.message.teleported") },
                        null,
                        1L,
                    )
                }
            }
        }
    }

    private fun resolveWorld(reference: String): World? =
        Bukkit.getWorld(reference) ?: NamespacedKey.fromString(reference)?.let(Bukkit::getWorld)

    companion object {
        private val LOGGER = PluginContextLogger.getPluginLogger()
    }
}
