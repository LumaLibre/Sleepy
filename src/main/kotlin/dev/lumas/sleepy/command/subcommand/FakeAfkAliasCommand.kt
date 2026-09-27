package dev.lumas.sleepy.command.subcommand

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.BrigadierExecutor
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierSubCommand
import dev.lumas.sleepy.command.CommandManager
import dev.lumas.sleepy.command.toggleFakeAfk
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "fakeafk",
    description = "Appear AFK while still active.",
    permission = "sleepy.command.fakeafk",
    usage = "/<command> fakeafk",
    parent = CommandManager::class,
    playerOnly = true,
)
class FakeAfkAliasCommand : BrigadierSubCommand {
    @BrigadierExecutor
    fun execute(source: CommandSourceStack) {
        toggleFakeAfk(source.sender as Player)
    }
}
