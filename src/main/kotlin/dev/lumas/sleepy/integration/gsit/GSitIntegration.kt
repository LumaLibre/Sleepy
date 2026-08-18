package dev.lumas.sleepy.integration.gsit

import dev.geco.gsit.api.GSitAPI
import dev.geco.gsit.model.PoseType
import dev.geco.gsit.model.StopReason
import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.model.SpawnPose
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player

object GSitIntegration {

    private const val PLUGIN_NAME = "GSit"
    private val LOGGER = PluginContextLogger.getPluginLogger()

    val isAvailable: Boolean
        get() = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME)?.isEnabled == true

    fun apply(player: Player, pose: SpawnPose, centerOnBlock: Boolean): Boolean {
        if (!pose.requiresGSit) return false
        if (!isAvailable) {
            LOGGER.warning("AFK spawn pose '$pose' needs GSit, which is not installed")
            return false
        }

        val block = seatBlock(player)
        val rotation = player.location.yaw
        return try {
            when (pose) {
                SpawnPose.SIT -> GSitAPI.createSeat(block, player, true, rotation, centerOnBlock) != null
                else -> {
                    val type = pose.toPoseType() ?: return false
                    GSitAPI.createPose(block, player, type, rotation, centerOnBlock) != null
                }
            }
        } catch (exception: RuntimeException) {
            LOGGER.warning("Unable to pose ${player.name} as $pose: ${exception.message}", exception)
            false
        } catch (error: LinkageError) {
            LOGGER.warning("Installed GSit version does not support AFK spawn poses", error)
            false
        }
    }

    fun release(player: Player) {
        if (!isAvailable) return
        try {
            GSitAPI.getPoseByPlayer(player)?.let { GSitAPI.removePose(it, StopReason.PLUGIN) }
            GSitAPI.getSeatByEntity(player)?.let { GSitAPI.removeSeat(it, StopReason.PLUGIN) }
        } catch (exception: RuntimeException) {
            LOGGER.warning("Unable to get ${player.name} out of their AFK pose: ${exception.message}", exception)
        } catch (error: LinkageError) {
            LOGGER.warning("Installed GSit version does not support AFK spawn poses", error)
        }
    }

    private fun seatBlock(player: Player): Block {
        val feet = player.location.block
        return if (feet.isPassable) feet.getRelative(BlockFace.DOWN) else feet
    }

    private fun SpawnPose.toPoseType(): PoseType? = when (this) {
        SpawnPose.LAY -> PoseType.LAY
        SpawnPose.LAY_BACK -> PoseType.LAY_BACK
        SpawnPose.BELLYFLOP -> PoseType.BELLYFLOP
        SpawnPose.SPIN -> PoseType.SPIN
        else -> null
    }
}
