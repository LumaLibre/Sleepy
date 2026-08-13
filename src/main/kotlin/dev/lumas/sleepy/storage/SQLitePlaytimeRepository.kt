package dev.lumas.sleepy.storage

import dev.lumas.sleepy.model.PlaytimeEntry
import org.sqlite.SQLiteConfig
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID

class SQLitePlaytimeRepository(private val file: Path) : PlaytimeRepository {
    private val jdbcUrl = "jdbc:sqlite:${file.toAbsolutePath().normalize()}"
    private val connectionProperties = SQLiteConfig().apply {
        setBusyTimeout(5_000)
        enforceForeignKeys(true)
        setJournalMode(SQLiteConfig.JournalMode.WAL)
        setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)
    }.toProperties()

    override fun initialize() {
        try {
            file.toAbsolutePath().parent?.let(Files::createDirectories)
            Class.forName("org.sqlite.JDBC")
            connect().use { connection ->
                connection.createStatement().use { statement ->
                    SqlStatement.SCHEMA.text
                        .split(';')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .forEach(statement::executeUpdate)
                }
                ensurePointsColumn(connection)
            }
        } catch (exception: ReflectiveOperationException) {
            throw IllegalStateException("Unable to initialize Sleepy's SQLite database", exception)
        } catch (exception: SQLException) {
            throw IllegalStateException("Unable to initialize Sleepy's SQLite database", exception)
        }
    }

    override fun load(uuid: UUID, currentName: String): PlaytimeEntry = try {
        connect().use { connection ->
            connection.prepareStatement(SqlStatement.LOAD_PLAYER.text).use { statement ->
                statement.setString(1, uuid.toString())
                statement.executeQuery().use { result ->
                    if (result.next()) {
                        PlaytimeEntry(
                            uuid,
                            currentName,
                            result.getLong("playtime_seconds"),
                            result.getLong("afk_seconds"),
                            result.getLong("points"),
                        )
                    } else {
                        PlaytimeEntry(uuid, currentName, 0, 0)
                    }
                }
            }
        }
    } catch (exception: SQLException) {
        throw databaseFailure("load $uuid", exception)
    }

    override fun save(entry: PlaytimeEntry) {
        try {
            connect().use { connection ->
                connection.prepareStatement(SqlStatement.SAVE_PLAYER.text).use { statement ->
                    statement.setString(1, entry.uuid.toString())
                    statement.setString(2, safeName(entry))
                    statement.setLong(3, entry.playtimeSeconds)
                    statement.setLong(4, entry.afkSeconds)
                    statement.setLong(5, System.currentTimeMillis())
                    statement.setLong(6, entry.points)
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw databaseFailure("save ${entry.uuid}", exception)
        }
    }

    override fun savePoints(entry: PlaytimeEntry) {
        try {
            connect().use { connection ->
                connection.prepareStatement(SqlStatement.SAVE_POINTS.text).use { statement ->
                    statement.setString(1, entry.uuid.toString())
                    statement.setString(2, safeName(entry))
                    statement.setLong(3, entry.playtimeSeconds)
                    statement.setLong(4, entry.afkSeconds)
                    statement.setLong(5, entry.points)
                    statement.setLong(6, System.currentTimeMillis())
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw databaseFailure("save ${entry.uuid}'s points", exception)
        }
    }

    override fun updateName(uuid: UUID, name: String) {
        try {
            connect().use { connection ->
                connection.prepareStatement(SqlStatement.UPDATE_NAME.text).use { statement ->
                    statement.setString(1, name.take(16))
                    statement.setLong(2, System.currentTimeMillis())
                    statement.setString(3, uuid.toString())
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw databaseFailure("update $uuid's name", exception)
        }
    }

    override fun find(nameOrUuid: String): PlaytimeEntry? {
        val uuid = runCatching { UUID.fromString(nameOrUuid) }.getOrNull()
        val sql = if (uuid == null) SqlStatement.FIND_BY_NAME else SqlStatement.FIND_BY_UUID
        return try {
            connect().use { connection ->
                connection.prepareStatement(sql.text).use { statement ->
                    statement.setString(1, uuid?.toString() ?: nameOrUuid)
                    statement.executeQuery().use { result -> if (result.next()) result.toEntry() else null }
                }
            }
        } catch (exception: SQLException) {
            throw databaseFailure("find $nameOrUuid", exception)
        }
    }

    override fun top(limit: Int): List<PlaytimeEntry> = try {
        loadTop(SqlStatement.TOP_PLAYTIME, limit)
    } catch (exception: SQLException) {
        throw databaseFailure("load playtime leaderboard", exception)
    }

    override fun topAfk(limit: Int): List<PlaytimeEntry> = try {
        loadTop(SqlStatement.TOP_AFK_TIME, limit)
    } catch (exception: SQLException) {
        throw databaseFailure("load AFK-time leaderboard", exception)
    }

    private fun loadTop(sql: SqlStatement, limit: Int): List<PlaytimeEntry> =
        connect().use { connection ->
            connection.prepareStatement(sql.text).use { statement ->
                statement.setInt(1, limit)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) add(result.toEntry())
                    }
                }
            }
        }

    override fun migrationCompleted(sourceKey: String): Boolean = try {
        connect().use { connection ->
            connection.prepareStatement(SqlStatement.MIGRATION_EXISTS.text).use { statement ->
                statement.setString(1, sourceKey)
                statement.executeQuery().use(ResultSet::next)
            }
        }
    } catch (exception: SQLException) {
        throw databaseFailure("check migration", exception)
    }

    override fun markMigrationCompleted(sourceKey: String, importedRows: Int) {
        try {
            connect().use { connection ->
                connection.prepareStatement(SqlStatement.RECORD_MIGRATION.text).use { statement ->
                    statement.setString(1, sourceKey)
                    statement.setInt(2, importedRows)
                    statement.setLong(3, System.currentTimeMillis())
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            throw databaseFailure("record migration", exception)
        }
    }

    override fun close() = Unit

    private fun connect(): Connection = DriverManager.getConnection(jdbcUrl, connectionProperties)

    private fun ResultSet.toEntry(): PlaytimeEntry = PlaytimeEntry(
        UUID.fromString(getString("uuid")),
        getString("last_name"),
        getLong("playtime_seconds"),
        getLong("afk_seconds"),
        getLong("points"),
    )

    private fun ensurePointsColumn(connection: Connection) {
        val columns = connection.createStatement().use { statement ->
            statement.executeQuery(SqlStatement.PLAYER_COLUMNS.text).use { result ->
                buildSet {
                    while (result.next()) add(result.getString("name"))
                }
            }
        }
        when {
            "points" in columns -> Unit
            "afk_points" in columns -> connection.createStatement().use {
                it.executeUpdate(SqlStatement.RENAME_POINTS_COLUMN.text)
            }
            else -> connection.createStatement().use {
                it.executeUpdate(SqlStatement.ADD_POINTS_COLUMN.text)
            }
        }
    }

    private fun safeName(entry: PlaytimeEntry): String =
        entry.name.ifBlank { entry.uuid.toString().take(16) }.take(16)

    private fun databaseFailure(operation: String, cause: SQLException) =
        IllegalStateException("Unable to $operation in Sleepy's database", cause)
}
