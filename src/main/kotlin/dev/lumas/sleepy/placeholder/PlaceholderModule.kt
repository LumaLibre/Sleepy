package dev.lumas.sleepy.placeholder

import dev.lumas.core.model.placeholder.AbstractPlaceholder
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.PlayerActivity
import org.bukkit.OfflinePlayer

interface PlaceholderModule : AbstractPlaceholder<Sleepy> {
    fun activity(player: OfflinePlayer?): PlayerActivity? =
        player?.player?.let(Sleepy.activity::activity)
}
