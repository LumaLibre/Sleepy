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
    identifier = "afktop",
    parent = PlaceholderManager::class,
)
class AfkTopPlaceholder : PlaceholderModule {
    override fun onRequest(plugin: Sleepy, player: OfflinePlayer?, args: List<String>): String? {
        if (args.size < 2) return null
        val rank = args[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val entry = Sleepy.activity.afkLeaderboard.getOrNull(rank - 1) ?: return ""
        return entry.value(args.drop(1))
    }

    private fun PlaytimeEntry.value(args: List<String>): String? {
        val field = args.joinToString("_").lowercase()
        val time = Durations.parts(afkSeconds)
        return when (field) {
            "name", "player" -> name
            "uuid" -> uuid.toString()
            "time", "afktime", "afk_time" -> Durations.format(afkSeconds)
            "seconds", "afk_seconds" -> afkSeconds.toString()
            "days" -> time.days.toString()
            "hours" -> time.hours.toString()
            "minutes" -> time.minutes.toString()
            "remaining_seconds" -> time.seconds.toString()
            else -> null
        }
    }
}
