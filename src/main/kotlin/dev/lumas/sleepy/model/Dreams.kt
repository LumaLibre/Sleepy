package dev.lumas.sleepy.model

import net.kyori.adventure.text.Component

object Dreams { // TODO: useless?
    fun display(amount: Long): Component = Component.translatable(
        if (amount == 1L) "sleepy.currency.oneiros" else "sleepy.currency.oneira",
        Component.text(amount),
    )
}
