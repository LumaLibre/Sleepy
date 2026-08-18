package dev.lumas.sleepy.model

enum class SpawnPose {
    STAND,
    SIT,
    LAY,
    LAY_BACK,
    BELLYFLOP,
    SPIN,
    ;

    val requiresGSit: Boolean get() = this != STAND

    val isFullPose: Boolean get() = requiresGSit && this != SIT

    companion object {
        val names: String get() = entries.joinToString(", ") { it.name.lowercase() }

        fun find(value: String): SpawnPose? {
            val normalized = value.replace('-', '_')
            return entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        }
    }
}
