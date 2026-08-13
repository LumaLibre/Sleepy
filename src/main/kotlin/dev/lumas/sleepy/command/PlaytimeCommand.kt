package dev.lumas.sleepy.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "playtime",
    description = "View recorded playtime and its leaderboard.",
    permission = "sleepy.command.playtime",
    usage = "/<command> [player] | /<command> top [count]",
)
class PlaytimeCommand : BrigadierCommand() {
    override fun buildTree(
        builder: LiteralArgumentBuilder<CommandSourceStack>,
        commands: Commands,
    ): LiteralArgumentBuilder<CommandSourceStack> {
        builder.executes { context ->
            RecordedTimeCommands.view(context.source.sender, null, RecordedTimeMetric.PLAYTIME)
            Command.SINGLE_SUCCESS
        }

        builder.then(
            Commands.literal("top")
                .executes { context ->
                    RecordedTimeCommands.showTop(
                        context.source.sender,
                        RecordedTimeCommands.DEFAULT_TOP_SIZE,
                        RecordedTimeMetric.PLAYTIME,
                    )
                    Command.SINGLE_SUCCESS
                }
                .then(
                    Commands.argument("count", IntegerArgumentType.integer(1, RecordedTimeCommands.MAX_TOP_SIZE))
                        .executes { context ->
                            RecordedTimeCommands.showTop(
                                context.source.sender,
                                IntegerArgumentType.getInteger(context, "count"),
                                RecordedTimeMetric.PLAYTIME,
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
                        RecordedTimeMetric.PLAYTIME.othersPermission,
                    )
                }
                .executes { context ->
                    RecordedTimeCommands.view(
                        context.source.sender,
                        StringArgumentType.getString(context, "player"),
                        RecordedTimeMetric.PLAYTIME,
                    )
                    Command.SINGLE_SUCCESS
                },
        )
        return builder
    }

}
