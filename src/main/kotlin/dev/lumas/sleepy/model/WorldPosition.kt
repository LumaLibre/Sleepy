package dev.lumas.sleepy.model

import org.bukkit.Location
import org.bukkit.World

data class WorldPosition(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float? = null,
    val pitch: Float? = null,
) {
    fun isIn(world: World): Boolean =
        this.world.equals(world.name, ignoreCase = true) ||
            this.world.equals(world.key.asString(), ignoreCase = true)

    fun distanceSquared(location: Location): Double {
        val deltaX = x - location.x
        val deltaY = y - location.y
        val deltaZ = z - location.z
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
    }

    fun toLocation(world: World): Location {
        require(isIn(world)) {
            "World '${world.key.asString()}' does not match configured world '${this.world}'"
        }
        return Location(world, x, y, z, yaw ?: 0f, pitch ?: 0f)
    }

    companion object {
        fun parse(value: String): WorldPosition {
            val parts = value.split(',').map(String::trim)
            require(parts.size == 4 || parts.size == 6) {
                "Coordinate '$value' must use world,x,y,z or world,x,y,z,yaw,pitch"
            }

            val world = parts[0]
            require(world.isNotEmpty()) { "Coordinate '$value' must include a world" }
            val x = parts.coordinate(value, 1, "x")
            val y = parts.coordinate(value, 2, "y")
            val z = parts.coordinate(value, 3, "z")
            val yaw = parts.optionalAngle(value, 4, "yaw")
            val pitch = parts.optionalAngle(value, 5, "pitch")
            return WorldPosition(world, x, y, z, yaw, pitch)
        }

        private fun List<String>.coordinate(source: String, index: Int, name: String): Double {
            val coordinate = get(index).toDoubleOrNull()
            require(coordinate != null && coordinate.isFinite()) {
                "Coordinate '$source' has an invalid $name value"
            }
            return coordinate
        }

        private fun List<String>.optionalAngle(source: String, index: Int, name: String): Float? {
            if (size == 4) return null
            val angle = get(index).toFloatOrNull()
            require(angle != null && angle.isFinite()) {
                "Coordinate '$source' has an invalid $name value"
            }
            return angle
        }
    }
}
