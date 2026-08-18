package dev.lumas.sleepy.model

data class SpawnPoint(
    val position: WorldPosition,
    val pose: SpawnPose,
) {
    companion object {
        fun parse(value: String): SpawnPoint {
            val parts = value.split(',').map(String::trim)
            val last = parts.lastOrNull().orEmpty()
            if (parts.size <= 4 || last.toDoubleOrNull() != null) {
                return SpawnPoint(WorldPosition.parse(value), SpawnPose.STAND)
            }

            val pose = SpawnPose.find(last)
            require(pose != null) {
                "SpawnPoint '$value' has an unknown pose '$last'; use one of ${SpawnPose.names}"
            }
            return SpawnPoint(WorldPosition.parse(parts.dropLast(1).joinToString(",")), pose)
        }
    }
}
