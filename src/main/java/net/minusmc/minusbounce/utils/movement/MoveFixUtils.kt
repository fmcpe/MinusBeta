package net.minusmc.minusbounce.utils.movement

import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.utils.MinecraftInstance
import net.minusmc.minusbounce.utils.RotationUtils.targetRotation
import kotlin.math.*
object MoveFixUtils : MinecraftInstance(), Listenable {
    override fun handleEvents() = true
    var type: MovementFixType = MovementFixType.NONE

    @EventTarget
    fun onStrafe(event: StrafeEvent){
        if(type == MovementFixType.NORMAL || type == MovementFixType.FULL) {
            targetRotation?.let { event.yaw = it.yaw }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent){
        if(type == MovementFixType.NORMAL || type == MovementFixType.FULL) {
            targetRotation?.let { event.yaw = it.yaw }
        }
    }

    @EventTarget
    fun onInput(event: MoveInputEvent){
        val forward = event.forward
        val strafe = event.strafe

        targetRotation?.let{
            if(type == MovementFixType.FULL) {
                val offset = mc.thePlayer.rotationYaw - it.yaw

                event.forward = round(forward * cos(offset) + strafe * sin(offset))
                event.strafe = round(strafe * cos(offset) - forward * sin(offset))
            }
        }
    }
}