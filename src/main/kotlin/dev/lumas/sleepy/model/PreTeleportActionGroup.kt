package dev.lumas.sleepy.model

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class PreTeleportActionGroup(
    @field:Setting("before-seconds")
    @field:Comment("Execute this group once, this many seconds before the AFK teleport.")
    var beforeSeconds: Long = 30,

    @field:Setting
    @field:Comment(
        "Console commands without a leading slash. All strings support <player>, <uuid>, " +
            "<afk_seconds>, <seconds_before_teleport>, and PlaceholderAPI placeholders.",
    )
    var commands: MutableList<String> = mutableListOf(),

    @field:Setting
    @field:Comment("MiniMessage messages sent directly to the player.")
    var messages: MutableList<String> = mutableListOf(
        "<#b986f9><b>Info</b></#b986f9> <dark_gray>»</dark_gray> <gray>You have been AFK for ten minutes, you will be teleported soon!"
    ),

    @field:Setting
    @field:Comment("Adventure titles shown to the player.")
    var titles: MutableList<TitleAction> = mutableListOf(),
) {
    val isEmpty: Boolean
        get() = commands.isEmpty() && messages.isEmpty() && titles.isEmpty()
}
