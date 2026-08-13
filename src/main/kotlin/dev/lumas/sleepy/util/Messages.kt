package dev.lumas.sleepy.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

object Messages {
    fun send(sender: CommandSender, key: String, vararg arguments: Component) {
        val locale = (sender as? Player)?.locale() ?: Locale.US
        val body = GlobalTranslator.render(Component.translatable(key, *arguments), locale)
        if (PlainTextComponentSerializer.plainText().serialize(body).isEmpty()) return

        val message = GlobalTranslator.render(Component.translatable("sleepy.message.prefix"), locale)
            .append(Component.space())
            .append(body)
        sender.sendMessage(message)
    }

    fun sendUnprefixed(sender: CommandSender, key: String, vararg arguments: Component) {
        val locale = (sender as? Player)?.locale() ?: Locale.US
        val message = GlobalTranslator.render(Component.translatable(key, *arguments), locale)
        if (PlainTextComponentSerializer.plainText().serialize(message).isEmpty()) return
        sender.sendMessage(message)
    }
}
