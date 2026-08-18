package dev.lumas.sleepy.service

import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.config.SleepyConfig
import dev.lumas.sleepy.integration.gsit.GSitIntegration
import dev.lumas.sleepy.model.AfkRegion
import dev.lumas.sleepy.model.PlayerActivity
import dev.lumas.sleepy.util.Messages
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class RegionService {
    private val occupied = ConcurrentHashMap<UUID, String>()

    fun contains(location: Location): Boolean =
        SleepyConfig.instance.teleport.regions.any { it.contains(location) }

    fun hasAvailableRegion(): Boolean =
        SleepyConfig.instance.teleport.regions.any { resolveWorld(it.world) != null }

    fun teleport(player: Player, activity: PlayerActivity) {
        val available = SleepyConfig.instance.teleport.regions.mapNotNull { region ->
            resolveWorld(region.world)?.let { world -> region to world }
        }
        if (available.isEmpty() || activity.teleportPending) return

        val slot = selectSlot(player.uniqueId, available) ?: return
        val spawn = slot.region.spawn(slot.index)
        activity.teleportPending = true
        occupied[player.uniqueId] = slot.key

        val vehicle = player.vehicle
        if (vehicle != null) {
            if (!player.leaveVehicle()) {
                vehicle.removePassenger(player)
            }
            player.scheduler.execute(
                Sleepy.instance,
                { executeTeleport(player, activity, slot.region, slot.world, spawn) },
                {
                    activity.teleportPending = false
                    occupied.remove(player.uniqueId)
                },
                1L
            )
            return
        }

        executeTeleport(player, activity, slot.region, slot.world, spawn)
    }

    fun release(player: Player) {
        if (occupied.remove(player.uniqueId) == null) return
        if (!SleepyConfig.instance.teleport.unseatOnReturn) return
        if (!player.isOnline || !GSitIntegration.isAvailable) return
        player.scheduler.execute(Sleepy.instance, { GSitIntegration.release(player) }, null, 1L)
    }

    fun release(uuid: UUID) {
        occupied.remove(uuid)
    }

    private fun selectSlot(uuid: UUID, available: List<Pair<AfkRegion, World>>): SpawnSlot? {
        val candidates = available.flatMap { (region, world) ->
            val count = region.spawnCount
            if (count == 0) {
                listOf(SpawnSlot(region, world, RANDOM_SPAWN_INDEX))
            } else {
                (0 until count).map { index -> SpawnSlot(region, world, index) }
            }
        }
        if (candidates.isEmpty()) return null

        val occupants = occupied.filterKeys { it != uuid }.values.groupingBy { it }.eachCount()
        val fewest = candidates.groupBy { occupants[it.key] ?: 0 }.minBy { it.key }.value
        return fewest[ThreadLocalRandom.current().nextInt(fewest.size)]
    }

    private fun executeTeleport(player: Player, activity: PlayerActivity, selected: AfkRegion, world: World, spawn: AfkRegion.Spawn) {
        val schedulingLocation = spawn.position.toLocation(world)
        Bukkit.getRegionScheduler().execute(Sleepy.instance, schedulingLocation) {
            val destination = selected.resolveSpawn(world, spawn)
            if (destination == null) {
                activity.teleportPending = false
                occupied.remove(player.uniqueId)
                LOGGER.warning("AFK region '${selected.name}' has no safe random spawn location")
                return@execute
            }

            player.teleportAsync(destination).whenComplete { success, error ->
                if (success == true) {
                    activity.resetCamera(destination.yaw, destination.pitch)
                } else {
                    occupied.remove(player.uniqueId)
                }
                activity.teleportPending = false
                when {
                    error != null -> LOGGER.warning(
                        "Unable to teleport ${player.name}: ${error.message}",
                        error,
                    )

                    success == true -> player.scheduler.execute(
                        Sleepy.instance,
                        {
                            applyPose(player, activity, spawn)
                            Messages.send(player, "sleepy.message.teleported")
                        },
                        null,
                        1L,
                    )
                }
            }
        }
    }

    private fun applyPose(player: Player, activity: PlayerActivity, spawn: AfkRegion.Spawn) {
        if (!spawn.pose.requiresGSit) return
        if (!GSitIntegration.apply(player, spawn.pose, SleepyConfig.instance.teleport.centerPosesOnBlock)) return
        val posed = player.location // Seating can nudge the player's head
        activity.resetCamera(posed.yaw, posed.pitch)
    }

    private fun resolveWorld(reference: String): World? =
        Bukkit.getWorld(reference) ?: NamespacedKey.fromString(reference)?.let(Bukkit::getWorld)

    private class SpawnSlot(
        val region: AfkRegion,
        val world: World,
        val index: Int,
    ) {
        val key: String = "${region.name}#$index"
    }

    companion object {
        private val LOGGER = PluginContextLogger.getPluginLogger()
        private const val RANDOM_SPAWN_INDEX = -1
    }
}
