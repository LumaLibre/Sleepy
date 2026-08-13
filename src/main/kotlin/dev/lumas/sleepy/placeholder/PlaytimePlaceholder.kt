package dev.lumas.sleepy.placeholder

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.PlaceholderMeta
import dev.lumas.core.annotation.Register
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.util.Durations
import org.bukkit.OfflinePlayer

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
    identifier = "playtime",
    parent = PlaceholderManager::class,
)
class PlaytimePlaceholder : PlaceholderModule {
    override fun onRequest(plugin: Sleepy, player: OfflinePlayer?, args: List<String>): String? {
        val seconds = activity(player)?.playtimeSeconds ?: 0
        return when (args.firstOrNull()?.lowercase()) {
            null -> Durations.format(seconds)
            "seconds" -> seconds.toString()
            else -> null
        }
    }
}
