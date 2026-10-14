package dev.lumas.sleepy.api.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SleepySleepEvent(
    val player: Player,
    private var cancelled: Boolean = false,
    private val teleport: Boolean = false
) :
    Event(),
    Cancellable {

    companion object {
        private val handlerList: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList;
        }
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    override fun isCancelled() = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    fun isTeleport() = teleport
}