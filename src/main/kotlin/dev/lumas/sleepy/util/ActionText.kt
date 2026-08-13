package dev.lumas.sleepy.util

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object ActionText {
    fun resolve(
        player: Player,
        configured: String,
        afkSeconds: Long,
        secondsBeforeTeleport: Long,
    ): String {
        val resolved = configured
            .replace("<player>", player.name)
            .replace("<uuid>", player.uniqueId.toString())
            .replace("<afk_seconds>", afkSeconds.toString())
            .replace("<seconds_before_teleport>", secondsBeforeTeleport.toString())

        return if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceholderAPI.setPlaceholders(player, resolved)
        } else {
            resolved
        }
    }
}
