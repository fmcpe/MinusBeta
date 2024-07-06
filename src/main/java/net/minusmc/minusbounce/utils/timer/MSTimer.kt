/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.utils.timer

class MSTimer {
    var time = -1L

    fun hasTimeElapsed(delay: Double, reset: Boolean): Boolean {
        if ((System.currentTimeMillis() - this.time).toDouble() > delay) {
            if (reset) {
                this.reset()
            }

            return true
        } else {
            return false
        }
    }

    fun hasTimePassed(delay: Int): Boolean {
        return hasTimePassed(delay.toLong())
    }

    fun hasTimePassed(delay: Long): Boolean {
        return System.currentTimeMillis() >= time + delay
    }

    fun hasTimeLeft(delay: Long): Long {
        return delay + time - System.currentTimeMillis()
    }

    fun reset() {
        time = System.currentTimeMillis()
    }
}
