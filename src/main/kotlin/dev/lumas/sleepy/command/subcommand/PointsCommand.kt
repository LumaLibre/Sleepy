package dev.lumas.sleepy.command.subcommand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.CommandMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.brigadier.BrigadierSubCommand
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.command.CommandManager
import dev.lumas.sleepy.model.Dreams
import dev.lumas.sleepy.model.PlaytimeEntry
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

@Register(Autowire.BRIGADIER)
@CommandMeta(
    name = "points",
    aliases = ["oneira"],
    description = "View or give oneira.",
    permission = "sleepy.command.points",
    usage = "/<command> oneira [player] | /<command> points give <player> <amount>",
    parent = CommandManager::class,
)
class PointsCommand : BrigadierSubCommand {
    override fun buildTree(
        builder: LiteralArgumentBuilder<CommandSourceStack>,
        commands: Commands,
    ): LiteralArgumentBuilder<CommandSourceStack> {
        builder.executes { context ->
            val sender = context.source.sender
            if (sender is Player) {
                sendBalance(sender, sender.name, Sleepy.activity.activity(sender)?.points ?: 0)
            } else {
                Messages.send(sender, "sleepy.message.oneira.player_required")
            }
            Command.SINGLE_SUCCESS
        }

        builder.then(
            Commands.literal("give")
                .requires { it.sender.hasPermission("sleepy.command.points.give") }
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .suggests(::suggestPlayers)
                        .then(
                            Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes { context ->
                                    give(
                                        context.source.sender,
                                        StringArgumentType.getString(context, "player"),
                                        LongArgumentType.getLong(context, "amount"),
                                    )
                                    Command.SINGLE_SUCCESS
                                },
                        ),
                ),
        )

        builder.then(
            Commands.argument("player", StringArgumentType.word())
                .suggests(::suggestPlayers)
                .executes { context ->
                    viewOther(
                        context.source.sender,
                        StringArgumentType.getString(context, "player"),
                    )
                    Command.SINGLE_SUCCESS
                },
        )
        return builder
    }

    private fun viewOther(sender: CommandSender, target: String) {
        if (sender is Player && sender.name.equals(target, ignoreCase = true)) {
            sendBalance(sender, sender.name, Sleepy.activity.activity(sender)?.points ?: 0)
            return
        }
        if (!sender.hasPermission("sleepy.command.points.others")) {
            Messages.send(sender, "sleepy.message.no_permission")
            return
        }

        Bukkit.getPlayerExact(target)?.let { online ->
            sendBalance(sender, online.name, Sleepy.activity.activity(online)?.points ?: 0)
            return
        }
        findStored(target) { result ->
            reply(sender) {
                if (result == null) {
                    Messages.send(sender, "sleepy.message.oneira.not_found", Component.text(target))
                } else {
                    sendBalance(sender, result.name, result.points)
                }
            }
        }
    }

    private fun give(sender: CommandSender, target: String, amount: Long) {
        Bukkit.getPlayerExact(target)?.let { online ->
            val balance = Sleepy.activity.addPoints(online, amount)
            if (balance == null) {
                Messages.send(sender, "sleepy.message.oneira.not_found", Component.text(target))
                return
            }
            sendGiven(sender, online.name, amount, balance)
            if (sender !is Player || sender.uniqueId != online.uniqueId) {
                Messages.send(
                    online,
                    "sleepy.message.oneira.received",
                    Dreams.display(amount),
                    Dreams.display(balance),
                )
            }
            return
        }

        findStored(target) { stored ->
            if (stored == null) {
                reply(sender) {
                    Messages.send(sender, "sleepy.message.oneira.not_found", Component.text(target))
                }
                return@findStored
            }
            val balance = if (Long.MAX_VALUE - stored.points < amount) Long.MAX_VALUE else stored.points + amount
            Sleepy.repository.savePoints(stored.copy(points = balance))
            reply(sender) { sendGiven(sender, stored.name, amount, balance) }
        }
    }

    private fun findStored(target: String, action: (PlaytimeEntry?) -> Unit) {
        Bukkit.getAsyncScheduler().runNow(Sleepy.instance) {
            val result = Sleepy.repository.find(target)
            action(result)
        }
    }

    private fun sendBalance(sender: CommandSender, target: String, points: Long) {
        val amount = Dreams.display(points)
        if (sender is Player && sender.name.equals(target, ignoreCase = true)) {
            Messages.send(sender, "sleepy.message.oneira.self", amount)
        } else {
            Messages.send(sender, "sleepy.message.oneira.other", Component.text(target), amount)
        }
    }

    private fun sendGiven(sender: CommandSender, target: String, amount: Long, balance: Long) {
        Messages.send(
            sender,
            "sleepy.message.oneira.given",
            Dreams.display(amount),
            Component.text(target),
            Dreams.display(balance),
        )
    }

    private fun reply(sender: CommandSender, action: () -> Unit) {
        if (sender is Player) {
            sender.scheduler.execute(Sleepy.instance, action, null, 1L)
        } else {
            Bukkit.getGlobalRegionScheduler().execute(Sleepy.instance, action)
        }
    }

    private fun suggestPlayers(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        val sender = context.source.sender
        val mayTargetOthers = sender.hasPermission("sleepy.command.points.others") ||
            sender.hasPermission("sleepy.command.points.give")
        Bukkit.getOnlinePlayers().asSequence()
            .map(Player::getName)
            .filter { mayTargetOthers || sender.name.equals(it, ignoreCase = true) }
            .filter { it.lowercase().startsWith(remaining) }
            .forEach(builder::suggest)
        return builder.buildFuture()
    }
}
