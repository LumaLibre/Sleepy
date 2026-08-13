package dev.lumas.sleepy.util

import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.PlaytimeEntry
import dev.lumas.sleepy.storage.PlaytimeRepository
import org.bukkit.Bukkit

object PlayerNames {
    fun resolve(entry: PlaytimeEntry): String? {
        Sleepy.activity.activity(entry.uuid)?.name?.takeIf { isUsable(it, entry) }?.let { return it }
        entry.name.takeIf { isUsable(it, entry) }?.let { return it }
        return Bukkit.getOfflinePlayer(entry.uuid).name?.takeIf { isUsable(it, entry) }
    }

    fun isUuidFallback(name: String, uuid: java.util.UUID): Boolean =
        name.equals(uuid.toString(), ignoreCase = true) ||
            name.equals(uuid.toString().take(16), ignoreCase = true)

    fun resolveAndRepair(entry: PlaytimeEntry, repository: PlaytimeRepository): PlaytimeEntry? {
        val name = resolve(entry) ?: return null
        if (name != entry.name) {
            try {
                repository.updateName(entry.uuid, name)
            } catch (exception: RuntimeException) {
                LOGGER.warning("Unable to repair ${entry.uuid}'s stored player name: ${exception.message}", exception)
            }
        }
        return if (name == entry.name) entry else entry.copy(name = name)
    }

    private fun isUsable(name: String, entry: PlaytimeEntry): Boolean =
        name.isNotBlank() && !isUuidFallback(name, entry.uuid)

    private val LOGGER = PluginContextLogger.getPluginLogger()
}
