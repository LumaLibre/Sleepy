package dev.lumas.sleepy.config

import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.AfkRegion
import dev.lumas.sleepy.model.CommandAction
import dev.lumas.sleepy.model.PreTeleportActionGroup
import dev.lumas.sleepy.model.PointReward
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

@ConfigSerializable
class SleepyConfig {
    @field:Setting
    @field:Comment("Server fallback locale. Locale files live in plugins/Sleepy/locale.")
    var locale: String = "en-US"

    @field:Setting("client-side-translations")
    @field:Comment("Use a player's client locale when a matching locale file exists.")
    var clientSideTranslations: Boolean = true

    @field:Setting("afk-placeholder")
    @field:Comment("MiniMessage returned by %sleepy_isafk% while the player is AFK.")
    var afkPlaceholder: String = " <#ACA5FB>☽"

    @field:Setting("not-afk-placeholder")
    @field:Comment("Returned by %sleepy_isafk% while the player is active.")
    var notAfkPlaceholder: String = ""

    @field:Setting
    var detection: Detection = Detection()

    @field:Setting
    var teleport: Teleport = Teleport()

    @field:Setting("exempt-worlds")
    var exemptWorlds: MutableList<String> = mutableListOf()

    @field:Setting("exempt-keeps-afk-status")
    var exemptKeepsAfkStatus: Boolean = true

    @field:Setting("command-actions")
    var commandActions: MutableList<CommandAction> = mutableListOf()

    @field:Setting("point-rewards")
    @field:Comment("Persistent points awarded at configurable total-online-playtime intervals.")
    var pointRewards: MutableList<PointReward> = mutableListOf(PointReward())

    @field:Setting
    var storage: Storage = Storage()

    @field:Setting
    var migration: Migration = Migration()

    @ConfigSerializable
    class Detection {
        @field:Setting("mark-after-seconds")
        @field:Comment("Seconds without camera or keyboard-backed movement activity before AFK is set.")
        var markAfterSeconds: Long = 420

        @field:Setting("camera-tolerance-degrees")
        @field:Comment("Ignore camera jitter smaller than this many degrees.")
        var cameraToleranceDegrees: Double = 0.02

        @field:Setting("movement-distance")
        @field:Comment(
            "Blocks a player must travel while holding a movement input before that movement counts as activity.",
        )
        var movementDistance: Double = 0.4
    }

    @ConfigSerializable
    class Teleport {
        @field:Setting
        var enabled: Boolean = false

        @field:Setting("after-seconds")
        @field:Comment("Teleport to a random configured region after this many current AFK seconds.")
        var afterSeconds: Long = 600

        @field:Setting("on-manual-afk")
        var onManualAfk: Boolean = true

        @field:Setting
        @field:Comment(
            "AFK bubbles. Entering one is informational and prevents teleporting, but does not force AFK status.",
        )
        var regions: MutableList<AfkRegion> = mutableListOf(AfkRegion())

        @field:Setting("center-poses-on-block")
        var centerPosesOnBlock: Boolean = true

        @field:Setting("unseat-on-return")
        var unseatOnReturn: Boolean = false

        @field:Setting("pre-teleport-actions")
        @field:Comment(
            "Commands, MiniMessage messages, and titles to execute once at configurable countdown points.",
        )
        var preTeleportActions: MutableList<PreTeleportActionGroup> = mutableListOf(PreTeleportActionGroup())
    }

    @ConfigSerializable
    class Storage {
        @field:Setting("file-name")
        @field:Comment("SQLite database file name.")
        var fileName: String = "sleepy.db"

        @field:Setting("save-every-seconds")
        var saveEverySeconds: Long = 60

        @field:Setting("leaderboard-size")
        var leaderboardSize: Int = 100

        val boundedLeaderboardSize: Int get() = leaderboardSize.coerceIn(1, 1_000)
    }

    @ConfigSerializable
    class Migration {
        @field:Setting
        var enabled: Boolean = true

        // I don't need mysql support, so i'm not adding it in
        @field:Setting("jets-sqlite-path")
        @field:Comment(
            "Path to JetsAntiAFKPro's SQLite .db file, relative to the server directory or absolute.",
        )
        var jetsSqlitePath: String = "plugins/JetsAntiAFKPro/data.db"
    }

    private fun validate() {
        require(detection.markAfterSeconds > 0) { "detection.mark-after-seconds must be positive" }
        require(detection.movementDistance.isFinite() && detection.movementDistance > 0) {
            "detection.movement-distance must be finite and positive"
        }
        require(teleport.afterSeconds >= detection.markAfterSeconds) {
            "teleport.after-seconds cannot be lower than detection.mark-after-seconds"
        }
        require(storage.saveEverySeconds > 0) { "storage.save-every-seconds must be positive" }
        teleport.regions.forEach(AfkRegion::validate)
        commandActions.removeIf { it.everySeconds < 1 || it.commands.isEmpty() }
        require(pointRewards.all { it.everySeconds > 0 }) {
            "point-rewards.every-seconds must be positive"
        }
        require(pointRewards.all { it.amount > 0 }) {
            "point-rewards.amount must be positive"
        }
        teleport.preTeleportActions.removeIf { it.isEmpty }
        teleport.preTeleportActions.forEach { action ->
            val maximumWarning = teleport.afterSeconds - detection.markAfterSeconds
            require(action.beforeSeconds in 1..maximumWarning) {
                "teleport.pre-teleport-actions.before-seconds must be between 1 and " +
                        "(teleport.after-seconds - detection.mark-after-seconds)"
            }
        }
    }

    companion object {
        private val loader by lazy {
            YamlConfigurationLoader.builder()
                .path(Sleepy.instance.dataPath.resolve("config.yml"))
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build()
        }

        @Volatile
        private var cached: SleepyConfig? = null

        val instance: SleepyConfig
            get() = cached ?: synchronized(this) {
                cached ?: load().also { cached = it }
            }

        fun reload(): SleepyConfig = load().also { loaded ->
            synchronized(this) {
                cached = loaded
            }
        }

        private fun load(): SleepyConfig = try {
            val root = loader.load()
            migrateLegacyRegionFormat(root)
            val loaded = root.get(SleepyConfig::class.java, SleepyConfig())
            loaded.validate()
            root.set(SleepyConfig::class.java, loaded)
            loader.save(root)
            loaded
        } catch (exception: ConfigurateException) {
            throw IllegalStateException("Unable to load Sleepy's configuration", exception)
        }

        private fun migrateLegacyRegionFormat(root: ConfigurationNode) {
            root.node("teleport", "regions").childrenList().forEach { region ->
                val legacyWorld = region.node("world").string ?: return@forEach
                migrateLegacyPosition(region.node("min"), legacyWorld)
                migrateLegacyPosition(region.node("max"), legacyWorld)

                val spawns = region.node("spawn-locations")
                if (spawns.isList && spawns.childrenList().all(ConfigurationNode::isMap)) {
                    val migrated = spawns.childrenList().map { position -> legacyCoordinate(position, legacyWorld) }
                    spawns.setList(String::class.java, migrated)
                }
                region.removeChild("world")
            }
        }

        private fun migrateLegacyPosition(position: ConfigurationNode, world: String) {
            if (position.isMap) position.set(legacyCoordinate(position, world))
        }

        private fun legacyCoordinate(position: ConfigurationNode, world: String): String =
            listOf(
                world,
                formatNumber(position.node("x").getDouble()),
                formatNumber(position.node("y").getDouble()),
                formatNumber(position.node("z").getDouble()),
            ).joinToString(",")

        private fun formatNumber(value: Double): String =
            java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }
}
