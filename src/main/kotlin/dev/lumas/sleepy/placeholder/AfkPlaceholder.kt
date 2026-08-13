package dev.lumas.sleepy.placeholder

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.PlaceholderMeta
import dev.lumas.core.annotation.Register
import dev.lumas.sleepy.Sleepy
import org.bukkit.OfflinePlayer

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
    identifier = "afk",
    parent = PlaceholderManager::class,
)
class AfkPlaceholder : PlaceholderModule {
    override fun onRequest(plugin: Sleepy, player: OfflinePlayer?, args: List<String>): String? =
        when (args.firstOrNull()?.lowercase()) {
            "seconds" -> (activity(player)?.currentAfkSeconds ?: 0).toString()
            else -> null
        }
}
