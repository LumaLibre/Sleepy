package dev.lumas.sleepy.util

object Durations {
    data class Parts(
        val days: Long,
        val hours: Long,
        val minutes: Long,
        val seconds: Long,
    )

    fun parts(totalSeconds: Long): Parts = Parts(
        days = totalSeconds / 86_400,
        hours = totalSeconds % 86_400 / 3_600,
        minutes = totalSeconds % 3_600 / 60,
        seconds = totalSeconds % 60,
    )

    fun format(totalSeconds: Long): String {
        val (days, hours, minutes, seconds) = parts(totalSeconds)
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m ${seconds}s"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
