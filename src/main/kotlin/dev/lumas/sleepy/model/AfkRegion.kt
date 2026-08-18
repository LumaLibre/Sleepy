package dev.lumas.sleepy.model

import org.bukkit.Location
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@ConfigSerializable
data class AfkRegion(
    @field:Setting
    var name: String = "spawn-afk",

    @field:Setting
    @field:Comment("The first inclusive corner, using world,x,y,z or world,x,y,z,yaw,pitch.")
    var min: String = "minecraft:overworld,-5,60,-5",

    @field:Setting
    @field:Comment("The opposite inclusive corner. Values can be given in either order.")
    var max: String = "minecraft:overworld,5,80,5",

    @field:Setting("spawn-locations")
    @field:Comment(
        "Locations using world,x,y,z or world,x,y,z,yaw,pitch. If empty, a random position is used.\n" +
            "Append a pose to seat the player there with GSit: stand, sit, lay, lay_back, bellyflop, spin.",
    )
    var spawnLocations: MutableList<String> = mutableListOf(
        "minecraft:overworld,0,65,0",
    ),
) {
    val world: String get() = resolved().minimum.world
    val minX: Double get() = resolved().minX
    val minY: Double get() = resolved().minY
    val minZ: Double get() = resolved().minZ
    val maxX: Double get() = resolved().maxX
    val maxY: Double get() = resolved().maxY
    val maxZ: Double get() = resolved().maxZ

    @Transient
    private var resolvedRegion: ResolvedAfkRegion? = null

    fun contains(position: WorldPosition): Boolean =
        resolved().let { region ->
            worldsMatch(position.world, region.minimum.world) &&
                region.contains(position.x, position.y, position.z)
        }

    fun contains(location: Location): Boolean =
        resolved().let { region ->
            region.minimum.isIn(location.world) && region.contains(location.x, location.y, location.z)
        }

    fun validate() {
        val resolved = parseResolved()
        val minimum = resolved.minimum
        val maximum = resolved.maximum
        require(worldsMatch(minimum.world, maximum.world)) {
            "AFK region '$name' corners must use the same world"
        }
        resolved.configuredSpawns.forEachIndexed { index, spawn ->
            val position = spawn.position
            require(
                worldsMatch(position.world, minimum.world) &&
                    resolved.contains(position.x, position.y, position.z),
            ) {
                "AFK region '$name' spawn-locations[$index] ($position) must be inside its bounding box"
            }
        }
        resolvedRegion = resolved
    }

    val spawnCount: Int get() = resolved().configuredSpawns.size

    fun spawn(index: Int): Spawn {
        val resolved = resolved()
        resolved.configuredSpawns.getOrNull(index)?.let { configured ->
            return Spawn(configured.position, configured.pose, configured = true)
        }

        val random = ThreadLocalRandom.current()
        return Spawn(
            WorldPosition(
                resolved.minimum.world,
                random.coordinate(resolved.minX, resolved.maxX),
                resolved.minY,
                random.coordinate(resolved.minZ, resolved.maxZ),
            ),
            SpawnPose.STAND,
            configured = false,
        )
    }

    fun resolveSpawn(world: World, spawn: Spawn): Location? {
        require(resolved().minimum.isIn(world)) {
            "World '${world.key.asString()}' does not belong to AFK region '$name'"
        }
        if (spawn.configured) {
            return spawn.position.toLocation(world)
        }

        val x = floor(spawn.position.x).toInt()
        val z = floor(spawn.position.z).toInt()
        val top = minOf(world.maxHeight - 2, floor(maxY).toInt())
        val bottom = maxOf(world.minHeight + 1, ceil(minY).toInt())
        for (y in top downTo bottom) {
            val feet = world.getBlockAt(x, y, z)
            val head = world.getBlockAt(x, y + 1, z)
            val floor = world.getBlockAt(x, y - 1, z)
            if (feet.isPassable && head.isPassable && floor.type.isSolid) {
                return Location(world, spawn.position.x, y.toDouble(), spawn.position.z)
            }
        }
        return null
    }

    class Spawn internal constructor(
        val position: WorldPosition,
        val pose: SpawnPose,
        val configured: Boolean,
    )

    private fun resolved(): ResolvedAfkRegion =
        resolvedRegion ?: synchronized(this) {
            resolvedRegion ?: parseResolved().also { resolvedRegion = it }
        }

    private fun parseResolved(): ResolvedAfkRegion = ResolvedAfkRegion(
        minimum = WorldPosition.parse(min),
        maximum = WorldPosition.parse(max),
        configuredSpawns = spawnLocations.mapIndexed { index, configured ->
            try {
                SpawnPoint.parse(configured)
            } catch (exception: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "AFK region '$name' spawn-locations[$index]: ${exception.message}",
                    exception,
                )
            }
        },
    )

    private fun worldsMatch(first: String, second: String): Boolean {
        if (first.equals(second, ignoreCase = true)) return true
        val firstWorld = resolveWorld(first) ?: return false
        val secondWorld = resolveWorld(second) ?: return false
        return firstWorld.uid == secondWorld.uid
    }

    private fun resolveWorld(reference: String): World? =
        Bukkit.getWorld(reference) ?: NamespacedKey.fromString(reference)?.let(Bukkit::getWorld)

    private fun ThreadLocalRandom.coordinate(minimum: Double, maximum: Double): Double =
        if (minimum == maximum) minimum else nextDouble(minimum, maximum)

    private data class ResolvedAfkRegion(
        val minimum: WorldPosition,
        val maximum: WorldPosition,
        val configuredSpawns: List<SpawnPoint>,
    ) {
        val minX: Double = min(minimum.x, maximum.x)
        val minY: Double = min(minimum.y, maximum.y)
        val minZ: Double = min(minimum.z, maximum.z)
        val maxX: Double = max(minimum.x, maximum.x)
        val maxY: Double = max(minimum.y, maximum.y)
        val maxZ: Double = max(minimum.z, maximum.z)

        fun contains(x: Double, y: Double, z: Double): Boolean =
            x.isFinite() && y.isFinite() && z.isFinite() &&
                x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }
}
