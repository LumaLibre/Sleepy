package dev.lumas.sleepy.model

import java.util.UUID

data class PlaytimeEntry(
    val uuid: UUID,
    val name: String,
    val playtimeSeconds: Long,
    val afkSeconds: Long,
    val points: Long = 0,
)
