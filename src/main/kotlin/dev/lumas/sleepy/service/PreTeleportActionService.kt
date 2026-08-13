package dev.lumas.sleepy.service

import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.PlayerActivity
import dev.lumas.sleepy.model.PreTeleportActionGroup
import dev.lumas.sleepy.model.TitleAction
import dev.lumas.sleepy.util.ActionText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Duration

class PreTeleportActionService {
    private val miniMessage = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()

    fun executeDue(player: Player, activity: PlayerActivity, tick: PlayerActivity.TickResult, teleportAfterSeconds: Long, groups: List<PreTeleportActionGroup>, ) {
        groups.groupBy(PreTeleportActionGroup::beforeSeconds).forEach { (beforeSeconds, matching) ->
            val triggerAt = teleportAfterSeconds - beforeSeconds
            val reachedTrigger = tick.previousAfkSeconds < triggerAt && tick.afkSeconds >= triggerAt
            if (!reachedTrigger || !activity.claimPreTeleportActions(beforeSeconds)) return@forEach
            matching.forEach { execute(player, tick.afkSeconds, beforeSeconds, it) }
        }
    }

    private fun execute(player: Player, afkSeconds: Long, beforeSeconds: Long, group: PreTeleportActionGroup, ) {
        group.commands.forEach { configured ->
            val command = resolve(player, configured, afkSeconds, beforeSeconds).removePrefix("/")
            if (command.isEmpty()) return@forEach
            Bukkit.getGlobalRegionScheduler().execute(Sleepy.instance) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            }
        }

        group.messages.forEach { configured ->
            val message = component(player, configured, afkSeconds, beforeSeconds)
            if (plainText.serialize(message).isNotEmpty()) player.sendMessage(message)
        }

        group.titles.forEach { configured ->
            val title = component(player, configured.title, afkSeconds, beforeSeconds)
            val subtitle = component(player, configured.subtitle, afkSeconds, beforeSeconds)
            if (plainText.serialize(title).isEmpty() && plainText.serialize(subtitle).isEmpty()) return@forEach
            player.showTitle(Title.title(title, subtitle, configured.times()))
        }
    }

    private fun component(player: Player, configured: String, afkSeconds: Long, beforeSeconds: Long, ): Component =
        miniMessage.deserialize(resolve(player, configured, afkSeconds, beforeSeconds))

    private fun resolve(player: Player, configured: String, afkSeconds: Long, beforeSeconds: Long): String =
        ActionText.resolve(player, configured, afkSeconds, beforeSeconds)

    private fun TitleAction.times(): Title.Times = Title.Times.times(
        Duration.ofMillis(fadeInMillis.coerceAtLeast(0)),
        Duration.ofMillis(stayMillis.coerceAtLeast(0)),
        Duration.ofMillis(fadeOutMillis.coerceAtLeast(0)),
    )
}
