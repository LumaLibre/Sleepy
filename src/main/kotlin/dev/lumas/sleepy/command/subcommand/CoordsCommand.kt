package dev.lumas.sleepy.command.subcommand

import dev.lumas.core.annotation.Argument
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.BrigadierExecutor
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierSubCommand
import dev.lumas.sleepy.command.CommandManager
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "coords",
    description = "Copy your current coordinates to the clipboard.",
    usage = "/<command> coords [includeYawPitch]",
    permission = "sleepy.command.coords",
    parent = CommandManager::class,
    playerOnly = true,
)
class CoordsCommand : BrigadierSubCommand {
    @BrigadierExecutor
    fun execute(
        source: CommandSourceStack,
        @Argument(value = "includeYawPitch", optional = true) includeYawPitch: Boolean?,
    ) {
        val player = source.sender as Player
        val location = player.location
        val coordinates = buildList {
            add(player.world.key.asString())
            add(location.blockX.toString())
            add(location.blockY.toString())
            add(location.blockZ.toString())
            if (includeYawPitch == true) {
                add(location.yaw.toString())
                add(location.pitch.toString())
            }
        }.joinToString(",")

        val component = Component.text(coordinates, COORDINATE_COLOR)
            .clickEvent(ClickEvent.copyToClipboard(coordinates))
            .hoverEvent(
                Component.translatable("sleepy.message.coords.hover")
                    .color(HOVER_COLOR),
            )
        Messages.send(player, "sleepy.message.coords.output", component)
    }

    companion object {
        private val COORDINATE_COLOR = TextColor.color(0x7DD3FC)
        private val HOVER_COLOR = TextColor.color(0xBAE6FD)
    }
}
