package dev.lumas.sleepy.model

import kotlin.random.Random

object PointBooster {
    const val PERMISSION_PREFIX = "sleepy.booster."

    /** The highest granted `sleepy.booster.<n>` level, or 1 when none is granted. */
    fun level(grantedPermissions: Iterable<String>): Int =
        grantedPermissions
            .filter { it.startsWith(PERMISSION_PREFIX, ignoreCase = true) }
            .mapNotNull { it.substring(PERMISSION_PREFIX.length).toIntOrNull() }
            .filter { it > 1 }
            .maxOrNull() ?: 1

    /** Rolls a reward between [amount] and [amount] × [level], inclusive. */
    fun roll(amount: Long, level: Int, random: Random = Random.Default): Long {
        val multiplier = random.nextInt(1, level.coerceAtLeast(1) + 1).toLong()
        return if (amount > Long.MAX_VALUE / multiplier) Long.MAX_VALUE else amount * multiplier
    }
}
