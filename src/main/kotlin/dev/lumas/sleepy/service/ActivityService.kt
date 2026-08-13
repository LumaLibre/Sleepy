package dev.lumas.sleepy.service

import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.config.SleepyConfig
import dev.lumas.sleepy.model.AfkCause
import dev.lumas.sleepy.model.CommandAction
import dev.lumas.sleepy.model.Dreams
import dev.lumas.sleepy.model.PlayerActivity
import dev.lumas.sleepy.model.PlaytimeEntry
import dev.lumas.sleepy.model.PointReward
import dev.lumas.sleepy.storage.PlaytimeRepository
import dev.lumas.sleepy.util.Messages
import dev.lumas.sleepy.util.ActionText
import dev.lumas.sleepy.util.PlayerNames
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ActivityService(
    private val repository: PlaytimeRepository,
) {
    private val regions = RegionService()
    private val preTeleportActions = PreTeleportActionService()
    private val activities = ConcurrentHashMap<UUID, PlayerActivity>()
    private val pointPersistenceLocks = ConcurrentHashMap<UUID, Any>()

    @Volatile
    var leaderboard: List<PlaytimeEntry> = emptyList()
        private set

    @Volatile
    var afkLeaderboard: List<PlaytimeEntry> = emptyList()
        private set

    fun join(player: Player) {
        val uuid = player.uniqueId
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            val loaded = try {
                repository.load(uuid, player.name)
            } catch (exception: RuntimeException) {
                LOGGER.warning(
                    "Unable to load ${player.name}'s playtime: ${exception.message}",
                    exception,
                )
                PlaytimeEntry(uuid, player.name, 0, 0)
            }

            player.scheduler.run(
                Sleepy.instance,
                {
                    if (!player.isOnline) return@run
                    val activity = PlayerActivity(loaded)
                    activities[uuid] = activity
                    player.scheduler.runAtFixedRate(
                        Sleepy.instance,
                        { task -> tick(player, activity, task) },
                        { activities.remove(uuid) },
                        20L,
                        20L,
                    )
                },
                null,
            )
        }
    }

    fun quit(player: Player) {
        activities.remove(player.uniqueId)?.snapshot()?.let(::saveAllAsync)
    }

    fun shutdown() {
        activities.values.forEach { activity ->
            synchronized(pointLock(activity.uuid)) {
                activity.snapshot().also {
                    repository.save(it)
                    repository.savePoints(it)
                }
            }
        }
        activities.clear()
        pointPersistenceLocks.clear()
    }

    fun recordActivity(player: Player) {
        val activity = activities[player.uniqueId] ?: return
        if (activity.recordActivity()) {
            Messages.send(player, "sleepy.message.afk.return")
        }
    }

    fun toggleManual(player: Player): Boolean = activities[player.uniqueId]?.toggleManual() ?: false

    fun addPoints(player: Player, amount: Long): Long? {
        val activity = activities[player.uniqueId] ?: return null
        val balance = activity.addPoints(amount)
        savePointsAsync(activity)
        return balance
    }

    fun withdrawPoints(player: Player, amount: Long): Boolean {
        val activity = activities[player.uniqueId] ?: return false
        if (!activity.withdrawPoints(amount)) return false
        savePointsAsync(activity)
        return true
    }

    fun activity(uuid: UUID): PlayerActivity? = activities[uuid]

    fun activity(player: Player): PlayerActivity? = activity(player.uniqueId)

    fun refreshLeaderboard() {
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            try {
                val limit = SleepyConfig.instance.storage.boundedLeaderboardSize
                leaderboard = repository.top(limit)
                    .mapNotNull { PlayerNames.resolveAndRepair(it, repository) }
                afkLeaderboard = repository.topAfk(limit)
                    .mapNotNull { PlayerNames.resolveAndRepair(it, repository) }
            } catch (exception: RuntimeException) {
                LOGGER.warning("Unable to refresh playtime and AFK-time leaderboards: ${exception.message}", exception)
            }
        }
    }

    private fun tick(player: Player, activity: PlayerActivity, task: ScheduledTask) {
        if (!player.isOnline || activities[player.uniqueId] !== activity) {
            task.cancel()
            return
        }

        activity.name = player.name
        val config = SleepyConfig.instance
        val inRegion = regions.contains(player.location)
        val result = activity.tick(inRegion, isExempt(player, config), config.detection.markAfterSeconds)

        runPointRewards(player, activity, activity.playtimeSeconds, config.pointRewards)

        if (result.enteredRegion) {
            Messages.send(player, "sleepy.message.afk.region.enter")
        }
        when {
            result.becameAfk && result.cause == AfkCause.INACTIVITY ->
                Messages.send(player, "sleepy.message.afk.auto")

            result.returned -> Messages.send(player, "sleepy.message.afk.return")
        }

        if (result.cause != AfkCause.NONE) {
            runActions(player, result.afkSeconds, config.teleport.afterSeconds, config.commandActions)
            val canTeleport = config.teleport.enabled && !inRegion && regions.hasAvailableRegion()
            if (canTeleport) {
                preTeleportActions.executeDue(
                    player,
                    activity,
                    result,
                    config.teleport.afterSeconds,
                    config.teleport.preTeleportActions,
                )
                if (result.afkSeconds >= config.teleport.afterSeconds) {
                    regions.teleport(player, activity)
                }
            }
        }

        if (activity.playtimeSeconds % config.storage.saveEverySeconds == 0L) {
            saveAsync(activity.snapshot())
            refreshLeaderboard()
        }
    }

    private fun runPointRewards(player: Player, activity: PlayerActivity, playtimeSeconds: Long, rewards: List<PointReward>) {
        var awarded = false
        // TODO: configurability for isAfk
        rewards.filter { it.isDue(playtimeSeconds) }
            .forEach { reward ->
                val balance = activity.addPoints(reward.amount)
                awarded = true
                if (reward.sendMessage) {
                    Messages.send(
                        player,
                        "sleepy.message.oneira.earned",
                        Dreams.display(reward.amount),
                        Dreams.display(balance),
                    )
                }
            }
        if (awarded) {
            savePointsAsync(activity)
        }
    }

    private fun isExempt(player: Player, config: SleepyConfig): Boolean =
        config.exemptWorlds.any { it == player.world.key.asString() || it.equals(player.world.name, ignoreCase = true) } || player.hasPermission(EXEMPT_PERMISSION)

    private fun runActions(
        player: Player,
        afkSeconds: Long,
        teleportAfterSeconds: Long,
        actions: List<CommandAction>,
    ) {
        actions.filter { afkSeconds > 0 && afkSeconds % it.everySeconds == 0L }
            .flatMap(CommandAction::commands)
            .forEach { configured ->
                val finalCommand = ActionText.resolve(
                    player,
                    configured,
                    afkSeconds,
                    (teleportAfterSeconds - afkSeconds).coerceAtLeast(0),
                ).removePrefix("/")
                if (finalCommand.isEmpty()) return@forEach
                Bukkit.getGlobalRegionScheduler().execute(Sleepy.instance) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand)
                }
            }
    }

    private fun saveAsync(snapshot: PlaytimeEntry) {
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            try {
                repository.save(snapshot)
            } catch (exception: RuntimeException) {
                LOGGER.warning(
                    "Unable to save ${snapshot.name}'s playtime: ${exception.message}",
                    exception,
                )
            }
        }
    }

    private fun savePointsAsync(activity: PlayerActivity) {
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            try {
                synchronized(pointLock(activity.uuid)) {
                    repository.savePoints(activity.snapshot())
                }
            } catch (exception: RuntimeException) {
                LOGGER.warning("Unable to save ${activity.name}'s points: ${exception.message}", exception)
            }
        }
    }

    private fun saveAllAsync(snapshot: PlaytimeEntry) {
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            try {
                synchronized(pointLock(snapshot.uuid)) {
                    repository.save(snapshot)
                    repository.savePoints(snapshot)
                }
            } catch (exception: RuntimeException) {
                LOGGER.warning("Unable to save ${snapshot.name}'s data: ${exception.message}", exception)
            }
        }
    }

    private fun pointLock(uuid: UUID): Any =
        pointPersistenceLocks.computeIfAbsent(uuid) { Any() }

    companion object {
        private val LOGGER = PluginContextLogger.getPluginLogger()
        const val EXEMPT_PERMISSION = "sleepy.exempt"
    }
}
