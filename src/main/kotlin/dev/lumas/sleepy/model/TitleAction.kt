package dev.lumas.sleepy.model

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class TitleAction(
    @field:Setting
    @field:Comment("MiniMessage title text. May be empty when only a subtitle is wanted.")
    var title: String = "<yellow>Teleporting soon</yellow>",
    @field:Setting
    @field:Comment("MiniMessage subtitle text.")
    var subtitle: String = "<gray>Move to cancel.</gray>",
    @field:Setting("fade-in-millis")
    var fadeInMillis: Long = 250,
    @field:Setting("stay-millis")
    var stayMillis: Long = 2_000,
    @field:Setting("fade-out-millis")
    var fadeOutMillis: Long = 500,
)
