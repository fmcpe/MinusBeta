package net.minusmc.minusbounce.features.module.modules.movement.nowebs.other

import net.minusmc.minusbounce.features.module.modules.movement.nowebs.NoWebMode
import net.minusmc.minusbounce.utils.MovementUtils
import net.minusmc.minusbounce.utils.RotationUtils

class IntaveNoWeb: NoWebMode("Intave") {
    override fun onUpdate() {
        if(MovementUtils.isMoving && mc.thePlayer.isInWeb){
            mc.gameSettings.keyBindJump.pressed = false
            if(!mc.thePlayer.onGround){
                mc.timer.timerSpeed = 1.0F
                if(mc.thePlayer.ticksExisted % 2 == 0 || mc.thePlayer.ticksExisted % 5 == 0){
                    MovementUtils.strafe(0.65F, RotationUtils.targetRotation?.yaw ?: mc.thePlayer.rotationYaw, mc.thePlayer.moveForward, mc.thePlayer.moveStrafing)
                } else {
                    MovementUtils.strafe(0.35F, RotationUtils.targetRotation?.yaw ?: mc.thePlayer.rotationYaw, mc.thePlayer.moveForward, mc.thePlayer.moveStrafing)
                    for(i in 1.rangeTo(3)){
                        mc.thePlayer.jump()
                    }
                }

                if(!mc.thePlayer.isSprinting){
                    mc.thePlayer.motionX *= 0.75
                    mc.thePlayer.motionZ *= 0.75
                }
            }
        }
    }
}
