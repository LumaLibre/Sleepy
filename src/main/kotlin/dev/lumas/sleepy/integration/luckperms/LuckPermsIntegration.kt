package dev.lumas.sleepy.integration.luckperms

import dev.lumas.core.util.PluginContextLogger
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.context.ContextManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object LuckPermsIntegration {

    private const val PLUGIN_NAME = "LuckPerms"
    private val LOGGER = PluginContextLogger.getPluginLogger()

    private var contextManager: ContextManager? = null
    private var calculator: AfkContextCalculator? = null

    val isAvailable: Boolean
        get() = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME)?.isEnabled == true

    val isRegistered: Boolean
        get() = calculator != null

    fun register(): Boolean {
        if (isRegistered) return true
        if (!isAvailable) return false

        return try {
            val manager = LuckPermsProvider.get().contextManager
            val created = AfkContextCalculator()
            manager.registerCalculator(created)
            contextManager = manager
            calculator = created
            true
        } catch (exception: IllegalStateException) {
            LOGGER.warning("LuckPerms is installed but its API is not loaded yet: ${exception.message}", exception)
            false
        } catch (exception: RuntimeException) {
            LOGGER.warning("Unable to register the AFK LuckPerms context: ${exception.message}", exception)
            false
        } catch (error: LinkageError) {
            LOGGER.warning("Installed LuckPerms version does not support the AFK context calculator", error)
            false
        }
    }

    fun unregister() {
        val registered = calculator ?: return
        try {
            contextManager?.unregisterCalculator(registered)
        } catch (exception: RuntimeException) {
            LOGGER.warning("Unable to unregister the AFK LuckPerms context: ${exception.message}", exception)
        } catch (error: LinkageError) {
            LOGGER.warning("Installed LuckPerms version does not support the AFK context calculator", error)
        }
        calculator = null
        contextManager = null
    }

    fun signalUpdate(player: Player) {
        val manager = contextManager ?: return
        try {
            manager.signalContextUpdate(player)
        } catch (exception: RuntimeException) {
            LOGGER.warning("Unable to signal ${player.name}'s AFK context update: ${exception.message}", exception)
        } catch (error: LinkageError) {
            LOGGER.warning("Installed LuckPerms version does not support the AFK context calculator", error)
        }
    }
}
