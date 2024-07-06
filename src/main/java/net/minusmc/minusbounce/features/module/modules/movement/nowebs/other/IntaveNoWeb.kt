/*
 * MinusBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/MinusMC/MinusBounce
 */
package net.minusmc.minusbounce.features.module.modules.movement.nowebs.other

import net.minusmc.minusbounce.features.module.modules.movement.nowebs.NoWebMode
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.MovementUtils.direction
import net.minusmc.minusbounce.utils.MovementUtils.isMoving


class IntaveNoWeb: NoWebMode("Intave") {
    override fun onTick() {
        if (isMoving && mc.thePlayer.isInWeb) {
            mc.gameSettings.keyBindJump.pressed = false
            if (!mc.thePlayer.onGround) {
                mc.timer.timerSpeed = 1.0f
                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    MovementUtils.strafe(
                        0.65f,
                        Math.toDegrees(direction()).toFloat()
                    )
                } else if (mc.thePlayer.ticksExisted % 5 == 0) {
                    MovementUtils.strafe(
                        0.65f,
                        Math.toDegrees(direction()).toFloat()
                    )
                }
            } else {
                MovementUtils.strafe(
                    0.35f,
                    Math.toDegrees(direction()).toFloat()
                )
                mc.thePlayer.jump()
                mc.thePlayer.jump()
                mc.thePlayer.jump()
            }

            if (!mc.thePlayer.isSprinting) {
                mc.thePlayer.motionX *= 0.75
                mc.thePlayer.motionZ *= 0.75
            }
        }
    }
}
