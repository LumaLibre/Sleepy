package dev.lumas.sleepy.command.subcommand

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.BrigadierExecutor
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierSubCommand
import dev.lumas.sleepy.command.CommandManager
import dev.lumas.sleepy.command.toggleAfk
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "afk",
    description = "Toggle your AFK status.",
    permission = "sleepy.command.afk",
    usage = "/<command> afk",
    parent = CommandManager::class,
    playerOnly = true,
)
class AfkAliasCommand : BrigadierSubCommand {
    @BrigadierExecutor
    fun execute(source: CommandSourceStack) {
        toggleAfk(source.sender as Player)
    }
}
