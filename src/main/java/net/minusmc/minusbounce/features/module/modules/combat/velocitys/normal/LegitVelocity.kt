package net.minusmc.minusbounce.features.module.modules.combat.velocitys.normal

import net.minusmc.minusbounce.event.UpdateEvent
import net.minusmc.minusbounce.features.module.modules.combat.velocitys.VelocityMode

class LegitVelocity : VelocityMode("Legit") {
    fun onUpdate(event: UpdateEvent) {
        if (mc.objectMouseOver.entityHit != null && mc.thePlayer.hurtTime == 9 && !mc.thePlayer.isBurning) {
            mc.thePlayer.movementInput.jump = true
        }
    }
}
