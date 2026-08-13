package dev.lumas.sleepy.model

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class Position(
    @field:Setting var x: Double = 0.0,
    @field:Setting var y: Double = 0.0,
    @field:Setting var z: Double = 0.0,
)
