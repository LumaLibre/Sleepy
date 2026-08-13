package dev.lumas.sleepy.placeholder

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.PlaceholderMeta
import dev.lumas.core.annotation.Register
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.config.SleepyConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.OfflinePlayer

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
    identifier = "isafk",
    parent = PlaceholderManager::class,
)
class IsAfkPlaceholder : PlaceholderModule {
    override fun onRequest(plugin: Sleepy, player: OfflinePlayer?, args: List<String>): String {
        val configured = if (activity(player)?.isAfk == true) {
            SleepyConfig.instance.afkPlaceholder
        } else {
            SleepyConfig.instance.notAfkPlaceholder
        }
        return LEGACY.serialize(MINI_MESSAGE.deserialize(configured))
    }

    companion object {
        private val MINI_MESSAGE = MiniMessage.miniMessage()
        private val LEGACY = LegacyComponentSerializer.legacySection()
    }
}
