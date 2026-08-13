package dev.lumas.sleepy.command.subcommand

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.BrigadierExecutor
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierSubCommand
import dev.lumas.sleepy.command.CommandManager
import dev.lumas.sleepy.config.SleepyConfig
import dev.lumas.sleepy.config.TranslatorService
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.command.brigadier.CommandSourceStack

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "reload",
    description = "Reload Sleepy.",
    permission = "sleepy.command.reload",
    usage = "/<command> reload",
    parent = CommandManager::class,
)
class ReloadCommand : BrigadierSubCommand {
    @BrigadierExecutor
    fun execute(source: CommandSourceStack) {
        SleepyConfig.reload()
        TranslatorService.instance.reload()
        Messages.send(source.sender, "sleepy.message.reload")
    }
}
