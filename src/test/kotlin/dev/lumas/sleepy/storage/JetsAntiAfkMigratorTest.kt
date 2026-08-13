package dev.lumas.sleepy.storage

import dev.lumas.sleepy.model.PlaytimeEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.DriverManager
import java.util.UUID

class JetsAntiAfkMigratorTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `migrates Jets SQLite once without reducing newer playtime`() {
        val jets = temporaryDirectory.resolve("data.db")
        val uuid = UUID.randomUUID()
        DriverManager.getConnection("jdbc:sqlite:$jets").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(testSql("jets-schema.sql"))
            }
            connection.prepareStatement(testSql("jets-insert.sql")).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setLong(2, 1200)
                statement.executeUpdate()
            }
        }

        val repository = SQLitePlaytimeRepository(temporaryDirectory.resolve("sleepy.db"))
        repository.initialize()
        val migrator = JetsAntiAfkMigrator(repository)
        assertEquals(1, migrator.migrate(jets))
        assertEquals(1200, repository.load(uuid, "Player").playtimeSeconds)
        assertEquals(0, migrator.migrate(jets))

        repository.save(PlaytimeEntry(uuid, "Player", 1300, 50, 12))
        repository.savePoints(PlaytimeEntry(uuid, "Player", 1300, 50, 12))
        repository.save(PlaytimeEntry(uuid, "Player", 1250, 40, 12))
        val stored = repository.load(uuid, "Player")
        assertEquals(1300, stored.playtimeSeconds)
        assertEquals(50, stored.afkSeconds)
        assertEquals(12, stored.points)
        repository.savePoints(stored.copy(points = 3))
        repository.save(stored.copy(points = 12))
        assertEquals(3, repository.load(uuid, "Player").points)
        assertEquals(uuid, repository.find("Player")?.uuid)
        assertEquals(uuid, repository.find(uuid.toString())?.uuid)
        val afkLeader = UUID.randomUUID()
        repository.save(PlaytimeEntry(afkLeader, "AfkLeader", 1, 100, 0))
        assertEquals(uuid, repository.top(1).single().uuid)
        assertEquals(afkLeader, repository.topAfk(1).single().uuid)
        repository.updateName(uuid, "UpdatedName")
        assertEquals("UpdatedName", repository.find(uuid.toString())?.name)
        repository.close()
    }

    private fun testSql(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/sql/$name")) { "Missing test SQL resource: $name" }
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText().trim() }
}
