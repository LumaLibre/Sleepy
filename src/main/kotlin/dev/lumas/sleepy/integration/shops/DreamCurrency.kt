package dev.lumas.sleepy.integration.shops

import dev.lumas.shops.api.currency.CurrencyType
import dev.lumas.shops.interfaces.Currency
import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.config.TranslatorService
import dev.lumas.sleepy.model.Dreams
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import java.util.Locale

data class DreamCurrency(val amount: Long) : Currency<Long> {
    override fun type(): CurrencyType<*> = ShopsIntegration.currencyType

    override fun get(): Any = amount

    override fun price(): Long = amount

    override fun getBalance(player: Player): Long =
        Sleepy.activity.activity(player)?.points ?: 0

    override fun hasEnough(player: Player, multiplier: Int): Boolean =
        requiredAmount(multiplier)?.let { getBalance(player) >= it } ?: false

    override fun withdraw(player: Player, multiplier: Int): Boolean =
        requiredAmount(multiplier)?.let { Sleepy.activity.withdrawPoints(player, it) } ?: false

    override fun readablePrice(multiplier: Int): Component =
        GlobalTranslator.render(
            Dreams.display(requiredAmount(multiplier) ?: Long.MAX_VALUE),
            TranslatorService.instance.defaultLocale
        )

    private fun requiredAmount(multiplier: Int): Long? {
        if (amount < 1 || multiplier < 1) return null
        return try {
            Math.multiplyExact(amount, multiplier.toLong())
        } catch (_: ArithmeticException) {
            null
        }
    }
}
