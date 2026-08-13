package dev.lumas.sleepy.storage

import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.model.PlaytimeEntry
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.UUID

class JetsAntiAfkMigrator(
    private val destination: PlaytimeRepository,
) {
    fun migrate(source: Path, onProgress: (Int) -> Unit = {}): Int {
        val normalized = source.toAbsolutePath().normalize()
        if (!Files.isRegularFile(normalized)) return 0
        val sourceKey = "jetsantiafkpro-sqlite:$normalized"
        if (destination.migrationCompleted(sourceKey)) return 0

        var imported = 0
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.getConnection("jdbc:sqlite:$normalized").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(SqlStatement.JETS_SELECT_PLAYTIME.text).use { result ->
                        while (result.next()) {
                            val rawUuid = result.getString("uuid")
                            val uuid = try {
                                UUID.fromString(rawUuid)
                            } catch (_: IllegalArgumentException) {
                                PluginContextLogger.getPluginLogger()
                                    .warning("Skipping invalid JetsAntiAFKPro UUID: $rawUuid")
                                continue
                            }
                            val legacySeconds = maxOf(0, result.getLong("playtime"))
                            val existing = destination.load(uuid, uuid.toString().take(16))
                            destination.save(
                                PlaytimeEntry(
                                    uuid,
                                    existing.name,
                                    maxOf(existing.playtimeSeconds, legacySeconds),
                                    existing.afkSeconds,
                                    existing.points,
                                ),
                            )
                            imported++
                            if (imported % PROGRESS_INTERVAL == 0) onProgress(imported)
                        }
                    }
                }
            }
            destination.markMigrationCompleted(sourceKey, imported)
            return imported
        } catch (exception: ReflectiveOperationException) {
            throw IllegalStateException("Unable to migrate JetsAntiAFKPro database $normalized", exception)
        } catch (exception: java.sql.SQLException) {
            throw IllegalStateException("Unable to migrate JetsAntiAFKPro database $normalized", exception)
        }
    }

    companion object {
        private const val PROGRESS_INTERVAL = 100
    }
}
