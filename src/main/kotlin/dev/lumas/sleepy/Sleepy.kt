package dev.lumas.sleepy

import dev.lumas.core.manager.Modules
import dev.lumas.sleepy.config.SleepyConfig
import dev.lumas.sleepy.integration.shops.ShopsIntegration
import dev.lumas.sleepy.service.ActivityService
import dev.lumas.sleepy.storage.JetsAntiAfkMigrator
import dev.lumas.sleepy.storage.PlaytimeRepository
import dev.lumas.sleepy.storage.SQLitePlaytimeRepository
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level

class Sleepy : JavaPlugin() {
    private lateinit var modules: Modules
    private var shopsCurrencyRegistered = false

    override fun onLoad() {
        instance = this
        modules = Modules(this)
        if (server.pluginManager.getPlugin("Shops") != null) {
            try {
                ShopsIntegration.register()
                shopsCurrencyRegistered = true
                logger.info("Registered oneira as the Shops currency sleepy:oneira")
            } catch (exception: RuntimeException) {
                logger.log(Level.SEVERE, "Unable to register the oneira Shops currency", exception)
            } catch (error: LinkageError) {
                logger.log(Level.SEVERE, "Installed Shops version does not support the oneira currency integration", error)
            }
        }
    }

    override fun onEnable() {
        val startupStarted = System.nanoTime()

        val config = step("Loading configuration") {
            SleepyConfig.instance
        }
        val databaseFile = dataPath.resolve(config.storage.fileName).toAbsolutePath().normalize()
        repository = step("Initializing SQLite database at $databaseFile") {
            SQLitePlaytimeRepository(databaseFile).also(PlaytimeRepository::initialize)
        }
        step("Checking legacy playtime migration") {
            migrateLegacyData(config)
        }

        if (!shopsCurrencyRegistered) {
            logger.info("oneira Shops currency integration is inactive")
        }

        activity = step("Creating activity service") {
            ActivityService(repository)
        }
        step("Registering LumaCore modules") {
            modules.register()
        }
        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        step("Starting activity tracking for ${onlinePlayers.size} online player(s)") {
            onlinePlayers.forEach(activity::join)
        }
        step("Scheduling the initial playtime leaderboard refresh") {
            activity.refreshLeaderboard()
        }

        logger.info("Finished in ${elapsedMillis(startupStarted)} ms")
    }

    override fun onDisable() {
        if (shopsCurrencyRegistered) ShopsIntegration.unregister()
        if (::modules.isInitialized) modules.unregister()
        if (hasActivity()) activity.shutdown()
        if (hasRepository()) repository.close()
        logger.info("Done, bye-bye")
    }

    private fun migrateLegacyData(config: SleepyConfig) {
        if (!config.migration.enabled) {
            logger.info("JetsAntiAFKPro migration is disabled")
            return
        }
        val configured = Path.of(config.migration.jetsSqlitePath)
        val source = if (configured.isAbsolute) configured else Path.of(System.getProperty("user.dir")).resolve(configured)
        val normalized = source.toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalized)) {
            logger.info("No JetsAntiAFKPro SQLite database found at $normalized; skipping migration")
            return
        }

        logger.info("Found JetsAntiAFKPro SQLite database at $normalized; checking migration state")
        try {
            val rows = JetsAntiAfkMigrator(repository).migrate(normalized) { imported ->
                logger.info("Migrated $imported JetsAntiAFKPro playtime records so far")
            }
            if (rows > 0) {
                logger.info("Migrated $rows JetsAntiAFKPro playtime records from $normalized")
            } else {
                logger.info("JetsAntiAFKPro migration is already complete or contained no records")
            }
        } catch (exception: RuntimeException) {
            logger.log(Level.SEVERE, "Unable to migrate legacy playtime data", exception)
            throw exception
        }
    }

    private fun <T> step(description: String, action: () -> T): T {
        logger.info("$description...")
        val started = System.nanoTime()
        return try {
            action().also {
                logger.info("$description completed in ${elapsedMillis(started)}ms")
            }
        } catch (exception: Throwable) {
            logger.log(Level.SEVERE, "$description failed after ${elapsedMillis(started)} ms", exception)
            throw exception
        }
    }

    private fun elapsedMillis(started: Long): Long =
        (System.nanoTime() - started) / 1_000_000L

    companion object {
        lateinit var instance: Sleepy
            private set

        lateinit var activity: ActivityService
            private set

        lateinit var repository: PlaytimeRepository
            private set

        private fun hasActivity(): Boolean = ::activity.isInitialized

        private fun hasRepository(): Boolean = ::repository.isInitialized
    }
}
