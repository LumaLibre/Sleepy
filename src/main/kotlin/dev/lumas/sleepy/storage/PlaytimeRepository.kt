package dev.lumas.sleepy.storage

import dev.lumas.sleepy.model.PlaytimeEntry
import java.util.UUID

interface PlaytimeRepository : AutoCloseable {
    fun initialize()
    fun load(uuid: UUID, currentName: String): PlaytimeEntry
    fun save(entry: PlaytimeEntry)
    fun savePoints(entry: PlaytimeEntry)
    fun updateName(uuid: UUID, name: String)
    fun find(nameOrUuid: String): PlaytimeEntry?
    fun top(limit: Int): List<PlaytimeEntry>
    fun topAfk(limit: Int): List<PlaytimeEntry>
    fun migrationCompleted(sourceKey: String): Boolean
    fun markMigrationCompleted(sourceKey: String, importedRows: Int)
    override fun close()
}
