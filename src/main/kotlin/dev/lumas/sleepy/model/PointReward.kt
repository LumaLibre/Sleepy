package dev.lumas.sleepy.model

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class PointReward(
    @field:Setting("every-seconds")
    @field:Comment("Award points each time the player's total online playtime reaches this multiple.")
    var everySeconds: Long = 3600,

    @field:Setting
    @field:Comment("The number of points to award.")
    var amount: Long = 1,

    @field:Setting("send-message")
    @field:Comment("Tell the player when this reward is awarded.")
    var sendMessage: Boolean = false,
) {
    fun isDue(playtimeSeconds: Long): Boolean =
        playtimeSeconds > 0 && playtimeSeconds % everySeconds == 0L
}
