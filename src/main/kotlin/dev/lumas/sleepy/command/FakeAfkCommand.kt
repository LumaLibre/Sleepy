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
    name = "fakeafk",
    description = "Appear AFK while still active.",
    permission = "sleepy.command.fakeafk",
    usage = "/<command>",
    playerOnly = true,
)
class FakeAfkCommand : BrigadierCommand() {
    @BrigadierExecutor
    fun execute(source: CommandSourceStack) {
        toggleFakeAfk(source.sender as Player)
    }
}

internal fun toggleFakeAfk(player: Player) {
    val enabled = Sleepy.activity.toggleFakeAfk(player)
    val key = if (enabled) "sleepy.message.fakeafk.enabled" else "sleepy.message.fakeafk.disabled"
    Messages.send(player, key)
}
