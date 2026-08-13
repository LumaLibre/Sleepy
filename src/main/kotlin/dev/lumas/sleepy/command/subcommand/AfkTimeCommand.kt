package dev.lumas.sleepy.command.subcommand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierSubCommand
import dev.lumas.sleepy.command.CommandManager
import dev.lumas.sleepy.command.RecordedTimeCommands
import dev.lumas.sleepy.command.RecordedTimeMetric
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "afktime",
    description = "View recorded AFK time and its leaderboard.",
    permission = "sleepy.command.afktime",
    usage = "/<command> afktime [player] | /<command> afktime top [count]",
    parent = CommandManager::class,
)
class AfkTimeCommand : BrigadierSubCommand {
    override fun buildTree(
        builder: LiteralArgumentBuilder<CommandSourceStack>,
        commands: Commands,
    ): LiteralArgumentBuilder<CommandSourceStack> {
        builder.executes { context ->
            RecordedTimeCommands.view(context.source.sender, null, RecordedTimeMetric.AFK_TIME)
            Command.SINGLE_SUCCESS
        }
        builder.then(
            Commands.literal("top")
                .executes { context ->
                    RecordedTimeCommands.showTop(
                        context.source.sender,
                        RecordedTimeCommands.DEFAULT_TOP_SIZE,
                        RecordedTimeMetric.AFK_TIME,
                    )
                    Command.SINGLE_SUCCESS
                }
                .then(
                    Commands.argument(
                        "count",
                        IntegerArgumentType.integer(1, RecordedTimeCommands.MAX_TOP_SIZE),
                    ).executes { context ->
                        RecordedTimeCommands.showTop(
                            context.source.sender,
                            IntegerArgumentType.getInteger(context, "count"),
                            RecordedTimeMetric.AFK_TIME,
                        )
                        Command.SINGLE_SUCCESS
                    },
                ),
        )
        builder.then(
            Commands.argument("player", StringArgumentType.word())
                .suggests { context, suggestions ->
                    RecordedTimeCommands.suggestPlayers(
                        context,
                        suggestions,
                        RecordedTimeMetric.AFK_TIME.othersPermission,
                    )
                }
                .executes { context ->
                    RecordedTimeCommands.view(
                        context.source.sender,
                        StringArgumentType.getString(context, "player"),
                        RecordedTimeMetric.AFK_TIME,
                    )
                    Command.SINGLE_SUCCESS
                },
        )
        return builder
    }
}
