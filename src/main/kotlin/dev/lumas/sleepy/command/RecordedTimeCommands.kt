package dev.lumas.sleepy.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.PlayerActivity
import dev.lumas.sleepy.model.PlaytimeEntry
import dev.lumas.sleepy.util.Durations
import dev.lumas.sleepy.util.Messages
import dev.lumas.sleepy.util.PlayerNames
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

internal enum class RecordedTimeMetric(
    val othersPermission: String,
    val selfMessage: String,
    val otherMessage: String,
    val notFoundMessage: String,
    val playerRequiredMessage: String,
    val topEntryMessage: String,
    val topEmptyMessage: String,
    val onlineSeconds: (PlayerActivity) -> Long,
    val storedSeconds: (PlaytimeEntry) -> Long,
    val loadTop: (Int) -> List<PlaytimeEntry>,
) {
    PLAYTIME(
        othersPermission = "sleepy.command.playtime.others",
        selfMessage = "sleepy.message.playtime.self",
        otherMessage = "sleepy.message.playtime.other",
        notFoundMessage = "sleepy.message.playtime.not_found",
        playerRequiredMessage = "sleepy.message.playtime.player_required",
        topEntryMessage = "sleepy.message.playtime.leaderboard.entry",
        topEmptyMessage = "sleepy.message.playtime.top.empty",
        onlineSeconds = PlayerActivity::playtimeSeconds,
        storedSeconds = PlaytimeEntry::playtimeSeconds,
        loadTop = { Sleepy.repository.top(it) },
    ),
    AFK_TIME(
        othersPermission = "sleepy.command.afktime.others",
        selfMessage = "sleepy.message.afktime.self",
        otherMessage = "sleepy.message.afktime.other",
        notFoundMessage = "sleepy.message.afktime.not_found",
        playerRequiredMessage = "sleepy.message.afktime.player_required",
        topEntryMessage = "sleepy.message.afktime.top.entry",
        topEmptyMessage = "sleepy.message.afktime.top.empty",
        onlineSeconds = PlayerActivity::totalAfkSeconds,
        storedSeconds = PlaytimeEntry::afkSeconds,
        loadTop = { Sleepy.repository.topAfk(it) },
    ),
}

internal object RecordedTimeCommands {
    fun view(sender: CommandSender, target: String?, metric: RecordedTimeMetric) {
        if (target == null) {
            if (sender !is Player) {
                Messages.send(sender, metric.playerRequiredMessage)
                return
            }
            val seconds = Sleepy.activity.activity(sender)?.let(metric.onlineSeconds) ?: 0
            sendResult(sender, sender.name, seconds, metric)
            return
        }

        if (sender is Player && !sender.name.equals(target, ignoreCase = true) &&
            !sender.hasPermission(metric.othersPermission)
        ) {
            Messages.send(sender, "sleepy.message.no_permission")
            return
        }

        Bukkit.getPlayerExact(target)?.let { online ->
            val seconds = Sleepy.activity.activity(online)?.let(metric.onlineSeconds) ?: 0
            sendResult(sender, online.name, seconds, metric)
            return
        }

        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            val result = (Sleepy.repository.find(target) ?: Bukkit.getOfflinePlayerIfCached(target)
                ?.uniqueId
                ?.let { Sleepy.repository.find(it.toString()) })
                ?.let { PlayerNames.resolveAndRepair(it, Sleepy.repository) }
            reply(sender) {
                if (result == null) {
                    Messages.send(sender, metric.notFoundMessage, Component.text(target))
                } else {
                    sendResult(sender, result.name, metric.storedSeconds(result), metric)
                }
            }
        }
    }

    fun showTop(sender: CommandSender, count: Int, metric: RecordedTimeMetric) {
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            val entries = metric.loadTop(MAX_TOP_SIZE)
                .mapNotNull { PlayerNames.resolveAndRepair(it, Sleepy.repository) }
                .take(count)
            reply(sender) {
                if (entries.isEmpty()) {
                    Messages.send(sender, metric.topEmptyMessage)
                    return@reply
                }

                entries.forEachIndexed { index, entry ->
                    val time = Durations.parts(metric.storedSeconds(entry))
                    Messages.sendUnprefixed(
                        sender,
                        metric.topEntryMessage,
                        Component.text((index + 1).toString()),
                        Component.text(entry.name),
                        Component.text(time.days.toString()),
                        Component.text(time.hours.toString()),
                        Component.text(time.minutes.toString()),
                    )
                }
            }
        }
    }

    fun suggestPlayers(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder,
        othersPermission: String,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        val sender = context.source.sender
        val mayTargetOthers = sender.hasPermission(othersPermission)
        Bukkit.getOnlinePlayers().asSequence()
            .map(Player::getName)
            .filter { mayTargetOthers || sender.name.equals(it, ignoreCase = true) }
            .filter { it.lowercase().startsWith(remaining) }
            .forEach(builder::suggest)
        return builder.buildFuture()
    }

    private fun sendResult(
        sender: CommandSender,
        target: String,
        seconds: Long,
        metric: RecordedTimeMetric,
    ) {
        val formatted = Component.text(Durations.format(seconds))
        if (sender is Player && sender.name.equals(target, ignoreCase = true)) {
            Messages.send(sender, metric.selfMessage, formatted)
        } else {
            Messages.send(sender, metric.otherMessage, Component.text(target), formatted)
        }
    }

    private fun reply(sender: CommandSender, action: () -> Unit) {
        if (sender is Player) {
            sender.scheduler.execute(Sleepy.instance, action, null, 1L)
        } else {
            Bukkit.getGlobalRegionScheduler().execute(Sleepy.instance, action)
        }
    }

    const val DEFAULT_TOP_SIZE = 10
    const val MAX_TOP_SIZE = 100
}
