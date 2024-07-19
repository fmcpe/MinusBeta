package net.minusmc.minusbounce.features.module.modules.combat.velocitys.intave

import net.minusmc.minusbounce.event.KnockBackEvent
import net.minusmc.minusbounce.event.MoveInputEvent
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode

class IntaveVelocity : VelocityMode("Intave") {
    private var counter = 0

    override fun gameLoop() {
        mc.thePlayer.jumpTicks = 0
    }

    override fun onInput(event: MoveInputEvent) {
        if(mc.objectMouseOver.entityHit != null && mc.thePlayer.hurtTime == 9 && !mc.thePlayer.isBurning && counter++ % 2 == 0){
            event.jump = true
        }

        if(mc.thePlayer.hurtTime > 0 && mc.objectMouseOver.entityHit != null){
            event.forward = 1.0F
        }
    }

    override fun onKnockBack(event: KnockBackEvent) {
        event.full = false
        event.reduceY = true
    }
}