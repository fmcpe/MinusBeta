package net.minusmc.minusbounce.features.module.modules.combat.velocitys.intave

import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minusmc.minusbounce.event.KnockBackEvent
import net.minusmc.minusbounce.event.MoveInputEvent
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode

class IntaveVelocity : VelocityMode("Intave") {
    override fun onDisable() {
        mc.thePlayer.movementInput.jump = false
    }

    override fun onUpdate() {
        mc.objectMouseOver ?: return
        if(mc.objectMouseOver.entityHit != null && mc.thePlayer.hurtTime == 9 && !mc.thePlayer.isBurning){
            mc.thePlayer.movementInput.jump = true
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if(packet is S12PacketEntityVelocity && packet.entityID == mc.thePlayer.entityId){
            if(mc.objectMouseOver.entityHit != null && mc.thePlayer.hurtTime == 9 && !mc.thePlayer.isBurning){
                mc.thePlayer.movementInput.jump = true
            }
        }
    }

    override fun onInput(event: MoveInputEvent) {
        if(mc.thePlayer.hurtTime > 0 && mc.objectMouseOver.entityHit != null){
            event.forward = 1.0F
        }
    }

    override fun onKnockBack(event: KnockBackEvent) {
        event.full = false
        event.reduceY = true
    }
}