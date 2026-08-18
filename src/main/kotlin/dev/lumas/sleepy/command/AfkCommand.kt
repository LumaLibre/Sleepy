package dev.lumas.sleepy.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.BrigadierExecutor
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierCommand
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "afk",
    description = "Toggle your AFK status.",
    permission = "sleepy.command.afk",
    usage = "/<command> [warp]",
    playerOnly = true,
)
class AfkCommand : BrigadierCommand() {
    @BrigadierExecutor
    fun execute(source: CommandSourceStack) {
        toggleAfk(source.sender as Player)
    }

    override fun buildTree(
        root: LiteralArgumentBuilder<CommandSourceStack>,
        commands: Commands,
    ): LiteralArgumentBuilder<CommandSourceStack> = super.buildTree(root, commands).then(afkWarpNode())
}

internal const val AFK_WARP_PERMISSION = "sleepy.command.afk.warp"

internal fun afkWarpNode(): LiteralArgumentBuilder<CommandSourceStack> =
    Commands.literal("warp")
        .requires { source -> source.sender is Player && source.sender.hasPermission(AFK_WARP_PERMISSION) }
        .executes { context ->
            warpAfk(context.source.sender as Player)
            Command.SINGLE_SUCCESS
        }

internal fun toggleAfk(player: Player) {
    val enabled = Sleepy.activity.toggleManual(player)
    val key = if (enabled) "sleepy.message.afk.enabled" else "sleepy.message.afk.disabled"
    Messages.send(player, key)
}

internal fun warpAfk(player: Player) {
    val warped = Sleepy.activity.warpManual(player)
    Messages.send(player, "sleepy.message.afk.enabled")
    if (!warped) Messages.send(player, "sleepy.message.afk.warp.unavailable")
}
