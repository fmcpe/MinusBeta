package net.minusmc.minusbounce.features.module.modules.combat.velocitys.intave

import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minusmc.minusbounce.event.AttackEvent
import net.minusmc.minusbounce.event.KnockBackEvent
import net.minusmc.minusbounce.event.MoveInputEvent
import net.minusmc.minusbounce.event.PacketEvent
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode

class IntaveVelocity : VelocityMode("Intave") {
    private var blockVelocity = false

    override fun onDisable() {
        mc.thePlayer.movementInput.jump = false
    }

    override fun onUpdate() {
        blockVelocity = true

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

    override fun onAttack(event: AttackEvent) {
        if(mc.thePlayer.hurtTime > 0 && blockVelocity){
            mc.thePlayer.isSprinting = false
            mc.thePlayer.motionX *= 0.6
            mc.thePlayer.motionZ *= 0.6
            blockVelocity = false
        }
    }

    override fun onInput(event: MoveInputEvent) {
        if(mc.thePlayer.hurtTime > 0 && mc.objectMouseOver.entityHit != null){
            event.forward = 1.0F
            event.strafe = 0.0F
        }
    }

    override fun onKnockBack(event: KnockBackEvent) {
        event.full = false
        event.reduceY = true
    }
}