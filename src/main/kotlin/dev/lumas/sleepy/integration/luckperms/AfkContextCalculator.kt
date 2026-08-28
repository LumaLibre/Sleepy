package dev.lumas.sleepy.integration.luckperms

import dev.lumas.sleepy.Sleepy
import dev.lumas.sleepy.model.AfkCause
import net.luckperms.api.context.ContextCalculator
import net.luckperms.api.context.ContextConsumer
import net.luckperms.api.context.ContextSet
import net.luckperms.api.context.ImmutableContextSet
import org.bukkit.entity.Player

class AfkContextCalculator : ContextCalculator<Player> {

    override fun calculate(target: Player, consumer: ContextConsumer) {
        val activity = Sleepy.activity.activity(target)
        consumer.accept(AFK_KEY, (activity?.isAfk == true).toString())
        consumer.accept(CAUSE_KEY, (activity?.cause ?: AfkCause.NONE).contextValue)
        consumer.accept(REGION_KEY, (activity?.isInAfkRegion == true).toString())
    }

    override fun estimatePotentialContexts(): ContextSet {
        val builder = ImmutableContextSet.builder()
            .add(AFK_KEY, "true")
            .add(AFK_KEY, "false")
            .add(REGION_KEY, "true")
            .add(REGION_KEY, "false")
        AfkCause.entries.forEach { builder.add(CAUSE_KEY, it.contextValue) }
        return builder.build()
    }

    companion object {
        const val AFK_KEY = "sleepy:afk"
        const val CAUSE_KEY = "sleepy:afk-cause"
        const val REGION_KEY = "sleepy:afk-region"
    }
}

private val AfkCause.contextValue: String
    get() = name.lowercase()
