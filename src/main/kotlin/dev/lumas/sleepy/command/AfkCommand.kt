package dev.lumas.sleepy.command

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.BrigadierExecutor
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierCommand
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "afk",
    description = "Toggle your AFK status.",
    permission = "sleepy.command.afk",
    playerOnly = true,
)
class AfkCommand : BrigadierCommand() {
    @BrigadierExecutor
    fun execute(source: CommandSourceStack) {
        toggleAfk(source.sender as Player)
    }
}

internal fun toggleAfk(player: Player) {
    val enabled = Sleepy.activity.toggleManual(player)
    val key = if (enabled) "sleepy.message.afk.enabled" else "sleepy.message.afk.disabled"
    Messages.send(player, key)
}
