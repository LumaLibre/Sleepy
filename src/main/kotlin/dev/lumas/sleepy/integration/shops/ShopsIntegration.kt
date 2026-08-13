package dev.lumas.sleepy.integration.shops

import dev.lumas.shops.api.ShopsAPI
import dev.lumas.shops.api.currency.CurrencyType
import net.kyori.adventure.key.Key

object ShopsIntegration {
    val key: Key = Key.key("sleepy", "oneira")

    lateinit var currencyType: CurrencyType<Long>
        private set

    fun register() {
        val registry = ShopsAPI.getInstance().currencies()
        registry.unregister(key)
        currencyType = CurrencyType.builder(key, Long::class.javaObjectType)
            .factory { amount -> DreamCurrency(amount?.coerceAtLeast(0) ?: 0) }
            .translation("sleepy.currency.display")
            .editor(DreamCurrencyEditor())
            .build()
        ShopsAPI.getInstance().registerCurrency(currencyType)
    }

    fun unregister() {
        ShopsAPI.getInstance().currencies().unregister(key)
    }
}
