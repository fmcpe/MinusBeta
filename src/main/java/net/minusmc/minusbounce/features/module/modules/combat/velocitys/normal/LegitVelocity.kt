package net.minusmc.minusbounce.features.module.modules.combat.velocitys.normal

import net.minusmc.minusbounce.event.*
import net.minusmc.minusbounce.utils.RotationUtils
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minusmc.minusbounce.features.module.modules.combat.KillAura
import net.minusmc.minusbounce.MinusBounce

class LegitVelocity : VelocityMode("LegitAura") {
    protected val killAura: KillAura
		get() = MinusBounce.moduleManager[KillAura::class.java]!!

    override fun onStrafe(event: StrafeEvent){
        if(RotationUtils.targetRotation != null && killAura.state){
            event.correction = true
        }
    }

    override fun onJump(event: JumpEvent){
        if(RotationUtils.targetRotation != null && killAura.state){
            event.correction = true
        }
    }

    override fun onInput(event: MoveInputEvent){
        if(RotationUtils.targetRotation != null && killAura.state){
            event.forward = 1f
            event.correction = false
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId){
            if(mc.thePlayer.onGround)
                mc.thePlayer.jump()
        }
    }
}
