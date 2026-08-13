package dev.lumas.sleepy.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierCommandManager
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "sleepy",
    description = "Base command.",
    usage = "/<command> <subcommand>",
)
class CommandManager : BrigadierCommandManager() {
    override fun buildRootExecutor(
        root: LiteralArgumentBuilder<CommandSourceStack>,
        commands: Commands,
    ) {
        root.executes { context ->
            Messages.send(context.source.sender, "sleepy.message.command.subcommand_required")
            Command.SINGLE_SUCCESS
        }
    }
}
