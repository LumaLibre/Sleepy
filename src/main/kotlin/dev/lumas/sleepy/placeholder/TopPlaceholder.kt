package dev.lumas.sleepy.placeholder

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.PlaceholderMeta
import dev.lumas.core.annotation.Register
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.PlaytimeEntry
import dev.lumas.sleepy.util.Durations
import org.bukkit.OfflinePlayer

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
    identifier = "top",
    parent = PlaceholderManager::class,
)
class TopPlaceholder : PlaceholderModule {
    override fun onRequest(plugin: Sleepy, player: OfflinePlayer?, args: List<String>): String? {
        if (args.size < 2) return null
        val rank = args[0].toIntOrNull() ?: return null
        if (rank < 1) return null
        val entry = Sleepy.activity.leaderboard.getOrNull(rank - 1) ?: return ""
        return entry.value(args.drop(1))
    }

    private fun PlaytimeEntry.value(args: List<String>): String? = when (args.joinToString("_").lowercase()) {
        "name", "player" -> name
        "uuid" -> uuid.toString()
        "playtime" -> Durations.format(playtimeSeconds)
        "seconds", "playtime_seconds" -> playtimeSeconds.toString()
        "afk_seconds" -> afkSeconds.toString()
        else -> null
    }
}
