package dev.lumas.sleepy.model

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class CommandAction(
    @field:Setting("every-seconds")
    @field:Comment("Run these commands each time the player's current AFK duration reaches this multiple.")
    var everySeconds: Long = 300,

    @field:Setting
    @field:Comment(
        "Console commands without a leading slash. Supports <player>, <uuid>, <afk_seconds>, " +
            "<seconds_before_teleport>, and PlaceholderAPI placeholders.",
    )
    var commands: MutableList<String> = mutableListOf(),
)
